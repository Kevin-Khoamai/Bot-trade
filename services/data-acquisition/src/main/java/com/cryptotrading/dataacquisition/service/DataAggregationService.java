package com.cryptotrading.dataacquisition.service;

import com.cryptotrading.dataacquisition.dto.MarketDataDto;
import com.cryptotrading.dataacquisition.model.CryptoPrice;
import com.cryptotrading.dataacquisition.repository.CryptoPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for aggregating and validating market data from multiple sources
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataAggregationService {

    private final CryptoPriceRepository cryptoPriceRepository;
    private final KafkaProducerService kafkaProducerService;
    private final RedisCacheService redisCacheService;

    // Temporary storage for aggregating data from multiple exchanges
    private final Map<String, List<MarketDataDto>> pendingAggregation = new ConcurrentHashMap<>();
    
    // Price validation thresholds
    private static final BigDecimal MAX_PRICE_DEVIATION = new BigDecimal("0.05"); // 5%
    private static final BigDecimal MIN_VOLUME = new BigDecimal("0.001");

    /**
     * Consume market data from Binance topic
     */
    @KafkaListener(topics = "${kafka.topics.binance-trades}", groupId = "aggregation-group")
    public void consumeBinanceData(MarketDataDto marketData) {
        log.debug("Received Binance data: {}:{}", marketData.getSymbol(), marketData.getClosePrice());
        processMarketDataForAggregation(marketData);
    }

    /**
     * Consume market data from Coinbase topic
     */
    @KafkaListener(topics = "${kafka.topics.coinbase-trades}", groupId = "aggregation-group")
    public void consumeCoinbaseData(MarketDataDto marketData) {
        log.debug("Received Coinbase data: {}:{}", marketData.getSymbol(), marketData.getClosePrice());
        processMarketDataForAggregation(marketData);
    }

    /**
     * Process market data for aggregation
     */
    private void processMarketDataForAggregation(MarketDataDto marketData) {
        if (!isValidMarketData(marketData)) {
            log.warn("Invalid market data received: {}", marketData);
            return;
        }

        String symbol = normalizeSymbol(marketData.getSymbol());
        String aggregationKey = generateAggregationKey(symbol, marketData.getTimestamp());
        
        // Add to pending aggregation
        pendingAggregation.computeIfAbsent(aggregationKey, k -> new ArrayList<>()).add(marketData);
        
        // Check if we have data from multiple exchanges for aggregation
        List<MarketDataDto> dataList = pendingAggregation.get(aggregationKey);
        if (shouldAggregate(dataList)) {
            aggregateAndPublish(symbol, dataList);
            pendingAggregation.remove(aggregationKey);
        }
    }

    /**
     * Validate market data
     */
    private boolean isValidMarketData(MarketDataDto marketData) {
        if (marketData == null || marketData.getSymbol() == null || 
            marketData.getClosePrice() == null || marketData.getVolume() == null) {
            return false;
        }

        // Check for reasonable price values
        BigDecimal price = marketData.getClosePrice();
        if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(new BigDecimal("1000000")) > 0) {
            log.warn("Price out of reasonable range: {}", price);
            return false;
        }

        // Check for minimum volume
        if (marketData.getVolume().compareTo(MIN_VOLUME) < 0) {
            log.warn("Volume too low: {}", marketData.getVolume());
            return false;
        }

        // Validate OHLC relationships
        if (!isValidOHLC(marketData)) {
            log.warn("Invalid OHLC data: {}", marketData);
            return false;
        }

        return true;
    }

    /**
     * Validate OHLC relationships
     */
    private boolean isValidOHLC(MarketDataDto marketData) {
        BigDecimal open = marketData.getOpenPrice();
        BigDecimal high = marketData.getHighPrice();
        BigDecimal low = marketData.getLowPrice();
        BigDecimal close = marketData.getClosePrice();

        if (open == null || high == null || low == null || close == null) {
            return false;
        }

        // High should be >= all other prices
        if (high.compareTo(open) < 0 || high.compareTo(low) < 0 || high.compareTo(close) < 0) {
            return false;
        }

        // Low should be <= all other prices
        if (low.compareTo(open) > 0 || low.compareTo(high) > 0 || low.compareTo(close) > 0) {
            return false;
        }

        return true;
    }

    /**
     * Check if data should be aggregated
     */
    private boolean shouldAggregate(List<MarketDataDto> dataList) {
        // Aggregate if we have data from multiple exchanges or after timeout
        Set<String> exchanges = new HashSet<>();
        for (MarketDataDto data : dataList) {
            exchanges.add(data.getExchange());
        }
        
        return exchanges.size() > 1 || dataList.size() >= 2;
    }

    /**
     * Aggregate data from multiple sources and publish
     */
    private void aggregateAndPublish(String symbol, List<MarketDataDto> dataList) {
        try {
            MarketDataDto aggregated = aggregateMarketData(symbol, dataList);
            
            if (aggregated != null) {
                // Validate aggregated data
                if (validateAggregatedData(aggregated, dataList)) {
                    // Store in database
                    CryptoPrice cryptoPrice = convertToEntity(aggregated);
                    saveCryptoPriceIfNotExists(cryptoPrice);
                    
                    // Cache aggregated data
                    redisCacheService.cacheMarketData(aggregated);
                    
                    // Publish aggregated data
                    kafkaProducerService.sendAggregatedData(aggregated);
                    
                    log.info("Aggregated and published data for symbol: {}", symbol);
                } else {
                    log.warn("Aggregated data failed validation for symbol: {}", symbol);
                }
            }
        } catch (Exception e) {
            log.error("Error aggregating data for symbol {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Aggregate market data from multiple sources
     */
    private MarketDataDto aggregateMarketData(String symbol, List<MarketDataDto> dataList) {
        if (dataList.isEmpty()) {
            return null;
        }

        // Use the most recent timestamp
        LocalDateTime latestTimestamp = dataList.stream()
                .map(MarketDataDto::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        // Calculate weighted average prices based on volume
        BigDecimal totalVolume = dataList.stream()
                .map(MarketDataDto::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal weightedOpen = calculateWeightedAverage(dataList, MarketDataDto::getOpenPrice, totalVolume);
        BigDecimal weightedHigh = dataList.stream()
                .map(MarketDataDto::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal weightedLow = dataList.stream()
                .map(MarketDataDto::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal weightedClose = calculateWeightedAverage(dataList, MarketDataDto::getClosePrice, totalVolume);

        return MarketDataDto.builder()
                .exchange("AGGREGATED")
                .symbol(symbol)
                .timestamp(latestTimestamp)
                .openPrice(weightedOpen)
                .highPrice(weightedHigh)
                .lowPrice(weightedLow)
                .closePrice(weightedClose)
                .volume(totalVolume)
                .dataType("AGGREGATED")
                .source("MULTI_EXCHANGE")
                .build();
    }

    /**
     * Calculate weighted average price
     */
    private BigDecimal calculateWeightedAverage(List<MarketDataDto> dataList, 
                                              java.util.function.Function<MarketDataDto, BigDecimal> priceExtractor,
                                              BigDecimal totalVolume) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        
        for (MarketDataDto data : dataList) {
            BigDecimal price = priceExtractor.apply(data);
            BigDecimal volume = data.getVolume();
            weightedSum = weightedSum.add(price.multiply(volume));
        }
        
        return weightedSum.divide(totalVolume, 8, RoundingMode.HALF_UP);
    }

    /**
     * Validate aggregated data against source data
     */
    private boolean validateAggregatedData(MarketDataDto aggregated, List<MarketDataDto> sourceData) {
        // Check if aggregated price is within reasonable deviation from source prices
        BigDecimal aggregatedPrice = aggregated.getClosePrice();
        
        for (MarketDataDto source : sourceData) {
            BigDecimal sourcePrice = source.getClosePrice();
            BigDecimal deviation = aggregatedPrice.subtract(sourcePrice)
                    .abs()
                    .divide(sourcePrice, 4, RoundingMode.HALF_UP);
            
            if (deviation.compareTo(MAX_PRICE_DEVIATION) > 0) {
                log.warn("Price deviation too high: {} vs {} ({}%)", 
                        aggregatedPrice, sourcePrice, deviation.multiply(BigDecimal.valueOf(100)));
                return false;
            }
        }
        
        return true;
    }

    /**
     * Convert MarketDataDto to CryptoPrice entity
     */
    private CryptoPrice convertToEntity(MarketDataDto marketData) {
        return CryptoPrice.fromMarketData(
                marketData.getExchange(),
                marketData.getSymbol(),
                marketData.getTimestamp(),
                marketData.getOpenPrice(),
                marketData.getHighPrice(),
                marketData.getLowPrice(),
                marketData.getClosePrice(),
                marketData.getVolume()
        );
    }

    /**
     * Save crypto price if it doesn't exist
     */
    private void saveCryptoPriceIfNotExists(CryptoPrice cryptoPrice) {
        Optional<CryptoPrice> existing = cryptoPriceRepository
                .findByExchangeAndSymbolAndTimestamp(
                        cryptoPrice.getExchange(),
                        cryptoPrice.getSymbol(),
                        cryptoPrice.getTimestamp()
                );
        
        if (existing.isEmpty()) {
            cryptoPriceRepository.save(cryptoPrice);
        }
    }

    /**
     * Normalize symbol format
     */
    private String normalizeSymbol(String symbol) {
        // Convert Coinbase format (BTC-USD) to Binance format (BTCUSDT)
        if (symbol.contains("-")) {
            return symbol.replace("-", "").replace("USD", "USDT");
        }
        return symbol.toUpperCase();
    }

    /**
     * Generate aggregation key for grouping data
     */
    private String generateAggregationKey(String symbol, LocalDateTime timestamp) {
        // Group by symbol and minute
        return String.format("%s_%s", symbol, 
                timestamp.withSecond(0).withNano(0).toString());
    }
}

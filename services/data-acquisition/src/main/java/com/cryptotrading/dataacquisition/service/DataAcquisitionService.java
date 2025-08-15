package com.cryptotrading.dataacquisition.service;

import com.cryptotrading.dataacquisition.client.BinanceRestClient;
import com.cryptotrading.dataacquisition.client.CoinbaseRestClient;
import com.cryptotrading.dataacquisition.dto.MarketDataDto;
import com.cryptotrading.dataacquisition.model.CryptoPrice;
import com.cryptotrading.dataacquisition.repository.CryptoPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Main service for data acquisition from multiple exchanges
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataAcquisitionService {

    private final BinanceRestClient binanceRestClient;
    private final CoinbaseRestClient coinbaseRestClient;
    private final CryptoPriceRepository cryptoPriceRepository;
    private final KafkaProducerService kafkaProducerService;
    private final RedisCacheService redisCacheService;

    @Value("${data-collection.symbols:BTCUSDT,ETHUSDT,ADAUSDT}")
    private String symbolsString;

    @Value("${data-collection.intervals:1m,5m,15m,1h}")
    private String intervalsString;

    private List<String> getSymbols() {
        return Arrays.asList(symbolsString.split(","));
    }

    private List<String> getIntervals() {
        return Arrays.asList(intervalsString.split(","));
    }

    /**
     * Scheduled task to fetch market data every 5 minutes
     */
    @Scheduled(fixedDelayString = "${data-collection.fetch-interval}")
    public void fetchMarketDataScheduled() {
        log.info("Starting scheduled market data fetch for {} symbols", getSymbols().size());

        for (String symbol : getSymbols()) {
            fetchAndProcessMarketData(symbol);
        }
        
        log.info("Completed scheduled market data fetch");
    }

    /**
     * Fetch and process market data for a specific symbol
     */
    public void fetchAndProcessMarketData(String symbol) {
        log.debug("Fetching market data for symbol: {}", symbol);
        
        // Fetch from Binance
        fetchBinanceData(symbol)
                .doOnNext(this::processMarketData)
                .doOnError(error -> log.error("Error fetching Binance data for {}: {}", symbol, error.getMessage()))
                .subscribe();
        
        // Fetch from Coinbase
        String coinbaseProductId = coinbaseRestClient.convertSymbolToProductId(symbol);
        fetchCoinbaseData(coinbaseProductId)
                .doOnNext(this::processMarketData)
                .doOnError(error -> log.error("Error fetching Coinbase data for {}: {}", symbol, error.getMessage()))
                .subscribe();
    }

    /**
     * Fetch data from Binance for multiple intervals
     */
    private Flux<MarketDataDto> fetchBinanceData(String symbol) {
        return Flux.fromIterable(getIntervals())
                .flatMap(interval -> {
                    // Check cache first
                    Optional<List<MarketDataDto>> cached = 
                        redisCacheService.getCachedMarketDataList("BINANCE", symbol, interval);
                    
                    if (cached.isPresent()) {
                        log.debug("Using cached data for Binance {}:{}", symbol, interval);
                        return Flux.fromIterable(cached.get());
                    }
                    
                    // Fetch from API
                    return binanceRestClient.getKlineData(symbol, interval, 100)
                            .collectList()
                            .doOnNext(dataList -> {
                                // Cache the fetched data
                                redisCacheService.cacheMarketDataList("BINANCE", symbol, interval, dataList);
                            })
                            .flatMapMany(Flux::fromIterable);
                })
                .onErrorResume(error -> {
                    log.warn("Error fetching Binance data for {}: {}", symbol, error.getMessage());
                    return Flux.empty();
                });
    }

    /**
     * Fetch data from Coinbase
     */
    private Flux<MarketDataDto> fetchCoinbaseData(String productId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24); // Last 24 hours
        
        return coinbaseRestClient.getCandleData(productId, 300, startTime, endTime) // 5-minute candles
                .onErrorResume(error -> {
                    log.warn("Error fetching Coinbase data for {}: {}", productId, error.getMessage());
                    return Flux.empty();
                });
    }

    /**
     * Process market data: validate, store, cache, and publish
     */
    private void processMarketData(MarketDataDto marketData) {
        try {
            // Validate data
            if (!isValidMarketData(marketData)) {
                log.warn("Invalid market data received: {}", marketData);
                return;
            }

            // Convert to entity and save to database
            CryptoPrice cryptoPrice = convertToEntity(marketData);
            if (cryptoPrice.isValid()) {
                saveCryptoPrice(cryptoPrice);
            }

            // Cache the data
            redisCacheService.cacheMarketData(marketData);

            // Publish to Kafka
            kafkaProducerService.sendMarketData(marketData);

            log.debug("Processed market data for {}:{}", marketData.getExchange(), marketData.getSymbol());
            
        } catch (Exception e) {
            log.error("Error processing market data: {}", e.getMessage());
        }
    }

    /**
     * Save crypto price to database (avoid duplicates)
     */
    private void saveCryptoPrice(CryptoPrice cryptoPrice) {
        try {
            Optional<CryptoPrice> existing = cryptoPriceRepository
                    .findByExchangeAndSymbolAndTimestamp(
                            cryptoPrice.getExchange(),
                            cryptoPrice.getSymbol(),
                            cryptoPrice.getTimestamp()
                    );
            
            if (existing.isEmpty()) {
                cryptoPriceRepository.save(cryptoPrice);
                log.debug("Saved new crypto price: {}:{}:{}", 
                         cryptoPrice.getExchange(), cryptoPrice.getSymbol(), cryptoPrice.getTimestamp());
            } else {
                log.debug("Crypto price already exists, skipping: {}:{}:{}", 
                         cryptoPrice.getExchange(), cryptoPrice.getSymbol(), cryptoPrice.getTimestamp());
            }
        } catch (Exception e) {
            log.error("Error saving crypto price: {}", e.getMessage());
        }
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
     * Validate market data
     */
    private boolean isValidMarketData(MarketDataDto marketData) {
        return marketData != null &&
               marketData.getExchange() != null &&
               marketData.getSymbol() != null &&
               marketData.getTimestamp() != null &&
               marketData.getOpenPrice() != null &&
               marketData.getHighPrice() != null &&
               marketData.getLowPrice() != null &&
               marketData.getClosePrice() != null &&
               marketData.getVolume() != null;
    }

    /**
     * Fetch current prices for all symbols
     */
    public void fetchCurrentPrices() {
        log.info("Fetching current prices for all symbols");
        
        for (String symbol : getSymbols()) {
            // Fetch current price from Binance
            binanceRestClient.getCurrentPrice(symbol)
                    .doOnNext(priceData -> {
                        redisCacheService.cacheRealtimePrice("BINANCE", symbol, priceData);
                        kafkaProducerService.sendPriceUpdate(symbol, priceData);
                    })
                    .subscribe();
            
            // Fetch current price from Coinbase
            String coinbaseProductId = coinbaseRestClient.convertSymbolToProductId(symbol);
            coinbaseRestClient.getTickerData(coinbaseProductId)
                    .doOnNext(tickerData -> {
                        redisCacheService.cacheRealtimePrice("COINBASE", coinbaseProductId, tickerData);
                        kafkaProducerService.sendPriceUpdate(coinbaseProductId, tickerData);
                    })
                    .subscribe();
        }
    }

    /**
     * Get latest price for a symbol from cache or database
     */
    public Optional<CryptoPrice> getLatestPrice(String exchange, String symbol) {
        // Try cache first
        Optional<MarketDataDto> cached = redisCacheService.getCachedMarketData(exchange, symbol);
        if (cached.isPresent()) {
            return Optional.of(convertToEntity(cached.get()));
        }
        
        // Fallback to database
        return cryptoPriceRepository.findLatestByExchangeAndSymbol(exchange, symbol);
    }

    /**
     * Manual trigger for data fetch
     */
    public void triggerDataFetch() {
        log.info("Manual trigger for data fetch");
        fetchMarketDataScheduled();
    }
}

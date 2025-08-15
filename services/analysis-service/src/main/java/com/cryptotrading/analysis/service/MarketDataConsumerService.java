package com.cryptotrading.analysis.service;

import com.cryptotrading.analysis.dto.MarketDataDto;
import com.cryptotrading.analysis.model.TechnicalIndicator;
import com.cryptotrading.analysis.repository.TechnicalIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for consuming market data from Kafka and triggering analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataConsumerService {

    private final IndicatorCalculationService indicatorCalculationService;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final AnalysisResultService analysisResultService;
    private final RedisCacheService redisCacheService;

    // Buffer to collect market data for batch processing
    private final ConcurrentMap<String, MarketDataBuffer> dataBuffers = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int BUFFER_SIZE = 100; // Number of data points to collect before analysis
    private static final long BUFFER_TIMEOUT_MS = 60000; // 1 minute timeout

    /**
     * Consume market data from Binance topic
     */
    @KafkaListener(topics = "${kafka.topics.binance-trades}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeBinanceData(@Payload MarketDataDto marketData,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.debug("Received Binance data: {} from topic: {}, partition: {}, offset: {}", 
                 marketData.getSymbol(), topic, partition, offset);
        
        processMarketData(marketData);
    }

    /**
     * Consume market data from Coinbase topic
     */
    @KafkaListener(topics = "${kafka.topics.coinbase-trades}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeCoinbaseData(@Payload MarketDataDto marketData,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.debug("Received Coinbase data: {} from topic: {}, partition: {}, offset: {}", 
                 marketData.getSymbol(), topic, partition, offset);
        
        processMarketData(marketData);
    }

    /**
     * Consume aggregated market data
     */
    @KafkaListener(topics = "${kafka.topics.aggregated-data}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAggregatedData(@Payload MarketDataDto marketData,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.debug("Received aggregated data: {} from topic: {}, partition: {}, offset: {}", 
                 marketData.getSymbol(), topic, partition, offset);
        
        processMarketData(marketData);
    }

    /**
     * Process incoming market data
     */
    private void processMarketData(MarketDataDto marketData) {
        if (!marketData.isValidForAnalysis()) {
            log.warn("Invalid market data received: {}", marketData);
            return;
        }

        String symbol = marketData.getSymbol();
        
        // Add to buffer
        MarketDataBuffer buffer = dataBuffers.computeIfAbsent(symbol, k -> new MarketDataBuffer(symbol));
        buffer.addData(marketData);

        // Check if buffer is ready for processing
        if (buffer.isReadyForProcessing()) {
            processBufferedData(buffer);
        }
    }

    /**
     * Process buffered market data for analysis
     */
    private void processBufferedData(MarketDataBuffer buffer) {
        try {
            String symbol = buffer.getSymbol();
            List<MarketDataDto> dataList = buffer.getDataAndClear();
            
            log.info("Processing {} data points for symbol: {}", dataList.size(), symbol);
            
            // Calculate technical indicators
            List<TechnicalIndicator> indicators = indicatorCalculationService.calculateAllIndicators(symbol, dataList);
            
            if (!indicators.isEmpty()) {
                // Save indicators to database
                saveIndicators(indicators);
                
                // Cache indicators in Redis
                cacheIndicators(indicators);
                
                // Publish analysis results
                analysisResultService.publishAnalysisResults(symbol, indicators);
                
                log.info("Processed {} indicators for symbol: {}", indicators.size(), symbol);
            }
            
        } catch (Exception e) {
            log.error("Error processing buffered data for symbol {}: {}", buffer.getSymbol(), e.getMessage());
        }
    }

    /**
     * Save indicators to database
     */
    private void saveIndicators(List<TechnicalIndicator> indicators) {
        try {
            // Filter out existing indicators to avoid duplicates
            List<TechnicalIndicator> newIndicators = indicators.stream()
                    .filter(indicator -> !technicalIndicatorRepository.existsBySymbolAndTypeAndTimestampAndPeriod(
                            indicator.getSymbol(),
                            indicator.getIndicatorType(),
                            indicator.getTimestamp(),
                            indicator.getPeriod()))
                    .toList();
            
            if (!newIndicators.isEmpty()) {
                technicalIndicatorRepository.saveAll(newIndicators);
                log.debug("Saved {} new indicators to database", newIndicators.size());
            }
        } catch (Exception e) {
            log.error("Error saving indicators to database: {}", e.getMessage());
        }
    }

    /**
     * Cache indicators in Redis
     */
    private void cacheIndicators(List<TechnicalIndicator> indicators) {
        try {
            for (TechnicalIndicator indicator : indicators) {
                redisCacheService.cacheIndicator(indicator);
            }
            log.debug("Cached {} indicators in Redis", indicators.size());
        } catch (Exception e) {
            log.error("Error caching indicators: {}", e.getMessage());
        }
    }

    /**
     * Get buffer status for monitoring
     */
    public ConcurrentMap<String, MarketDataBuffer> getBufferStatus() {
        return new ConcurrentHashMap<>(dataBuffers);
    }

    /**
     * Clear buffer for a symbol
     */
    public void clearBuffer(String symbol) {
        dataBuffers.remove(symbol);
        log.info("Cleared buffer for symbol: {}", symbol);
    }

    /**
     * Force process buffer for a symbol
     */
    public void forceProcessBuffer(String symbol) {
        MarketDataBuffer buffer = dataBuffers.get(symbol);
        if (buffer != null && !buffer.isEmpty()) {
            processBufferedData(buffer);
            log.info("Force processed buffer for symbol: {}", symbol);
        }
    }

    /**
     * Inner class to buffer market data
     */
    public static class MarketDataBuffer {
        private final String symbol;
        private final List<MarketDataDto> dataList;
        private final long createdTime;
        private volatile long lastUpdateTime;

        public MarketDataBuffer(String symbol) {
            this.symbol = symbol;
            this.dataList = new java.util.concurrent.CopyOnWriteArrayList<>();
            this.createdTime = System.currentTimeMillis();
            this.lastUpdateTime = createdTime;
        }

        public synchronized void addData(MarketDataDto data) {
            dataList.add(data);
            lastUpdateTime = System.currentTimeMillis();
            
            // Sort by timestamp to maintain order
            dataList.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        }

        public boolean isReadyForProcessing() {
            long currentTime = System.currentTimeMillis();
            return dataList.size() >= BUFFER_SIZE || 
                   (currentTime - lastUpdateTime) > BUFFER_TIMEOUT_MS;
        }

        public synchronized List<MarketDataDto> getDataAndClear() {
            List<MarketDataDto> result = new java.util.ArrayList<>(dataList);
            dataList.clear();
            return result;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getSize() {
            return dataList.size();
        }

        public boolean isEmpty() {
            return dataList.isEmpty();
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }

        public LocalDateTime getOldestDataTime() {
            return dataList.isEmpty() ? null : dataList.get(0).getTimestamp();
        }

        public LocalDateTime getNewestDataTime() {
            return dataList.isEmpty() ? null : dataList.get(dataList.size() - 1).getTimestamp();
        }
    }
}

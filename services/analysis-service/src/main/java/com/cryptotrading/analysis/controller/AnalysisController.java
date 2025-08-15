package com.cryptotrading.analysis.controller;

import com.cryptotrading.analysis.dto.IndicatorResultDto;
import com.cryptotrading.analysis.model.TechnicalIndicator;
import com.cryptotrading.analysis.repository.TechnicalIndicatorRepository;
import com.cryptotrading.analysis.service.AnalysisResultService;
import com.cryptotrading.analysis.service.MarketDataConsumerService;
import com.cryptotrading.analysis.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for analysis operations
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final RedisCacheService redisCacheService;
    private final MarketDataConsumerService marketDataConsumerService;
    private final AnalysisResultService analysisResultService;
    private final PredictionService predictionService;
    private final VWAPCalculationService vwapCalculationService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Analysis Service is healthy");
    }

    /**
     * Get latest indicators for a symbol
     */
    @GetMapping("/indicators/{symbol}")
    public ResponseEntity<List<IndicatorResultDto>> getLatestIndicators(@PathVariable String symbol) {
        // Try cache first
        Optional<List<IndicatorResultDto>> cached = redisCacheService.getCachedIndicatorList(symbol);
        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }

        // Fallback to database
        List<String> indicatorTypes = technicalIndicatorRepository.findDistinctIndicatorTypesBySymbol(symbol);
        List<IndicatorResultDto> indicators = indicatorTypes.stream()
                .map(type -> technicalIndicatorRepository.findLatestBySymbolAndTypeAndPeriod(symbol, type, null))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(IndicatorResultDto::fromEntity)
                .toList();

        return ResponseEntity.ok(indicators);
    }

    /**
     * Get specific indicator for a symbol
     */
    @GetMapping("/indicators/{symbol}/{indicatorType}")
    public ResponseEntity<IndicatorResultDto> getIndicator(
            @PathVariable String symbol,
            @PathVariable String indicatorType,
            @RequestParam(required = false) Integer period) {
        
        // Try cache first
        Optional<IndicatorResultDto> cached = redisCacheService.getCachedIndicator(symbol, indicatorType, period);
        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }

        // Fallback to database
        Optional<TechnicalIndicator> indicator = technicalIndicatorRepository
                .findLatestBySymbolAndTypeAndPeriod(symbol, indicatorType, period);
        
        return indicator.map(IndicatorResultDto::fromEntity)
                       .map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get historical indicators for a symbol within date range
     */
    @GetMapping("/indicators/{symbol}/{indicatorType}/history")
    public ResponseEntity<List<IndicatorResultDto>> getHistoricalIndicators(
            @PathVariable String symbol,
            @PathVariable String indicatorType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        List<TechnicalIndicator> indicators = technicalIndicatorRepository
                .findBySymbolAndTypeAndTimestampBetween(symbol, indicatorType, startTime, endTime);
        
        List<IndicatorResultDto> result = indicators.stream()
                .map(IndicatorResultDto::fromEntity)
                .toList();
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get recent indicators for a symbol
     */
    @GetMapping("/indicators/{symbol}/{indicatorType}/recent")
    public ResponseEntity<List<IndicatorResultDto>> getRecentIndicators(
            @PathVariable String symbol,
            @PathVariable String indicatorType,
            @RequestParam(required = false) Integer period,
            @RequestParam(defaultValue = "100") int limit) {
        
        List<TechnicalIndicator> indicators = technicalIndicatorRepository
                .findRecentBySymbolAndTypeAndPeriod(symbol, indicatorType, period, limit);
        
        List<IndicatorResultDto> result = indicators.stream()
                .map(IndicatorResultDto::fromEntity)
                .toList();
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get comprehensive analysis result for a symbol
     */
    @GetMapping("/result/{symbol}")
    public ResponseEntity<Object> getAnalysisResult(@PathVariable String symbol) {
        Optional<Object> cached = redisCacheService.getCachedAnalysisResult(symbol);
        return cached.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get trading signal for a symbol
     */
    @GetMapping("/signal/{symbol}")
    public ResponseEntity<RedisCacheService.SignalData> getTradingSignal(@PathVariable String symbol) {
        Optional<RedisCacheService.SignalData> signal = redisCacheService.getCachedSignal(symbol);
        return signal.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get prediction for a symbol
     */
    @GetMapping("/prediction/{symbol}")
    public ResponseEntity<Object> getPrediction(@PathVariable String symbol) {
        Optional<Object> prediction = redisCacheService.getCachedPrediction(symbol);
        return prediction.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all available symbols with indicators
     */
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getAvailableSymbols() {
        List<String> symbols = technicalIndicatorRepository.findDistinctSymbols();
        return ResponseEntity.ok(symbols);
    }

    /**
     * Get all indicator types for a symbol
     */
    @GetMapping("/indicators/{symbol}/types")
    public ResponseEntity<List<String>> getIndicatorTypes(@PathVariable String symbol) {
        List<String> types = technicalIndicatorRepository.findDistinctIndicatorTypesBySymbol(symbol);
        return ResponseEntity.ok(types);
    }

    /**
     * Get buffer status for monitoring
     */
    @GetMapping("/buffers")
    public ResponseEntity<Map<String, Object>> getBufferStatus() {
        Map<String, MarketDataConsumerService.MarketDataBuffer> buffers = 
            marketDataConsumerService.getBufferStatus();
        
        Map<String, Object> status = buffers.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> Map.of(
                        "symbol", entry.getValue().getSymbol(),
                        "size", entry.getValue().getSize(),
                        "createdTime", entry.getValue().getCreatedTime(),
                        "lastUpdateTime", entry.getValue().getLastUpdateTime(),
                        "oldestDataTime", entry.getValue().getOldestDataTime(),
                        "newestDataTime", entry.getValue().getNewestDataTime()
                    )
                ));
        
        return ResponseEntity.ok(status);
    }

    /**
     * Force process buffer for a symbol
     */
    @PostMapping("/buffers/{symbol}/process")
    public ResponseEntity<String> forceProcessBuffer(@PathVariable String symbol) {
        marketDataConsumerService.forceProcessBuffer(symbol);
        return ResponseEntity.ok("Buffer processing triggered for symbol: " + symbol);
    }

    /**
     * Clear buffer for a symbol
     */
    @DeleteMapping("/buffers/{symbol}")
    public ResponseEntity<String> clearBuffer(@PathVariable String symbol) {
        marketDataConsumerService.clearBuffer(symbol);
        return ResponseEntity.ok("Buffer cleared for symbol: " + symbol);
    }

    /**
     * Get cache statistics
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<RedisCacheService.CacheStats> getCacheStats() {
        RedisCacheService.CacheStats stats = redisCacheService.getCacheStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear cache for a symbol
     */
    @DeleteMapping("/cache/{symbol}")
    public ResponseEntity<String> clearCache(@PathVariable String symbol) {
        redisCacheService.deleteAllCachedDataForSymbol(symbol);
        return ResponseEntity.ok("Cache cleared for symbol: " + symbol);
    }

    /**
     * Get analysis statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<AnalysisStats> getAnalysisStats() {
        long totalIndicators = technicalIndicatorRepository.count();
        List<String> symbols = technicalIndicatorRepository.findDistinctSymbols();
        RedisCacheService.CacheStats cacheStats = redisCacheService.getCacheStats();
        
        AnalysisStats stats = new AnalysisStats(
            totalIndicators,
            symbols.size(),
            cacheStats.indicatorCount(),
            cacheStats.predictionCount(),
            cacheStats.signalCount()
        );
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get predictions for a symbol
     */
    @GetMapping("/predictions/{symbol}")
    public ResponseEntity<List<IndicatorResultDto>> getPredictions(@PathVariable String symbol) {
        try {
            List<TechnicalIndicator> predictions = predictionService.getCachedPredictions(symbol);

            if (predictions.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<IndicatorResultDto> predictionDtos = predictions.stream()
                    .map(IndicatorResultDto::fromEntity)
                    .toList();

            return ResponseEntity.ok(predictionDtos);

        } catch (Exception e) {
            log.error("Error retrieving predictions for symbol {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get VWAP data for a symbol
     */
    @GetMapping("/vwap/{symbol}")
    public ResponseEntity<VWAPResponse> getVWAP(@PathVariable String symbol) {
        try {
            BigDecimal vwap = vwapCalculationService.getCachedVWAP(symbol);

            if (vwap == null) {
                return ResponseEntity.notFound().build();
            }

            // Get current price for deviation calculation
            Optional<Object> cachedPrice = redisCacheService.getCachedIndicator(symbol, "CLOSE_PRICE");
            BigDecimal currentPrice = null;
            BigDecimal deviation = null;

            if (cachedPrice.isPresent()) {
                // Assuming cached price is stored as BigDecimal
                currentPrice = (BigDecimal) cachedPrice.get();
                deviation = vwapCalculationService.calculateVWAPDeviation(symbol, currentPrice);
            }

            VWAPResponse response = new VWAPResponse(vwap, currentPrice, deviation, LocalDateTime.now());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error retrieving VWAP for symbol {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get comprehensive analysis including indicators, predictions, and VWAP
     */
    @GetMapping("/comprehensive/{symbol}")
    public ResponseEntity<ComprehensiveAnalysis> getComprehensiveAnalysis(@PathVariable String symbol) {
        try {
            // Get indicators
            List<TechnicalIndicator> indicators = technicalIndicatorRepository
                    .findLatestBySymbol(symbol, PageRequest.of(0, 50));

            // Get predictions
            List<TechnicalIndicator> predictions = predictionService.getCachedPredictions(symbol);

            // Get VWAP
            BigDecimal vwap = vwapCalculationService.getCachedVWAP(symbol);

            // Convert to DTOs
            List<IndicatorResultDto> indicatorDtos = indicators.stream()
                    .map(IndicatorResultDto::fromEntity)
                    .toList();

            List<IndicatorResultDto> predictionDtos = predictions.stream()
                    .map(IndicatorResultDto::fromEntity)
                    .toList();

            ComprehensiveAnalysis analysis = new ComprehensiveAnalysis(
                symbol,
                indicatorDtos,
                predictionDtos,
                vwap,
                LocalDateTime.now()
            );

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            log.error("Error retrieving comprehensive analysis for symbol {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * VWAP response record
     */
    public record VWAPResponse(
        BigDecimal vwap,
        BigDecimal currentPrice,
        BigDecimal deviationPercent,
        LocalDateTime timestamp
    ) {}

    /**
     * Comprehensive analysis response record
     */
    public record ComprehensiveAnalysis(
        String symbol,
        List<IndicatorResultDto> indicators,
        List<IndicatorResultDto> predictions,
        BigDecimal vwap,
        LocalDateTime timestamp
    ) {}

    /**
     * Analysis statistics record
     */
    public record AnalysisStats(
        long totalIndicators,
        int symbolCount,
        int cachedIndicators,
        int cachedPredictions,
        int cachedSignals
    ) {}
}

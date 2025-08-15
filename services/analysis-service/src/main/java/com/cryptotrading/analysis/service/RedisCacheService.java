package com.cryptotrading.analysis.service;

import com.cryptotrading.analysis.dto.IndicatorResultDto;
import com.cryptotrading.analysis.model.TechnicalIndicator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching analysis results in Redis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${analysis.cache.indicator-ttl:300}")
    private long indicatorTtlSeconds;

    @Value("${analysis.cache.prediction-ttl:600}")
    private long predictionTtlSeconds;

    private static final String INDICATOR_KEY_PREFIX = "analysis:indicator:";
    private static final String PREDICTION_KEY_PREFIX = "analysis:prediction:";
    private static final String SIGNAL_KEY_PREFIX = "analysis:signal:";
    private static final String ANALYSIS_KEY_PREFIX = "analysis:result:";

    /**
     * Cache technical indicator
     */
    public void cacheIndicator(TechnicalIndicator indicator) {
        String key = generateIndicatorKey(indicator.getSymbol(), indicator.getIndicatorType(), 
                                        indicator.getPeriod());
        try {
            IndicatorResultDto dto = IndicatorResultDto.fromEntity(indicator);
            redisTemplate.opsForValue().set(key, dto, Duration.ofSeconds(indicatorTtlSeconds));
            log.debug("Cached indicator: {}", key);
        } catch (Exception e) {
            log.error("Error caching indicator {}: {}", key, e.getMessage());
        }
    }

    /**
     * Get cached indicator
     */
    public Optional<IndicatorResultDto> getCachedIndicator(String symbol, String indicatorType, Integer period) {
        String key = generateIndicatorKey(symbol, indicatorType, period);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof IndicatorResultDto) {
                return Optional.of((IndicatorResultDto) cached);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving cached indicator {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache list of indicators for a symbol
     */
    public void cacheIndicatorList(String symbol, List<IndicatorResultDto> indicators) {
        String key = generateIndicatorListKey(symbol);
        try {
            redisTemplate.opsForValue().set(key, indicators, Duration.ofSeconds(indicatorTtlSeconds));
            log.debug("Cached indicator list for symbol: {}", symbol);
        } catch (Exception e) {
            log.error("Error caching indicator list for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Get cached indicator list for a symbol
     */
    @SuppressWarnings("unchecked")
    public Optional<List<IndicatorResultDto>> getCachedIndicatorList(String symbol) {
        String key = generateIndicatorListKey(symbol);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                return Optional.of((List<IndicatorResultDto>) cached);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving cached indicator list for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache prediction result
     */
    public void cachePrediction(String symbol, Object prediction) {
        String key = generatePredictionKey(symbol);
        try {
            redisTemplate.opsForValue().set(key, prediction, Duration.ofSeconds(predictionTtlSeconds));
            log.debug("Cached prediction for symbol: {}", symbol);
        } catch (Exception e) {
            log.error("Error caching prediction for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Get cached prediction
     */
    public Optional<Object> getCachedPrediction(String symbol) {
        String key = generatePredictionKey(symbol);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.error("Error retrieving cached prediction for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache trading signal
     */
    public void cacheSignal(String symbol, String signal, String reason) {
        String key = generateSignalKey(symbol);
        try {
            SignalData signalData = new SignalData(symbol, signal, reason, System.currentTimeMillis());
            redisTemplate.opsForValue().set(key, signalData, Duration.ofMinutes(15)); // Shorter TTL for signals
            log.debug("Cached signal for symbol: {} - {}", symbol, signal);
        } catch (Exception e) {
            log.error("Error caching signal for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Get cached signal
     */
    public Optional<SignalData> getCachedSignal(String symbol) {
        String key = generateSignalKey(symbol);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof SignalData) {
                return Optional.of((SignalData) cached);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving cached signal for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache comprehensive analysis result
     */
    public void cacheAnalysisResult(String symbol, Object analysisResult) {
        String key = generateAnalysisKey(symbol);
        try {
            redisTemplate.opsForValue().set(key, analysisResult, Duration.ofSeconds(indicatorTtlSeconds));
            log.debug("Cached analysis result for symbol: {}", symbol);
        } catch (Exception e) {
            log.error("Error caching analysis result for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Get cached analysis result
     */
    public Optional<Object> getCachedAnalysisResult(String symbol) {
        String key = generateAnalysisKey(symbol);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.error("Error retrieving cached analysis result for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache indicator with custom TTL
     */
    public void cacheIndicatorWithTtl(TechnicalIndicator indicator, long ttlSeconds) {
        String key = generateIndicatorKey(indicator.getSymbol(), indicator.getIndicatorType(), 
                                        indicator.getPeriod());
        try {
            IndicatorResultDto dto = IndicatorResultDto.fromEntity(indicator);
            redisTemplate.opsForValue().set(key, dto, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached indicator with custom TTL: {} ({}s)", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Error caching indicator with TTL {}: {}", key, e.getMessage());
        }
    }

    /**
     * Delete cached data
     */
    public void deleteCachedData(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted cached data: {}", key);
        } catch (Exception e) {
            log.error("Error deleting cached data {}: {}", key, e.getMessage());
        }
    }

    /**
     * Delete all cached data for a symbol
     */
    public void deleteAllCachedDataForSymbol(String symbol) {
        try {
            String pattern = "*:" + symbol + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Deleted {} cached entries for symbol: {}", keys.size(), symbol);
            }
        } catch (Exception e) {
            log.error("Error deleting cached data for symbol {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Get all keys matching pattern
     */
    public Set<String> getKeysMatching(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Error getting keys matching pattern {}: {}", pattern, e.getMessage());
            return Set.of();
        }
    }

    /**
     * Check if key exists
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking if key exists {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Set expiration for a key
     */
    public void setExpiration(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
            log.debug("Set expiration for key: {} to {} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("Error setting expiration for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        try {
            Set<String> indicatorKeys = getKeysMatching(INDICATOR_KEY_PREFIX + "*");
            Set<String> predictionKeys = getKeysMatching(PREDICTION_KEY_PREFIX + "*");
            Set<String> signalKeys = getKeysMatching(SIGNAL_KEY_PREFIX + "*");
            Set<String> analysisKeys = getKeysMatching(ANALYSIS_KEY_PREFIX + "*");
            
            return new CacheStats(
                indicatorKeys.size(),
                predictionKeys.size(),
                signalKeys.size(),
                analysisKeys.size()
            );
        } catch (Exception e) {
            log.error("Error getting cache stats: {}", e.getMessage());
            return new CacheStats(0, 0, 0, 0);
        }
    }

    // Key generation methods

    private String generateIndicatorKey(String symbol, String indicatorType, Integer period) {
        return String.format("%s%s:%s:%s", INDICATOR_KEY_PREFIX, symbol, indicatorType, 
                           period != null ? period : "null");
    }

    private String generateIndicatorListKey(String symbol) {
        return String.format("%s%s:list", INDICATOR_KEY_PREFIX, symbol);
    }

    private String generatePredictionKey(String symbol) {
        return String.format("%s%s", PREDICTION_KEY_PREFIX, symbol);
    }

    private String generateSignalKey(String symbol) {
        return String.format("%s%s", SIGNAL_KEY_PREFIX, symbol);
    }

    private String generateAnalysisKey(String symbol) {
        return String.format("%s%s", ANALYSIS_KEY_PREFIX, symbol);
    }

    // Data classes

    public record SignalData(String symbol, String signal, String reason, long timestamp) {}

    public record CacheStats(int indicatorCount, int predictionCount, int signalCount, int analysisCount) {}
}

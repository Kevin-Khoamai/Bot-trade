package com.cryptotrading.strategy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for retrieving current market data and technical indicators
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    // Cache for market data
    private final Map<String, Map<String, Object>> marketDataCache = new ConcurrentHashMap<>();

    /**
     * Get current market data for a symbol
     */
    public Map<String, Object> getCurrentMarketData(String symbol) {
        try {
            // Try to get from cache first
            Map<String, Object> cachedData = marketDataCache.get(symbol);
            if (cachedData != null) {
                return new HashMap<>(cachedData);
            }

            // Get from Redis cache
            Map<String, Object> marketData = getMarketDataFromRedis(symbol);
            
            // If not in Redis, get from analysis service
            if (marketData.isEmpty()) {
                marketData = getMarketDataFromAnalysisService(symbol);
            }

            // Cache the result
            if (!marketData.isEmpty()) {
                marketDataCache.put(symbol, marketData);
            }

            return marketData;

        } catch (Exception e) {
            log.error("Error getting market data for symbol {}: {}", symbol, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get market data from Redis cache
     */
    private Map<String, Object> getMarketDataFromRedis(String symbol) {
        Map<String, Object> marketData = new HashMap<>();

        try {
            // Get current price
            Object currentPrice = redisTemplate.opsForValue().get("price:latest:" + symbol);
            if (currentPrice != null) {
                marketData.put("currentPrice", currentPrice);
            }

            // Get technical indicators
            Map<String, Object> indicators = getIndicatorsFromRedis(symbol);
            marketData.putAll(indicators);

            // Get volume data
            Object volume = redisTemplate.opsForValue().get("volume:latest:" + symbol);
            if (volume != null) {
                marketData.put("volume", volume);
            }

            // Get VWAP
            Object vwap = redisTemplate.opsForValue().get("vwap:latest:" + symbol);
            if (vwap != null) {
                marketData.put("vwap", vwap);
            }

            log.debug("Retrieved market data from Redis for symbol: {}", symbol);

        } catch (Exception e) {
            log.error("Error getting market data from Redis for symbol {}: {}", symbol, e.getMessage());
        }

        return marketData;
    }

    /**
     * Get technical indicators from Redis
     */
    private Map<String, Object> getIndicatorsFromRedis(String symbol) {
        Map<String, Object> indicators = new HashMap<>();

        try {
            // RSI
            Object rsi = redisTemplate.opsForValue().get("indicator:rsi:" + symbol);
            if (rsi != null) {
                indicators.put("rsi", rsi);
            }

            // MACD
            Object macd = redisTemplate.opsForValue().get("indicator:macd:" + symbol);
            if (macd != null) {
                indicators.put("macd", macd);
            }

            Object macdSignal = redisTemplate.opsForValue().get("indicator:macd_signal:" + symbol);
            if (macdSignal != null) {
                indicators.put("macdSignal", macdSignal);
            }

            // Moving Averages
            Object sma20 = redisTemplate.opsForValue().get("indicator:sma20:" + symbol);
            if (sma20 != null) {
                indicators.put("sma20", sma20);
            }

            Object ema12 = redisTemplate.opsForValue().get("indicator:ema12:" + symbol);
            if (ema12 != null) {
                indicators.put("ema12", ema12);
            }

            Object ema26 = redisTemplate.opsForValue().get("indicator:ema26:" + symbol);
            if (ema26 != null) {
                indicators.put("ema26", ema26);
            }

            // Bollinger Bands
            Object bollingerUpper = redisTemplate.opsForValue().get("indicator:bb_upper:" + symbol);
            if (bollingerUpper != null) {
                indicators.put("bollingerUpper", bollingerUpper);
            }

            Object bollingerLower = redisTemplate.opsForValue().get("indicator:bb_lower:" + symbol);
            if (bollingerLower != null) {
                indicators.put("bollingerLower", bollingerLower);
            }

            // Stochastic
            Object stochasticK = redisTemplate.opsForValue().get("indicator:stoch_k:" + symbol);
            if (stochasticK != null) {
                indicators.put("stochasticK", stochasticK);
            }

            Object stochasticD = redisTemplate.opsForValue().get("indicator:stoch_d:" + symbol);
            if (stochasticD != null) {
                indicators.put("stochasticD", stochasticD);
            }

            // ATR
            Object atr = redisTemplate.opsForValue().get("indicator:atr:" + symbol);
            if (atr != null) {
                indicators.put("atr", atr);
            }

        } catch (Exception e) {
            log.error("Error getting indicators from Redis for symbol {}: {}", symbol, e.getMessage());
        }

        return indicators;
    }

    /**
     * Get market data from analysis service via REST API
     */
    private Map<String, Object> getMarketDataFromAnalysisService(String symbol) {
        Map<String, Object> marketData = new HashMap<>();

        try {
            // Call analysis service for current indicators
            String analysisServiceUrl = "http://analysis-service/api/analysis/indicators/" + symbol;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(analysisServiceUrl, Map.class);
            
            if (response != null) {
                marketData.putAll(response);
                log.debug("Retrieved market data from analysis service for symbol: {}", symbol);
            }

        } catch (Exception e) {
            log.error("Error getting market data from analysis service for symbol {}: {}", symbol, e.getMessage());
        }

        return marketData;
    }

    /**
     * Get prediction data for a symbol
     */
    public Map<String, Object> getPredictionData(String symbol) {
        Map<String, Object> predictions = new HashMap<>();

        try {
            // Get predictions from Redis
            Object arimaPrediction = redisTemplate.opsForValue().get("prediction:arima:" + symbol);
            if (arimaPrediction != null) {
                predictions.put("arimaPrediction", arimaPrediction);
            }

            Object mlPrediction = redisTemplate.opsForValue().get("prediction:ml:" + symbol);
            if (mlPrediction != null) {
                predictions.put("mlPrediction", mlPrediction);
            }

            Object trendPrediction = redisTemplate.opsForValue().get("prediction:trend:" + symbol);
            if (trendPrediction != null) {
                predictions.put("trendPrediction", trendPrediction);
            }

            Object predictionConfidence = redisTemplate.opsForValue().get("prediction:confidence:" + symbol);
            if (predictionConfidence != null) {
                predictions.put("predictionConfidence", predictionConfidence);
            }

            // If not in Redis, try analysis service
            if (predictions.isEmpty()) {
                try {
                    String predictionServiceUrl = "http://analysis-service/api/analysis/predictions/" + symbol;
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> response = restTemplate.getForObject(predictionServiceUrl, Map.class);
                    
                    if (response != null) {
                        predictions.putAll(response);
                    }
                } catch (Exception e) {
                    log.warn("Could not get predictions from analysis service for {}: {}", symbol, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error getting prediction data for symbol {}: {}", symbol, e.getMessage());
        }

        return predictions;
    }

    /**
     * Get current price for a symbol
     */
    public BigDecimal getCurrentPrice(String symbol) {
        try {
            Object price = redisTemplate.opsForValue().get("price:latest:" + symbol);
            if (price instanceof BigDecimal) {
                return (BigDecimal) price;
            } else if (price instanceof Number) {
                return BigDecimal.valueOf(((Number) price).doubleValue());
            } else if (price != null) {
                return new BigDecimal(price.toString());
            }
        } catch (Exception e) {
            log.error("Error getting current price for symbol {}: {}", symbol, e.getMessage());
        }
        return null;
    }

    /**
     * Cache market data
     */
    public void cacheMarketData(String symbol, Map<String, Object> marketData) {
        try {
            marketDataCache.put(symbol, new HashMap<>(marketData));
            
            // Also cache in Redis with TTL
            for (Map.Entry<String, Object> entry : marketData.entrySet()) {
                String key = "market:" + symbol + ":" + entry.getKey();
                redisTemplate.opsForValue().set(key, entry.getValue(), Duration.ofMinutes(5));
            }
            
            log.debug("Cached market data for symbol: {}", symbol);
            
        } catch (Exception e) {
            log.error("Error caching market data for symbol {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Clear cache for a symbol
     */
    public void clearCache(String symbol) {
        marketDataCache.remove(symbol);
        log.debug("Cleared market data cache for symbol: {}", symbol);
    }

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        marketDataCache.clear();
        log.info("Cleared all market data caches");
    }

    /**
     * Get market data age (how old is the cached data)
     */
    public long getMarketDataAge(String symbol) {
        try {
            Long timestamp = redisTemplate.opsForValue().getOperations()
                    .getExpire("market:" + symbol + ":currentPrice");
            return timestamp != null ? timestamp : -1;
        } catch (Exception e) {
            log.error("Error getting market data age for symbol {}: {}", symbol, e.getMessage());
            return -1;
        }
    }

    /**
     * Check if market data is fresh (less than 1 minute old)
     */
    public boolean isMarketDataFresh(String symbol) {
        long age = getMarketDataAge(symbol);
        return age > 0 && age < 60; // Less than 60 seconds
    }
}

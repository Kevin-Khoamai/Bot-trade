package com.cryptotrading.dataacquisition.service;

import com.cryptotrading.dataacquisition.dto.MarketDataDto;
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
 * Service for caching market data in Redis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${data-collection.cache-ttl:30}")
    private long cacheTtlSeconds;

    private static final String PRICE_KEY_PREFIX = "price:";
    private static final String TICKER_KEY_PREFIX = "ticker:";
    private static final String REALTIME_KEY_PREFIX = "realtime:";
    private static final String STATS_KEY_PREFIX = "stats:";

    /**
     * Cache market data with TTL
     */
    public void cacheMarketData(MarketDataDto marketData) {
        String key = generatePriceKey(marketData.getExchange(), marketData.getSymbol());
        try {
            redisTemplate.opsForValue().set(key, marketData, Duration.ofSeconds(cacheTtlSeconds));
            log.debug("Cached market data for key: {}", key);
        } catch (Exception e) {
            log.error("Error caching market data for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Get cached market data
     */
    public Optional<MarketDataDto> getCachedMarketData(String exchange, String symbol) {
        String key = generatePriceKey(exchange, symbol);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof MarketDataDto) {
                return Optional.of((MarketDataDto) cached);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving cached market data for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache real-time price data
     */
    public void cacheRealtimePrice(String exchange, String symbol, Object priceData) {
        String key = generateRealtimeKey(exchange, symbol);
        try {
            redisTemplate.opsForValue().set(key, priceData, Duration.ofSeconds(10)); // Shorter TTL for real-time data
            log.debug("Cached real-time price for key: {}", key);
        } catch (Exception e) {
            log.error("Error caching real-time price for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Get cached real-time price
     */
    public Optional<Object> getCachedRealtimePrice(String exchange, String symbol) {
        String key = generateRealtimeKey(exchange, symbol);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.error("Error retrieving cached real-time price for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache ticker data
     */
    public void cacheTickerData(String exchange, String symbol, Object tickerData) {
        String key = generateTickerKey(exchange, symbol);
        try {
            redisTemplate.opsForValue().set(key, tickerData, Duration.ofSeconds(cacheTtlSeconds));
            log.debug("Cached ticker data for key: {}", key);
        } catch (Exception e) {
            log.error("Error caching ticker data for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Get cached ticker data
     */
    public Optional<Object> getCachedTickerData(String exchange, String symbol) {
        String key = generateTickerKey(exchange, symbol);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.error("Error retrieving cached ticker data for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache statistics data
     */
    public void cacheStatsData(String exchange, String symbol, Object statsData) {
        String key = generateStatsKey(exchange, symbol);
        try {
            redisTemplate.opsForValue().set(key, statsData, Duration.ofMinutes(5)); // Longer TTL for stats
            log.debug("Cached stats data for key: {}", key);
        } catch (Exception e) {
            log.error("Error caching stats data for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Get cached statistics data
     */
    public Optional<Object> getCachedStatsData(String exchange, String symbol) {
        String key = generateStatsKey(exchange, symbol);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.error("Error retrieving cached stats data for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache list of market data
     */
    public void cacheMarketDataList(String exchange, String symbol, String interval, List<MarketDataDto> dataList) {
        String key = String.format("%s%s:%s:%s", PRICE_KEY_PREFIX, exchange, symbol, interval);
        try {
            redisTemplate.opsForValue().set(key, dataList, Duration.ofMinutes(10));
            log.debug("Cached market data list for key: {}", key);
        } catch (Exception e) {
            log.error("Error caching market data list for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Get cached list of market data
     */
    @SuppressWarnings("unchecked")
    public Optional<List<MarketDataDto>> getCachedMarketDataList(String exchange, String symbol, String interval) {
        String key = String.format("%s%s:%s:%s", PRICE_KEY_PREFIX, exchange, symbol, interval);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                return Optional.of((List<MarketDataDto>) cached);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving cached market data list for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Delete cached data
     */
    public void deleteCachedData(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted cached data for key: {}", key);
        } catch (Exception e) {
            log.error("Error deleting cached data for key {}: {}", key, e.getMessage());
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

    // Key generation methods

    private String generatePriceKey(String exchange, String symbol) {
        return String.format("%s%s:%s", PRICE_KEY_PREFIX, exchange, symbol);
    }

    private String generateTickerKey(String exchange, String symbol) {
        return String.format("%s%s:%s", TICKER_KEY_PREFIX, exchange, symbol);
    }

    private String generateRealtimeKey(String exchange, String symbol) {
        return String.format("%s%s:%s", REALTIME_KEY_PREFIX, exchange, symbol);
    }

    private String generateStatsKey(String exchange, String symbol) {
        return String.format("%s%s:%s", STATS_KEY_PREFIX, exchange, symbol);
    }
}

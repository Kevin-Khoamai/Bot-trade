package com.cryptotrading.apigateway.service;

import com.cryptotrading.apigateway.model.TickerData;
import com.cryptotrading.apigateway.model.TradeData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealTimeDataService {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeDataService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory cache for ultra-fast access
    private final Map<String, TickerData> currentPricesCache = new ConcurrentHashMap<>();
    private final Map<String, List<TradeData>> recentTradesCache = new ConcurrentHashMap<>();

    // Redis keys
    private static final String CURRENT_PRICE_KEY = "realtime:current:";
    private static final String RECENT_TRADES_KEY = "realtime:trades:";
    private static final int MAX_RECENT_TRADES = 100;

    /**
     * Update current price in cache and Redis
     */
    public void updateCurrentPrice(TickerData tickerData) {
        try {
            String symbol = tickerData.getProductId();
            
            // Update in-memory cache
            currentPricesCache.put(symbol, tickerData);
            
            // Update Redis cache with expiration
            String redisKey = CURRENT_PRICE_KEY + symbol;
            redisTemplate.opsForValue().set(redisKey, tickerData, Duration.ofMinutes(5));
            
            logger.debug("Updated current price for {}: {}", symbol, tickerData.getPrice());
            
        } catch (Exception e) {
            logger.error("Error updating current price for {}", tickerData.getProductId(), e);
        }
    }

    /**
     * Get current price from cache
     */
    public TickerData getCurrentPrice(String symbol) {
        try {
            // Try in-memory cache first
            TickerData cached = currentPricesCache.get(symbol);
            if (cached != null) {
                return cached;
            }
            
            // Try Redis cache
            String redisKey = CURRENT_PRICE_KEY + symbol;
            Object redisValue = redisTemplate.opsForValue().get(redisKey);
            if (redisValue instanceof TickerData) {
                TickerData tickerData = (TickerData) redisValue;
                currentPricesCache.put(symbol, tickerData);
                return tickerData;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error getting current price for {}", symbol, e);
            return null;
        }
    }

    /**
     * Get all current prices
     */
    public Map<String, TickerData> getAllCurrentPrices() {
        return new HashMap<>(currentPricesCache);
    }

    /**
     * Update recent trades
     */
    public void updateRecentTrade(TradeData tradeData) {
        try {
            String symbol = tradeData.getProductId();
            
            // Update in-memory cache
            recentTradesCache.computeIfAbsent(symbol, k -> new ArrayList<>());
            List<TradeData> trades = recentTradesCache.get(symbol);
            
            // Add new trade and maintain size limit
            trades.add(0, tradeData); // Add to beginning
            if (trades.size() > MAX_RECENT_TRADES) {
                trades.subList(MAX_RECENT_TRADES, trades.size()).clear();
            }
            
            // Update Redis cache
            String redisKey = RECENT_TRADES_KEY + symbol;
            redisTemplate.opsForList().leftPush(redisKey, tradeData);
            redisTemplate.opsForList().trim(redisKey, 0, MAX_RECENT_TRADES - 1);
            redisTemplate.expire(redisKey, Duration.ofHours(1));
            
            logger.debug("Updated recent trades for {}: {} trades", symbol, trades.size());
            
        } catch (Exception e) {
            logger.error("Error updating recent trade for {}", tradeData.getProductId(), e);
        }
    }

    /**
     * Get recent trades for a symbol
     */
    public List<TradeData> getRecentTrades(String symbol, int limit) {
        try {
            // Try in-memory cache first
            List<TradeData> cached = recentTradesCache.get(symbol);
            if (cached != null && !cached.isEmpty()) {
                return cached.subList(0, Math.min(limit, cached.size()));
            }
            
            // Try Redis cache
            String redisKey = RECENT_TRADES_KEY + symbol;
            List<Object> redisValues = redisTemplate.opsForList().range(redisKey, 0, limit - 1);
            
            if (redisValues != null && !redisValues.isEmpty()) {
                List<TradeData> trades = new ArrayList<>();
                for (Object value : redisValues) {
                    if (value instanceof TradeData) {
                        trades.add((TradeData) value);
                    }
                }
                
                // Update in-memory cache
                recentTradesCache.put(symbol, new ArrayList<>(trades));
                return trades;
            }
            
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.error("Error getting recent trades for {}", symbol, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get market summary
     */
    public Map<String, Object> getMarketSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            Map<String, TickerData> allPrices = getAllCurrentPrices();
            
            summary.put("totalSymbols", allPrices.size());
            summary.put("lastUpdate", System.currentTimeMillis());
            
            // Calculate some basic statistics
            if (!allPrices.isEmpty()) {
                double totalVolume = allPrices.values().stream()
                    .filter(t -> t.getVolume24h() != null)
                    .mapToDouble(t -> t.getVolume24h().doubleValue())
                    .sum();
                
                summary.put("totalVolume24h", totalVolume);
            }
            
            summary.put("symbols", allPrices.keySet());
            
        } catch (Exception e) {
            logger.error("Error generating market summary", e);
        }
        
        return summary;
    }

    /**
     * Clear cache for a symbol
     */
    public void clearCache(String symbol) {
        currentPricesCache.remove(symbol);
        recentTradesCache.remove(symbol);
        
        // Clear Redis cache
        redisTemplate.delete(CURRENT_PRICE_KEY + symbol);
        redisTemplate.delete(RECENT_TRADES_KEY + symbol);
    }

    /**
     * Clear all cache
     */
    public void clearAllCache() {
        currentPricesCache.clear();
        recentTradesCache.clear();
        
        // Clear Redis cache (be careful with this in production)
        Set<String> priceKeys = redisTemplate.keys(CURRENT_PRICE_KEY + "*");
        Set<String> tradeKeys = redisTemplate.keys(RECENT_TRADES_KEY + "*");
        
        if (priceKeys != null && !priceKeys.isEmpty()) {
            redisTemplate.delete(priceKeys);
        }
        if (tradeKeys != null && !tradeKeys.isEmpty()) {
            redisTemplate.delete(tradeKeys);
        }
    }
}

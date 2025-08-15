package com.cryptotrading.apigateway.service;

import com.cryptotrading.apigateway.model.PriceData;
import com.cryptotrading.apigateway.repository.PriceDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class HistoricalDataService {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalDataService.class);

    @Autowired
    private PriceDataRepository priceDataRepository;

    /**
     * Get historical prices for a symbol within a time range
     */
    public List<PriceData> getHistoricalPrices(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return priceDataRepository.findBySymbolAndTimestampBetween(symbol, startTime, endTime);
        } catch (Exception e) {
            logger.error("Error getting historical prices for {}", symbol, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get historical prices with pagination
     */
    public Page<PriceData> getHistoricalPrices(String symbol, LocalDateTime startTime, LocalDateTime endTime, 
                                              int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            return priceDataRepository.findBySymbolAndTimestampBetween(symbol, startTime, endTime, pageable);
        } catch (Exception e) {
            logger.error("Error getting paginated historical prices for {}", symbol, e);
            return Page.empty();
        }
    }

    /**
     * Get OHLCV data for candlestick charts
     */
    public List<PriceData> getOHLCVData(String symbol, String intervalType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return priceDataRepository.findOHLCVData(symbol, intervalType, startTime, endTime);
        } catch (Exception e) {
            logger.error("Error getting OHLCV data for {}", symbol, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get price data for different time intervals
     */
    public List<PriceData> getPriceDataByInterval(String symbol, String interval, int limit) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(endTime, interval, limit);
            
            return priceDataRepository.findBySymbolAndIntervalTypeAndTimestampBetween(
                symbol, interval, startTime, endTime);
        } catch (Exception e) {
            logger.error("Error getting price data by interval for {}", symbol, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get latest prices for all symbols
     */
    public List<PriceData> getLatestPricesForAllSymbols() {
        try {
            return priceDataRepository.findLatestPricesForAllSymbols();
        } catch (Exception e) {
            logger.error("Error getting latest prices for all symbols", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get price statistics for a symbol
     */
    public Map<String, Object> getPriceStatistics(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Object[] result = priceDataRepository.getPriceStatistics(symbol, startTime, endTime);
            
            if (result != null && result.length >= 4) {
                stats.put("minPrice", result[0]);
                stats.put("maxPrice", result[1]);
                stats.put("avgPrice", result[2]);
                stats.put("dataPoints", result[3]);
                
                // Calculate additional statistics
                List<PriceData> prices = getHistoricalPrices(symbol, startTime, endTime);
                if (!prices.isEmpty()) {
                    stats.put("firstPrice", prices.get(0).getPrice());
                    stats.put("lastPrice", prices.get(prices.size() - 1).getPrice());
                    
                    // Calculate price change
                    BigDecimal firstPrice = prices.get(0).getPrice();
                    BigDecimal lastPrice = prices.get(prices.size() - 1).getPrice();
                    if (firstPrice != null && lastPrice != null && firstPrice.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal change = lastPrice.subtract(firstPrice);
                        BigDecimal changePercent = change.divide(firstPrice, 4, BigDecimal.ROUND_HALF_UP)
                                                        .multiply(BigDecimal.valueOf(100));
                        stats.put("priceChange", change);
                        stats.put("priceChangePercent", changePercent);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error getting price statistics for {}", symbol, e);
        }
        
        return stats;
    }

    /**
     * Get available symbols
     */
    public List<String> getAvailableSymbols() {
        try {
            return priceDataRepository.findDistinctSymbols();
        } catch (Exception e) {
            logger.error("Error getting available symbols", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get available exchanges
     */
    public List<String> getAvailableExchanges() {
        try {
            return priceDataRepository.findDistinctExchanges();
        } catch (Exception e) {
            logger.error("Error getting available exchanges", e);
            return new ArrayList<>();
        }
    }

    /**
     * Calculate start time based on interval and limit
     */
    private LocalDateTime calculateStartTime(LocalDateTime endTime, String interval, int limit) {
        switch (interval.toLowerCase()) {
            case "1m":
                return endTime.minus(limit, ChronoUnit.MINUTES);
            case "5m":
                return endTime.minus(limit * 5, ChronoUnit.MINUTES);
            case "15m":
                return endTime.minus(limit * 15, ChronoUnit.MINUTES);
            case "30m":
                return endTime.minus(limit * 30, ChronoUnit.MINUTES);
            case "1h":
                return endTime.minus(limit, ChronoUnit.HOURS);
            case "4h":
                return endTime.minus(limit * 4, ChronoUnit.HOURS);
            case "1d":
                return endTime.minus(limit, ChronoUnit.DAYS);
            case "1w":
                return endTime.minus(limit * 7, ChronoUnit.DAYS);
            default:
                return endTime.minus(limit, ChronoUnit.HOURS); // Default to hours
        }
    }

    /**
     * Get market overview data
     */
    public Map<String, Object> getMarketOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        try {
            List<String> symbols = getAvailableSymbols();
            List<String> exchanges = getAvailableExchanges();
            List<PriceData> latestPrices = getLatestPricesForAllSymbols();
            
            overview.put("totalSymbols", symbols.size());
            overview.put("totalExchanges", exchanges.size());
            overview.put("symbols", symbols);
            overview.put("exchanges", exchanges);
            overview.put("latestPrices", latestPrices);
            overview.put("lastUpdate", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting market overview", e);
        }
        
        return overview;
    }
}

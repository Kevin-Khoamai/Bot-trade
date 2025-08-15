package com.cryptotrading.analysis.service;

import com.cryptotrading.analysis.dto.MarketDataDto;
import com.cryptotrading.analysis.model.TechnicalIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for calculating technical indicators using simple mathematical calculations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IndicatorCalculationService {

    @Value("${analysis.indicators.period:14}")
    private int defaultPeriod;

    // Cache for calculated indicators
    private final Map<String, List<TechnicalIndicator>> indicatorCache = new ConcurrentHashMap<>();

    /**
     * Calculate all technical indicators for given market data
     */
    public List<TechnicalIndicator> calculateAllIndicators(String symbol, List<MarketDataDto> marketDataList) {
        try {
            log.debug("Calculating indicators for symbol: {} with {} data points", symbol, marketDataList.size());

            if (marketDataList.isEmpty()) {
                log.warn("No market data provided for symbol: {}", symbol);
                return new ArrayList<>();
            }

            List<TechnicalIndicator> indicators = new ArrayList<>();
            LocalDateTime timestamp = LocalDateTime.now();

            List<BigDecimal> prices = marketDataList.stream()
                    .map(MarketDataDto::getClosePrice)
                    .toList();

            List<BigDecimal> volumes = marketDataList.stream()
                    .map(MarketDataDto::getVolume)
                    .toList();

            // Simple Moving Averages
            BigDecimal sma20 = calculateSMA(prices, 20);
            if (sma20 != null) {
                indicators.add(TechnicalIndicator.create(symbol, timestamp, 
                        TechnicalIndicator.IndicatorType.SMA, sma20, 20));
            }

            BigDecimal sma50 = calculateSMA(prices, 50);
            if (sma50 != null) {
                indicators.add(TechnicalIndicator.create(symbol, timestamp, 
                        TechnicalIndicator.IndicatorType.SMA, sma50, 50));
            }

            // Exponential Moving Averages (simplified)
            BigDecimal ema12 = calculateEMA(prices, 12);
            if (ema12 != null) {
                indicators.add(TechnicalIndicator.create(symbol, timestamp, 
                        TechnicalIndicator.IndicatorType.EMA, ema12, 12));
            }

            BigDecimal ema26 = calculateEMA(prices, 26);
            if (ema26 != null) {
                indicators.add(TechnicalIndicator.create(symbol, timestamp, 
                        TechnicalIndicator.IndicatorType.EMA, ema26, 26));
            }

            // RSI (simplified)
            BigDecimal rsi = calculateRSI(prices, defaultPeriod);
            if (rsi != null) {
                indicators.add(TechnicalIndicator.create(symbol, timestamp, 
                        TechnicalIndicator.IndicatorType.RSI, rsi, defaultPeriod));
            }

            // Cache the result
            indicatorCache.put(symbol, indicators);

            log.debug("Successfully calculated {} indicators for symbol: {}", indicators.size(), symbol);
            return indicators;

        } catch (Exception e) {
            log.error("Error calculating indicators for symbol {}: {}", symbol, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Calculate Simple Moving Average
     */
    private BigDecimal calculateSMA(List<BigDecimal> values, int period) {
        if (values.size() < period) {
            return null;
        }

        List<BigDecimal> lastValues = values.subList(values.size() - period, values.size());
        BigDecimal sum = lastValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Exponential Moving Average (simplified)
     */
    private BigDecimal calculateEMA(List<BigDecimal> values, int period) {
        if (values.size() < period) {
            return null;
        }

        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal ema = calculateSMA(values.subList(0, period), period);
        
        for (int i = period; i < values.size(); i++) {
            BigDecimal price = values.get(i);
            ema = price.multiply(multiplier).add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }
        
        return ema.setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Calculate RSI (simplified)
     */
    private BigDecimal calculateRSI(List<BigDecimal> prices, int period) {
        if (prices.size() < period + 1) {
            return null;
        }

        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();

        for (int i = 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains.add(change);
                losses.add(BigDecimal.ZERO);
            } else {
                gains.add(BigDecimal.ZERO);
                losses.add(change.abs());
            }
        }

        if (gains.size() < period) {
            return null;
        }

        BigDecimal avgGain = calculateSMA(gains.subList(gains.size() - period, gains.size()), period);
        BigDecimal avgLoss = calculateSMA(losses.subList(losses.size() - period, losses.size()), period);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP)
        );

        return rsi.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Get cached indicators for a symbol
     */
    public List<TechnicalIndicator> getCachedIndicators(String symbol) {
        return indicatorCache.getOrDefault(symbol, new ArrayList<>());
    }

    /**
     * Clear cache for a symbol
     */
    public void clearCache(String symbol) {
        indicatorCache.remove(symbol);
    }

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        indicatorCache.clear();
    }
}

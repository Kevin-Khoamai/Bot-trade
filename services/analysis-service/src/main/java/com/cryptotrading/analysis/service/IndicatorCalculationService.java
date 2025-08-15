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

            // VWAP (Volume Weighted Average Price)
            BigDecimal vwap = calculateVWAP(marketDataList);
            if (vwap != null) {
                indicators.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.VWAP, vwap, null));
            }

            // MACD
            if (ema12 != null && ema26 != null) {
                BigDecimal macd = ema12.subtract(ema26);
                indicators.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.MACD, macd, null));

                // MACD Signal Line (9-period EMA of MACD)
                List<BigDecimal> macdHistory = getMacdHistory(symbol, 9);
                macdHistory.add(macd);
                BigDecimal macdSignal = calculateEMA(macdHistory, 9);
                if (macdSignal != null) {
                    indicators.add(TechnicalIndicator.create(symbol, timestamp,
                            TechnicalIndicator.IndicatorType.MACD_SIGNAL, macdSignal, 9));
                }
            }

            // Bollinger Bands
            BollingerBands bollingerBands = calculateBollingerBands(prices, 20, 2.0);
            if (bollingerBands != null) {
                indicators.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.BB_UPPER, bollingerBands.getUpperBand(), 20));
                indicators.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.BB_MIDDLE, bollingerBands.getMiddleBand(), 20));
                indicators.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.BB_LOWER, bollingerBands.getLowerBand(), 20));
            }

            // Stochastic Oscillator
            StochasticResult stochastic = calculateStochastic(marketDataList, 14, 3);
            if (stochastic != null) {
                indicators.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.STOCH_K, stochastic.getK(), 14));
                indicators.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.STOCH_D, stochastic.getD(), 3));
            }

            // Average True Range (ATR)
            BigDecimal atr = calculateATR(marketDataList, 14);
            if (atr != null) {
                indicators.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.ATR, atr, 14));
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
     * Calculate Volume Weighted Average Price (VWAP)
     */
    private BigDecimal calculateVWAP(List<MarketDataDto> marketDataList) {
        if (marketDataList.isEmpty()) {
            return null;
        }

        BigDecimal totalVolumePrice = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;

        for (MarketDataDto data : marketDataList) {
            BigDecimal typicalPrice = data.getTypicalPrice();
            BigDecimal volume = data.getVolume();

            totalVolumePrice = totalVolumePrice.add(typicalPrice.multiply(volume));
            totalVolume = totalVolume.add(volume);
        }

        if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return totalVolumePrice.divide(totalVolume, 8, RoundingMode.HALF_UP);
    }

    /**
     * Get MACD history for signal calculation
     */
    private List<BigDecimal> getMacdHistory(String symbol, int periods) {
        // In a real implementation, this would fetch from database or cache
        // For now, return empty list to avoid null pointer
        return new ArrayList<>();
    }

    /**
     * Calculate Bollinger Bands
     */
    private BollingerBands calculateBollingerBands(List<BigDecimal> prices, int period, double multiplier) {
        if (prices.size() < period) {
            return null;
        }

        BigDecimal sma = calculateSMA(prices, period);
        if (sma == null) {
            return null;
        }

        // Calculate standard deviation
        List<BigDecimal> lastPrices = prices.subList(prices.size() - period, prices.size());
        BigDecimal variance = BigDecimal.ZERO;

        for (BigDecimal price : lastPrices) {
            BigDecimal diff = price.subtract(sma);
            variance = variance.add(diff.multiply(diff));
        }

        variance = variance.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        BigDecimal multiplierBD = BigDecimal.valueOf(multiplier);
        BigDecimal upperBand = sma.add(stdDev.multiply(multiplierBD));
        BigDecimal lowerBand = sma.subtract(stdDev.multiply(multiplierBD));

        return new BollingerBands(upperBand, sma, lowerBand);
    }

    /**
     * Calculate Stochastic Oscillator
     */
    private StochasticResult calculateStochastic(List<MarketDataDto> marketDataList, int kPeriod, int dPeriod) {
        if (marketDataList.size() < kPeriod) {
            return null;
        }

        List<MarketDataDto> lastData = marketDataList.subList(marketDataList.size() - kPeriod, marketDataList.size());

        BigDecimal highestHigh = lastData.stream()
                .map(MarketDataDto::getHighPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowestLow = lastData.stream()
                .map(MarketDataDto::getLowPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal currentClose = marketDataList.get(marketDataList.size() - 1).getClosePrice();

        BigDecimal k = BigDecimal.ZERO;
        if (!highestHigh.equals(lowestLow)) {
            k = currentClose.subtract(lowestLow)
                    .divide(highestHigh.subtract(lowestLow), 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // For %D, we would need historical %K values. Simplified here.
        BigDecimal d = k; // In real implementation, this would be SMA of %K

        return new StochasticResult(k, d);
    }

    /**
     * Calculate Average True Range (ATR)
     */
    private BigDecimal calculateATR(List<MarketDataDto> marketDataList, int period) {
        if (marketDataList.size() < period + 1) {
            return null;
        }

        List<BigDecimal> trueRanges = new ArrayList<>();

        for (int i = 1; i < marketDataList.size(); i++) {
            MarketDataDto current = marketDataList.get(i);
            MarketDataDto previous = marketDataList.get(i - 1);

            BigDecimal tr1 = current.getHighPrice().subtract(current.getLowPrice());
            BigDecimal tr2 = current.getHighPrice().subtract(previous.getClosePrice()).abs();
            BigDecimal tr3 = current.getLowPrice().subtract(previous.getClosePrice()).abs();

            BigDecimal trueRange = tr1.max(tr2).max(tr3);
            trueRanges.add(trueRange);
        }

        if (trueRanges.size() < period) {
            return null;
        }

        return calculateSMA(trueRanges.subList(trueRanges.size() - period, trueRanges.size()), period);
    }

    /**
     * Get cached indicators for a symbol
     */
    public List<TechnicalIndicator> getCachedIndicators(String symbol) {
        return indicatorCache.getOrDefault(symbol, new ArrayList<>());
    }

    // Helper classes for complex indicators
    @Data
    @AllArgsConstructor
    private static class BollingerBands {
        private BigDecimal upperBand;
        private BigDecimal middleBand;
        private BigDecimal lowerBand;
    }

    @Data
    @AllArgsConstructor
    private static class StochasticResult {
        private BigDecimal k;
        private BigDecimal d;
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

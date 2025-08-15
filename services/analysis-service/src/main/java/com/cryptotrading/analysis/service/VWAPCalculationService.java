package com.cryptotrading.analysis.service;

import com.cryptotrading.analysis.dto.MarketDataDto;
import com.cryptotrading.analysis.model.TechnicalIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for calculating Volume Weighted Average Price (VWAP) and related indicators
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VWAPCalculationService {

    // Cache for VWAP calculations
    private final Map<String, VWAPData> vwapCache = new ConcurrentHashMap<>();
    
    /**
     * Calculate VWAP for a symbol using market data
     */
    public TechnicalIndicator calculateVWAP(String symbol, List<MarketDataDto> marketDataList) {
        if (marketDataList.isEmpty()) {
            return null;
        }

        try {
            BigDecimal vwap = calculateVWAPValue(marketDataList);
            if (vwap != null) {
                return TechnicalIndicator.create(
                    symbol, 
                    LocalDateTime.now(), 
                    TechnicalIndicator.IndicatorType.VWAP, 
                    vwap, 
                    null
                );
            }
        } catch (Exception e) {
            log.error("Error calculating VWAP for symbol {}: {}", symbol, e.getMessage());
        }

        return null;
    }

    /**
     * Calculate intraday VWAP (resets daily)
     */
    public TechnicalIndicator calculateIntradayVWAP(String symbol, List<MarketDataDto> todayData) {
        if (todayData.isEmpty()) {
            return null;
        }

        try {
            // Filter data for current trading day
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            List<MarketDataDto> intradayData = todayData.stream()
                    .filter(data -> data.getTimestamp().isAfter(startOfDay))
                    .toList();

            BigDecimal intradayVWAP = calculateVWAPValue(intradayData);
            if (intradayVWAP != null) {
                return TechnicalIndicator.create(
                    symbol, 
                    LocalDateTime.now(), 
                    TechnicalIndicator.IndicatorType.VWAP, 
                    intradayVWAP, 
                    1440 // 1 day in minutes
                );
            }
        } catch (Exception e) {
            log.error("Error calculating intraday VWAP for symbol {}: {}", symbol, e.getMessage());
        }

        return null;
    }

    /**
     * Calculate rolling VWAP over specified periods
     */
    public List<TechnicalIndicator> calculateRollingVWAP(String symbol, List<MarketDataDto> marketDataList, 
                                                        int[] periods) {
        List<TechnicalIndicator> indicators = new ArrayList<>();
        
        for (int period : periods) {
            if (marketDataList.size() >= period) {
                List<MarketDataDto> periodData = marketDataList.subList(
                    marketDataList.size() - period, 
                    marketDataList.size()
                );
                
                BigDecimal rollingVWAP = calculateVWAPValue(periodData);
                if (rollingVWAP != null) {
                    indicators.add(TechnicalIndicator.create(
                        symbol, 
                        LocalDateTime.now(), 
                        TechnicalIndicator.IndicatorType.VWAP, 
                        rollingVWAP, 
                        period
                    ));
                }
            }
        }
        
        return indicators;
    }

    /**
     * Calculate VWAP bands (similar to Bollinger Bands but using VWAP)
     */
    public VWAPBands calculateVWAPBands(String symbol, List<MarketDataDto> marketDataList, 
                                       double standardDeviations) {
        if (marketDataList.size() < 20) {
            return null;
        }

        try {
            BigDecimal vwap = calculateVWAPValue(marketDataList);
            if (vwap == null) {
                return null;
            }

            // Calculate standard deviation of prices from VWAP
            BigDecimal variance = BigDecimal.ZERO;
            BigDecimal totalVolume = BigDecimal.ZERO;

            for (MarketDataDto data : marketDataList) {
                BigDecimal typicalPrice = data.getTypicalPrice();
                BigDecimal volume = data.getVolume();
                BigDecimal deviation = typicalPrice.subtract(vwap);
                
                variance = variance.add(deviation.multiply(deviation).multiply(volume));
                totalVolume = totalVolume.add(volume);
            }

            if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }

            variance = variance.divide(totalVolume, 8, RoundingMode.HALF_UP);
            BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
            BigDecimal multiplier = BigDecimal.valueOf(standardDeviations);

            BigDecimal upperBand = vwap.add(stdDev.multiply(multiplier));
            BigDecimal lowerBand = vwap.subtract(stdDev.multiply(multiplier));

            return new VWAPBands(upperBand, vwap, lowerBand, stdDev);

        } catch (Exception e) {
            log.error("Error calculating VWAP bands for symbol {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate VWAP value from market data
     */
    private BigDecimal calculateVWAPValue(List<MarketDataDto> marketDataList) {
        if (marketDataList.isEmpty()) {
            return null;
        }

        BigDecimal totalVolumePrice = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;

        for (MarketDataDto data : marketDataList) {
            BigDecimal typicalPrice = data.getTypicalPrice();
            BigDecimal volume = data.getVolume();
            
            if (volume.compareTo(BigDecimal.ZERO) > 0) {
                totalVolumePrice = totalVolumePrice.add(typicalPrice.multiply(volume));
                totalVolume = totalVolume.add(volume);
            }
        }

        if (totalVolume.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return totalVolumePrice.divide(totalVolume, 8, RoundingMode.HALF_UP);
    }

    /**
     * Update VWAP cache with new data point
     */
    public void updateVWAPCache(String symbol, MarketDataDto newData) {
        VWAPData vwapData = vwapCache.computeIfAbsent(symbol, k -> new VWAPData());
        vwapData.addDataPoint(newData);
        
        // Keep only recent data (e.g., last 1000 points)
        if (vwapData.getDataPoints().size() > 1000) {
            List<MarketDataDto> dataPoints = vwapData.getDataPoints();
            vwapData.setDataPoints(new ArrayList<>(dataPoints.subList(dataPoints.size() - 1000, dataPoints.size())));
        }
    }

    /**
     * Get cached VWAP for a symbol
     */
    public BigDecimal getCachedVWAP(String symbol) {
        VWAPData vwapData = vwapCache.get(symbol);
        if (vwapData != null && !vwapData.getDataPoints().isEmpty()) {
            return calculateVWAPValue(vwapData.getDataPoints());
        }
        return null;
    }

    /**
     * Calculate VWAP deviation (current price vs VWAP)
     */
    public BigDecimal calculateVWAPDeviation(String symbol, BigDecimal currentPrice) {
        BigDecimal vwap = getCachedVWAP(symbol);
        if (vwap != null && currentPrice != null) {
            return currentPrice.subtract(vwap)
                    .divide(vwap, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)); // Convert to percentage
        }
        return null;
    }

    /**
     * Data class for VWAP calculation
     */
    private static class VWAPData {
        private List<MarketDataDto> dataPoints = new ArrayList<>();
        private BigDecimal cumulativeVolumePrice = BigDecimal.ZERO;
        private BigDecimal cumulativeVolume = BigDecimal.ZERO;

        public void addDataPoint(MarketDataDto data) {
            dataPoints.add(data);
            BigDecimal typicalPrice = data.getTypicalPrice();
            BigDecimal volume = data.getVolume();
            
            cumulativeVolumePrice = cumulativeVolumePrice.add(typicalPrice.multiply(volume));
            cumulativeVolume = cumulativeVolume.add(volume);
        }

        public List<MarketDataDto> getDataPoints() {
            return dataPoints;
        }

        public void setDataPoints(List<MarketDataDto> dataPoints) {
            this.dataPoints = dataPoints;
            // Recalculate cumulative values
            recalculateCumulatives();
        }

        private void recalculateCumulatives() {
            cumulativeVolumePrice = BigDecimal.ZERO;
            cumulativeVolume = BigDecimal.ZERO;
            
            for (MarketDataDto data : dataPoints) {
                BigDecimal typicalPrice = data.getTypicalPrice();
                BigDecimal volume = data.getVolume();
                
                cumulativeVolumePrice = cumulativeVolumePrice.add(typicalPrice.multiply(volume));
                cumulativeVolume = cumulativeVolume.add(volume);
            }
        }

        public BigDecimal getCurrentVWAP() {
            if (cumulativeVolume.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }
            return cumulativeVolumePrice.divide(cumulativeVolume, 8, RoundingMode.HALF_UP);
        }
    }

    /**
     * Data class for VWAP bands
     */
    public static class VWAPBands {
        private final BigDecimal upperBand;
        private final BigDecimal vwap;
        private final BigDecimal lowerBand;
        private final BigDecimal standardDeviation;

        public VWAPBands(BigDecimal upperBand, BigDecimal vwap, BigDecimal lowerBand, BigDecimal standardDeviation) {
            this.upperBand = upperBand;
            this.vwap = vwap;
            this.lowerBand = lowerBand;
            this.standardDeviation = standardDeviation;
        }

        // Getters
        public BigDecimal getUpperBand() { return upperBand; }
        public BigDecimal getVwap() { return vwap; }
        public BigDecimal getLowerBand() { return lowerBand; }
        public BigDecimal getStandardDeviation() { return standardDeviation; }
    }
}

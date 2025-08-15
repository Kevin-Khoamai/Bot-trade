package com.cryptotrading.analysis.service;

import com.cryptotrading.analysis.dto.MarketDataDto;
import com.cryptotrading.analysis.model.TechnicalIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Service for price prediction using various models
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    @Value("${analysis.prediction.horizon:60}")
    private int predictionHorizonMinutes;

    @Value("${analysis.prediction.training-window:168}")
    private int trainingWindowHours;

    private final RedisCacheService redisCacheService;

    // Cache for model parameters
    private final Map<String, ARIMAModel> arimaModels = new HashMap<>();
    private final Map<String, LinearRegressionModel> mlModels = new HashMap<>();

    /**
     * Generate predictions for a symbol using multiple models
     */
    public List<TechnicalIndicator> generatePredictions(String symbol, List<MarketDataDto> historicalData) {
        List<TechnicalIndicator> predictions = new ArrayList<>();
        LocalDateTime timestamp = LocalDateTime.now();

        try {
            if (historicalData.size() < 20) {
                log.warn("Insufficient data for prediction: {} data points for symbol {}", 
                        historicalData.size(), symbol);
                return predictions;
            }

            // Extract price series
            List<BigDecimal> prices = historicalData.stream()
                    .map(MarketDataDto::getClosePrice)
                    .toList();

            // ARIMA Prediction
            BigDecimal arimaPrediction = generateARIMAPrediction(symbol, prices);
            if (arimaPrediction != null) {
                predictions.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.ARIMA_PREDICTION, arimaPrediction, 
                        predictionHorizonMinutes));
            }

            // Machine Learning Prediction (Linear Regression)
            BigDecimal mlPrediction = generateMLPrediction(symbol, historicalData);
            if (mlPrediction != null) {
                predictions.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.ML_PREDICTION, mlPrediction, 
                        predictionHorizonMinutes));
            }

            // Trend Analysis Prediction
            BigDecimal trendPrediction = generateTrendPrediction(symbol, prices);
            if (trendPrediction != null) {
                predictions.add(TechnicalIndicator.create(symbol, timestamp,
                        TechnicalIndicator.IndicatorType.TREND_PREDICTION, trendPrediction, 
                        predictionHorizonMinutes));
            }

            // Cache predictions
            cachePredictions(symbol, predictions);

            log.info("Generated {} predictions for symbol: {}", predictions.size(), symbol);

        } catch (Exception e) {
            log.error("Error generating predictions for symbol {}: {}", symbol, e.getMessage(), e);
        }

        return predictions;
    }

    /**
     * Generate ARIMA prediction (simplified implementation)
     */
    private BigDecimal generateARIMAPrediction(String symbol, List<BigDecimal> prices) {
        try {
            // Get or create ARIMA model for this symbol
            ARIMAModel model = arimaModels.computeIfAbsent(symbol, k -> new ARIMAModel());
            
            // Update model with new data
            model.updateModel(prices);
            
            // Generate prediction
            return model.predict();
            
        } catch (Exception e) {
            log.error("Error in ARIMA prediction for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Generate ML prediction using linear regression
     */
    private BigDecimal generateMLPrediction(String symbol, List<MarketDataDto> historicalData) {
        try {
            // Get or create ML model for this symbol
            LinearRegressionModel model = mlModels.computeIfAbsent(symbol, k -> new LinearRegressionModel());
            
            // Update model with new data
            model.updateModel(historicalData);
            
            // Generate prediction
            return model.predict();
            
        } catch (Exception e) {
            log.error("Error in ML prediction for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Generate trend-based prediction
     */
    private BigDecimal generateTrendPrediction(String symbol, List<BigDecimal> prices) {
        try {
            if (prices.size() < 10) {
                return null;
            }

            // Calculate short-term and long-term trends
            List<BigDecimal> shortTerm = prices.subList(Math.max(0, prices.size() - 5), prices.size());
            List<BigDecimal> longTerm = prices.subList(Math.max(0, prices.size() - 20), prices.size());

            BigDecimal shortTrend = calculateTrend(shortTerm);
            BigDecimal longTrend = calculateTrend(longTerm);

            // Weighted combination of trends
            BigDecimal currentPrice = prices.get(prices.size() - 1);
            BigDecimal shortWeight = BigDecimal.valueOf(0.7);
            BigDecimal longWeight = BigDecimal.valueOf(0.3);

            BigDecimal trendPrediction = currentPrice
                    .add(shortTrend.multiply(shortWeight))
                    .add(longTrend.multiply(longWeight));

            return trendPrediction.setScale(8, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.error("Error in trend prediction for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * Calculate trend from price series
     */
    private BigDecimal calculateTrend(List<BigDecimal> prices) {
        if (prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        SimpleRegression regression = new SimpleRegression();
        
        for (int i = 0; i < prices.size(); i++) {
            regression.addData(i, prices.get(i).doubleValue());
        }

        double slope = regression.getSlope();
        return BigDecimal.valueOf(slope).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Cache predictions in Redis
     */
    private void cachePredictions(String symbol, List<TechnicalIndicator> predictions) {
        try {
            for (TechnicalIndicator prediction : predictions) {
                redisCacheService.cachePrediction(symbol + ":" + prediction.getIndicatorType(), prediction);
            }
        } catch (Exception e) {
            log.error("Error caching predictions for {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Get cached predictions
     */
    public List<TechnicalIndicator> getCachedPredictions(String symbol) {
        List<TechnicalIndicator> predictions = new ArrayList<>();
        
        try {
            // Try to get each prediction type from cache
            for (TechnicalIndicator.IndicatorType type : Arrays.asList(
                    TechnicalIndicator.IndicatorType.ARIMA_PREDICTION,
                    TechnicalIndicator.IndicatorType.ML_PREDICTION,
                    TechnicalIndicator.IndicatorType.TREND_PREDICTION)) {
                
                Optional<Object> cached = redisCacheService.getCachedPrediction(symbol + ":" + type.name());
                if (cached.isPresent() && cached.get() instanceof TechnicalIndicator) {
                    predictions.add((TechnicalIndicator) cached.get());
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving cached predictions for {}: {}", symbol, e.getMessage());
        }
        
        return predictions;
    }

    /**
     * Simplified ARIMA model implementation
     */
    private static class ARIMAModel {
        private List<BigDecimal> data = new ArrayList<>();
        private BigDecimal lastPrediction = BigDecimal.ZERO;

        public void updateModel(List<BigDecimal> newData) {
            // Keep only recent data for efficiency
            this.data = new ArrayList<>(newData.subList(Math.max(0, newData.size() - 100), newData.size()));
        }

        public BigDecimal predict() {
            if (data.size() < 3) {
                return null;
            }

            // Simplified ARIMA(1,1,1) - just using moving average with trend
            BigDecimal recent = data.get(data.size() - 1);
            BigDecimal previous = data.get(data.size() - 2);
            BigDecimal trend = recent.subtract(previous);

            // Simple prediction: last price + trend
            return recent.add(trend.multiply(BigDecimal.valueOf(0.5)));
        }
    }

    /**
     * Simplified Linear Regression model
     */
    private static class LinearRegressionModel {
        private SimpleRegression regression = new SimpleRegression();
        private int dataPoints = 0;

        public void updateModel(List<MarketDataDto> newData) {
            regression.clear();
            
            // Use multiple features: price, volume, volatility
            for (int i = 0; i < newData.size(); i++) {
                MarketDataDto data = newData.get(i);
                
                // Simple feature: time index vs price
                regression.addData(i, data.getClosePrice().doubleValue());
            }
            
            dataPoints = newData.size();
        }

        public BigDecimal predict() {
            if (dataPoints < 5) {
                return null;
            }

            // Predict next point
            double prediction = regression.predict(dataPoints);
            return BigDecimal.valueOf(prediction).setScale(8, RoundingMode.HALF_UP);
        }
    }
}

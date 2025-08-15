package com.cryptotrading.analysis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for prediction models and analysis parameters
 */
@Configuration
@ConfigurationProperties(prefix = "analysis.prediction")
@Data
public class PredictionConfig {

    /**
     * Prediction horizon in minutes
     */
    private int horizon = 60;

    /**
     * Training data window in hours
     */
    private int trainingWindow = 168; // 1 week

    /**
     * Minimum data points required for prediction
     */
    private int minDataPoints = 20;

    /**
     * Maximum data points to keep in memory
     */
    private int maxDataPoints = 1000;

    /**
     * ARIMA model configuration
     */
    private ARIMAConfig arima = new ARIMAConfig();

    /**
     * Neural network configuration
     */
    private NeuralNetworkConfig neuralNetwork = new NeuralNetworkConfig();

    /**
     * Prediction confidence thresholds
     */
    private ConfidenceConfig confidence = new ConfidenceConfig();

    /**
     * ARIMA model parameters
     */
    @Data
    public static class ARIMAConfig {
        private int p = 2; // autoregressive order
        private int d = 1; // degree of differencing
        private int q = 2; // moving average order
        private double confidenceLevel = 0.95;
        private int maxIterations = 100;
    }

    /**
     * Neural network parameters
     */
    @Data
    public static class NeuralNetworkConfig {
        private List<Integer> hiddenLayers = List.of(50, 30, 10);
        private int epochs = 100;
        private double learningRate = 0.001;
        private int batchSize = 32;
        private double dropoutRate = 0.2;
        private String activationFunction = "relu";
        private String optimizer = "adam";
    }

    /**
     * Confidence thresholds for predictions
     */
    @Data
    public static class ConfidenceConfig {
        private double highConfidence = 0.8;
        private double mediumConfidence = 0.6;
        private double lowConfidence = 0.4;
        private double minAcceptableConfidence = 0.3;
    }

    /**
     * Get prediction horizon in milliseconds
     */
    public long getHorizonMillis() {
        return horizon * 60L * 1000L;
    }

    /**
     * Get training window in milliseconds
     */
    public long getTrainingWindowMillis() {
        return trainingWindow * 60L * 60L * 1000L;
    }

    /**
     * Check if we have enough data for prediction
     */
    public boolean hasEnoughData(int dataPoints) {
        return dataPoints >= minDataPoints;
    }

    /**
     * Check if data should be trimmed
     */
    public boolean shouldTrimData(int dataPoints) {
        return dataPoints > maxDataPoints;
    }

    /**
     * Get trim size (how many points to keep)
     */
    public int getTrimSize() {
        return (int) (maxDataPoints * 0.8); // Keep 80% when trimming
    }
}

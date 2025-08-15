package com.cryptotrading.analysis.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing technical indicators
 */
@Entity
@Table(name = "technical_indicators",
       indexes = {
           @Index(name = "idx_symbol_indicator_timestamp", columnList = "symbol, indicator_type, timestamp"),
           @Index(name = "idx_timestamp", columnList = "timestamp")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "indicator_type", nullable = false, length = 50)
    private String indicatorType;

    @Column(name = "indicator_value", nullable = false, precision = 20, scale = 8)
    private BigDecimal indicatorValue;

    @Column
    private Integer period;

    @Column(name = "additional_params")
    private String additionalParams; // JSON string for extra parameters

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Create a technical indicator
     */
    public static TechnicalIndicator create(String symbol, LocalDateTime timestamp,
                                          IndicatorType type, BigDecimal value, Integer period) {
        return TechnicalIndicator.builder()
                .symbol(symbol)
                .timestamp(timestamp)
                .indicatorType(type.name())
                .indicatorValue(value)
                .period(period)
                .build();
    }

    /**
     * Create a technical indicator with additional parameters
     */
    public static TechnicalIndicator create(String symbol, LocalDateTime timestamp,
                                          IndicatorType type, BigDecimal value, 
                                          Integer period, String additionalParams) {
        return TechnicalIndicator.builder()
                .symbol(symbol)
                .timestamp(timestamp)
                .indicatorType(type.name())
                .indicatorValue(value)
                .period(period)
                .additionalParams(additionalParams)
                .build();
    }

    /**
     * Enum for indicator types
     */
    public enum IndicatorType {
        // Moving Averages
        SMA,    // Simple Moving Average
        EMA,    // Exponential Moving Average
        WMA,    // Weighted Moving Average
        
        // Momentum Indicators
        RSI,    // Relative Strength Index
        STOCH,  // Stochastic Oscillator
        WILLIAMS_R, // Williams %R
        
        // Trend Indicators
        MACD,   // MACD Line
        MACD_SIGNAL, // MACD Signal Line
        MACD_HISTOGRAM, // MACD Histogram
        ADX,    // Average Directional Index
        
        // Volatility Indicators
        BB_UPPER,   // Bollinger Band Upper
        BB_MIDDLE,  // Bollinger Band Middle
        BB_LOWER,   // Bollinger Band Lower
        ATR,    // Average True Range
        
        // Volume Indicators
        OBV,    // On Balance Volume
        VOLUME_SMA, // Volume Simple Moving Average
        VWAP,   // Volume Weighted Average Price

        // Stochastic Indicators
        STOCH_K,    // Stochastic %K
        STOCH_D,    // Stochastic %D

        // Custom Indicators
        PRICE_CHANGE,   // Price change percentage
        VOLATILITY,     // Price volatility
        MOMENTUM,       // Price momentum

        // Prediction Models
        ARIMA_PREDICTION,   // ARIMA model prediction
        ML_PREDICTION,      // Machine Learning prediction
        TREND_PREDICTION    // Trend analysis prediction
    }

    /**
     * Check if this is a valid indicator
     */
    public boolean isValid() {
        return symbol != null && !symbol.trim().isEmpty() &&
               timestamp != null &&
               indicatorType != null && !indicatorType.trim().isEmpty() &&
               indicatorValue != null &&
               !indicatorValue.equals(BigDecimal.ZERO.setScale(8));
    }

    /**
     * Get indicator type as enum
     */
    public IndicatorType getIndicatorTypeEnum() {
        try {
            return IndicatorType.valueOf(indicatorType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Generate cache key for Redis
     */
    public String getCacheKey() {
        return String.format("indicator:%s:%s:%s:%d", 
                           symbol, indicatorType, timestamp, period != null ? period : 0);
    }

    /**
     * Generate cache key prefix for symbol and indicator type
     */
    public static String getCacheKeyPrefix(String symbol, IndicatorType type) {
        return String.format("indicator:%s:%s", symbol, type.name());
    }
}

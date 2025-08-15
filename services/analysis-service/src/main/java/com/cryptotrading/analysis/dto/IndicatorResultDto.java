package com.cryptotrading.analysis.dto;

import com.cryptotrading.analysis.model.TechnicalIndicator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for technical indicator results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorResultDto {

    private String symbol;
    private LocalDateTime timestamp;
    private String indicatorType;
    private BigDecimal value;
    private Integer period;
    private Map<String, Object> additionalData;
    private String signal; // BUY, SELL, HOLD, NEUTRAL

    /**
     * Create from TechnicalIndicator entity
     */
    public static IndicatorResultDto fromEntity(TechnicalIndicator indicator) {
        return IndicatorResultDto.builder()
                .symbol(indicator.getSymbol())
                .timestamp(indicator.getTimestamp())
                .indicatorType(indicator.getIndicatorType())
                .value(indicator.getIndicatorValue())
                .period(indicator.getPeriod())
                .build();
    }

    /**
     * Create with signal
     */
    public static IndicatorResultDto withSignal(TechnicalIndicator indicator, String signal) {
        IndicatorResultDto dto = fromEntity(indicator);
        dto.setSignal(signal);
        return dto;
    }

    /**
     * Create with additional data
     */
    public static IndicatorResultDto withAdditionalData(TechnicalIndicator indicator, 
                                                       Map<String, Object> additionalData) {
        IndicatorResultDto dto = fromEntity(indicator);
        dto.setAdditionalData(additionalData);
        return dto;
    }
}

/**
 * DTO for MACD indicator results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class MacdResultDto {
    private String symbol;
    private LocalDateTime timestamp;
    private BigDecimal macdLine;
    private BigDecimal signalLine;
    private BigDecimal histogram;
    private String signal; // BUY, SELL, HOLD
}

/**
 * DTO for Bollinger Bands results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class BollingerBandsResultDto {
    private String symbol;
    private LocalDateTime timestamp;
    private BigDecimal upperBand;
    private BigDecimal middleBand;
    private BigDecimal lowerBand;
    private BigDecimal bandwidth;
    private BigDecimal percentB;
    private String signal; // BUY, SELL, HOLD
}

/**
 * DTO for Stochastic Oscillator results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class StochasticResultDto {
    private String symbol;
    private LocalDateTime timestamp;
    private BigDecimal percentK;
    private BigDecimal percentD;
    private String signal; // BUY, SELL, HOLD
}

/**
 * DTO for comprehensive analysis results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class AnalysisResultDto {
    private String symbol;
    private LocalDateTime timestamp;
    private BigDecimal currentPrice;
    
    // Moving Averages
    private Map<Integer, BigDecimal> smaValues;
    private Map<Integer, BigDecimal> emaValues;
    
    // Momentum Indicators
    private BigDecimal rsi;
    private StochasticResultDto stochastic;
    
    // Trend Indicators
    private MacdResultDto macd;
    private BigDecimal adx;
    
    // Volatility Indicators
    private BollingerBandsResultDto bollingerBands;
    private BigDecimal atr;
    
    // Volume Indicators
    private BigDecimal obv;
    
    // Overall signals
    private String trendSignal;    // BULLISH, BEARISH, NEUTRAL
    private String momentumSignal; // OVERBOUGHT, OVERSOLD, NEUTRAL
    private String volatilitySignal; // HIGH, LOW, NORMAL
    private String overallSignal;  // STRONG_BUY, BUY, HOLD, SELL, STRONG_SELL
    
    // Confidence score (0-100)
    private Integer confidenceScore;
}

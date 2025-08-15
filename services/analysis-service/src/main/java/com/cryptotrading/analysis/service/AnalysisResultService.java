package com.cryptotrading.analysis.service;

import com.cryptotrading.analysis.dto.IndicatorResultDto;
import com.cryptotrading.analysis.model.TechnicalIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for analyzing technical indicators and publishing results
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisResultService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisCacheService redisCacheService;

    @Value("${kafka.topics.indicators}")
    private String indicatorsTopic;

    @Value("${kafka.topics.predictions}")
    private String predictionsTopic;

    @Value("${kafka.topics.analysis-alerts}")
    private String alertsTopic;

    /**
     * Publish analysis results for a symbol
     */
    public void publishAnalysisResults(String symbol, List<TechnicalIndicator> indicators) {
        try {
            // Convert to DTOs
            List<IndicatorResultDto> indicatorDtos = indicators.stream()
                    .map(IndicatorResultDto::fromEntity)
                    .toList();

            // Generate trading signals
            AnalysisResult analysisResult = generateAnalysisResult(symbol, indicators);

            // Cache results
            redisCacheService.cacheIndicatorList(symbol, indicatorDtos);
            redisCacheService.cacheAnalysisResult(symbol, analysisResult);

            // Publish to Kafka
            publishIndicators(symbol, indicatorDtos);
            publishAnalysisResult(symbol, analysisResult);

            // Check for alerts
            checkAndPublishAlerts(symbol, analysisResult);

            log.info("Published analysis results for symbol: {}", symbol);

        } catch (Exception e) {
            log.error("Error publishing analysis results for symbol {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Generate comprehensive analysis result
     */
    private AnalysisResult generateAnalysisResult(String symbol, List<TechnicalIndicator> indicators) {
        AnalysisResult.AnalysisResultBuilder builder = AnalysisResult.builder()
                .symbol(symbol)
                .timestamp(LocalDateTime.now());

        // Group indicators by type
        Map<String, TechnicalIndicator> indicatorMap = new HashMap<>();
        for (TechnicalIndicator indicator : indicators) {
            String key = indicator.getIndicatorType() + "_" + (indicator.getPeriod() != null ? indicator.getPeriod() : "0");
            indicatorMap.put(key, indicator);
        }

        // Extract current price (from latest close price or indicator)
        BigDecimal currentPrice = extractCurrentPrice(indicators);
        builder.currentPrice(currentPrice);

        // Analyze trend signals
        String trendSignal = analyzeTrendSignals(indicatorMap, currentPrice);
        builder.trendSignal(trendSignal);

        // Analyze momentum signals
        String momentumSignal = analyzeMomentumSignals(indicatorMap);
        builder.momentumSignal(momentumSignal);

        // Analyze volatility signals
        String volatilitySignal = analyzeVolatilitySignals(indicatorMap);
        builder.volatilitySignal(volatilitySignal);

        // Generate overall signal
        String overallSignal = generateOverallSignal(trendSignal, momentumSignal, volatilitySignal);
        builder.overallSignal(overallSignal);

        // Calculate confidence score
        int confidenceScore = calculateConfidenceScore(indicatorMap, trendSignal, momentumSignal);
        builder.confidenceScore(confidenceScore);

        return builder.build();
    }

    /**
     * Extract current price from indicators
     */
    private BigDecimal extractCurrentPrice(List<TechnicalIndicator> indicators) {
        // Try to find the most recent close price or use SMA as fallback
        return indicators.stream()
                .filter(i -> "SMA".equals(i.getIndicatorType()) && i.getPeriod() != null && i.getPeriod() == 5)
                .findFirst()
                .map(TechnicalIndicator::getIndicatorValue)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Analyze trend signals from moving averages and MACD
     */
    private String analyzeTrendSignals(Map<String, TechnicalIndicator> indicators, BigDecimal currentPrice) {
        int bullishSignals = 0;
        int bearishSignals = 0;

        // Check moving average alignment
        TechnicalIndicator sma20 = indicators.get("SMA_20");
        TechnicalIndicator sma50 = indicators.get("SMA_50");
        TechnicalIndicator ema20 = indicators.get("EMA_20");

        if (sma20 != null && sma50 != null) {
            if (sma20.getIndicatorValue().compareTo(sma50.getIndicatorValue()) > 0) {
                bullishSignals++;
            } else {
                bearishSignals++;
            }
        }

        // Check price vs moving averages
        if (sma20 != null && currentPrice.compareTo(sma20.getIndicatorValue()) > 0) {
            bullishSignals++;
        } else if (sma20 != null) {
            bearishSignals++;
        }

        // Check MACD
        TechnicalIndicator macd = indicators.get("MACD_0");
        TechnicalIndicator macdSignal = indicators.get("MACD_SIGNAL_9");
        if (macd != null && macdSignal != null) {
            if (macd.getIndicatorValue().compareTo(macdSignal.getIndicatorValue()) > 0) {
                bullishSignals++;
            } else {
                bearishSignals++;
            }
        }

        if (bullishSignals > bearishSignals) {
            return "BULLISH";
        } else if (bearishSignals > bullishSignals) {
            return "BEARISH";
        } else {
            return "NEUTRAL";
        }
    }

    /**
     * Analyze momentum signals from RSI and Stochastic
     */
    private String analyzeMomentumSignals(Map<String, TechnicalIndicator> indicators) {
        // Check RSI
        TechnicalIndicator rsi = indicators.get("RSI_14");
        if (rsi != null) {
            BigDecimal rsiValue = rsi.getIndicatorValue();
            if (rsiValue.compareTo(BigDecimal.valueOf(70)) > 0) {
                return "OVERBOUGHT";
            } else if (rsiValue.compareTo(BigDecimal.valueOf(30)) < 0) {
                return "OVERSOLD";
            }
        }

        // Check Stochastic
        TechnicalIndicator stochK = indicators.get("STOCH_14");
        if (stochK != null) {
            BigDecimal stochValue = stochK.getIndicatorValue();
            if (stochValue.compareTo(BigDecimal.valueOf(80)) > 0) {
                return "OVERBOUGHT";
            } else if (stochValue.compareTo(BigDecimal.valueOf(20)) < 0) {
                return "OVERSOLD";
            }
        }

        return "NEUTRAL";
    }

    /**
     * Analyze volatility signals from ATR and Bollinger Bands
     */
    private String analyzeVolatilitySignals(Map<String, TechnicalIndicator> indicators) {
        TechnicalIndicator atr = indicators.get("ATR_14");
        if (atr != null) {
            // This is a simplified volatility analysis
            // In practice, you'd compare ATR to historical values
            BigDecimal atrValue = atr.getIndicatorValue();
            if (atrValue.compareTo(BigDecimal.valueOf(0.02)) > 0) { // 2% threshold
                return "HIGH";
            } else if (atrValue.compareTo(BigDecimal.valueOf(0.005)) < 0) { // 0.5% threshold
                return "LOW";
            }
        }

        return "NORMAL";
    }

    /**
     * Generate overall trading signal
     */
    private String generateOverallSignal(String trendSignal, String momentumSignal, String volatilitySignal) {
        if ("BULLISH".equals(trendSignal) && "OVERSOLD".equals(momentumSignal)) {
            return "STRONG_BUY";
        } else if ("BULLISH".equals(trendSignal)) {
            return "BUY";
        } else if ("BEARISH".equals(trendSignal) && "OVERBOUGHT".equals(momentumSignal)) {
            return "STRONG_SELL";
        } else if ("BEARISH".equals(trendSignal)) {
            return "SELL";
        } else {
            return "HOLD";
        }
    }

    /**
     * Calculate confidence score based on signal alignment
     */
    private int calculateConfidenceScore(Map<String, TechnicalIndicator> indicators, 
                                       String trendSignal, String momentumSignal) {
        int score = 50; // Base score

        // Increase confidence if multiple indicators align
        if (!"NEUTRAL".equals(trendSignal)) {
            score += 20;
        }
        if (!"NEUTRAL".equals(momentumSignal)) {
            score += 15;
        }

        // Check indicator availability
        if (indicators.containsKey("RSI_14")) score += 5;
        if (indicators.containsKey("MACD_0")) score += 5;
        if (indicators.containsKey("SMA_20")) score += 5;

        return Math.min(100, Math.max(0, score));
    }

    /**
     * Publish indicators to Kafka
     */
    private void publishIndicators(String symbol, List<IndicatorResultDto> indicators) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(indicatorsTopic, symbol, indicators);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published indicators for symbol: {}", symbol);
                } else {
                    log.error("Failed to publish indicators for symbol {}: {}", symbol, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing indicators for symbol {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Publish analysis result to Kafka
     */
    private void publishAnalysisResult(String symbol, AnalysisResult analysisResult) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(indicatorsTopic, symbol + "_ANALYSIS", analysisResult);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published analysis result for symbol: {}", symbol);
                } else {
                    log.error("Failed to publish analysis result for symbol {}: {}", symbol, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing analysis result for symbol {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Check for alerts and publish if necessary
     */
    private void checkAndPublishAlerts(String symbol, AnalysisResult analysisResult) {
        try {
            // Check for strong signals
            if ("STRONG_BUY".equals(analysisResult.getOverallSignal()) || 
                "STRONG_SELL".equals(analysisResult.getOverallSignal())) {
                
                Alert alert = Alert.builder()
                        .symbol(symbol)
                        .alertType("STRONG_SIGNAL")
                        .message(String.format("Strong %s signal detected for %s", 
                                analysisResult.getOverallSignal(), symbol))
                        .signal(analysisResult.getOverallSignal())
                        .confidenceScore(analysisResult.getConfidenceScore())
                        .timestamp(LocalDateTime.now())
                        .build();

                publishAlert(alert);
            }

            // Check for extreme RSI conditions
            if ("OVERBOUGHT".equals(analysisResult.getMomentumSignal()) || 
                "OVERSOLD".equals(analysisResult.getMomentumSignal())) {
                
                Alert alert = Alert.builder()
                        .symbol(symbol)
                        .alertType("MOMENTUM_EXTREME")
                        .message(String.format("%s condition detected for %s", 
                                analysisResult.getMomentumSignal(), symbol))
                        .signal(analysisResult.getMomentumSignal())
                        .confidenceScore(analysisResult.getConfidenceScore())
                        .timestamp(LocalDateTime.now())
                        .build();

                publishAlert(alert);
            }

        } catch (Exception e) {
            log.error("Error checking alerts for symbol {}: {}", symbol, e.getMessage());
        }
    }

    /**
     * Publish alert to Kafka
     */
    private void publishAlert(Alert alert) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(alertsTopic, alert.getSymbol(), alert);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published alert: {} for symbol: {}", alert.getAlertType(), alert.getSymbol());
                } else {
                    log.error("Failed to publish alert for symbol {}: {}", alert.getSymbol(), ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error publishing alert: {}", e.getMessage());
        }
    }

    // Data classes

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnalysisResult {
        private String symbol;
        private LocalDateTime timestamp;
        private BigDecimal currentPrice;
        private String trendSignal;
        private String momentumSignal;
        private String volatilitySignal;
        private String overallSignal;
        private Integer confidenceScore;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Alert {
        private String symbol;
        private String alertType;
        private String message;
        private String signal;
        private Integer confidenceScore;
        private LocalDateTime timestamp;
    }
}

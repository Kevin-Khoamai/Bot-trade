package com.cryptotrading.strategy.service;

import com.cryptotrading.strategy.model.StrategyExecution;
import com.cryptotrading.strategy.model.TradingSignal;
import com.cryptotrading.strategy.model.TradingStrategy;
import com.cryptotrading.strategy.repository.StrategyExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for risk management and position validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskManagementService {

    private final StrategyExecutionRepository executionRepository;

    @Value("${risk.max-position-size:10000}")
    private BigDecimal maxGlobalPositionSize;

    @Value("${risk.max-daily-loss:1000}")
    private BigDecimal maxGlobalDailyLoss;

    @Value("${risk.max-drawdown-percent:20}")
    private BigDecimal maxDrawdownPercent;

    @Value("${risk.min-risk-reward-ratio:1.5}")
    private BigDecimal minRiskRewardRatio;

    @Value("${risk.max-correlation-exposure:0.7}")
    private BigDecimal maxCorrelationExposure;

    /**
     * Validate trading signal against risk management rules
     */
    public boolean validateSignal(TradingStrategy strategy, TradingSignal signal) {
        try {
            log.debug("Validating signal for strategy: {} signal: {}", strategy.getName(), signal.getSignalType());

            // Basic signal validation
            if (!signal.isValid()) {
                log.warn("Invalid signal parameters for strategy: {}", strategy.getName());
                return false;
            }

            // Check strategy-specific risk limits
            if (!validateStrategyRiskLimits(strategy, signal)) {
                log.warn("Signal violates strategy risk limits for: {}", strategy.getName());
                return false;
            }

            // Check position size limits
            if (!validatePositionSize(strategy, signal)) {
                log.warn("Signal violates position size limits for: {}", strategy.getName());
                return false;
            }

            // Check daily loss limits
            if (!validateDailyLossLimits(strategy, signal)) {
                log.warn("Signal violates daily loss limits for: {}", strategy.getName());
                return false;
            }

            // Check risk-reward ratio
            if (!validateRiskRewardRatio(signal)) {
                log.warn("Signal has poor risk-reward ratio for: {}", strategy.getName());
                return false;
            }

            // Check maximum drawdown
            if (!validateMaxDrawdown(strategy)) {
                log.warn("Strategy exceeds maximum drawdown: {}", strategy.getName());
                return false;
            }

            // Check correlation exposure
            if (!validateCorrelationExposure(strategy, signal)) {
                log.warn("Signal increases correlation exposure beyond limits: {}", strategy.getName());
                return false;
            }

            // Check if strategy is in error state
            if (strategy.getStatus() == TradingStrategy.StrategyStatus.ERROR) {
                log.warn("Strategy is in error state: {}", strategy.getName());
                return false;
            }

            log.debug("Signal validation passed for strategy: {}", strategy.getName());
            return true;

        } catch (Exception e) {
            log.error("Error validating signal for strategy {}: {}", strategy.getName(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validate strategy-specific risk limits
     */
    private boolean validateStrategyRiskLimits(TradingStrategy strategy, TradingSignal signal) {
        // Check maximum position size for strategy
        if (strategy.getMaxPositionSize() != null) {
            if (signal.getQuantity().compareTo(strategy.getMaxPositionSize()) > 0) {
                log.warn("Signal quantity {} exceeds strategy max position size {} for: {}", 
                        signal.getQuantity(), strategy.getMaxPositionSize(), strategy.getName());
                return false;
            }
        }

        // Check strategy daily loss limit
        if (strategy.getMaxDailyLoss() != null) {
            BigDecimal todayLoss = calculateTodayLoss(strategy);
            BigDecimal potentialLoss = calculatePotentialLoss(signal);
            
            if (todayLoss.add(potentialLoss).compareTo(strategy.getMaxDailyLoss()) > 0) {
                log.warn("Signal would exceed daily loss limit for strategy: {}", strategy.getName());
                return false;
            }
        }

        return true;
    }

    /**
     * Validate position size limits
     */
    private boolean validatePositionSize(TradingStrategy strategy, TradingSignal signal) {
        // Check global position size limit
        if (signal.getQuantity().multiply(signal.getEntryPrice()).compareTo(maxGlobalPositionSize) > 0) {
            log.warn("Signal position value exceeds global limit: {} > {}", 
                    signal.getQuantity().multiply(signal.getEntryPrice()), maxGlobalPositionSize);
            return false;
        }

        // Check existing exposure for this symbol
        List<StrategyExecution> openPositions = executionRepository.findOpenPositionsBySymbol(signal.getSymbol());
        BigDecimal currentExposure = openPositions.stream()
                .map(pos -> pos.getQuantity().multiply(pos.getEntryPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newExposure = signal.getQuantity().multiply(signal.getEntryPrice());
        BigDecimal totalExposure = currentExposure.add(newExposure);

        // Limit total exposure per symbol to 50% of max position size
        BigDecimal maxSymbolExposure = maxGlobalPositionSize.multiply(BigDecimal.valueOf(0.5));
        if (totalExposure.compareTo(maxSymbolExposure) > 0) {
            log.warn("Total exposure for symbol {} would exceed limit: {} > {}", 
                    signal.getSymbol(), totalExposure, maxSymbolExposure);
            return false;
        }

        return true;
    }

    /**
     * Validate daily loss limits
     */
    private boolean validateDailyLossLimits(TradingStrategy strategy, TradingSignal signal) {
        BigDecimal todayLoss = calculateTodayLoss(strategy);
        
        // Check global daily loss limit
        if (todayLoss.compareTo(maxGlobalDailyLoss) > 0) {
            log.warn("Strategy {} already exceeded global daily loss limit: {} > {}", 
                    strategy.getName(), todayLoss, maxGlobalDailyLoss);
            return false;
        }

        return true;
    }

    /**
     * Validate risk-reward ratio
     */
    private boolean validateRiskRewardRatio(TradingSignal signal) {
        BigDecimal riskRewardRatio = signal.calculateRiskRewardRatio();
        
        if (riskRewardRatio == null) {
            // If we can't calculate risk-reward, allow the signal but log warning
            log.warn("Cannot calculate risk-reward ratio for signal: {}", signal.getSymbol());
            return true;
        }

        if (riskRewardRatio.compareTo(minRiskRewardRatio) < 0) {
            log.warn("Signal risk-reward ratio {} is below minimum {}", riskRewardRatio, minRiskRewardRatio);
            return false;
        }

        return true;
    }

    /**
     * Validate maximum drawdown
     */
    private boolean validateMaxDrawdown(TradingStrategy strategy) {
        if (strategy.getMaxDrawdown() == null) {
            return true;
        }

        if (strategy.getMaxDrawdown().compareTo(maxDrawdownPercent) > 0) {
            log.warn("Strategy {} drawdown {} exceeds maximum {}", 
                    strategy.getName(), strategy.getMaxDrawdown(), maxDrawdownPercent);
            return false;
        }

        return true;
    }

    /**
     * Validate correlation exposure
     */
    private boolean validateCorrelationExposure(TradingStrategy strategy, TradingSignal signal) {
        // This is a simplified correlation check
        // In a real implementation, you would calculate actual correlations between symbols
        
        List<StrategyExecution> openPositions = executionRepository.findOpenPositions();
        
        // Count positions in the same asset class (simplified: same base currency)
        String baseCurrency = extractBaseCurrency(signal.getSymbol());
        long correlatedPositions = openPositions.stream()
                .filter(pos -> extractBaseCurrency(pos.getSymbol()).equals(baseCurrency))
                .count();

        // Limit to maximum number of correlated positions
        int maxCorrelatedPositions = 5; // Configurable
        if (correlatedPositions >= maxCorrelatedPositions) {
            log.warn("Too many correlated positions for base currency {}: {}", baseCurrency, correlatedPositions);
            return false;
        }

        return true;
    }

    /**
     * Calculate today's loss for a strategy
     */
    private BigDecimal calculateTodayLoss(TradingStrategy strategy) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<StrategyExecution> todayExecutions = executionRepository
                .findExecutionsByStrategyBetween(strategy, startOfDay, endOfDay);

        return todayExecutions.stream()
                .filter(exec -> exec.getNetPnl() != null && exec.getNetPnl().compareTo(BigDecimal.ZERO) < 0)
                .map(StrategyExecution::getNetPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
    }

    /**
     * Calculate potential loss from a signal
     */
    private BigDecimal calculatePotentialLoss(TradingSignal signal) {
        if (signal.getStopLossPrice() == null || signal.getEntryPrice() == null) {
            // If no stop loss, assume 5% potential loss
            return signal.getQuantity().multiply(signal.getEntryPrice()).multiply(BigDecimal.valueOf(0.05));
        }

        BigDecimal priceDiff = signal.getEntryPrice().subtract(signal.getStopLossPrice()).abs();
        return signal.getQuantity().multiply(priceDiff);
    }

    /**
     * Extract base currency from symbol (e.g., BTCUSDT -> BTC)
     */
    private String extractBaseCurrency(String symbol) {
        // Simplified extraction - in reality, you'd have a proper symbol parser
        if (symbol.endsWith("USDT")) {
            return symbol.substring(0, symbol.length() - 4);
        } else if (symbol.endsWith("USD")) {
            return symbol.substring(0, symbol.length() - 3);
        } else if (symbol.contains("-")) {
            return symbol.split("-")[0];
        }
        return symbol;
    }

    /**
     * Calculate position size based on risk percentage
     */
    public BigDecimal calculatePositionSize(TradingStrategy strategy, TradingSignal signal, 
                                          BigDecimal accountBalance, BigDecimal riskPercent) {
        try {
            if (signal.getStopLossPrice() == null || signal.getEntryPrice() == null) {
                // If no stop loss defined, use default position size
                return strategy.getMaxPositionSize() != null 
                    ? strategy.getMaxPositionSize() 
                    : BigDecimal.valueOf(100); // Default quantity
            }

            BigDecimal riskAmount = accountBalance.multiply(riskPercent.divide(BigDecimal.valueOf(100)));
            BigDecimal priceRisk = signal.getEntryPrice().subtract(signal.getStopLossPrice()).abs();

            if (priceRisk.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.valueOf(100); // Default if no price risk
            }

            BigDecimal calculatedSize = riskAmount.divide(priceRisk, 8, RoundingMode.HALF_UP);

            // Apply maximum position size limit
            if (strategy.getMaxPositionSize() != null) {
                calculatedSize = calculatedSize.min(strategy.getMaxPositionSize());
            }

            return calculatedSize;

        } catch (Exception e) {
            log.error("Error calculating position size for strategy {}: {}", strategy.getName(), e.getMessage());
            return BigDecimal.valueOf(100); // Default fallback
        }
    }

    /**
     * Check if strategy should be halted due to poor performance
     */
    public boolean shouldHaltStrategy(TradingStrategy strategy) {
        // Halt if drawdown exceeds limit
        if (strategy.getMaxDrawdown() != null && 
            strategy.getMaxDrawdown().compareTo(maxDrawdownPercent) > 0) {
            return true;
        }

        // Halt if daily loss exceeds limit
        BigDecimal todayLoss = calculateTodayLoss(strategy);
        if (todayLoss.compareTo(maxGlobalDailyLoss) > 0) {
            return true;
        }

        // Halt if win rate is too low (less than 20% with more than 20 trades)
        if (strategy.getTotalTrades() != null && strategy.getTotalTrades() > 20) {
            BigDecimal winRate = strategy.getWinRate();
            if (winRate.compareTo(BigDecimal.valueOf(20)) < 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get risk assessment for a strategy
     */
    public RiskAssessment assessStrategyRisk(TradingStrategy strategy) {
        BigDecimal todayLoss = calculateTodayLoss(strategy);
        BigDecimal winRate = strategy.getWinRate();
        BigDecimal drawdown = strategy.getMaxDrawdown() != null ? strategy.getMaxDrawdown() : BigDecimal.ZERO;

        RiskLevel riskLevel = RiskLevel.LOW;
        
        if (drawdown.compareTo(BigDecimal.valueOf(15)) > 0 || 
            todayLoss.compareTo(maxGlobalDailyLoss.multiply(BigDecimal.valueOf(0.8))) > 0) {
            riskLevel = RiskLevel.HIGH;
        } else if (drawdown.compareTo(BigDecimal.valueOf(10)) > 0 || 
                   winRate.compareTo(BigDecimal.valueOf(40)) < 0) {
            riskLevel = RiskLevel.MEDIUM;
        }

        return RiskAssessment.builder()
                .strategy(strategy)
                .riskLevel(riskLevel)
                .currentDrawdown(drawdown)
                .todayLoss(todayLoss)
                .winRate(winRate)
                .recommendation(generateRiskRecommendation(riskLevel, strategy))
                .build();
    }

    /**
     * Generate risk recommendation
     */
    private String generateRiskRecommendation(RiskLevel riskLevel, TradingStrategy strategy) {
        return switch (riskLevel) {
            case HIGH -> "Consider halting strategy or reducing position sizes";
            case MEDIUM -> "Monitor closely and consider reducing risk";
            case LOW -> "Strategy operating within acceptable risk parameters";
        };
    }

    /**
     * Risk level enumeration
     */
    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }

    /**
     * Risk assessment result
     */
    @lombok.Data
    @lombok.Builder
    public static class RiskAssessment {
        private TradingStrategy strategy;
        private RiskLevel riskLevel;
        private BigDecimal currentDrawdown;
        private BigDecimal todayLoss;
        private BigDecimal winRate;
        private String recommendation;
    }
}

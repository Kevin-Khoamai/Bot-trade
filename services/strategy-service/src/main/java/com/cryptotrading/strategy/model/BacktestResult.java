package com.cryptotrading.strategy.model;

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
 * Entity representing backtest results for a trading strategy
 */
@Entity
@Table(name = "backtest_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id", nullable = false)
    private TradingStrategy strategy;

    @Column(name = "backtest_name", nullable = false)
    private String backtestName;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    // Backtest Period
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "initial_capital", precision = 20, scale = 8, nullable = false)
    private BigDecimal initialCapital;

    // Performance Metrics
    @Column(name = "final_capital", precision = 20, scale = 8)
    private BigDecimal finalCapital;

    @Column(name = "total_return", precision = 10, scale = 4)
    private BigDecimal totalReturn; // Percentage

    @Column(name = "annualized_return", precision = 10, scale = 4)
    private BigDecimal annualizedReturn; // Percentage

    @Column(name = "total_trades")
    private Integer totalTrades;

    @Column(name = "winning_trades")
    private Integer winningTrades;

    @Column(name = "losing_trades")
    private Integer losingTrades;

    @Column(name = "win_rate", precision = 5, scale = 2)
    private BigDecimal winRate; // Percentage

    @Column(name = "average_win", precision = 20, scale = 8)
    private BigDecimal averageWin;

    @Column(name = "average_loss", precision = 20, scale = 8)
    private BigDecimal averageLoss;

    @Column(name = "largest_win", precision = 20, scale = 8)
    private BigDecimal largestWin;

    @Column(name = "largest_loss", precision = 20, scale = 8)
    private BigDecimal largestLoss;

    @Column(name = "profit_factor", precision = 10, scale = 4)
    private BigDecimal profitFactor;

    // Risk Metrics
    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown; // Percentage

    @Column(name = "max_drawdown_duration_days")
    private Integer maxDrawdownDurationDays;

    @Column(name = "volatility", precision = 10, scale = 4)
    private BigDecimal volatility; // Annualized volatility

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "sortino_ratio", precision = 10, scale = 4)
    private BigDecimal sortinoRatio;

    @Column(name = "calmar_ratio", precision = 10, scale = 4)
    private BigDecimal calmarRatio;

    @Column(name = "var_95", precision = 20, scale = 8)
    private BigDecimal var95; // Value at Risk 95%

    @Column(name = "cvar_95", precision = 20, scale = 8)
    private BigDecimal cvar95; // Conditional Value at Risk 95%

    // Benchmark Comparison
    @Column(name = "benchmark_return", precision = 10, scale = 4)
    private BigDecimal benchmarkReturn; // Buy and hold return

    @Column(name = "alpha", precision = 10, scale = 4)
    private BigDecimal alpha; // Excess return vs benchmark

    @Column(name = "beta", precision = 10, scale = 4)
    private BigDecimal beta; // Correlation with benchmark

    @Column(name = "information_ratio", precision = 10, scale = 4)
    private BigDecimal informationRatio;

    // Execution Statistics
    @Column(name = "average_trade_duration_hours")
    private BigDecimal averageTradeDurationHours;

    @Column(name = "total_fees", precision = 20, scale = 8)
    private BigDecimal totalFees;

    @Column(name = "slippage_impact", precision = 20, scale = 8)
    private BigDecimal slippageImpact;

    // Configuration
    @Column(name = "backtest_config", columnDefinition = "TEXT")
    private String backtestConfig; // JSON configuration used

    @Column(name = "market_conditions", columnDefinition = "TEXT")
    private String marketConditions; // JSON market analysis during period

    // Status and Metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BacktestStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "execution_time_seconds")
    private Long executionTimeSeconds;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Backtest status
     */
    public enum BacktestStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Calculate profit factor (gross profit / gross loss)
     */
    public BigDecimal calculateProfitFactor() {
        if (averageLoss == null || averageLoss.compareTo(BigDecimal.ZERO) == 0 || losingTrades == null || losingTrades == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal grossProfit = averageWin != null && winningTrades != null 
            ? averageWin.multiply(BigDecimal.valueOf(winningTrades))
            : BigDecimal.ZERO;

        BigDecimal grossLoss = averageLoss.multiply(BigDecimal.valueOf(losingTrades)).abs();

        return grossLoss.compareTo(BigDecimal.ZERO) == 0 
            ? BigDecimal.ZERO 
            : grossProfit.divide(grossLoss, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate win rate percentage
     */
    public BigDecimal calculateWinRate() {
        if (totalTrades == null || totalTrades == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(winningTrades != null ? winningTrades : 0)
                .divide(BigDecimal.valueOf(totalTrades), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate total return percentage
     */
    public BigDecimal calculateTotalReturn() {
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal profit = finalCapital != null 
            ? finalCapital.subtract(initialCapital)
            : BigDecimal.ZERO;

        return profit.divide(initialCapital, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Check if backtest was profitable
     */
    public boolean isProfitable() {
        return finalCapital != null && initialCapital != null 
            && finalCapital.compareTo(initialCapital) > 0;
    }

    /**
     * Check if backtest outperformed benchmark
     */
    public boolean outperformedBenchmark() {
        if (totalReturn == null || benchmarkReturn == null) {
            return false;
        }
        return totalReturn.compareTo(benchmarkReturn) > 0;
    }

    /**
     * Get risk-adjusted return (Sharpe ratio category)
     */
    public String getRiskAdjustedReturnCategory() {
        if (sharpeRatio == null) {
            return "Unknown";
        }

        if (sharpeRatio.compareTo(BigDecimal.valueOf(2.0)) >= 0) {
            return "Excellent";
        } else if (sharpeRatio.compareTo(BigDecimal.valueOf(1.0)) >= 0) {
            return "Good";
        } else if (sharpeRatio.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
            return "Fair";
        } else if (sharpeRatio.compareTo(BigDecimal.ZERO) >= 0) {
            return "Poor";
        } else {
            return "Very Poor";
        }
    }

    /**
     * Complete the backtest
     */
    public void complete() {
        this.status = BacktestStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        
        // Calculate derived metrics
        this.winRate = calculateWinRate();
        this.totalReturn = calculateTotalReturn();
        this.profitFactor = calculateProfitFactor();
    }

    /**
     * Mark backtest as failed
     */
    public void fail(String errorMessage) {
        this.status = BacktestStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}

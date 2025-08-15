package com.cryptotrading.strategy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a trading strategy
 */
@Entity
@Table(name = "trading_strategies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StrategyType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StrategyStatus status;

    @Column(name = "symbol")
    private String symbol; // e.g., BTCUSDT

    @Column(name = "timeframe")
    private String timeframe; // e.g., 1m, 5m, 1h

    // Risk Management Parameters
    @Column(name = "max_position_size", precision = 20, scale = 8)
    private BigDecimal maxPositionSize;

    @Column(name = "stop_loss_percent", precision = 5, scale = 2)
    private BigDecimal stopLossPercent;

    @Column(name = "take_profit_percent", precision = 5, scale = 2)
    private BigDecimal takeProfitPercent;

    @Column(name = "max_daily_loss", precision = 20, scale = 8)
    private BigDecimal maxDailyLoss;

    // Strategy Configuration (JSON)
    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;

    // Drools Rules
    @Column(name = "entry_rules", columnDefinition = "TEXT")
    private String entryRules;

    @Column(name = "exit_rules", columnDefinition = "TEXT")
    private String exitRules;

    // Performance Metrics
    @Column(name = "total_trades")
    private Integer totalTrades = 0;

    @Column(name = "winning_trades")
    private Integer winningTrades = 0;

    @Column(name = "total_pnl", precision = 20, scale = 8)
    private BigDecimal totalPnl = BigDecimal.ZERO;

    @Column(name = "max_drawdown", precision = 5, scale = 2)
    private BigDecimal maxDrawdown = BigDecimal.ZERO;

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio = BigDecimal.ZERO;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    // Relationships
    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<StrategyExecution> executions = new ArrayList<>();

    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BacktestResult> backtestResults = new ArrayList<>();

    /**
     * Strategy types
     */
    public enum StrategyType {
        MOMENTUM,           // Trend following strategies
        MEAN_REVERSION,     // Mean reversion strategies
        ARBITRAGE,          // Arbitrage strategies
        SCALPING,           // High-frequency scalping
        SWING_TRADING,      // Swing trading strategies
        PREDICTION_BASED,   // ML/AI prediction based
        CUSTOM              // Custom user-defined strategies
    }

    /**
     * Strategy status
     */
    public enum StrategyStatus {
        DRAFT,              // Strategy being developed
        BACKTESTING,        // Currently backtesting
        PAPER_TRADING,      // Paper trading mode
        LIVE,               // Live trading
        PAUSED,             // Temporarily paused
        STOPPED,            // Permanently stopped
        ERROR               // Error state
    }

    /**
     * Check if strategy is active (can execute trades)
     */
    public boolean isActive() {
        return status == StrategyStatus.LIVE || status == StrategyStatus.PAPER_TRADING;
    }

    /**
     * Check if strategy can be backtested
     */
    public boolean canBacktest() {
        return status == StrategyStatus.DRAFT || 
               status == StrategyStatus.BACKTESTING || 
               status == StrategyStatus.PAUSED;
    }

    /**
     * Calculate win rate
     */
    public BigDecimal getWinRate() {
        if (totalTrades == null || totalTrades == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(winningTrades)
                .divide(BigDecimal.valueOf(totalTrades), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Update performance metrics
     */
    public void updatePerformanceMetrics(BigDecimal pnl, boolean isWinning) {
        this.totalTrades = (this.totalTrades == null ? 0 : this.totalTrades) + 1;
        this.totalPnl = (this.totalPnl == null ? BigDecimal.ZERO : this.totalPnl).add(pnl);
        
        if (isWinning) {
            this.winningTrades = (this.winningTrades == null ? 0 : this.winningTrades) + 1;
        }
        
        this.lastExecutedAt = LocalDateTime.now();
    }

    /**
     * Reset performance metrics (for new backtesting)
     */
    public void resetPerformanceMetrics() {
        this.totalTrades = 0;
        this.winningTrades = 0;
        this.totalPnl = BigDecimal.ZERO;
        this.maxDrawdown = BigDecimal.ZERO;
        this.sharpeRatio = BigDecimal.ZERO;
    }
}

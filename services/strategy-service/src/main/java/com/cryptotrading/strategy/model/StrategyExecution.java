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
 * Entity representing a strategy execution/trade
 */
@Entity
@Table(name = "strategy_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id", nullable = false)
    private TradingStrategy strategy;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_type", nullable = false)
    private ExecutionType executionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private TradeSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status;

    // Trade Details
    @Column(name = "quantity", precision = 20, scale = 8, nullable = false)
    private BigDecimal quantity;

    @Column(name = "entry_price", precision = 20, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "exit_price", precision = 20, scale = 8)
    private BigDecimal exitPrice;

    @Column(name = "stop_loss_price", precision = 20, scale = 8)
    private BigDecimal stopLossPrice;

    @Column(name = "take_profit_price", precision = 20, scale = 8)
    private BigDecimal takeProfitPrice;

    // P&L Calculation
    @Column(name = "realized_pnl", precision = 20, scale = 8)
    private BigDecimal realizedPnl;

    @Column(name = "unrealized_pnl", precision = 20, scale = 8)
    private BigDecimal unrealizedPnl;

    @Column(name = "fees", precision = 20, scale = 8)
    private BigDecimal fees;

    @Column(name = "net_pnl", precision = 20, scale = 8)
    private BigDecimal netPnl;

    // Execution Context
    @Column(name = "trigger_reason", length = 500)
    private String triggerReason;

    @Column(name = "market_conditions", columnDefinition = "TEXT")
    private String marketConditions; // JSON with indicators at execution time

    @Column(name = "execution_context", columnDefinition = "TEXT")
    private String executionContext; // JSON with additional context

    // External References
    @Column(name = "exchange_order_id")
    private String exchangeOrderId;

    @Column(name = "exchange")
    private String exchange;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Execution types
     */
    public enum ExecutionType {
        LIVE_TRADING,       // Real money trading
        PAPER_TRADING,      // Simulated trading
        BACKTEST           // Historical backtesting
    }

    /**
     * Trade sides
     */
    public enum TradeSide {
        BUY,
        SELL
    }

    /**
     * Execution status
     */
    public enum ExecutionStatus {
        PENDING,            // Order placed but not filled
        PARTIALLY_FILLED,   // Partially executed
        FILLED,             // Fully executed (entry)
        OPEN,               // Position is open
        CLOSED,             // Position closed
        CANCELLED,          // Order cancelled
        REJECTED,           // Order rejected
        ERROR               // Error occurred
    }

    /**
     * Check if execution is a winning trade
     */
    public boolean isWinningTrade() {
        return netPnl != null && netPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if position is open
     */
    public boolean isPositionOpen() {
        return status == ExecutionStatus.OPEN || status == ExecutionStatus.FILLED;
    }

    /**
     * Calculate current P&L based on current market price
     */
    public BigDecimal calculateUnrealizedPnl(BigDecimal currentPrice) {
        if (entryPrice == null || currentPrice == null || !isPositionOpen()) {
            return BigDecimal.ZERO;
        }

        BigDecimal priceDiff = side == TradeSide.BUY 
            ? currentPrice.subtract(entryPrice)
            : entryPrice.subtract(currentPrice);

        return priceDiff.multiply(quantity);
    }

    /**
     * Calculate realized P&L when position is closed
     */
    public BigDecimal calculateRealizedPnl() {
        if (entryPrice == null || exitPrice == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal priceDiff = side == TradeSide.BUY 
            ? exitPrice.subtract(entryPrice)
            : entryPrice.subtract(exitPrice);

        BigDecimal grossPnl = priceDiff.multiply(quantity);
        BigDecimal totalFees = fees != null ? fees : BigDecimal.ZERO;
        
        return grossPnl.subtract(totalFees);
    }

    /**
     * Update P&L calculations
     */
    public void updatePnl(BigDecimal currentPrice) {
        if (isPositionOpen()) {
            this.unrealizedPnl = calculateUnrealizedPnl(currentPrice);
        } else if (status == ExecutionStatus.CLOSED) {
            this.realizedPnl = calculateRealizedPnl();
            this.netPnl = this.realizedPnl;
            this.unrealizedPnl = BigDecimal.ZERO;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Close position
     */
    public void closePosition(BigDecimal exitPrice, String reason) {
        this.exitPrice = exitPrice;
        this.exitTime = LocalDateTime.now();
        this.status = ExecutionStatus.CLOSED;
        this.triggerReason = reason;
        
        this.realizedPnl = calculateRealizedPnl();
        this.netPnl = this.realizedPnl;
        this.unrealizedPnl = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Check if stop loss should be triggered
     */
    public boolean shouldTriggerStopLoss(BigDecimal currentPrice) {
        if (stopLossPrice == null || !isPositionOpen()) {
            return false;
        }

        return side == TradeSide.BUY 
            ? currentPrice.compareTo(stopLossPrice) <= 0
            : currentPrice.compareTo(stopLossPrice) >= 0;
    }

    /**
     * Check if take profit should be triggered
     */
    public boolean shouldTriggerTakeProfit(BigDecimal currentPrice) {
        if (takeProfitPrice == null || !isPositionOpen()) {
            return false;
        }

        return side == TradeSide.BUY 
            ? currentPrice.compareTo(takeProfitPrice) >= 0
            : currentPrice.compareTo(takeProfitPrice) <= 0;
    }
}

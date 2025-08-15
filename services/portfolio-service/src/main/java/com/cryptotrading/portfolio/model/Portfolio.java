package com.cryptotrading.portfolio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a trading portfolio
 */
@Entity
@Table(name = "portfolios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PortfolioType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PortfolioStatus status;

    // Financial Information
    @Column(name = "initial_capital", precision = 20, scale = 8, nullable = false)
    private BigDecimal initialCapital;

    @Column(name = "current_capital", precision = 20, scale = 8)
    private BigDecimal currentCapital;

    @Column(name = "available_cash", precision = 20, scale = 8)
    private BigDecimal availableCash;

    @Column(name = "locked_cash", precision = 20, scale = 8)
    private BigDecimal lockedCash = BigDecimal.ZERO;

    @Column(name = "total_value", precision = 20, scale = 8)
    private BigDecimal totalValue;

    @Column(name = "positions_value", precision = 20, scale = 8)
    private BigDecimal positionsValue = BigDecimal.ZERO;

    // P&L Information
    @Column(name = "total_pnl", precision = 20, scale = 8)
    private BigDecimal totalPnl = BigDecimal.ZERO;

    @Column(name = "unrealized_pnl", precision = 20, scale = 8)
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;

    @Column(name = "realized_pnl", precision = 20, scale = 8)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "daily_pnl", precision = 20, scale = 8)
    private BigDecimal dailyPnl = BigDecimal.ZERO;

    @Column(name = "total_return", precision = 10, scale = 4)
    private BigDecimal totalReturn = BigDecimal.ZERO;

    @Column(name = "daily_return", precision = 10, scale = 4)
    private BigDecimal dailyReturn = BigDecimal.ZERO;

    // Risk Metrics
    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown = BigDecimal.ZERO;

    @Column(name = "high_water_mark", precision = 20, scale = 8)
    private BigDecimal highWaterMark;

    @Column(name = "volatility", precision = 10, scale = 6)
    private BigDecimal volatility;

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "sortino_ratio", precision = 10, scale = 4)
    private BigDecimal sortinoRatio;

    @Column(name = "var_95", precision = 20, scale = 8)
    private BigDecimal var95;

    @Column(name = "cvar_95", precision = 20, scale = 8)
    private BigDecimal cvar95;

    // Trading Statistics
    @Column(name = "total_trades")
    private Integer totalTrades = 0;

    @Column(name = "winning_trades")
    private Integer winningTrades = 0;

    @Column(name = "losing_trades")
    private Integer losingTrades = 0;

    @Column(name = "total_fees", precision = 20, scale = 8)
    private BigDecimal totalFees = BigDecimal.ZERO;

    // Configuration
    @Column(name = "base_currency", length = 10)
    private String baseCurrency = "USDT";

    @Column(name = "risk_tolerance")
    private String riskTolerance; // CONSERVATIVE, MODERATE, AGGRESSIVE

    @Column(name = "max_position_size", precision = 20, scale = 8)
    private BigDecimal maxPositionSize;

    @Column(name = "max_daily_loss", precision = 20, scale = 8)
    private BigDecimal maxDailyLoss;

    @Column(name = "target_allocation", columnDefinition = "TEXT")
    private String targetAllocation; // JSON

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_valuation_at")
    private LocalDateTime lastValuationAt;

    // Relationships
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Position> positions = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PortfolioSnapshot> snapshots = new ArrayList<>();

    /**
     * Portfolio types
     */
    public enum PortfolioType {
        LIVE_TRADING,       // Real money trading
        PAPER_TRADING,      // Simulated trading
        BACKTESTING,        // Historical testing
        DEMO                // Demo account
    }

    /**
     * Portfolio status
     */
    public enum PortfolioStatus {
        ACTIVE,             // Portfolio is active
        PAUSED,             // Portfolio is paused
        CLOSED,             // Portfolio is closed
        LIQUIDATING         // Portfolio is being liquidated
    }

    /**
     * Update portfolio valuation
     */
    public void updateValuation() {
        // Calculate positions value
        this.positionsValue = positions.stream()
                .map(Position::getPositionValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total value
        this.totalValue = this.availableCash.add(this.positionsValue);

        // Calculate P&L
        this.unrealizedPnl = positions.stream()
                .map(Position::getUnrealizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalPnl = this.realizedPnl.add(this.unrealizedPnl);

        // Calculate returns
        if (this.initialCapital.compareTo(BigDecimal.ZERO) != 0) {
            this.totalReturn = this.totalPnl.divide(this.initialCapital, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Update high water mark and drawdown
        updateDrawdown();

        this.lastValuationAt = LocalDateTime.now();
    }

    /**
     * Update drawdown calculations
     */
    private void updateDrawdown() {
        if (this.highWaterMark == null) {
            this.highWaterMark = this.totalValue;
        }

        if (this.totalValue.compareTo(this.highWaterMark) > 0) {
            this.highWaterMark = this.totalValue;
        }

        if (this.highWaterMark.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentDrawdown = this.highWaterMark.subtract(this.totalValue)
                    .divide(this.highWaterMark, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (currentDrawdown.compareTo(this.maxDrawdown) > 0) {
                this.maxDrawdown = currentDrawdown;
            }
        }
    }

    /**
     * Add cash to portfolio
     */
    public void addCash(BigDecimal amount) {
        this.availableCash = this.availableCash.add(amount);
        this.currentCapital = this.currentCapital.add(amount);
        updateValuation();
    }

    /**
     * Remove cash from portfolio
     */
    public void removeCash(BigDecimal amount) {
        if (amount.compareTo(this.availableCash) > 0) {
            throw new IllegalArgumentException("Insufficient cash available");
        }
        this.availableCash = this.availableCash.subtract(amount);
        this.currentCapital = this.currentCapital.subtract(amount);
        updateValuation();
    }

    /**
     * Lock cash for pending orders
     */
    public void lockCash(BigDecimal amount) {
        if (amount.compareTo(this.availableCash) > 0) {
            throw new IllegalArgumentException("Insufficient cash to lock");
        }
        this.availableCash = this.availableCash.subtract(amount);
        this.lockedCash = this.lockedCash.add(amount);
    }

    /**
     * Unlock cash when orders are cancelled
     */
    public void unlockCash(BigDecimal amount) {
        this.lockedCash = this.lockedCash.subtract(amount);
        this.availableCash = this.availableCash.add(amount);
    }

    /**
     * Get position by symbol and exchange
     */
    public Position getPosition(String symbol, String exchange) {
        return positions.stream()
                .filter(p -> p.getSymbol().equals(symbol) && p.getExchange().equals(exchange))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add or update position
     */
    public void addOrUpdatePosition(Position position) {
        Position existingPosition = getPosition(position.getSymbol(), position.getExchange());
        if (existingPosition != null) {
            positions.remove(existingPosition);
        }
        positions.add(position);
        position.setPortfolio(this);
    }

    /**
     * Get win rate
     */
    public BigDecimal getWinRate() {
        if (totalTrades == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(winningTrades)
                .divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get profit factor
     */
    public BigDecimal getProfitFactor() {
        BigDecimal grossProfit = positions.stream()
                .filter(p -> p.getTotalPnl().compareTo(BigDecimal.ZERO) > 0)
                .map(Position::getTotalPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossLoss = positions.stream()
                .filter(p -> p.getTotalPnl().compareTo(BigDecimal.ZERO) < 0)
                .map(Position::getTotalPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();

        if (grossLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return grossProfit.divide(grossLoss, 4, RoundingMode.HALF_UP);
    }

    /**
     * Get cash utilization percentage
     */
    public BigDecimal getCashUtilization() {
        if (currentCapital.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal usedCash = currentCapital.subtract(availableCash);
        return usedCash.divide(currentCapital, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get portfolio leverage
     */
    public BigDecimal getLeverage() {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalExposure = positions.stream()
                .map(Position::getExposure)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalExposure.divide(totalValue, 4, RoundingMode.HALF_UP);
    }

    /**
     * Check if portfolio is healthy
     */
    public boolean isHealthy() {
        // Portfolio is healthy if:
        // 1. Not exceeding max drawdown
        // 2. Not exceeding daily loss limit
        // 3. Has positive or neutral performance trend
        
        boolean drawdownOk = maxDailyLoss == null || 
                           maxDrawdown.compareTo(BigDecimal.valueOf(20)) <= 0; // 20% max drawdown
        
        boolean dailyLossOk = maxDailyLoss == null || 
                            dailyPnl.abs().compareTo(maxDailyLoss) <= 0;
        
        return drawdownOk && dailyLossOk && status == PortfolioStatus.ACTIVE;
    }

    /**
     * Get number of open positions
     */
    public long getOpenPositionsCount() {
        return positions.stream()
                .filter(Position::isOpen)
                .count();
    }

    /**
     * Get total exposure
     */
    public BigDecimal getTotalExposure() {
        return positions.stream()
                .map(Position::getExposure)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

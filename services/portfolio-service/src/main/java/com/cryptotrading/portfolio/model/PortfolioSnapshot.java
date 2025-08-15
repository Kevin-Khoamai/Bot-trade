package com.cryptotrading.portfolio.model;

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
 * Entity representing a point-in-time snapshot of portfolio performance
 */
@Entity
@Table(name = "portfolio_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false)
    private SnapshotType snapshotType;

    // Portfolio Values
    @Column(name = "total_value", precision = 20, scale = 8, nullable = false)
    private BigDecimal totalValue;

    @Column(name = "cash_value", precision = 20, scale = 8)
    private BigDecimal cashValue;

    @Column(name = "positions_value", precision = 20, scale = 8)
    private BigDecimal positionsValue;

    @Column(name = "available_cash", precision = 20, scale = 8)
    private BigDecimal availableCash;

    @Column(name = "locked_cash", precision = 20, scale = 8)
    private BigDecimal lockedCash;

    // P&L Information
    @Column(name = "total_pnl", precision = 20, scale = 8)
    private BigDecimal totalPnl;

    @Column(name = "unrealized_pnl", precision = 20, scale = 8)
    private BigDecimal unrealizedPnl;

    @Column(name = "realized_pnl", precision = 20, scale = 8)
    private BigDecimal realizedPnl;

    @Column(name = "daily_pnl", precision = 20, scale = 8)
    private BigDecimal dailyPnl;

    @Column(name = "total_return", precision = 10, scale = 4)
    private BigDecimal totalReturn;

    @Column(name = "daily_return", precision = 10, scale = 4)
    private BigDecimal dailyReturn;

    // Risk Metrics
    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown;

    @Column(name = "volatility", precision = 10, scale = 6)
    private BigDecimal volatility;

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "var_95", precision = 20, scale = 8)
    private BigDecimal var95;

    @Column(name = "beta", precision = 10, scale = 4)
    private BigDecimal beta;

    // Position Information
    @Column(name = "open_positions_count")
    private Integer openPositionsCount;

    @Column(name = "total_exposure", precision = 20, scale = 8)
    private BigDecimal totalExposure;

    @Column(name = "leverage", precision = 10, scale = 4)
    private BigDecimal leverage;

    // Trading Statistics
    @Column(name = "total_trades")
    private Integer totalTrades;

    @Column(name = "winning_trades")
    private Integer winningTrades;

    @Column(name = "losing_trades")
    private Integer losingTrades;

    @Column(name = "win_rate", precision = 5, scale = 2)
    private BigDecimal winRate;

    @Column(name = "profit_factor", precision = 10, scale = 4)
    private BigDecimal profitFactor;

    @Column(name = "total_fees", precision = 20, scale = 8)
    private BigDecimal totalFees;

    // Market Context
    @Column(name = "market_conditions", columnDefinition = "TEXT")
    private String marketConditions; // JSON

    @Column(name = "benchmark_return", precision = 10, scale = 4)
    private BigDecimal benchmarkReturn;

    @Column(name = "alpha", precision = 10, scale = 4)
    private BigDecimal alpha;

    // Asset Allocation
    @Column(name = "asset_allocation", columnDefinition = "TEXT")
    private String assetAllocation; // JSON

    @Column(name = "sector_allocation", columnDefinition = "TEXT")
    private String sectorAllocation; // JSON

    @Column(name = "geographic_allocation", columnDefinition = "TEXT")
    private String geographicAllocation; // JSON

    // Timestamps
    @CreationTimestamp
    @Column(name = "snapshot_time")
    private LocalDateTime snapshotTime;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    /**
     * Snapshot types
     */
    public enum SnapshotType {
        REAL_TIME,      // Real-time snapshot
        HOURLY,         // Hourly snapshot
        DAILY,          // End of day snapshot
        WEEKLY,         // End of week snapshot
        MONTHLY,        // End of month snapshot
        QUARTERLY,      // End of quarter snapshot
        YEARLY,         // End of year snapshot
        CUSTOM          // Custom period snapshot
    }

    /**
     * Create snapshot from portfolio
     */
    public static PortfolioSnapshot createFromPortfolio(Portfolio portfolio, SnapshotType type) {
        return PortfolioSnapshot.builder()
                .portfolio(portfolio)
                .snapshotType(type)
                .totalValue(portfolio.getTotalValue())
                .cashValue(portfolio.getAvailableCash().add(portfolio.getLockedCash()))
                .positionsValue(portfolio.getPositionsValue())
                .availableCash(portfolio.getAvailableCash())
                .lockedCash(portfolio.getLockedCash())
                .totalPnl(portfolio.getTotalPnl())
                .unrealizedPnl(portfolio.getUnrealizedPnl())
                .realizedPnl(portfolio.getRealizedPnl())
                .dailyPnl(portfolio.getDailyPnl())
                .totalReturn(portfolio.getTotalReturn())
                .dailyReturn(portfolio.getDailyReturn())
                .maxDrawdown(portfolio.getMaxDrawdown())
                .volatility(portfolio.getVolatility())
                .sharpeRatio(portfolio.getSharpeRatio())
                .var95(portfolio.getVar95())
                .openPositionsCount((int) portfolio.getOpenPositionsCount())
                .totalExposure(portfolio.getTotalExposure())
                .leverage(portfolio.getLeverage())
                .totalTrades(portfolio.getTotalTrades())
                .winningTrades(portfolio.getWinningTrades())
                .losingTrades(portfolio.getLosingTrades())
                .winRate(portfolio.getWinRate())
                .profitFactor(portfolio.getProfitFactor())
                .totalFees(portfolio.getTotalFees())
                .build();
    }

    /**
     * Calculate period return
     */
    public BigDecimal calculatePeriodReturn(PortfolioSnapshot previousSnapshot) {
        if (previousSnapshot == null || previousSnapshot.getTotalValue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return this.totalValue.subtract(previousSnapshot.getTotalValue())
                .divide(previousSnapshot.getTotalValue(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate period volatility
     */
    public BigDecimal calculatePeriodVolatility(java.util.List<PortfolioSnapshot> periodSnapshots) {
        if (periodSnapshots.size() < 2) {
            return BigDecimal.ZERO;
        }

        // Calculate daily returns
        java.util.List<BigDecimal> returns = new java.util.ArrayList<>();
        for (int i = 1; i < periodSnapshots.size(); i++) {
            PortfolioSnapshot current = periodSnapshots.get(i);
            PortfolioSnapshot previous = periodSnapshots.get(i - 1);
            BigDecimal dailyReturn = current.calculatePeriodReturn(previous);
            returns.add(dailyReturn);
        }

        // Calculate mean return
        BigDecimal meanReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, java.math.RoundingMode.HALF_UP);

        // Calculate variance
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(meanReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size() - 1), 6, java.math.RoundingMode.HALF_UP);

        // Return standard deviation (volatility)
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    /**
     * Check if snapshot represents a new high water mark
     */
    public boolean isNewHighWaterMark(java.util.List<PortfolioSnapshot> historicalSnapshots) {
        return historicalSnapshots.stream()
                .map(PortfolioSnapshot::getTotalValue)
                .noneMatch(value -> value.compareTo(this.totalValue) > 0);
    }

    /**
     * Calculate maximum drawdown from peak
     */
    public BigDecimal calculateDrawdownFromPeak(BigDecimal peakValue) {
        if (peakValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return peakValue.subtract(this.totalValue)
                .divide(peakValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get performance grade based on return and risk
     */
    public String getPerformanceGrade() {
        if (sharpeRatio == null) {
            return "N/A";
        }

        if (sharpeRatio.compareTo(BigDecimal.valueOf(2.0)) >= 0) {
            return "A+";
        } else if (sharpeRatio.compareTo(BigDecimal.valueOf(1.5)) >= 0) {
            return "A";
        } else if (sharpeRatio.compareTo(BigDecimal.valueOf(1.0)) >= 0) {
            return "B";
        } else if (sharpeRatio.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
            return "C";
        } else if (sharpeRatio.compareTo(BigDecimal.ZERO) >= 0) {
            return "D";
        } else {
            return "F";
        }
    }

    /**
     * Check if portfolio is outperforming benchmark
     */
    public boolean isOutperformingBenchmark() {
        return benchmarkReturn != null && 
               totalReturn != null && 
               totalReturn.compareTo(benchmarkReturn) > 0;
    }

    /**
     * Get risk level based on volatility and drawdown
     */
    public String getRiskLevel() {
        if (volatility == null || maxDrawdown == null) {
            return "Unknown";
        }

        boolean highVolatility = volatility.compareTo(BigDecimal.valueOf(0.20)) > 0; // 20% annual volatility
        boolean highDrawdown = maxDrawdown.compareTo(BigDecimal.valueOf(15)) > 0; // 15% drawdown

        if (highVolatility && highDrawdown) {
            return "High";
        } else if (highVolatility || highDrawdown) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    /**
     * Calculate information ratio (excess return / tracking error)
     */
    public BigDecimal calculateInformationRatio() {
        if (benchmarkReturn == null || alpha == null || volatility == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal excessReturn = totalReturn.subtract(benchmarkReturn);
        return excessReturn.divide(volatility, 4, java.math.RoundingMode.HALF_UP);
    }
}

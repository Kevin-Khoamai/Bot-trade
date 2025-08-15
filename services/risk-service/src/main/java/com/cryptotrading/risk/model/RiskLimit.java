package com.cryptotrading.risk.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing risk limits and thresholds
 */
@Entity
@Table(name = "risk_limits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "limit_name", nullable = false)
    private String limitName;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false)
    private LimitType limitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private LimitScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LimitStatus status;

    // Limit Values
    @Column(name = "limit_value", precision = 20, scale = 8, nullable = false)
    private BigDecimal limitValue;

    @Column(name = "warning_threshold", precision = 20, scale = 8)
    private BigDecimal warningThreshold;

    @Column(name = "critical_threshold", precision = 20, scale = 8)
    private BigDecimal criticalThreshold;

    @Column(name = "current_value", precision = 20, scale = 8)
    private BigDecimal currentValue;

    @Column(name = "utilization_percentage", precision = 5, scale = 2)
    private BigDecimal utilizationPercentage;

    // Limit Configuration
    @Column(name = "currency")
    private String currency;

    @Column(name = "time_horizon")
    private String timeHorizon; // 1D, 10D, 1M, etc.

    @Column(name = "confidence_level", precision = 5, scale = 2)
    private BigDecimal confidenceLevel;

    @Column(name = "measurement_frequency")
    private String measurementFrequency; // REAL_TIME, HOURLY, DAILY

    // Breach Information
    @Column(name = "breach_count")
    private Integer breachCount = 0;

    @Column(name = "last_breach_date")
    private LocalDateTime lastBreachDate;

    @Column(name = "consecutive_breaches")
    private Integer consecutiveBreaches = 0;

    @Column(name = "breach_tolerance")
    private Integer breachTolerance = 0;

    // Actions and Notifications
    @Column(name = "auto_action_enabled")
    private Boolean autoActionEnabled = false;

    @Column(name = "auto_action_type")
    private String autoActionType;

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;

    @Column(name = "notification_recipients", columnDefinition = "TEXT")
    private String notificationRecipients; // JSON array

    @Column(name = "escalation_rules", columnDefinition = "TEXT")
    private String escalationRules; // JSON

    // Metadata
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "review_frequency")
    private String reviewFrequency; // MONTHLY, QUARTERLY, ANNUALLY

    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;

    /**
     * Limit types
     */
    public enum LimitType {
        VAR_LIMIT,              // Value at Risk limit
        DRAWDOWN_LIMIT,         // Maximum drawdown limit
        POSITION_LIMIT,         // Position size limit
        CONCENTRATION_LIMIT,    // Concentration limit
        LEVERAGE_LIMIT,         // Leverage limit
        LOSS_LIMIT,             // Loss limit
        EXPOSURE_LIMIT,         // Exposure limit
        VOLATILITY_LIMIT,       // Volatility limit
        CORRELATION_LIMIT,      // Correlation limit
        LIQUIDITY_LIMIT,        // Liquidity limit
        SECTOR_LIMIT,           // Sector concentration limit
        CURRENCY_LIMIT,         // Currency exposure limit
        COUNTERPARTY_LIMIT,     // Counterparty limit
        STRESS_TEST_LIMIT,      // Stress test limit
        CUSTOM_LIMIT            // Custom defined limit
    }

    /**
     * Limit scope
     */
    public enum LimitScope {
        PORTFOLIO,      // Portfolio level limit
        USER,           // User level limit
        GLOBAL,         // Global limit
        STRATEGY,       // Strategy level limit
        ASSET,          // Asset level limit
        EXCHANGE        // Exchange level limit
    }

    /**
     * Limit status
     */
    public enum LimitStatus {
        ACTIVE,         // Limit is active
        INACTIVE,       // Limit is inactive
        SUSPENDED,      // Limit is temporarily suspended
        EXPIRED,        // Limit has expired
        PENDING         // Limit pending approval
    }

    /**
     * Update current value and calculate utilization
     */
    public void updateCurrentValue(BigDecimal newValue) {
        this.currentValue = newValue;
        
        if (limitValue.compareTo(BigDecimal.ZERO) != 0) {
            this.utilizationPercentage = newValue.divide(limitValue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    /**
     * Check if limit is breached
     */
    public boolean isBreached() {
        if (currentValue == null || limitValue == null) {
            return false;
        }
        return currentValue.compareTo(limitValue) > 0;
    }

    /**
     * Check if warning threshold is breached
     */
    public boolean isWarningBreached() {
        if (currentValue == null || warningThreshold == null) {
            return false;
        }
        return currentValue.compareTo(warningThreshold) > 0;
    }

    /**
     * Check if critical threshold is breached
     */
    public boolean isCriticalBreached() {
        if (currentValue == null || criticalThreshold == null) {
            return false;
        }
        return currentValue.compareTo(criticalThreshold) > 0;
    }

    /**
     * Record a breach
     */
    public void recordBreach() {
        this.breachCount++;
        this.lastBreachDate = LocalDateTime.now();
        this.consecutiveBreaches++;
    }

    /**
     * Reset consecutive breaches (when limit is back within bounds)
     */
    public void resetConsecutiveBreaches() {
        this.consecutiveBreaches = 0;
    }

    /**
     * Get breach severity
     */
    public RiskAlert.AlertSeverity getBreachSeverity() {
        if (isCriticalBreached()) {
            return RiskAlert.AlertSeverity.CRITICAL;
        } else if (isBreached()) {
            return RiskAlert.AlertSeverity.HIGH;
        } else if (isWarningBreached()) {
            return RiskAlert.AlertSeverity.MEDIUM;
        } else {
            return RiskAlert.AlertSeverity.LOW;
        }
    }

    /**
     * Check if limit requires immediate action
     */
    public boolean requiresImmediateAction() {
        return isCriticalBreached() || 
               (isBreached() && consecutiveBreaches > breachTolerance) ||
               (autoActionEnabled && isBreached());
    }

    /**
     * Get remaining capacity
     */
    public BigDecimal getRemainingCapacity() {
        if (currentValue == null || limitValue == null) {
            return limitValue;
        }
        return limitValue.subtract(currentValue).max(BigDecimal.ZERO);
    }

    /**
     * Get remaining capacity percentage
     */
    public BigDecimal getRemainingCapacityPercentage() {
        if (utilizationPercentage == null) {
            return BigDecimal.valueOf(100);
        }
        return BigDecimal.valueOf(100).subtract(utilizationPercentage).max(BigDecimal.ZERO);
    }

    /**
     * Check if limit is effective (within date range)
     */
    public boolean isEffective() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = effectiveFrom == null || now.isAfter(effectiveFrom);
        boolean beforeEnd = effectiveUntil == null || now.isBefore(effectiveUntil);
        return status == LimitStatus.ACTIVE && afterStart && beforeEnd;
    }

    /**
     * Check if limit needs review
     */
    public boolean needsReview() {
        return nextReviewDate != null && LocalDateTime.now().isAfter(nextReviewDate);
    }

    /**
     * Get limit health status
     */
    public String getHealthStatus() {
        if (!isEffective()) {
            return "INACTIVE";
        } else if (isCriticalBreached()) {
            return "CRITICAL";
        } else if (isBreached()) {
            return "BREACHED";
        } else if (isWarningBreached()) {
            return "WARNING";
        } else if (utilizationPercentage != null && utilizationPercentage.compareTo(BigDecimal.valueOf(80)) > 0) {
            return "HIGH_UTILIZATION";
        } else {
            return "HEALTHY";
        }
    }

    /**
     * Create standard VaR limit
     */
    public static RiskLimit createVarLimit(UUID portfolioId, String userId, BigDecimal varLimit, 
                                         BigDecimal confidenceLevel, String timeHorizon) {
        return RiskLimit.builder()
                .portfolioId(portfolioId)
                .userId(userId)
                .limitName("VaR Limit " + timeHorizon + " " + confidenceLevel + "%")
                .description("Value at Risk limit for " + timeHorizon + " at " + confidenceLevel + "% confidence")
                .limitType(LimitType.VAR_LIMIT)
                .scope(LimitScope.PORTFOLIO)
                .status(LimitStatus.ACTIVE)
                .limitValue(varLimit)
                .warningThreshold(varLimit.multiply(BigDecimal.valueOf(0.8)))
                .criticalThreshold(varLimit.multiply(BigDecimal.valueOf(0.95)))
                .confidenceLevel(confidenceLevel)
                .timeHorizon(timeHorizon)
                .measurementFrequency("REAL_TIME")
                .notificationEnabled(true)
                .effectiveFrom(LocalDateTime.now())
                .build();
    }

    /**
     * Create drawdown limit
     */
    public static RiskLimit createDrawdownLimit(UUID portfolioId, String userId, BigDecimal drawdownLimit) {
        return RiskLimit.builder()
                .portfolioId(portfolioId)
                .userId(userId)
                .limitName("Maximum Drawdown Limit")
                .description("Maximum allowable portfolio drawdown")
                .limitType(LimitType.DRAWDOWN_LIMIT)
                .scope(LimitScope.PORTFOLIO)
                .status(LimitStatus.ACTIVE)
                .limitValue(drawdownLimit)
                .warningThreshold(drawdownLimit.multiply(BigDecimal.valueOf(0.8)))
                .criticalThreshold(drawdownLimit.multiply(BigDecimal.valueOf(0.95)))
                .measurementFrequency("REAL_TIME")
                .notificationEnabled(true)
                .effectiveFrom(LocalDateTime.now())
                .build();
    }
}

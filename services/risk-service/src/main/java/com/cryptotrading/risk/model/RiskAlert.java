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
 * Entity representing a risk alert
 */
@Entity
@Table(name = "risk_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "portfolio_name")
    private String portfolioName;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status;

    // Alert Details
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "metric_name")
    private String metricName;

    @Column(name = "current_value", precision = 20, scale = 8)
    private BigDecimal currentValue;

    @Column(name = "threshold_value", precision = 20, scale = 8)
    private BigDecimal thresholdValue;

    @Column(name = "breach_percentage", precision = 10, scale = 4)
    private BigDecimal breachPercentage;

    // Risk Context
    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "impact_assessment", columnDefinition = "TEXT")
    private String impactAssessment;

    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions;

    // Alert Metadata
    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "rule_id")
    private String ruleId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "alert_data", columnDefinition = "TEXT")
    private String alertData; // JSON

    // Notification Details
    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @Column(name = "notification_channels", columnDefinition = "TEXT")
    private String notificationChannels; // JSON array

    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;

    @Column(name = "escalated_to")
    private String escalatedTo;

    // Resolution Details
    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "false_positive")
    private Boolean falsePositive = false;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Alert types
     */
    public enum AlertType {
        VAR_BREACH,                 // VaR limit exceeded
        DRAWDOWN_LIMIT,             // Drawdown limit exceeded
        CONCENTRATION_RISK,         // Concentration risk too high
        LEVERAGE_LIMIT,             // Leverage limit exceeded
        VOLATILITY_SPIKE,           // Volatility spike detected
        CORRELATION_BREAKDOWN,      // Correlation breakdown
        LIQUIDITY_RISK,             // Liquidity risk alert
        POSITION_LIMIT,             // Position limit exceeded
        LOSS_LIMIT,                 // Loss limit exceeded
        MARGIN_CALL,                // Margin call required
        STRESS_TEST_FAILURE,        // Stress test failure
        MODEL_BREAKDOWN,            // Risk model breakdown
        DATA_QUALITY,               // Data quality issue
        SYSTEM_RISK,                // System risk alert
        REGULATORY_BREACH,          // Regulatory limit breach
        OPERATIONAL_RISK            // Operational risk event
    }

    /**
     * Alert severity levels
     */
    public enum AlertSeverity {
        INFO(1, "Informational", "#0000FF"),
        LOW(2, "Low", "#00FF00"),
        MEDIUM(3, "Medium", "#FFFF00"),
        HIGH(4, "High", "#FFA500"),
        CRITICAL(5, "Critical", "#FF0000");

        private final int level;
        private final String description;
        private final String colorCode;

        AlertSeverity(int level, String description, String colorCode) {
            this.level = level;
            this.description = description;
            this.colorCode = colorCode;
        }

        public int getLevel() { return level; }
        public String getDescription() { return description; }
        public String getColorCode() { return colorCode; }
    }

    /**
     * Alert status
     */
    public enum AlertStatus {
        ACTIVE,         // Alert is active
        ACKNOWLEDGED,   // Alert has been acknowledged
        RESOLVED,       // Alert has been resolved
        EXPIRED,        // Alert has expired
        SUPPRESSED,     // Alert has been suppressed
        FALSE_POSITIVE  // Alert marked as false positive
    }

    /**
     * Create VaR breach alert
     */
    public static RiskAlert createVarBreachAlert(UUID portfolioId, String portfolioName, String userId,
                                                BigDecimal currentVar, BigDecimal varLimit) {
        BigDecimal breachPercent = currentVar.subtract(varLimit)
                .divide(varLimit, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return RiskAlert.builder()
                .portfolioId(portfolioId)
                .portfolioName(portfolioName)
                .userId(userId)
                .alertType(AlertType.VAR_BREACH)
                .severity(breachPercent.compareTo(BigDecimal.valueOf(50)) > 0 ? AlertSeverity.CRITICAL : AlertSeverity.HIGH)
                .status(AlertStatus.ACTIVE)
                .title("VaR Limit Exceeded")
                .message(String.format("Portfolio VaR (%s) has exceeded the limit (%s) by %.2f%%", 
                        currentVar, varLimit, breachPercent))
                .metricName("VAR_1_DAY_95")
                .currentValue(currentVar)
                .thresholdValue(varLimit)
                .breachPercentage(breachPercent)
                .impactAssessment("High risk of significant losses if market conditions deteriorate")
                .recommendedActions("Consider reducing position sizes or hedging exposure")
                .sourceSystem("RISK_SERVICE")
                .build();
    }

    /**
     * Create drawdown alert
     */
    public static RiskAlert createDrawdownAlert(UUID portfolioId, String portfolioName, String userId,
                                              BigDecimal currentDrawdown, BigDecimal drawdownLimit) {
        return RiskAlert.builder()
                .portfolioId(portfolioId)
                .portfolioName(portfolioName)
                .userId(userId)
                .alertType(AlertType.DRAWDOWN_LIMIT)
                .severity(currentDrawdown.compareTo(BigDecimal.valueOf(20)) > 0 ? AlertSeverity.CRITICAL : AlertSeverity.HIGH)
                .status(AlertStatus.ACTIVE)
                .title("Drawdown Limit Exceeded")
                .message(String.format("Portfolio drawdown (%.2f%%) has exceeded the limit (%.2f%%)", 
                        currentDrawdown, drawdownLimit))
                .metricName("MAX_DRAWDOWN")
                .currentValue(currentDrawdown)
                .thresholdValue(drawdownLimit)
                .breachPercentage(currentDrawdown.subtract(drawdownLimit))
                .impactAssessment("Portfolio experiencing significant losses from peak")
                .recommendedActions("Review positions and consider risk reduction measures")
                .sourceSystem("RISK_SERVICE")
                .build();
    }

    /**
     * Create concentration risk alert
     */
    public static RiskAlert createConcentrationAlert(UUID portfolioId, String portfolioName, String userId,
                                                   BigDecimal concentrationRisk, String asset) {
        return RiskAlert.builder()
                .portfolioId(portfolioId)
                .portfolioName(portfolioName)
                .userId(userId)
                .alertType(AlertType.CONCENTRATION_RISK)
                .severity(concentrationRisk.compareTo(BigDecimal.valueOf(50)) > 0 ? AlertSeverity.HIGH : AlertSeverity.MEDIUM)
                .status(AlertStatus.ACTIVE)
                .title("High Concentration Risk Detected")
                .message(String.format("Portfolio has %.2f%% concentration in %s", concentrationRisk, asset))
                .metricName("CONCENTRATION_RISK")
                .currentValue(concentrationRisk)
                .thresholdValue(BigDecimal.valueOf(30))
                .impactAssessment("High exposure to single asset increases portfolio risk")
                .recommendedActions("Consider diversifying holdings across multiple assets")
                .sourceSystem("RISK_SERVICE")
                .build();
    }

    /**
     * Acknowledge alert
     */
    public void acknowledge(String acknowledgedBy) {
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = LocalDateTime.now();
    }

    /**
     * Resolve alert
     */
    public void resolve(String resolvedBy, String resolutionNotes) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = resolutionNotes;
    }

    /**
     * Mark as false positive
     */
    public void markAsFalsePositive(String resolvedBy, String reason) {
        this.status = AlertStatus.FALSE_POSITIVE;
        this.falsePositive = true;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = "False Positive: " + reason;
    }

    /**
     * Escalate alert
     */
    public void escalate(String escalatedTo) {
        this.escalationLevel++;
        this.escalatedTo = escalatedTo;
        
        // Increase severity if not already critical
        if (this.severity != AlertSeverity.CRITICAL) {
            this.severity = AlertSeverity.values()[Math.min(this.severity.ordinal() + 1, AlertSeverity.values().length - 1)];
        }
    }

    /**
     * Check if alert is active
     */
    public boolean isActive() {
        return status == AlertStatus.ACTIVE;
    }

    /**
     * Check if alert requires immediate attention
     */
    public boolean requiresImmediateAttention() {
        return severity == AlertSeverity.CRITICAL || 
               (severity == AlertSeverity.HIGH && escalationLevel > 0);
    }

    /**
     * Get age in minutes
     */
    public long getAgeInMinutes() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }

    /**
     * Check if alert is stale (older than 1 hour without acknowledgment)
     */
    public boolean isStale() {
        return status == AlertStatus.ACTIVE && getAgeInMinutes() > 60;
    }

    /**
     * Get priority score for sorting
     */
    public int getPriorityScore() {
        int score = severity.getLevel() * 10;
        score += escalationLevel * 5;
        score += Math.min(getAgeInMinutes() / 10, 10); // Age factor
        return score;
    }
}

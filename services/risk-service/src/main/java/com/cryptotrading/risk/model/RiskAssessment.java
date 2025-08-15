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
 * Entity representing a comprehensive risk assessment
 */
@Entity
@Table(name = "risk_assessments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "portfolio_name")
    private String portfolioName;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false)
    private AssessmentType assessmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    // Portfolio Metrics
    @Column(name = "portfolio_value", precision = 20, scale = 8)
    private BigDecimal portfolioValue;

    @Column(name = "total_exposure", precision = 20, scale = 8)
    private BigDecimal totalExposure;

    @Column(name = "leverage", precision = 10, scale = 4)
    private BigDecimal leverage;

    @Column(name = "concentration_risk", precision = 10, scale = 4)
    private BigDecimal concentrationRisk;

    // Value at Risk (VaR) Metrics
    @Column(name = "var_1_day_95", precision = 20, scale = 8)
    private BigDecimal var1Day95;

    @Column(name = "var_1_day_99", precision = 20, scale = 8)
    private BigDecimal var1Day99;

    @Column(name = "var_10_day_95", precision = 20, scale = 8)
    private BigDecimal var10Day95;

    @Column(name = "var_10_day_99", precision = 20, scale = 8)
    private BigDecimal var10Day99;

    @Column(name = "cvar_95", precision = 20, scale = 8)
    private BigDecimal cvar95; // Conditional VaR (Expected Shortfall)

    @Column(name = "cvar_99", precision = 20, scale = 8)
    private BigDecimal cvar99;

    // Volatility and Correlation
    @Column(name = "portfolio_volatility", precision = 10, scale = 6)
    private BigDecimal portfolioVolatility;

    @Column(name = "correlation_risk", precision = 10, scale = 4)
    private BigDecimal correlationRisk;

    @Column(name = "diversification_ratio", precision = 10, scale = 4)
    private BigDecimal diversificationRatio;

    // Drawdown Metrics
    @Column(name = "current_drawdown", precision = 10, scale = 4)
    private BigDecimal currentDrawdown;

    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown;

    @Column(name = "drawdown_duration_days")
    private Integer drawdownDurationDays;

    // Performance vs Risk
    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "sortino_ratio", precision = 10, scale = 4)
    private BigDecimal sortinoRatio;

    @Column(name = "calmar_ratio", precision = 10, scale = 4)
    private BigDecimal calmarRatio;

    @Column(name = "information_ratio", precision = 10, scale = 4)
    private BigDecimal informationRatio;

    // Stress Testing
    @Column(name = "stress_test_loss", precision = 20, scale = 8)
    private BigDecimal stressTestLoss;

    @Column(name = "worst_case_scenario", precision = 20, scale = 8)
    private BigDecimal worstCaseScenario;

    @Column(name = "tail_risk", precision = 10, scale = 4)
    private BigDecimal tailRisk;

    // Liquidity Risk
    @Column(name = "liquidity_score", precision = 5, scale = 2)
    private BigDecimal liquidityScore;

    @Column(name = "time_to_liquidate_hours")
    private BigDecimal timeToLiquidateHours;

    @Column(name = "liquidity_risk_level")
    private String liquidityRiskLevel;

    // Market Risk Factors
    @Column(name = "market_beta", precision = 10, scale = 4)
    private BigDecimal marketBeta;

    @Column(name = "sector_concentration", precision = 10, scale = 4)
    private BigDecimal sectorConcentration;

    @Column(name = "currency_exposure", precision = 10, scale = 4)
    private BigDecimal currencyExposure;

    // Risk Limits and Breaches
    @Column(name = "risk_limit_utilization", precision = 5, scale = 2)
    private BigDecimal riskLimitUtilization;

    @Column(name = "limit_breaches_count")
    private Integer limitBreachesCount;

    @Column(name = "critical_breaches_count")
    private Integer criticalBreachesCount;

    // Assessment Metadata
    @Column(name = "confidence_level", precision = 5, scale = 2)
    private BigDecimal confidenceLevel;

    @Column(name = "data_quality_score", precision = 5, scale = 2)
    private BigDecimal dataQualityScore;

    @Column(name = "model_accuracy", precision = 5, scale = 2)
    private BigDecimal modelAccuracy;

    @Column(name = "assessment_notes", columnDefinition = "TEXT")
    private String assessmentNotes;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "assessment_date")
    private LocalDateTime assessmentDate;

    @Column(name = "next_assessment_due")
    private LocalDateTime nextAssessmentDue;

    /**
     * Assessment types
     */
    public enum AssessmentType {
        REAL_TIME,          // Real-time continuous assessment
        DAILY,              // Daily risk assessment
        WEEKLY,             // Weekly comprehensive assessment
        MONTHLY,            // Monthly detailed assessment
        STRESS_TEST,        // Stress testing assessment
        REGULATORY,         // Regulatory compliance assessment
        AD_HOC              // Ad-hoc assessment
    }

    /**
     * Risk levels
     */
    public enum RiskLevel {
        VERY_LOW(1, "Very Low", "#00FF00"),
        LOW(2, "Low", "#90EE90"),
        MODERATE(3, "Moderate", "#FFFF00"),
        HIGH(4, "High", "#FFA500"),
        VERY_HIGH(5, "Very High", "#FF4500"),
        CRITICAL(6, "Critical", "#FF0000");

        private final int level;
        private final String description;
        private final String colorCode;

        RiskLevel(int level, String description, String colorCode) {
            this.level = level;
            this.description = description;
            this.colorCode = colorCode;
        }

        public int getLevel() { return level; }
        public String getDescription() { return description; }
        public String getColorCode() { return colorCode; }
    }

    /**
     * Calculate overall risk score (0-100)
     */
    public BigDecimal calculateOverallRiskScore() {
        BigDecimal score = BigDecimal.ZERO;
        int factors = 0;

        // VaR contribution (30%)
        if (var1Day95 != null && portfolioValue != null && portfolioValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal varPercent = var1Day95.divide(portfolioValue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            score = score.add(varPercent.multiply(BigDecimal.valueOf(30)));
            factors++;
        }

        // Volatility contribution (20%)
        if (portfolioVolatility != null) {
            BigDecimal volScore = portfolioVolatility.multiply(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(20));
            score = score.add(volScore);
            factors++;
        }

        // Drawdown contribution (20%)
        if (currentDrawdown != null) {
            score = score.add(currentDrawdown.multiply(BigDecimal.valueOf(20)));
            factors++;
        }

        // Concentration risk contribution (15%)
        if (concentrationRisk != null) {
            score = score.add(concentrationRisk.multiply(BigDecimal.valueOf(15)));
            factors++;
        }

        // Leverage contribution (15%)
        if (leverage != null) {
            BigDecimal leverageScore = leverage.subtract(BigDecimal.ONE).max(BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(15));
            score = score.add(leverageScore);
            factors++;
        }

        return factors > 0 ? score.divide(BigDecimal.valueOf(factors), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * Determine risk level based on overall score
     */
    public RiskLevel determineRiskLevel() {
        BigDecimal score = calculateOverallRiskScore();
        
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return RiskLevel.CRITICAL;
        } else if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return RiskLevel.VERY_HIGH;
        } else if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return RiskLevel.HIGH;
        } else if (score.compareTo(BigDecimal.valueOf(25)) >= 0) {
            return RiskLevel.MODERATE;
        } else if (score.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return RiskLevel.LOW;
        } else {
            return RiskLevel.VERY_LOW;
        }
    }

    /**
     * Check if assessment requires immediate attention
     */
    public boolean requiresImmediateAttention() {
        return riskLevel == RiskLevel.CRITICAL || 
               riskLevel == RiskLevel.VERY_HIGH ||
               (criticalBreachesCount != null && criticalBreachesCount > 0) ||
               (currentDrawdown != null && currentDrawdown.compareTo(BigDecimal.valueOf(20)) > 0);
    }

    /**
     * Get risk capacity utilization
     */
    public BigDecimal getRiskCapacityUtilization() {
        if (riskLimitUtilization != null) {
            return riskLimitUtilization;
        }
        
        // Calculate based on VaR if limit utilization not available
        if (var1Day95 != null && portfolioValue != null && portfolioValue.compareTo(BigDecimal.ZERO) > 0) {
            return var1Day95.divide(portfolioValue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Get diversification benefit
     */
    public BigDecimal getDiversificationBenefit() {
        if (diversificationRatio != null) {
            return BigDecimal.ONE.subtract(diversificationRatio).multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if portfolio is well diversified
     */
    public boolean isWellDiversified() {
        return concentrationRisk != null && concentrationRisk.compareTo(BigDecimal.valueOf(30)) <= 0 &&
               diversificationRatio != null && diversificationRatio.compareTo(BigDecimal.valueOf(0.7)) <= 0;
    }

    /**
     * Get risk-adjusted return quality
     */
    public String getRiskAdjustedReturnQuality() {
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
}

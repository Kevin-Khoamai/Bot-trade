package com.cryptotrading.risk.service;

import com.cryptotrading.risk.model.RiskAlert;
import com.cryptotrading.risk.model.RiskAssessment;
import com.cryptotrading.risk.model.RiskLimit;
import com.cryptotrading.risk.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Core service for risk management and monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskManagementService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final VarCalculationService varCalculationService;
    private final RiskAlertService riskAlertService;
    private final RiskLimitService riskLimitService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Listen to portfolio events from Portfolio Service
     */
    @KafkaListener(topics = "portfolio-events", groupId = "risk-management-group")
    public void processPortfolioEvent(Map<String, Object> portfolioData) {
        try {
            log.info("Processing portfolio event: {}", portfolioData);

            String eventType = (String) portfolioData.get("eventType");
            UUID portfolioId = UUID.fromString((String) portfolioData.get("portfolioId"));
            String portfolioName = (String) portfolioData.get("portfolioName");
            String userId = (String) portfolioData.get("userId");

            switch (eventType) {
                case "PORTFOLIO_CREATED":
                    handlePortfolioCreated(portfolioId, portfolioName, userId);
                    break;
                case "PORTFOLIO_UPDATED":
                    performRealTimeRiskAssessment(portfolioId);
                    break;
                case "PORTFOLIO_CLOSED":
                    handlePortfolioClosed(portfolioId);
                    break;
                default:
                    log.debug("Unhandled portfolio event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing portfolio event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to position updates from Portfolio Service
     */
    @KafkaListener(topics = "position-updates", groupId = "risk-management-group")
    public void processPositionUpdate(Map<String, Object> positionData) {
        try {
            log.debug("Processing position update: {}", positionData);

            UUID portfolioId = UUID.fromString((String) positionData.get("portfolioId"));
            
            // Trigger real-time risk assessment
            performRealTimeRiskAssessment(portfolioId);

        } catch (Exception e) {
            log.error("Error processing position update: {}", e.getMessage(), e);
        }
    }

    /**
     * Perform real-time risk assessment
     */
    @Transactional
    public RiskAssessment performRealTimeRiskAssessment(UUID portfolioId) {
        try {
            log.debug("Performing real-time risk assessment for portfolio: {}", portfolioId);

            // Get portfolio data (this would typically call Portfolio Service)
            Map<String, Object> portfolioData = getPortfolioData(portfolioId);
            
            if (portfolioData.isEmpty()) {
                log.warn("No portfolio data found for: {}", portfolioId);
                return null;
            }

            // Create risk assessment
            RiskAssessment assessment = createRiskAssessment(portfolioData);
            assessment = riskAssessmentRepository.save(assessment);

            // Check risk limits and generate alerts
            checkRiskLimitsAndGenerateAlerts(assessment);

            // Publish risk assessment event
            publishRiskAssessmentEvent(assessment);

            log.info("Real-time risk assessment completed for portfolio: {} with risk level: {}", 
                    portfolioId, assessment.getRiskLevel());

            return assessment;

        } catch (Exception e) {
            log.error("Error performing real-time risk assessment for portfolio {}: {}", portfolioId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create comprehensive risk assessment
     */
    private RiskAssessment createRiskAssessment(Map<String, Object> portfolioData) {
        UUID portfolioId = UUID.fromString((String) portfolioData.get("portfolioId"));
        String portfolioName = (String) portfolioData.get("portfolioName");
        String userId = (String) portfolioData.get("userId");
        BigDecimal portfolioValue = new BigDecimal(portfolioData.get("totalValue").toString());

        // Get historical returns for VaR calculation
        List<BigDecimal> returns = getPortfolioReturns(portfolioId, 252); // 1 year of daily returns

        // Calculate VaR metrics
        BigDecimal var1Day95 = varCalculationService.calculateParametricVaR(returns, 0.95);
        BigDecimal var1Day99 = varCalculationService.calculateParametricVaR(returns, 0.99);
        BigDecimal var10Day95 = var1Day95.multiply(BigDecimal.valueOf(Math.sqrt(10))).setScale(8, RoundingMode.HALF_UP);
        BigDecimal var10Day99 = var1Day99.multiply(BigDecimal.valueOf(Math.sqrt(10))).setScale(8, RoundingMode.HALF_UP);
        
        BigDecimal cvar95 = varCalculationService.calculateConditionalVaR(returns, 0.95);
        BigDecimal cvar99 = varCalculationService.calculateConditionalVaR(returns, 0.99);

        // Calculate other risk metrics
        BigDecimal volatility = calculateVolatility(returns);
        BigDecimal currentDrawdown = calculateCurrentDrawdown(portfolioData);
        BigDecimal maxDrawdown = calculateMaxDrawdown(portfolioData);
        BigDecimal sharpeRatio = calculateSharpeRatio(returns);
        BigDecimal concentrationRisk = calculateConcentrationRisk(portfolioData);
        BigDecimal leverage = calculateLeverage(portfolioData);

        // Create assessment
        RiskAssessment assessment = RiskAssessment.builder()
                .portfolioId(portfolioId)
                .portfolioName(portfolioName)
                .userId(userId)
                .assessmentType(RiskAssessment.AssessmentType.REAL_TIME)
                .portfolioValue(portfolioValue)
                .var1Day95(var1Day95)
                .var1Day99(var1Day99)
                .var10Day95(var10Day95)
                .var10Day99(var10Day99)
                .cvar95(cvar95)
                .cvar99(cvar99)
                .portfolioVolatility(volatility)
                .currentDrawdown(currentDrawdown)
                .maxDrawdown(maxDrawdown)
                .sharpeRatio(sharpeRatio)
                .concentrationRisk(concentrationRisk)
                .leverage(leverage)
                .assessmentDate(LocalDateTime.now())
                .confidenceLevel(BigDecimal.valueOf(95))
                .dataQualityScore(BigDecimal.valueOf(85)) // Would be calculated based on actual data quality
                .modelAccuracy(BigDecimal.valueOf(90)) // Would be calculated based on backtesting
                .build();

        // Determine risk level
        assessment.setRiskLevel(assessment.determineRiskLevel());

        return assessment;
    }

    /**
     * Check risk limits and generate alerts
     */
    private void checkRiskLimitsAndGenerateAlerts(RiskAssessment assessment) {
        try {
            List<RiskLimit> limits = riskLimitService.getActiveRiskLimits(assessment.getPortfolioId());

            for (RiskLimit limit : limits) {
                boolean breached = false;
                BigDecimal currentValue = null;

                // Check different limit types
                switch (limit.getLimitType()) {
                    case VAR_LIMIT:
                        currentValue = assessment.getVar1Day95();
                        breached = currentValue != null && currentValue.compareTo(limit.getLimitValue()) > 0;
                        break;
                    case DRAWDOWN_LIMIT:
                        currentValue = assessment.getCurrentDrawdown();
                        breached = currentValue != null && currentValue.compareTo(limit.getLimitValue()) > 0;
                        break;
                    case CONCENTRATION_LIMIT:
                        currentValue = assessment.getConcentrationRisk();
                        breached = currentValue != null && currentValue.compareTo(limit.getLimitValue()) > 0;
                        break;
                    case LEVERAGE_LIMIT:
                        currentValue = assessment.getLeverage();
                        breached = currentValue != null && currentValue.compareTo(limit.getLimitValue()) > 0;
                        break;
                }

                if (currentValue != null) {
                    limit.updateCurrentValue(currentValue);
                }

                if (breached) {
                    limit.recordBreach();
                    generateRiskAlert(assessment, limit);
                } else {
                    limit.resetConsecutiveBreaches();
                }

                riskLimitService.updateRiskLimit(limit);
            }

        } catch (Exception e) {
            log.error("Error checking risk limits: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate risk alert
     */
    private void generateRiskAlert(RiskAssessment assessment, RiskLimit limit) {
        try {
            RiskAlert alert = null;

            switch (limit.getLimitType()) {
                case VAR_LIMIT:
                    alert = RiskAlert.createVarBreachAlert(
                            assessment.getPortfolioId(),
                            assessment.getPortfolioName(),
                            assessment.getUserId(),
                            assessment.getVar1Day95(),
                            limit.getLimitValue()
                    );
                    break;
                case DRAWDOWN_LIMIT:
                    alert = RiskAlert.createDrawdownAlert(
                            assessment.getPortfolioId(),
                            assessment.getPortfolioName(),
                            assessment.getUserId(),
                            assessment.getCurrentDrawdown(),
                            limit.getLimitValue()
                    );
                    break;
                case CONCENTRATION_LIMIT:
                    alert = RiskAlert.createConcentrationAlert(
                            assessment.getPortfolioId(),
                            assessment.getPortfolioName(),
                            assessment.getUserId(),
                            assessment.getConcentrationRisk(),
                            "Portfolio"
                    );
                    break;
            }

            if (alert != null) {
                riskAlertService.createAlert(alert);
                log.warn("Risk alert generated: {} for portfolio: {}", alert.getTitle(), assessment.getPortfolioName());
            }

        } catch (Exception e) {
            log.error("Error generating risk alert: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle portfolio creation
     */
    private void handlePortfolioCreated(UUID portfolioId, String portfolioName, String userId) {
        try {
            // Create default risk limits for new portfolio
            riskLimitService.createDefaultRiskLimits(portfolioId, userId);
            
            log.info("Default risk limits created for new portfolio: {}", portfolioName);

        } catch (Exception e) {
            log.error("Error handling portfolio creation: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle portfolio closure
     */
    private void handlePortfolioClosed(UUID portfolioId) {
        try {
            // Deactivate risk limits
            riskLimitService.deactivateRiskLimits(portfolioId);
            
            // Close any open alerts
            riskAlertService.closePortfolioAlerts(portfolioId);
            
            log.info("Risk management cleanup completed for closed portfolio: {}", portfolioId);

        } catch (Exception e) {
            log.error("Error handling portfolio closure: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled daily risk assessment
     */
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    public void performDailyRiskAssessments() {
        try {
            log.info("Starting daily risk assessments");

            // Get all active portfolios (would call Portfolio Service)
            List<UUID> activePortfolios = getActivePortfolios();

            for (UUID portfolioId : activePortfolios) {
                try {
                    performDailyRiskAssessment(portfolioId);
                } catch (Exception e) {
                    log.error("Error in daily risk assessment for portfolio {}: {}", portfolioId, e.getMessage());
                }
            }

            log.info("Daily risk assessments completed for {} portfolios", activePortfolios.size());

        } catch (Exception e) {
            log.error("Error in scheduled daily risk assessments: {}", e.getMessage(), e);
        }
    }

    /**
     * Perform daily risk assessment
     */
    @Transactional
    public RiskAssessment performDailyRiskAssessment(UUID portfolioId) {
        // Similar to real-time assessment but with daily assessment type
        // and more comprehensive analysis
        return performRealTimeRiskAssessment(portfolioId);
    }

    /**
     * Publish risk assessment event
     */
    private void publishRiskAssessmentEvent(RiskAssessment assessment) {
        try {
            Map<String, Object> eventData = Map.of(
                    "eventType", "RISK_ASSESSMENT_COMPLETED",
                    "portfolioId", assessment.getPortfolioId().toString(),
                    "riskLevel", assessment.getRiskLevel().name(),
                    "var1Day95", assessment.getVar1Day95(),
                    "currentDrawdown", assessment.getCurrentDrawdown(),
                    "assessmentDate", assessment.getAssessmentDate()
            );

            kafkaTemplate.send("risk-events", assessment.getPortfolioId().toString(), eventData);
            log.debug("Published risk assessment event for portfolio: {}", assessment.getPortfolioId());

        } catch (Exception e) {
            log.error("Error publishing risk assessment event: {}", e.getMessage(), e);
        }
    }

    // Helper methods (would be implemented with actual data sources)
    private Map<String, Object> getPortfolioData(UUID portfolioId) {
        // Mock implementation - would call Portfolio Service
        return Map.of(
                "portfolioId", portfolioId.toString(),
                "portfolioName", "Test Portfolio",
                "userId", "user123",
                "totalValue", BigDecimal.valueOf(100000)
        );
    }

    private List<BigDecimal> getPortfolioReturns(UUID portfolioId, int days) {
        // Mock implementation - would get actual historical returns
        List<BigDecimal> returns = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < days; i++) {
            returns.add(BigDecimal.valueOf(random.nextGaussian() * 0.02)); // 2% daily volatility
        }
        return returns;
    }

    private List<UUID> getActivePortfolios() {
        // Mock implementation - would call Portfolio Service
        return Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
    }

    private BigDecimal calculateVolatility(List<BigDecimal> returns) {
        if (returns.isEmpty()) return BigDecimal.ZERO;
        
        // Calculate standard deviation
        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 8, RoundingMode.HALF_UP);
        
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size() - 1), 8, RoundingMode.HALF_UP);
        
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCurrentDrawdown(Map<String, Object> portfolioData) {
        // Mock implementation
        return BigDecimal.valueOf(5.5); // 5.5% drawdown
    }

    private BigDecimal calculateMaxDrawdown(Map<String, Object> portfolioData) {
        // Mock implementation
        return BigDecimal.valueOf(12.3); // 12.3% max drawdown
    }

    private BigDecimal calculateSharpeRatio(List<BigDecimal> returns) {
        if (returns.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 8, RoundingMode.HALF_UP);
        
        BigDecimal volatility = calculateVolatility(returns);
        
        return volatility.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : 
               mean.divide(volatility, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateConcentrationRisk(Map<String, Object> portfolioData) {
        // Mock implementation
        return BigDecimal.valueOf(25.0); // 25% concentration
    }

    private BigDecimal calculateLeverage(Map<String, Object> portfolioData) {
        // Mock implementation
        return BigDecimal.valueOf(1.5); // 1.5x leverage
    }
}

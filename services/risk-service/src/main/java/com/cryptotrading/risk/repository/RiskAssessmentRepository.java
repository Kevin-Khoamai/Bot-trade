package com.cryptotrading.risk.repository;

import com.cryptotrading.risk.model.RiskAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RiskAssessment entities
 */
@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    /**
     * Find assessments by portfolio ID
     */
    List<RiskAssessment> findByPortfolioId(UUID portfolioId);

    /**
     * Find assessments by user ID
     */
    List<RiskAssessment> findByUserId(String userId);

    /**
     * Find assessments by type
     */
    List<RiskAssessment> findByAssessmentType(RiskAssessment.AssessmentType assessmentType);

    /**
     * Find assessments by risk level
     */
    List<RiskAssessment> findByRiskLevel(RiskAssessment.RiskLevel riskLevel);

    /**
     * Find latest assessment for portfolio
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.portfolioId = :portfolioId ORDER BY r.assessmentDate DESC")
    Optional<RiskAssessment> findLatestByPortfolioId(@Param("portfolioId") UUID portfolioId);

    /**
     * Find latest assessments for user
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.userId = :userId AND r.id IN " +
           "(SELECT MAX(r2.id) FROM RiskAssessment r2 WHERE r2.userId = :userId GROUP BY r2.portfolioId) " +
           "ORDER BY r.assessmentDate DESC")
    List<RiskAssessment> findLatestByUserId(@Param("userId") String userId);

    /**
     * Find high risk assessments
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.riskLevel IN ('HIGH', 'VERY_HIGH', 'CRITICAL') " +
           "ORDER BY r.assessmentDate DESC")
    List<RiskAssessment> findHighRiskAssessments();

    /**
     * Find assessments requiring attention
     */
    @Query("SELECT r FROM RiskAssessment r WHERE " +
           "r.riskLevel IN ('VERY_HIGH', 'CRITICAL') OR " +
           "r.criticalBreachesCount > 0 OR " +
           "r.currentDrawdown > 20 " +
           "ORDER BY r.assessmentDate DESC")
    List<RiskAssessment> findAssessmentsRequiringAttention();

    /**
     * Find assessments by date range
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.assessmentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY r.assessmentDate DESC")
    List<RiskAssessment> findByAssessmentDateBetween(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Find assessments by portfolio and date range
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.portfolioId = :portfolioId AND " +
           "r.assessmentDate BETWEEN :startDate AND :endDate ORDER BY r.assessmentDate DESC")
    List<RiskAssessment> findByPortfolioIdAndDateBetween(@Param("portfolioId") UUID portfolioId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Find assessments with high VaR
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.var1Day95 >= :varThreshold ORDER BY r.var1Day95 DESC")
    List<RiskAssessment> findHighVarAssessments(@Param("varThreshold") BigDecimal varThreshold);

    /**
     * Find assessments with high drawdown
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.currentDrawdown >= :drawdownThreshold " +
           "ORDER BY r.currentDrawdown DESC")
    List<RiskAssessment> findHighDrawdownAssessments(@Param("drawdownThreshold") BigDecimal drawdownThreshold);

    /**
     * Find assessments with high concentration risk
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.concentrationRisk >= :concentrationThreshold " +
           "ORDER BY r.concentrationRisk DESC")
    List<RiskAssessment> findHighConcentrationAssessments(@Param("concentrationThreshold") BigDecimal concentrationThreshold);

    /**
     * Find assessments with low Sharpe ratio
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.sharpeRatio IS NOT NULL AND r.sharpeRatio <= :sharpeThreshold " +
           "ORDER BY r.sharpeRatio ASC")
    List<RiskAssessment> findLowSharpeRatioAssessments(@Param("sharpeThreshold") BigDecimal sharpeThreshold);

    /**
     * Find assessments with high leverage
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.leverage >= :leverageThreshold ORDER BY r.leverage DESC")
    List<RiskAssessment> findHighLeverageAssessments(@Param("leverageThreshold") BigDecimal leverageThreshold);

    /**
     * Count assessments by risk level
     */
    @Query("SELECT r.riskLevel, COUNT(r) FROM RiskAssessment r GROUP BY r.riskLevel")
    List<Object[]> countAssessmentsByRiskLevel();

    /**
     * Count assessments by type
     */
    @Query("SELECT r.assessmentType, COUNT(r) FROM RiskAssessment r GROUP BY r.assessmentType")
    List<Object[]> countAssessmentsByType();

    /**
     * Get risk statistics
     */
    @Query("SELECT " +
           "COUNT(r) as totalAssessments, " +
           "SUM(CASE WHEN r.riskLevel IN ('HIGH', 'VERY_HIGH', 'CRITICAL') THEN 1 ELSE 0 END) as highRiskCount, " +
           "AVG(r.var1Day95) as averageVar, " +
           "AVG(r.currentDrawdown) as averageDrawdown, " +
           "AVG(r.sharpeRatio) as averageSharpeRatio, " +
           "AVG(r.concentrationRisk) as averageConcentrationRisk " +
           "FROM RiskAssessment r WHERE r.assessmentDate >= :since")
    Object[] getRiskStatistics(@Param("since") LocalDateTime since);

    /**
     * Find assessments due for review
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.nextAssessmentDue <= :currentTime " +
           "ORDER BY r.nextAssessmentDue ASC")
    List<RiskAssessment> findAssessmentsDueForReview(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find recent assessments
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.assessmentDate >= :since ORDER BY r.assessmentDate DESC")
    List<RiskAssessment> findRecentAssessments(@Param("since") LocalDateTime since);

    /**
     * Find assessments with limit breaches
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.limitBreachesCount > 0 OR r.criticalBreachesCount > 0 " +
           "ORDER BY r.criticalBreachesCount DESC, r.limitBreachesCount DESC")
    List<RiskAssessment> findAssessmentsWithBreaches();

    /**
     * Find assessments by portfolio value range
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.portfolioValue BETWEEN :minValue AND :maxValue " +
           "ORDER BY r.portfolioValue DESC")
    List<RiskAssessment> findByPortfolioValueRange(@Param("minValue") BigDecimal minValue,
                                                  @Param("maxValue") BigDecimal maxValue);

    /**
     * Get daily risk summary
     */
    @Query("SELECT " +
           "DATE(r.assessmentDate) as assessmentDate, " +
           "COUNT(r) as totalAssessments, " +
           "SUM(CASE WHEN r.riskLevel = 'CRITICAL' THEN 1 ELSE 0 END) as criticalCount, " +
           "SUM(CASE WHEN r.riskLevel = 'VERY_HIGH' THEN 1 ELSE 0 END) as veryHighCount, " +
           "SUM(CASE WHEN r.riskLevel = 'HIGH' THEN 1 ELSE 0 END) as highCount, " +
           "AVG(r.var1Day95) as averageVar, " +
           "AVG(r.currentDrawdown) as averageDrawdown " +
           "FROM RiskAssessment r " +
           "WHERE r.assessmentDate >= :startDate " +
           "GROUP BY DATE(r.assessmentDate) " +
           "ORDER BY DATE(r.assessmentDate) DESC")
    List<Object[]> getDailyRiskSummary(@Param("startDate") LocalDateTime startDate);

    /**
     * Find assessments with poor data quality
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.dataQualityScore < :qualityThreshold " +
           "ORDER BY r.dataQualityScore ASC")
    List<RiskAssessment> findPoorDataQualityAssessments(@Param("qualityThreshold") BigDecimal qualityThreshold);

    /**
     * Find assessments with low model accuracy
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.modelAccuracy < :accuracyThreshold " +
           "ORDER BY r.modelAccuracy ASC")
    List<RiskAssessment> findLowModelAccuracyAssessments(@Param("accuracyThreshold") BigDecimal accuracyThreshold);

    /**
     * Get risk trend analysis
     */
    @Query("SELECT " +
           "r.portfolioId, " +
           "r.portfolioName, " +
           "COUNT(r) as assessmentCount, " +
           "AVG(r.var1Day95) as averageVar, " +
           "MIN(r.var1Day95) as minVar, " +
           "MAX(r.var1Day95) as maxVar, " +
           "STDDEV(r.var1Day95) as varVolatility " +
           "FROM RiskAssessment r " +
           "WHERE r.assessmentDate >= :startDate " +
           "GROUP BY r.portfolioId, r.portfolioName " +
           "ORDER BY AVG(r.var1Day95) DESC")
    List<Object[]> getRiskTrendAnalysis(@Param("startDate") LocalDateTime startDate);

    /**
     * Find top risky portfolios
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.id IN " +
           "(SELECT MAX(r2.id) FROM RiskAssessment r2 GROUP BY r2.portfolioId) " +
           "ORDER BY r.riskLevel DESC, r.var1Day95 DESC")
    List<RiskAssessment> findTopRiskyPortfolios(Pageable pageable);

    /**
     * Find assessments by confidence level range
     */
    @Query("SELECT r FROM RiskAssessment r WHERE r.confidenceLevel BETWEEN :minConfidence AND :maxConfidence " +
           "ORDER BY r.confidenceLevel DESC")
    List<RiskAssessment> findByConfidenceLevelRange(@Param("minConfidence") BigDecimal minConfidence,
                                                   @Param("maxConfidence") BigDecimal maxConfidence);

    /**
     * Delete old assessments
     */
    @Query("DELETE FROM RiskAssessment r WHERE r.assessmentDate < :cutoffDate AND " +
           "r.assessmentType NOT IN ('REGULATORY', 'STRESS_TEST')")
    void deleteOldAssessments(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find assessments by multiple risk levels
     */
    List<RiskAssessment> findByRiskLevelIn(List<RiskAssessment.RiskLevel> riskLevels);

    /**
     * Find assessments by multiple portfolios
     */
    List<RiskAssessment> findByPortfolioIdIn(List<UUID> portfolioIds);

    /**
     * Check if assessment exists for portfolio and date
     */
    boolean existsByPortfolioIdAndAssessmentDateBetween(UUID portfolioId, LocalDateTime startDate, LocalDateTime endDate);
}

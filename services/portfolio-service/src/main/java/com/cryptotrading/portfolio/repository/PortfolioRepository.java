package com.cryptotrading.portfolio.repository;

import com.cryptotrading.portfolio.model.Portfolio;
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
 * Repository for Portfolio entities
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    /**
     * Find portfolio by name
     */
    Optional<Portfolio> findByName(String name);

    /**
     * Find portfolios by user ID
     */
    List<Portfolio> findByUserId(String userId);

    /**
     * Find portfolios by type
     */
    List<Portfolio> findByType(Portfolio.PortfolioType type);

    /**
     * Find portfolios by status
     */
    List<Portfolio> findByStatus(Portfolio.PortfolioStatus status);

    /**
     * Find active portfolios
     */
    @Query("SELECT p FROM Portfolio p WHERE p.status = 'ACTIVE'")
    List<Portfolio> findActivePortfolios();

    /**
     * Find portfolios by user and status
     */
    List<Portfolio> findByUserIdAndStatus(String userId, Portfolio.PortfolioStatus status);

    /**
     * Find portfolios by user and type
     */
    List<Portfolio> findByUserIdAndType(String userId, Portfolio.PortfolioType type);

    /**
     * Find top performing portfolios
     */
    @Query("SELECT p FROM Portfolio p WHERE p.totalReturn IS NOT NULL ORDER BY p.totalReturn DESC")
    Page<Portfolio> findTopPerformingPortfolios(Pageable pageable);

    /**
     * Find profitable portfolios
     */
    @Query("SELECT p FROM Portfolio p WHERE p.totalPnl > 0 ORDER BY p.totalPnl DESC")
    List<Portfolio> findProfitablePortfolios();

    /**
     * Find losing portfolios
     */
    @Query("SELECT p FROM Portfolio p WHERE p.totalPnl < 0 ORDER BY p.totalPnl ASC")
    List<Portfolio> findLosingPortfolios();

    /**
     * Find portfolios with high Sharpe ratio
     */
    @Query("SELECT p FROM Portfolio p WHERE p.sharpeRatio >= :minSharpeRatio ORDER BY p.sharpeRatio DESC")
    List<Portfolio> findHighSharpeRatioPortfolios(@Param("minSharpeRatio") BigDecimal minSharpeRatio);

    /**
     * Find portfolios with low drawdown
     */
    @Query("SELECT p FROM Portfolio p WHERE p.maxDrawdown <= :maxDrawdown ORDER BY p.maxDrawdown ASC")
    List<Portfolio> findLowDrawdownPortfolios(@Param("maxDrawdown") BigDecimal maxDrawdown);

    /**
     * Find portfolios by value range
     */
    @Query("SELECT p FROM Portfolio p WHERE p.totalValue BETWEEN :minValue AND :maxValue ORDER BY p.totalValue DESC")
    List<Portfolio> findPortfoliosByValueRange(@Param("minValue") BigDecimal minValue, 
                                               @Param("maxValue") BigDecimal maxValue);

    /**
     * Find portfolios created within date range
     */
    @Query("SELECT p FROM Portfolio p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Portfolio> findPortfoliosCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    /**
     * Find portfolios with recent activity
     */
    @Query("SELECT p FROM Portfolio p WHERE p.lastValuationAt >= :since ORDER BY p.lastValuationAt DESC")
    List<Portfolio> findRecentlyActivePortfolios(@Param("since") LocalDateTime since);

    /**
     * Find portfolios needing rebalancing
     */
    @Query("SELECT p FROM Portfolio p WHERE p.status = 'ACTIVE' AND " +
           "(p.lastValuationAt IS NULL OR p.lastValuationAt < :threshold)")
    List<Portfolio> findPortfoliosNeedingRebalancing(@Param("threshold") LocalDateTime threshold);

    /**
     * Find portfolios exceeding risk limits
     */
    @Query("SELECT p FROM Portfolio p WHERE p.status = 'ACTIVE' AND " +
           "(p.maxDrawdown > :maxDrawdownLimit OR " +
           "(p.maxDailyLoss IS NOT NULL AND p.dailyPnl < -p.maxDailyLoss))")
    List<Portfolio> findPortfoliosExceedingRiskLimits(@Param("maxDrawdownLimit") BigDecimal maxDrawdownLimit);

    /**
     * Calculate total AUM (Assets Under Management)
     */
    @Query("SELECT SUM(p.totalValue) FROM Portfolio p WHERE p.status = 'ACTIVE'")
    Optional<BigDecimal> calculateTotalAUM();

    /**
     * Calculate total AUM by type
     */
    @Query("SELECT p.type, SUM(p.totalValue) FROM Portfolio p WHERE p.status = 'ACTIVE' GROUP BY p.type")
    List<Object[]> calculateAUMByType();

    /**
     * Calculate total AUM by user
     */
    @Query("SELECT p.userId, SUM(p.totalValue) FROM Portfolio p WHERE p.status = 'ACTIVE' GROUP BY p.userId ORDER BY SUM(p.totalValue) DESC")
    List<Object[]> calculateAUMByUser();

    /**
     * Get portfolio performance statistics
     */
    @Query("SELECT " +
           "COUNT(p) as totalPortfolios, " +
           "SUM(CASE WHEN p.totalPnl > 0 THEN 1 ELSE 0 END) as profitablePortfolios, " +
           "AVG(p.totalReturn) as averageReturn, " +
           "AVG(p.sharpeRatio) as averageSharpeRatio, " +
           "AVG(p.maxDrawdown) as averageDrawdown, " +
           "SUM(p.totalValue) as totalAUM " +
           "FROM Portfolio p WHERE p.status = 'ACTIVE'")
    Object[] getPerformanceStatistics();

    /**
     * Find portfolios by return range
     */
    @Query("SELECT p FROM Portfolio p WHERE p.totalReturn BETWEEN :minReturn AND :maxReturn ORDER BY p.totalReturn DESC")
    List<Portfolio> findPortfoliosByReturnRange(@Param("minReturn") BigDecimal minReturn,
                                                @Param("maxReturn") BigDecimal maxReturn);

    /**
     * Find portfolios with specific risk tolerance
     */
    List<Portfolio> findByRiskTolerance(String riskTolerance);

    /**
     * Find portfolios by base currency
     */
    List<Portfolio> findByBaseCurrency(String baseCurrency);

    /**
     * Count portfolios by status
     */
    @Query("SELECT p.status, COUNT(p) FROM Portfolio p GROUP BY p.status")
    List<Object[]> countPortfoliosByStatus();

    /**
     * Count portfolios by type
     */
    @Query("SELECT p.type, COUNT(p) FROM Portfolio p GROUP BY p.type")
    List<Object[]> countPortfoliosByType();

    /**
     * Find portfolios with high leverage
     */
    @Query("SELECT p FROM Portfolio p WHERE p.status = 'ACTIVE' AND " +
           "(p.totalValue > 0 AND (SELECT SUM(pos.marketValue) FROM Position pos WHERE pos.portfolio = p) / p.totalValue > :leverageThreshold)")
    List<Portfolio> findHighLeveragePortfolios(@Param("leverageThreshold") BigDecimal leverageThreshold);

    /**
     * Find portfolios with low cash utilization
     */
    @Query("SELECT p FROM Portfolio p WHERE p.status = 'ACTIVE' AND " +
           "p.currentCapital > 0 AND (p.availableCash / p.currentCapital) > :cashThreshold")
    List<Portfolio> findLowCashUtilizationPortfolios(@Param("cashThreshold") BigDecimal cashThreshold);

    /**
     * Get daily portfolio summary
     */
    @Query("SELECT " +
           "DATE(p.updatedAt) as date, " +
           "COUNT(p) as totalPortfolios, " +
           "SUM(p.totalValue) as totalValue, " +
           "AVG(p.dailyReturn) as averageDailyReturn, " +
           "SUM(p.dailyPnl) as totalDailyPnl " +
           "FROM Portfolio p " +
           "WHERE p.updatedAt >= :startDate " +
           "GROUP BY DATE(p.updatedAt) " +
           "ORDER BY DATE(p.updatedAt) DESC")
    List<Object[]> getDailyPortfolioSummary(@Param("startDate") LocalDateTime startDate);

    /**
     * Find portfolios requiring attention
     */
    @Query("SELECT p FROM Portfolio p WHERE " +
           "p.status = 'ACTIVE' AND (" +
           "p.maxDrawdown > :criticalDrawdown OR " +
           "(p.maxDailyLoss IS NOT NULL AND p.dailyPnl < -p.maxDailyLoss * 0.8) OR " +
           "p.lastValuationAt < :staleThreshold)")
    List<Portfolio> findPortfoliosRequiringAttention(@Param("criticalDrawdown") BigDecimal criticalDrawdown,
                                                     @Param("staleThreshold") LocalDateTime staleThreshold);

    /**
     * Find best performing portfolios in period
     */
    @Query("SELECT p FROM Portfolio p WHERE p.updatedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY p.totalReturn DESC")
    List<Portfolio> findBestPerformingInPeriod(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable);

    /**
     * Find worst performing portfolios in period
     */
    @Query("SELECT p FROM Portfolio p WHERE p.updatedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY p.totalReturn ASC")
    List<Portfolio> findWorstPerformingInPeriod(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                Pageable pageable);

    /**
     * Search portfolios by name or description
     */
    @Query("SELECT p FROM Portfolio p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Portfolio> searchPortfolios(@Param("searchTerm") String searchTerm);

    /**
     * Check if portfolio name exists for user
     */
    boolean existsByUserIdAndName(String userId, String name);

    /**
     * Delete old closed portfolios
     */
    @Query("DELETE FROM Portfolio p WHERE p.status = 'CLOSED' AND p.updatedAt < :cutoffDate")
    void deleteOldClosedPortfolios(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find portfolios by multiple statuses
     */
    List<Portfolio> findByStatusIn(List<Portfolio.PortfolioStatus> statuses);

    /**
     * Get portfolio allocation summary
     */
    @Query("SELECT " +
           "p.baseCurrency, " +
           "COUNT(p) as portfolioCount, " +
           "SUM(p.totalValue) as totalValue, " +
           "AVG(p.totalReturn) as averageReturn " +
           "FROM Portfolio p " +
           "WHERE p.status = 'ACTIVE' " +
           "GROUP BY p.baseCurrency " +
           "ORDER BY SUM(p.totalValue) DESC")
    List<Object[]> getPortfolioAllocationSummary();
}

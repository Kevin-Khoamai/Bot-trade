package com.cryptotrading.strategy.repository;

import com.cryptotrading.strategy.model.TradingStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TradingStrategy entities
 */
@Repository
public interface TradingStrategyRepository extends JpaRepository<TradingStrategy, UUID> {

    /**
     * Find strategy by name
     */
    Optional<TradingStrategy> findByName(String name);

    /**
     * Find strategies by status
     */
    List<TradingStrategy> findByStatus(TradingStrategy.StrategyStatus status);

    /**
     * Find strategies by type
     */
    List<TradingStrategy> findByType(TradingStrategy.StrategyType type);

    /**
     * Find strategies by symbol
     */
    List<TradingStrategy> findBySymbol(String symbol);

    /**
     * Find active strategies (LIVE or PAPER_TRADING)
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.status IN ('LIVE', 'PAPER_TRADING')")
    List<TradingStrategy> findActiveStrategies();

    /**
     * Find strategies by symbol and status
     */
    List<TradingStrategy> findBySymbolAndStatus(String symbol, TradingStrategy.StrategyStatus status);

    /**
     * Find strategies by type and status
     */
    List<TradingStrategy> findByTypeAndStatus(TradingStrategy.StrategyType type, TradingStrategy.StrategyStatus status);

    /**
     * Find strategies that haven't been executed recently
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.status = 'LIVE' AND " +
           "(s.lastExecutedAt IS NULL OR s.lastExecutedAt < :threshold)")
    List<TradingStrategy> findStaleStrategies(@Param("threshold") LocalDateTime threshold);

    /**
     * Find top performing strategies by total PnL
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.totalPnl IS NOT NULL " +
           "ORDER BY s.totalPnl DESC")
    Page<TradingStrategy> findTopPerformingStrategies(Pageable pageable);

    /**
     * Find strategies with positive PnL
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.totalPnl > 0 ORDER BY s.totalPnl DESC")
    List<TradingStrategy> findProfitableStrategies();

    /**
     * Find strategies with negative PnL
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.totalPnl < 0 ORDER BY s.totalPnl ASC")
    List<TradingStrategy> findLosingStrategies();

    /**
     * Find strategies by win rate threshold
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.totalTrades > 0 AND " +
           "(s.winningTrades * 100.0 / s.totalTrades) >= :minWinRate")
    List<TradingStrategy> findStrategiesByMinWinRate(@Param("minWinRate") Double minWinRate);

    /**
     * Find strategies with high Sharpe ratio
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.sharpeRatio >= :minSharpeRatio " +
           "ORDER BY s.sharpeRatio DESC")
    List<TradingStrategy> findHighSharpeRatioStrategies(@Param("minSharpeRatio") Double minSharpeRatio);

    /**
     * Find strategies created within date range
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY s.createdAt DESC")
    List<TradingStrategy> findStrategiesCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Find strategies with recent activity
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.lastExecutedAt >= :since " +
           "ORDER BY s.lastExecutedAt DESC")
    List<TradingStrategy> findRecentlyActiveStrategies(@Param("since") LocalDateTime since);

    /**
     * Count strategies by status
     */
    @Query("SELECT s.status, COUNT(s) FROM TradingStrategy s GROUP BY s.status")
    List<Object[]> countStrategiesByStatus();

    /**
     * Count strategies by type
     */
    @Query("SELECT s.type, COUNT(s) FROM TradingStrategy s GROUP BY s.type")
    List<Object[]> countStrategiesByType();

    /**
     * Get strategy performance summary
     */
    @Query("SELECT " +
           "COUNT(s) as totalStrategies, " +
           "SUM(CASE WHEN s.totalPnl > 0 THEN 1 ELSE 0 END) as profitableStrategies, " +
           "SUM(s.totalPnl) as totalPnl, " +
           "AVG(s.totalPnl) as averagePnl, " +
           "SUM(s.totalTrades) as totalTrades " +
           "FROM TradingStrategy s")
    Object[] getPerformanceSummary();

    /**
     * Find strategies needing attention (poor performance or errors)
     */
    @Query("SELECT s FROM TradingStrategy s WHERE " +
           "s.status = 'ERROR' OR " +
           "(s.totalTrades > 10 AND s.totalPnl < 0) OR " +
           "(s.totalTrades > 5 AND (s.winningTrades * 100.0 / s.totalTrades) < 30)")
    List<TradingStrategy> findStrategiesNeedingAttention();

    /**
     * Find strategies ready for live trading (good backtest results)
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.status = 'PAPER_TRADING' AND " +
           "s.totalTrades >= 20 AND s.totalPnl > 0 AND " +
           "(s.winningTrades * 100.0 / s.totalTrades) >= 60")
    List<TradingStrategy> findStrategiesReadyForLive();

    /**
     * Search strategies by name or description
     */
    @Query("SELECT s FROM TradingStrategy s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<TradingStrategy> searchStrategies(@Param("searchTerm") String searchTerm);

    /**
     * Find strategies with specific configuration
     */
    @Query("SELECT s FROM TradingStrategy s WHERE s.configuration LIKE %:configPattern%")
    List<TradingStrategy> findByConfigurationPattern(@Param("configPattern") String configPattern);

    /**
     * Check if strategy name exists
     */
    boolean existsByName(String name);

    /**
     * Delete strategies older than specified date
     */
    @Query("DELETE FROM TradingStrategy s WHERE s.createdAt < :cutoffDate AND s.status = 'DRAFT'")
    void deleteOldDraftStrategies(@Param("cutoffDate") LocalDateTime cutoffDate);
}

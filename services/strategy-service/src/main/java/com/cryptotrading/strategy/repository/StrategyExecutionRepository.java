package com.cryptotrading.strategy.repository;

import com.cryptotrading.strategy.model.StrategyExecution;
import com.cryptotrading.strategy.model.TradingStrategy;
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
 * Repository for StrategyExecution entities
 */
@Repository
public interface StrategyExecutionRepository extends JpaRepository<StrategyExecution, UUID> {

    /**
     * Find executions by strategy
     */
    List<StrategyExecution> findByStrategy(TradingStrategy strategy);

    /**
     * Find executions by strategy ID
     */
    List<StrategyExecution> findByStrategyId(UUID strategyId);

    /**
     * Find executions by symbol
     */
    List<StrategyExecution> findBySymbol(String symbol);

    /**
     * Find executions by status
     */
    List<StrategyExecution> findByStatus(StrategyExecution.ExecutionStatus status);

    /**
     * Find executions by execution type
     */
    List<StrategyExecution> findByExecutionType(StrategyExecution.ExecutionType executionType);

    /**
     * Find open positions
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.status IN ('FILLED', 'OPEN')")
    List<StrategyExecution> findOpenPositions();

    /**
     * Find open positions by strategy
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.strategy = :strategy AND e.status IN ('FILLED', 'OPEN')")
    List<StrategyExecution> findOpenPositionsByStrategy(@Param("strategy") TradingStrategy strategy);

    /**
     * Find open positions by symbol
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.symbol = :symbol AND e.status IN ('FILLED', 'OPEN')")
    List<StrategyExecution> findOpenPositionsBySymbol(@Param("symbol") String symbol);

    /**
     * Find executions within date range
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY e.createdAt DESC")
    List<StrategyExecution> findExecutionsBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Find executions by strategy within date range
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.strategy = :strategy AND " +
           "e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
    List<StrategyExecution> findExecutionsByStrategyBetween(@Param("strategy") TradingStrategy strategy,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Find profitable executions
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.netPnl > 0 ORDER BY e.netPnl DESC")
    List<StrategyExecution> findProfitableExecutions();

    /**
     * Find losing executions
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.netPnl < 0 ORDER BY e.netPnl ASC")
    List<StrategyExecution> findLosingExecutions();

    /**
     * Find executions by strategy and side
     */
    List<StrategyExecution> findByStrategyAndSide(TradingStrategy strategy, StrategyExecution.TradeSide side);

    /**
     * Find recent executions
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<StrategyExecution> findRecentExecutions(@Param("since") LocalDateTime since);

    /**
     * Find executions needing stop loss check
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.status = 'OPEN' AND e.stopLossPrice IS NOT NULL")
    List<StrategyExecution> findExecutionsWithStopLoss();

    /**
     * Find executions needing take profit check
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.status = 'OPEN' AND e.takeProfitPrice IS NOT NULL")
    List<StrategyExecution> findExecutionsWithTakeProfit();

    /**
     * Calculate total PnL by strategy
     */
    @Query("SELECT SUM(e.netPnl) FROM StrategyExecution e WHERE e.strategy = :strategy AND e.netPnl IS NOT NULL")
    Optional<BigDecimal> calculateTotalPnlByStrategy(@Param("strategy") TradingStrategy strategy);

    /**
     * Calculate total PnL by symbol
     */
    @Query("SELECT SUM(e.netPnl) FROM StrategyExecution e WHERE e.symbol = :symbol AND e.netPnl IS NOT NULL")
    Optional<BigDecimal> calculateTotalPnlBySymbol(@Param("symbol") String symbol);

    /**
     * Count executions by strategy and status
     */
    @Query("SELECT COUNT(e) FROM StrategyExecution e WHERE e.strategy = :strategy AND e.status = :status")
    Long countByStrategyAndStatus(@Param("strategy") TradingStrategy strategy,
                                  @Param("status") StrategyExecution.ExecutionStatus status);

    /**
     * Count winning trades by strategy
     */
    @Query("SELECT COUNT(e) FROM StrategyExecution e WHERE e.strategy = :strategy AND e.netPnl > 0")
    Long countWinningTradesByStrategy(@Param("strategy") TradingStrategy strategy);

    /**
     * Count losing trades by strategy
     */
    @Query("SELECT COUNT(e) FROM StrategyExecution e WHERE e.strategy = :strategy AND e.netPnl < 0")
    Long countLosingTradesByStrategy(@Param("strategy") TradingStrategy strategy);

    /**
     * Get average trade duration by strategy
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (e.exitTime - e.entryTime))/3600) FROM StrategyExecution e " +
           "WHERE e.strategy = :strategy AND e.exitTime IS NOT NULL")
    Optional<Double> getAverageTradeDurationHours(@Param("strategy") TradingStrategy strategy);

    /**
     * Get largest win by strategy
     */
    @Query("SELECT MAX(e.netPnl) FROM StrategyExecution e WHERE e.strategy = :strategy AND e.netPnl > 0")
    Optional<BigDecimal> getLargestWinByStrategy(@Param("strategy") TradingStrategy strategy);

    /**
     * Get largest loss by strategy
     */
    @Query("SELECT MIN(e.netPnl) FROM StrategyExecution e WHERE e.strategy = :strategy AND e.netPnl < 0")
    Optional<BigDecimal> getLargestLossByStrategy(@Param("strategy") TradingStrategy strategy);

    /**
     * Get average win by strategy
     */
    @Query("SELECT AVG(e.netPnl) FROM StrategyExecution e WHERE e.strategy = :strategy AND e.netPnl > 0")
    Optional<BigDecimal> getAverageWinByStrategy(@Param("strategy") TradingStrategy strategy);

    /**
     * Get average loss by strategy
     */
    @Query("SELECT AVG(e.netPnl) FROM StrategyExecution e WHERE e.strategy = :strategy AND e.netPnl < 0")
    Optional<BigDecimal> getAverageLossByStrategy(@Param("strategy") TradingStrategy strategy);

    /**
     * Find executions by exchange order ID
     */
    Optional<StrategyExecution> findByExchangeOrderId(String exchangeOrderId);

    /**
     * Find executions by exchange
     */
    List<StrategyExecution> findByExchange(String exchange);

    /**
     * Get execution statistics by strategy
     */
    @Query("SELECT " +
           "COUNT(e) as totalTrades, " +
           "SUM(CASE WHEN e.netPnl > 0 THEN 1 ELSE 0 END) as winningTrades, " +
           "SUM(CASE WHEN e.netPnl < 0 THEN 1 ELSE 0 END) as losingTrades, " +
           "SUM(e.netPnl) as totalPnl, " +
           "AVG(e.netPnl) as averagePnl, " +
           "MAX(e.netPnl) as largestWin, " +
           "MIN(e.netPnl) as largestLoss " +
           "FROM StrategyExecution e WHERE e.strategy = :strategy")
    Object[] getExecutionStatsByStrategy(@Param("strategy") TradingStrategy strategy);

    /**
     * Find executions with high unrealized PnL
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.status = 'OPEN' AND " +
           "ABS(e.unrealizedPnl) >= :threshold ORDER BY ABS(e.unrealizedPnl) DESC")
    List<StrategyExecution> findHighUnrealizedPnlExecutions(@Param("threshold") BigDecimal threshold);

    /**
     * Find long-running open positions
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.status = 'OPEN' AND " +
           "e.entryTime < :threshold ORDER BY e.entryTime ASC")
    List<StrategyExecution> findLongRunningPositions(@Param("threshold") LocalDateTime threshold);

    /**
     * Find executions by trigger reason pattern
     */
    @Query("SELECT e FROM StrategyExecution e WHERE e.triggerReason LIKE %:pattern%")
    List<StrategyExecution> findByTriggerReasonPattern(@Param("pattern") String pattern);

    /**
     * Get daily execution summary
     */
    @Query("SELECT " +
           "DATE(e.createdAt) as tradeDate, " +
           "COUNT(e) as totalTrades, " +
           "SUM(e.netPnl) as dailyPnl, " +
           "SUM(CASE WHEN e.netPnl > 0 THEN 1 ELSE 0 END) as winningTrades " +
           "FROM StrategyExecution e " +
           "WHERE e.createdAt >= :startDate " +
           "GROUP BY DATE(e.createdAt) " +
           "ORDER BY DATE(e.createdAt) DESC")
    List<Object[]> getDailyExecutionSummary(@Param("startDate") LocalDateTime startDate);

    /**
     * Delete old completed executions
     */
    @Query("DELETE FROM StrategyExecution e WHERE e.status = 'CLOSED' AND e.exitTime < :cutoffDate")
    void deleteOldCompletedExecutions(@Param("cutoffDate") LocalDateTime cutoffDate);
}

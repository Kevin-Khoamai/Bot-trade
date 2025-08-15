package com.cryptotrading.portfolio.repository;

import com.cryptotrading.portfolio.model.Position;
import com.cryptotrading.portfolio.model.Portfolio;
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
 * Repository for Position entities
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {

    /**
     * Find position by portfolio, symbol, and exchange
     */
    Optional<Position> findByPortfolioAndSymbolAndExchange(Portfolio portfolio, String symbol, String exchange);

    /**
     * Find positions by portfolio
     */
    List<Position> findByPortfolio(Portfolio portfolio);

    /**
     * Find positions by symbol
     */
    List<Position> findBySymbol(String symbol);

    /**
     * Find positions by exchange
     */
    List<Position> findByExchange(String exchange);

    /**
     * Find positions by side
     */
    List<Position> findBySide(Position.PositionSide side);

    /**
     * Find open positions
     */
    @Query("SELECT p FROM Position p WHERE p.side != 'FLAT' AND p.quantity != 0")
    List<Position> findOpenPositions();

    /**
     * Find open positions by portfolio
     */
    @Query("SELECT p FROM Position p WHERE p.portfolio = :portfolio AND p.side != 'FLAT' AND p.quantity != 0")
    List<Position> findOpenPositionsByPortfolio(@Param("portfolio") Portfolio portfolio);

    /**
     * Find open positions by symbol
     */
    @Query("SELECT p FROM Position p WHERE p.symbol = :symbol AND p.side != 'FLAT' AND p.quantity != 0")
    List<Position> findOpenPositionsBySymbol(@Param("symbol") String symbol);

    /**
     * Find profitable positions
     */
    @Query("SELECT p FROM Position p WHERE p.totalPnl > 0 ORDER BY p.totalPnl DESC")
    List<Position> findProfitablePositions();

    /**
     * Find losing positions
     */
    @Query("SELECT p FROM Position p WHERE p.totalPnl < 0 ORDER BY p.totalPnl ASC")
    List<Position> findLosingPositions();

    /**
     * Find positions by portfolio and profitable status
     */
    @Query("SELECT p FROM Position p WHERE p.portfolio = :portfolio AND p.totalPnl > 0 ORDER BY p.totalPnl DESC")
    List<Position> findProfitablePositionsByPortfolio(@Param("portfolio") Portfolio portfolio);

    /**
     * Find positions by portfolio and losing status
     */
    @Query("SELECT p FROM Position p WHERE p.portfolio = :portfolio AND p.totalPnl < 0 ORDER BY p.totalPnl ASC")
    List<Position> findLosingPositionsByPortfolio(@Param("portfolio") Portfolio portfolio);

    /**
     * Find large positions by value
     */
    @Query("SELECT p FROM Position p WHERE p.marketValue >= :threshold ORDER BY p.marketValue DESC")
    List<Position> findLargePositions(@Param("threshold") BigDecimal threshold);

    /**
     * Find positions with high unrealized P&L
     */
    @Query("SELECT p FROM Position p WHERE ABS(p.unrealizedPnl) >= :threshold ORDER BY ABS(p.unrealizedPnl) DESC")
    List<Position> findHighUnrealizedPnlPositions(@Param("threshold") BigDecimal threshold);

    /**
     * Find positions with stale prices
     */
    @Query("SELECT p FROM Position p WHERE p.lastPriceUpdate < :threshold")
    List<Position> findPositionsWithStalePrices(@Param("threshold") LocalDateTime threshold);

    /**
     * Find long positions
     */
    @Query("SELECT p FROM Position p WHERE p.side = 'LONG' AND p.quantity > 0")
    List<Position> findLongPositions();

    /**
     * Find short positions
     */
    @Query("SELECT p FROM Position p WHERE p.side = 'SHORT' AND p.quantity < 0")
    List<Position> findShortPositions();

    /**
     * Find positions by portfolio and side
     */
    List<Position> findByPortfolioAndSide(Portfolio portfolio, Position.PositionSide side);

    /**
     * Calculate total position value by portfolio
     */
    @Query("SELECT SUM(p.marketValue) FROM Position p WHERE p.portfolio = :portfolio AND p.marketValue IS NOT NULL")
    Optional<BigDecimal> calculateTotalPositionValueByPortfolio(@Param("portfolio") Portfolio portfolio);

    /**
     * Calculate total unrealized P&L by portfolio
     */
    @Query("SELECT SUM(p.unrealizedPnl) FROM Position p WHERE p.portfolio = :portfolio")
    Optional<BigDecimal> calculateTotalUnrealizedPnlByPortfolio(@Param("portfolio") Portfolio portfolio);

    /**
     * Calculate total realized P&L by portfolio
     */
    @Query("SELECT SUM(p.realizedPnl) FROM Position p WHERE p.portfolio = :portfolio")
    Optional<BigDecimal> calculateTotalRealizedPnlByPortfolio(@Param("portfolio") Portfolio portfolio);

    /**
     * Count positions by portfolio
     */
    @Query("SELECT COUNT(p) FROM Position p WHERE p.portfolio = :portfolio AND p.side != 'FLAT'")
    Long countOpenPositionsByPortfolio(@Param("portfolio") Portfolio portfolio);

    /**
     * Count positions by symbol
     */
    @Query("SELECT p.symbol, COUNT(p) FROM Position p WHERE p.side != 'FLAT' GROUP BY p.symbol ORDER BY COUNT(p) DESC")
    List<Object[]> countPositionsBySymbol();

    /**
     * Count positions by exchange
     */
    @Query("SELECT p.exchange, COUNT(p) FROM Position p WHERE p.side != 'FLAT' GROUP BY p.exchange")
    List<Object[]> countPositionsByExchange();

    /**
     * Get position statistics by portfolio
     */
    @Query("SELECT " +
           "COUNT(p) as totalPositions, " +
           "SUM(CASE WHEN p.totalPnl > 0 THEN 1 ELSE 0 END) as profitablePositions, " +
           "SUM(CASE WHEN p.totalPnl < 0 THEN 1 ELSE 0 END) as losingPositions, " +
           "SUM(p.marketValue) as totalValue, " +
           "SUM(p.totalPnl) as totalPnl, " +
           "AVG(p.totalPnl) as averagePnl " +
           "FROM Position p WHERE p.portfolio = :portfolio AND p.side != 'FLAT'")
    Object[] getPositionStatsByPortfolio(@Param("portfolio") Portfolio portfolio);

    /**
     * Find positions with high drawdown
     */
    @Query("SELECT p FROM Position p WHERE p.maxDrawdown >= :threshold ORDER BY p.maxDrawdown DESC")
    List<Position> findHighDrawdownPositions(@Param("threshold") BigDecimal threshold);

    /**
     * Find positions with low win rate
     */
    @Query("SELECT p FROM Position p WHERE p.tradeCount > 5 AND " +
           "(p.winningTrades * 100.0 / p.tradeCount) < :threshold ORDER BY (p.winningTrades * 100.0 / p.tradeCount) ASC")
    List<Position> findLowWinRatePositions(@Param("threshold") Double threshold);

    /**
     * Find positions by date range
     */
    @Query("SELECT p FROM Position p WHERE p.firstTradeDate BETWEEN :startDate AND :endDate ORDER BY p.firstTradeDate DESC")
    List<Position> findPositionsByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Find recently opened positions
     */
    @Query("SELECT p FROM Position p WHERE p.firstTradeDate >= :since ORDER BY p.firstTradeDate DESC")
    List<Position> findRecentlyOpenedPositions(@Param("since") LocalDateTime since);

    /**
     * Find recently closed positions
     */
    @Query("SELECT p FROM Position p WHERE p.closedAt >= :since ORDER BY p.closedAt DESC")
    List<Position> findRecentlyClosedPositions(@Param("since") LocalDateTime since);

    /**
     * Find long-running positions
     */
    @Query("SELECT p FROM Position p WHERE p.side != 'FLAT' AND p.firstTradeDate < :threshold ORDER BY p.firstTradeDate ASC")
    List<Position> findLongRunningPositions(@Param("threshold") LocalDateTime threshold);

    /**
     * Get top performing positions
     */
    @Query("SELECT p FROM Position p WHERE p.totalPnl IS NOT NULL ORDER BY p.totalPnl DESC")
    List<Position> findTopPerformingPositions(org.springframework.data.domain.Pageable pageable);

    /**
     * Get worst performing positions
     */
    @Query("SELECT p FROM Position p WHERE p.totalPnl IS NOT NULL ORDER BY p.totalPnl ASC")
    List<Position> findWorstPerformingPositions(org.springframework.data.domain.Pageable pageable);

    /**
     * Find positions with locked quantity
     */
    @Query("SELECT p FROM Position p WHERE p.lockedQuantity > 0")
    List<Position> findPositionsWithLockedQuantity();

    /**
     * Calculate total exposure by symbol
     */
    @Query("SELECT p.symbol, SUM(ABS(p.marketValue)) FROM Position p WHERE p.side != 'FLAT' GROUP BY p.symbol ORDER BY SUM(ABS(p.marketValue)) DESC")
    List<Object[]> calculateExposureBySymbol();

    /**
     * Calculate total exposure by exchange
     */
    @Query("SELECT p.exchange, SUM(ABS(p.marketValue)) FROM Position p WHERE p.side != 'FLAT' GROUP BY p.exchange ORDER BY SUM(ABS(p.marketValue)) DESC")
    List<Object[]> calculateExposureByExchange();

    /**
     * Find positions requiring price updates
     */
    @Query("SELECT p FROM Position p WHERE p.side != 'FLAT' AND " +
           "(p.lastPriceUpdate IS NULL OR p.lastPriceUpdate < :threshold)")
    List<Position> findPositionsRequiringPriceUpdate(@Param("threshold") LocalDateTime threshold);

    /**
     * Get daily position summary
     */
    @Query("SELECT " +
           "DATE(p.updatedAt) as date, " +
           "COUNT(p) as totalPositions, " +
           "SUM(p.marketValue) as totalValue, " +
           "SUM(p.dailyPnl) as totalDailyPnl, " +
           "AVG(p.dailyPnl) as averageDailyPnl " +
           "FROM Position p " +
           "WHERE p.updatedAt >= :startDate AND p.side != 'FLAT' " +
           "GROUP BY DATE(p.updatedAt) " +
           "ORDER BY DATE(p.updatedAt) DESC")
    List<Object[]> getDailyPositionSummary(@Param("startDate") LocalDateTime startDate);

    /**
     * Find positions by volatility range
     */
    @Query("SELECT p FROM Position p WHERE p.volatility BETWEEN :minVolatility AND :maxVolatility ORDER BY p.volatility DESC")
    List<Position> findPositionsByVolatilityRange(@Param("minVolatility") BigDecimal minVolatility,
                                                  @Param("maxVolatility") BigDecimal maxVolatility);

    /**
     * Delete old closed positions
     */
    @Query("DELETE FROM Position p WHERE p.side = 'FLAT' AND p.closedAt < :cutoffDate")
    void deleteOldClosedPositions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find positions by multiple symbols
     */
    List<Position> findBySymbolIn(List<String> symbols);

    /**
     * Find positions by multiple portfolios
     */
    List<Position> findByPortfolioIn(List<Portfolio> portfolios);
}

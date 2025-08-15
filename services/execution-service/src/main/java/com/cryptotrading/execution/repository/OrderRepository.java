package com.cryptotrading.execution.repository;

import com.cryptotrading.execution.model.Order;
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
 * Repository for Order entities
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find order by client order ID
     */
    Optional<Order> findByClientOrderId(String clientOrderId);

    /**
     * Find order by exchange order ID
     */
    Optional<Order> findByExchangeOrderId(String exchangeOrderId);

    /**
     * Find orders by strategy execution ID
     */
    List<Order> findByStrategyExecutionId(UUID strategyExecutionId);

    /**
     * Find orders by symbol
     */
    List<Order> findBySymbol(String symbol);

    /**
     * Find orders by exchange
     */
    List<Order> findByExchange(String exchange);

    /**
     * Find orders by status
     */
    List<Order> findByStatus(Order.OrderStatus status);

    /**
     * Find orders by side
     */
    List<Order> findBySide(Order.OrderSide side);

    /**
     * Find orders by type
     */
    List<Order> findByType(Order.OrderType type);

    /**
     * Find active orders (can be filled or cancelled)
     */
    @Query("SELECT o FROM Order o WHERE o.status IN ('SUBMITTED', 'ACKNOWLEDGED', 'PARTIALLY_FILLED')")
    List<Order> findActiveOrders();

    /**
     * Find active orders by symbol
     */
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.status IN ('SUBMITTED', 'ACKNOWLEDGED', 'PARTIALLY_FILLED')")
    List<Order> findActiveOrdersBySymbol(@Param("symbol") String symbol);

    /**
     * Find active orders by exchange
     */
    @Query("SELECT o FROM Order o WHERE o.exchange = :exchange AND o.status IN ('SUBMITTED', 'ACKNOWLEDGED', 'PARTIALLY_FILLED')")
    List<Order> findActiveOrdersByExchange(@Param("exchange") String exchange);

    /**
     * Find orders within date range
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findOrdersBetween(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Find orders by symbol within date range
     */
    @Query("SELECT o FROM Order o WHERE o.symbol = :symbol AND o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findOrdersBySymbolBetween(@Param("symbol") String symbol,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Find filled orders
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'FILLED' ORDER BY o.completedAt DESC")
    List<Order> findFilledOrders();

    /**
     * Find cancelled orders
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'CANCELLED' ORDER BY o.cancelledAt DESC")
    List<Order> findCancelledOrders();

    /**
     * Find rejected orders
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'REJECTED' ORDER BY o.completedAt DESC")
    List<Order> findRejectedOrders();

    /**
     * Find orders with errors
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'ERROR' OR o.errorCode IS NOT NULL ORDER BY o.updatedAt DESC")
    List<Order> findOrdersWithErrors();

    /**
     * Find large orders (above threshold)
     */
    @Query("SELECT o FROM Order o WHERE o.totalValue >= :threshold ORDER BY o.totalValue DESC")
    List<Order> findLargeOrders(@Param("threshold") BigDecimal threshold);

    /**
     * Find orders by urgency level
     */
    List<Order> findByUrgencyLevelGreaterThanEqual(Integer urgencyLevel);

    /**
     * Find stale orders (submitted but not acknowledged within time limit)
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'SUBMITTED' AND o.submittedAt < :threshold")
    List<Order> findStaleOrders(@Param("threshold") LocalDateTime threshold);

    /**
     * Find orders needing retry
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'ERROR' AND o.retryCount < 3 AND o.updatedAt < :threshold")
    List<Order> findOrdersNeedingRetry(@Param("threshold") LocalDateTime threshold);

    /**
     * Count orders by status
     */
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    /**
     * Count orders by exchange
     */
    @Query("SELECT o.exchange, COUNT(o) FROM Order o GROUP BY o.exchange")
    List<Object[]> countOrdersByExchange();

    /**
     * Count orders by symbol
     */
    @Query("SELECT o.symbol, COUNT(o) FROM Order o GROUP BY o.symbol ORDER BY COUNT(o) DESC")
    List<Object[]> countOrdersBySymbol();

    /**
     * Calculate total volume by symbol
     */
    @Query("SELECT o.symbol, SUM(o.filledQuantity) FROM Order o WHERE o.status = 'FILLED' GROUP BY o.symbol ORDER BY SUM(o.filledQuantity) DESC")
    List<Object[]> calculateVolumeBySymbol();

    /**
     * Calculate total value by exchange
     */
    @Query("SELECT o.exchange, SUM(o.totalValue) FROM Order o WHERE o.status = 'FILLED' GROUP BY o.exchange ORDER BY SUM(o.totalValue) DESC")
    List<Object[]> calculateValueByExchange();

    /**
     * Get execution statistics
     */
    @Query("SELECT " +
           "COUNT(o) as totalOrders, " +
           "SUM(CASE WHEN o.status = 'FILLED' THEN 1 ELSE 0 END) as filledOrders, " +
           "SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledOrders, " +
           "SUM(CASE WHEN o.status = 'REJECTED' THEN 1 ELSE 0 END) as rejectedOrders, " +
           "AVG(o.totalValue) as averageOrderValue, " +
           "SUM(o.totalFees) as totalFees " +
           "FROM Order o WHERE o.createdAt >= :since")
    Object[] getExecutionStatistics(@Param("since") LocalDateTime since);

    /**
     * Get fill rate by symbol
     */
    @Query("SELECT o.symbol, " +
           "COUNT(o) as totalOrders, " +
           "SUM(CASE WHEN o.status = 'FILLED' THEN 1 ELSE 0 END) as filledOrders, " +
           "(SUM(CASE WHEN o.status = 'FILLED' THEN 1 ELSE 0 END) * 100.0 / COUNT(o)) as fillRate " +
           "FROM Order o " +
           "WHERE o.createdAt >= :since " +
           "GROUP BY o.symbol " +
           "ORDER BY fillRate DESC")
    List<Object[]> getFillRateBySymbol(@Param("since") LocalDateTime since);

    /**
     * Get average execution time by exchange
     */
    @Query("SELECT o.exchange, " +
           "AVG(EXTRACT(EPOCH FROM (o.firstFillAt - o.submittedAt))) as avgExecutionTimeSeconds " +
           "FROM Order o " +
           "WHERE o.status = 'FILLED' AND o.submittedAt IS NOT NULL AND o.firstFillAt IS NOT NULL " +
           "GROUP BY o.exchange")
    List<Object[]> getAverageExecutionTimeByExchange();

    /**
     * Find orders with poor execution quality
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'FILLED' AND " +
           "(o.totalFees > o.totalValue * 0.01 OR " +  // High fees (>1%)
           "EXTRACT(EPOCH FROM (o.firstFillAt - o.submittedAt)) > 300)") // Slow execution (>5 min)
    List<Order> findPoorExecutionQualityOrders();

    /**
     * Find orders by execution algorithm
     */
    List<Order> findByExecutionAlgorithm(String executionAlgorithm);

    /**
     * Get daily order summary
     */
    @Query("SELECT " +
           "DATE(o.createdAt) as orderDate, " +
           "COUNT(o) as totalOrders, " +
           "SUM(CASE WHEN o.status = 'FILLED' THEN 1 ELSE 0 END) as filledOrders, " +
           "SUM(o.totalValue) as totalValue, " +
           "SUM(o.totalFees) as totalFees " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate " +
           "GROUP BY DATE(o.createdAt) " +
           "ORDER BY DATE(o.createdAt) DESC")
    List<Object[]> getDailyOrderSummary(@Param("startDate") LocalDateTime startDate);

    /**
     * Find orders requiring manual review
     */
    @Query("SELECT o FROM Order o WHERE " +
           "o.status = 'ERROR' OR " +
           "o.retryCount >= 3 OR " +
           "(o.status = 'SUBMITTED' AND o.submittedAt < :staleThreshold) OR " +
           "o.totalValue >= :largeOrderThreshold")
    List<Order> findOrdersRequiringReview(@Param("staleThreshold") LocalDateTime staleThreshold,
                                          @Param("largeOrderThreshold") BigDecimal largeOrderThreshold);

    /**
     * Check if client order ID exists
     */
    boolean existsByClientOrderId(String clientOrderId);

    /**
     * Delete old completed orders
     */
    @Query("DELETE FROM Order o WHERE o.status IN ('FILLED', 'CANCELLED', 'REJECTED') AND o.completedAt < :cutoffDate")
    void deleteOldCompletedOrders(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find orders by multiple statuses
     */
    List<Order> findByStatusIn(List<Order.OrderStatus> statuses);

    /**
     * Find recent orders
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :since ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(@Param("since") LocalDateTime since);

    /**
     * Find orders with partial fills
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PARTIALLY_FILLED' ORDER BY o.lastFillAt DESC")
    List<Order> findPartiallyFilledOrders();
}

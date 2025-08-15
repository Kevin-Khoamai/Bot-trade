package com.cryptotrading.apigateway.repository;

import com.cryptotrading.apigateway.model.PriceData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceDataRepository extends JpaRepository<PriceData, Long> {

    /**
     * Find the latest price for a specific symbol and exchange
     */
    @Query("SELECT p FROM PriceData p WHERE p.symbol = :symbol AND p.exchange = :exchange " +
           "ORDER BY p.timestamp DESC LIMIT 1")
    Optional<PriceData> findLatestBySymbolAndExchange(@Param("symbol") String symbol, 
                                                     @Param("exchange") String exchange);

    /**
     * Find historical prices for a symbol within a time range
     */
    @Query("SELECT p FROM PriceData p WHERE p.symbol = :symbol " +
           "AND p.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY p.timestamp ASC")
    List<PriceData> findBySymbolAndTimestampBetween(@Param("symbol") String symbol,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * Find historical prices with pagination
     */
    @Query("SELECT p FROM PriceData p WHERE p.symbol = :symbol " +
           "AND p.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY p.timestamp DESC")
    Page<PriceData> findBySymbolAndTimestampBetween(@Param("symbol") String symbol,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime,
                                                   Pageable pageable);

    /**
     * Find prices by interval type (1m, 5m, 15m, 1h, etc.)
     */
    @Query("SELECT p FROM PriceData p WHERE p.symbol = :symbol " +
           "AND p.intervalType = :intervalType " +
           "AND p.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY p.timestamp ASC")
    List<PriceData> findBySymbolAndIntervalTypeAndTimestampBetween(
            @Param("symbol") String symbol,
            @Param("intervalType") String intervalType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find all available symbols
     */
    @Query("SELECT DISTINCT p.symbol FROM PriceData p ORDER BY p.symbol")
    List<String> findDistinctSymbols();

    /**
     * Find all available exchanges
     */
    @Query("SELECT DISTINCT p.exchange FROM PriceData p ORDER BY p.exchange")
    List<String> findDistinctExchanges();

    /**
     * Get OHLCV data for candlestick charts
     */
    @Query("SELECT p FROM PriceData p WHERE p.symbol = :symbol " +
           "AND p.intervalType = :intervalType " +
           "AND p.timestamp BETWEEN :startTime AND :endTime " +
           "AND p.openPrice IS NOT NULL " +
           "ORDER BY p.timestamp ASC")
    List<PriceData> findOHLCVData(@Param("symbol") String symbol,
                                 @Param("intervalType") String intervalType,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    /**
     * Get latest prices for all symbols
     */
    @Query("SELECT p FROM PriceData p WHERE p.id IN (" +
           "SELECT MAX(p2.id) FROM PriceData p2 GROUP BY p2.symbol, p2.exchange" +
           ") ORDER BY p.symbol, p.exchange")
    List<PriceData> findLatestPricesForAllSymbols();

    /**
     * Get price statistics for a symbol
     */
    @Query("SELECT " +
           "MIN(p.price) as minPrice, " +
           "MAX(p.price) as maxPrice, " +
           "AVG(p.price) as avgPrice, " +
           "COUNT(p) as dataPoints " +
           "FROM PriceData p WHERE p.symbol = :symbol " +
           "AND p.timestamp BETWEEN :startTime AND :endTime")
    Object[] getPriceStatistics(@Param("symbol") String symbol,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * Delete old data (for cleanup)
     */
    @Query("DELETE FROM PriceData p WHERE p.timestamp < :cutoffTime")
    void deleteOldData(@Param("cutoffTime") LocalDateTime cutoffTime);
}

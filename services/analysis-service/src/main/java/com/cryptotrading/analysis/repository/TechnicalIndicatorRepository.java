package com.cryptotrading.analysis.repository;

import com.cryptotrading.analysis.model.TechnicalIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TechnicalIndicator entity
 */
@Repository
public interface TechnicalIndicatorRepository extends JpaRepository<TechnicalIndicator, UUID> {

    /**
     * Find indicator by symbol, type, timestamp and period
     */
    Optional<TechnicalIndicator> findBySymbolAndIndicatorTypeAndTimestampAndPeriod(
            String symbol, String indicatorType, LocalDateTime timestamp, Integer period);

    /**
     * Find latest indicator for a symbol and type
     */
    @Query("SELECT ti FROM TechnicalIndicator ti WHERE ti.symbol = :symbol " +
           "AND ti.indicatorType = :indicatorType AND ti.period = :period " +
           "ORDER BY ti.timestamp DESC LIMIT 1")
    Optional<TechnicalIndicator> findLatestBySymbolAndTypeAndPeriod(
            @Param("symbol") String symbol,
            @Param("indicatorType") String indicatorType,
            @Param("period") Integer period);

    /**
     * Find indicators for a symbol within time range
     */
    @Query("SELECT ti FROM TechnicalIndicator ti WHERE ti.symbol = :symbol " +
           "AND ti.indicatorType = :indicatorType " +
           "AND ti.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY ti.timestamp ASC")
    List<TechnicalIndicator> findBySymbolAndTypeAndTimestampBetween(
            @Param("symbol") String symbol,
            @Param("indicatorType") String indicatorType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find recent indicators for a symbol and type
     */
    @Query("SELECT ti FROM TechnicalIndicator ti WHERE ti.symbol = :symbol " +
           "AND ti.indicatorType = :indicatorType AND ti.period = :period " +
           "ORDER BY ti.timestamp DESC LIMIT :limit")
    List<TechnicalIndicator> findRecentBySymbolAndTypeAndPeriod(
            @Param("symbol") String symbol,
            @Param("indicatorType") String indicatorType,
            @Param("period") Integer period,
            @Param("limit") int limit);

    /**
     * Find all indicators for a symbol at a specific timestamp
     */
    @Query("SELECT ti FROM TechnicalIndicator ti WHERE ti.symbol = :symbol " +
           "AND ti.timestamp = :timestamp ORDER BY ti.indicatorType")
    List<TechnicalIndicator> findBySymbolAndTimestamp(
            @Param("symbol") String symbol,
            @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find all indicator types for a symbol
     */
    @Query("SELECT DISTINCT ti.indicatorType FROM TechnicalIndicator ti WHERE ti.symbol = :symbol")
    List<String> findDistinctIndicatorTypesBySymbol(@Param("symbol") String symbol);

    /**
     * Find all symbols that have indicators
     */
    @Query("SELECT DISTINCT ti.symbol FROM TechnicalIndicator ti")
    List<String> findDistinctSymbols();

    /**
     * Count indicators for a symbol and type within time range
     */
    @Query("SELECT COUNT(ti) FROM TechnicalIndicator ti WHERE ti.symbol = :symbol " +
           "AND ti.indicatorType = :indicatorType " +
           "AND ti.timestamp BETWEEN :startTime AND :endTime")
    long countBySymbolAndTypeAndTimestampBetween(
            @Param("symbol") String symbol,
            @Param("indicatorType") String indicatorType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Delete old indicators before a certain timestamp
     */
    @Query("DELETE FROM TechnicalIndicator ti WHERE ti.timestamp < :cutoffTime")
    void deleteOldIndicators(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find indicators for multiple symbols
     */
    @Query("SELECT ti FROM TechnicalIndicator ti WHERE ti.symbol IN :symbols " +
           "AND ti.indicatorType = :indicatorType " +
           "AND ti.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY ti.symbol, ti.timestamp ASC")
    List<TechnicalIndicator> findBySymbolsAndTypeAndTimestampBetween(
            @Param("symbols") List<String> symbols,
            @Param("indicatorType") String indicatorType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find latest indicators for all symbols of a specific type
     */
    @Query("SELECT ti FROM TechnicalIndicator ti WHERE ti.indicatorType = :indicatorType " +
           "AND ti.period = :period AND ti.timestamp = (" +
           "SELECT MAX(ti2.timestamp) FROM TechnicalIndicator ti2 " +
           "WHERE ti2.symbol = ti.symbol AND ti2.indicatorType = :indicatorType " +
           "AND ti2.period = :period)")
    List<TechnicalIndicator> findLatestByTypeAndPeriod(
            @Param("indicatorType") String indicatorType,
            @Param("period") Integer period);

    /**
     * Check if indicator exists for specific parameters
     */
    @Query("SELECT CASE WHEN COUNT(ti) > 0 THEN true ELSE false END FROM TechnicalIndicator ti " +
           "WHERE ti.symbol = :symbol AND ti.indicatorType = :indicatorType " +
           "AND ti.timestamp = :timestamp AND ti.period = :period")
    boolean existsBySymbolAndTypeAndTimestampAndPeriod(
            @Param("symbol") String symbol,
            @Param("indicatorType") String indicatorType,
            @Param("timestamp") LocalDateTime timestamp,
            @Param("period") Integer period);

    /**
     * Find indicators with values in a specific range
     */
    @Query("SELECT ti FROM TechnicalIndicator ti WHERE ti.symbol = :symbol " +
           "AND ti.indicatorType = :indicatorType " +
           "AND ti.indicatorValue BETWEEN :minValue AND :maxValue " +
           "ORDER BY ti.timestamp DESC")
    List<TechnicalIndicator> findBySymbolAndTypeAndValueRange(
            @Param("symbol") String symbol,
            @Param("indicatorType") String indicatorType,
            @Param("minValue") java.math.BigDecimal minValue,
            @Param("maxValue") java.math.BigDecimal maxValue);
}

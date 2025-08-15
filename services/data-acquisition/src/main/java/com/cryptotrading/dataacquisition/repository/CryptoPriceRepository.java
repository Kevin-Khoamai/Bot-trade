package com.cryptotrading.dataacquisition.repository;

import com.cryptotrading.dataacquisition.model.CryptoPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CryptoPrice entity
 */
@Repository
public interface CryptoPriceRepository extends JpaRepository<CryptoPrice, UUID> {

    /**
     * Find price data by exchange, symbol and timestamp
     */
    Optional<CryptoPrice> findByExchangeAndSymbolAndTimestamp(String exchange, String symbol, LocalDateTime timestamp);

    /**
     * Find latest price for a symbol on an exchange
     */
    @Query("SELECT cp FROM CryptoPrice cp WHERE cp.exchange = :exchange AND cp.symbol = :symbol " +
           "ORDER BY cp.timestamp DESC LIMIT 1")
    Optional<CryptoPrice> findLatestByExchangeAndSymbol(@Param("exchange") String exchange, 
                                                       @Param("symbol") String symbol);

    /**
     * Find price data for a symbol within a time range
     */
    @Query("SELECT cp FROM CryptoPrice cp WHERE cp.symbol = :symbol " +
           "AND cp.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY cp.timestamp ASC")
    List<CryptoPrice> findBySymbolAndTimestampBetween(@Param("symbol") String symbol,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Find price data for multiple symbols within a time range
     */
    @Query("SELECT cp FROM CryptoPrice cp WHERE cp.symbol IN :symbols " +
           "AND cp.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY cp.symbol, cp.timestamp ASC")
    List<CryptoPrice> findBySymbolInAndTimestampBetween(@Param("symbols") List<String> symbols,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Find recent price data for a symbol (last N records)
     */
    @Query("SELECT cp FROM CryptoPrice cp WHERE cp.symbol = :symbol " +
           "ORDER BY cp.timestamp DESC LIMIT :limit")
    List<CryptoPrice> findRecentBySymbol(@Param("symbol") String symbol, @Param("limit") int limit);

    /**
     * Find all symbols for an exchange
     */
    @Query("SELECT DISTINCT cp.symbol FROM CryptoPrice cp WHERE cp.exchange = :exchange")
    List<String> findDistinctSymbolsByExchange(@Param("exchange") String exchange);

    /**
     * Find all exchanges for a symbol
     */
    @Query("SELECT DISTINCT cp.exchange FROM CryptoPrice cp WHERE cp.symbol = :symbol")
    List<String> findDistinctExchangesBySymbol(@Param("symbol") String symbol);

    /**
     * Count records for a symbol within a time range
     */
    @Query("SELECT COUNT(cp) FROM CryptoPrice cp WHERE cp.symbol = :symbol " +
           "AND cp.timestamp BETWEEN :startTime AND :endTime")
    long countBySymbolAndTimestampBetween(@Param("symbol") String symbol,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * Delete old data before a certain timestamp
     */
    @Query("DELETE FROM CryptoPrice cp WHERE cp.timestamp < :cutoffTime")
    void deleteOldData(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find price data with pagination support
     */
    @Query("SELECT cp FROM CryptoPrice cp WHERE cp.symbol = :symbol " +
           "AND cp.timestamp >= :startTime " +
           "ORDER BY cp.timestamp ASC")
    List<CryptoPrice> findBySymbolFromTimestamp(@Param("symbol") String symbol,
                                               @Param("startTime") LocalDateTime startTime);

    /**
     * Check if data exists for a specific exchange, symbol and time range
     */
    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END FROM CryptoPrice cp " +
           "WHERE cp.exchange = :exchange AND cp.symbol = :symbol " +
           "AND cp.timestamp BETWEEN :startTime AND :endTime")
    boolean existsByExchangeAndSymbolAndTimestampBetween(@Param("exchange") String exchange,
                                                        @Param("symbol") String symbol,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);
}

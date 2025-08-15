package com.cryptotrading.portfolio.service;

import com.cryptotrading.portfolio.model.Portfolio;
import com.cryptotrading.portfolio.model.Position;
import com.cryptotrading.portfolio.model.PortfolioSnapshot;
import com.cryptotrading.portfolio.repository.PortfolioRepository;
import com.cryptotrading.portfolio.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core service for portfolio management and position tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioManagementService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final PositionService positionService;
    private final PortfolioValuationService valuationService;
    private final RiskCalculationService riskCalculationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Listen to order fills from Execution Service
     */
    @KafkaListener(topics = "order-fills", groupId = "portfolio-management-group")
    public void processOrderFill(Map<String, Object> fillData) {
        try {
            log.info("Processing order fill: {}", fillData);

            // Extract fill information
            String symbol = (String) fillData.get("symbol");
            String side = (String) fillData.get("side");
            BigDecimal quantity = new BigDecimal(fillData.get("quantity").toString());
            BigDecimal price = new BigDecimal(fillData.get("price").toString());
            BigDecimal fee = new BigDecimal(fillData.get("fee").toString());
            LocalDateTime timestamp = LocalDateTime.parse(fillData.get("timestamp").toString());

            // Update positions for all affected portfolios
            updatePositionsForFill(symbol, side, quantity, price, fee, timestamp);

        } catch (Exception e) {
            log.error("Error processing order fill: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to order completions from Execution Service
     */
    @KafkaListener(topics = "order-completions", groupId = "portfolio-management-group")
    public void processOrderCompletion(Map<String, Object> completionData) {
        try {
            log.info("Processing order completion: {}", completionData);

            String symbol = (String) completionData.get("symbol");
            String side = (String) completionData.get("side");
            BigDecimal totalQuantity = new BigDecimal(completionData.get("filledQuantity").toString());
            BigDecimal averagePrice = new BigDecimal(completionData.get("averagePrice").toString());
            BigDecimal totalFees = new BigDecimal(completionData.get("totalFees").toString());
            LocalDateTime completedAt = LocalDateTime.parse(completionData.get("completedAt").toString());

            // Final position update
            finalizePositionsForOrder(symbol, side, totalQuantity, averagePrice, totalFees, completedAt);

        } catch (Exception e) {
            log.error("Error processing order completion: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to market price updates
     */
    @KafkaListener(topics = "price-updates", groupId = "portfolio-management-group")
    public void processPriceUpdate(Map<String, Object> priceData) {
        try {
            String symbol = (String) priceData.get("symbol");
            BigDecimal price = new BigDecimal(priceData.get("price").toString());
            LocalDateTime timestamp = LocalDateTime.parse(priceData.get("timestamp").toString());

            // Update all positions for this symbol
            updatePositionPrices(symbol, price, timestamp);

        } catch (Exception e) {
            log.error("Error processing price update: {}", e.getMessage(), e);
        }
    }

    /**
     * Update positions for order fill
     */
    @Transactional
    public void updatePositionsForFill(String symbol, String side, BigDecimal quantity, 
                                     BigDecimal price, BigDecimal fee, LocalDateTime timestamp) {
        try {
            // Find all open positions for this symbol
            List<Position> positions = positionRepository.findOpenPositionsBySymbol(symbol);

            for (Position position : positions) {
                // Determine trade quantity based on side
                BigDecimal tradeQuantity = "BUY".equals(side) ? quantity : quantity.negate();
                
                // Update position
                position.updateWithTrade(tradeQuantity, price, fee, timestamp);
                positionRepository.save(position);

                // Update portfolio valuation
                Portfolio portfolio = position.getPortfolio();
                portfolio.updateValuation();
                portfolioRepository.save(portfolio);

                log.info("Updated position for portfolio {}: {} {} at {}", 
                        portfolio.getName(), tradeQuantity, symbol, price);
            }

        } catch (Exception e) {
            log.error("Error updating positions for fill: {}", e.getMessage(), e);
        }
    }

    /**
     * Finalize positions for completed order
     */
    @Transactional
    public void finalizePositionsForOrder(String symbol, String side, BigDecimal totalQuantity, 
                                        BigDecimal averagePrice, BigDecimal totalFees, LocalDateTime completedAt) {
        try {
            // Update trade statistics for all affected portfolios
            List<Position> positions = positionRepository.findOpenPositionsBySymbol(symbol);

            for (Position position : positions) {
                Portfolio portfolio = position.getPortfolio();
                
                // Update portfolio trade statistics
                portfolio.setTotalTrades(portfolio.getTotalTrades() + 1);
                portfolio.setTotalFees(portfolio.getTotalFees().add(totalFees));
                
                // Determine if trade was profitable
                if (position.getTotalPnl().compareTo(BigDecimal.ZERO) > 0) {
                    portfolio.setWinningTrades(portfolio.getWinningTrades() + 1);
                    position.setWinningTrades(position.getWinningTrades() + 1);
                } else if (position.getTotalPnl().compareTo(BigDecimal.ZERO) < 0) {
                    portfolio.setLosingTrades(portfolio.getLosingTrades() + 1);
                    position.setLosingTrades(position.getLosingTrades() + 1);
                }

                portfolioRepository.save(portfolio);
                positionRepository.save(position);
            }

        } catch (Exception e) {
            log.error("Error finalizing positions for order: {}", e.getMessage(), e);
        }
    }

    /**
     * Update position prices
     */
    @Transactional
    public void updatePositionPrices(String symbol, BigDecimal price, LocalDateTime timestamp) {
        try {
            List<Position> positions = positionRepository.findOpenPositionsBySymbol(symbol);

            for (Position position : positions) {
                position.updatePrice(price, timestamp);
                positionRepository.save(position);

                // Update portfolio valuation
                Portfolio portfolio = position.getPortfolio();
                portfolio.updateValuation();
                portfolioRepository.save(portfolio);
            }

            log.debug("Updated {} positions for symbol {} with price {}", positions.size(), symbol, price);

        } catch (Exception e) {
            log.error("Error updating position prices for {}: {}", symbol, e.getMessage(), e);
        }
    }

    /**
     * Create new portfolio
     */
    @Transactional
    public Portfolio createPortfolio(String name, String description, String userId, 
                                   Portfolio.PortfolioType type, BigDecimal initialCapital) {
        try {
            // Check if portfolio name already exists for user
            if (portfolioRepository.existsByUserIdAndName(userId, name)) {
                throw new IllegalArgumentException("Portfolio name already exists for user");
            }

            Portfolio portfolio = Portfolio.builder()
                    .name(name)
                    .description(description)
                    .userId(userId)
                    .type(type)
                    .status(Portfolio.PortfolioStatus.ACTIVE)
                    .initialCapital(initialCapital)
                    .currentCapital(initialCapital)
                    .availableCash(initialCapital)
                    .totalValue(initialCapital)
                    .highWaterMark(initialCapital)
                    .baseCurrency("USDT")
                    .build();

            portfolio = portfolioRepository.save(portfolio);

            log.info("Created new portfolio: {} for user: {} with capital: {}", 
                    name, userId, initialCapital);

            // Publish portfolio creation event
            publishPortfolioEvent("PORTFOLIO_CREATED", portfolio);

            return portfolio;

        } catch (Exception e) {
            log.error("Error creating portfolio: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get portfolio by ID
     */
    public Portfolio getPortfolio(UUID portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
    }

    /**
     * Get portfolios by user
     */
    public List<Portfolio> getPortfoliosByUser(String userId) {
        return portfolioRepository.findByUserId(userId);
    }

    /**
     * Update portfolio valuation
     */
    @Transactional
    public void updatePortfolioValuation(UUID portfolioId) {
        try {
            Portfolio portfolio = getPortfolio(portfolioId);
            
            // Update all position prices first
            valuationService.updatePortfolioValuation(portfolio);
            
            // Calculate risk metrics
            riskCalculationService.calculatePortfolioRiskMetrics(portfolio);
            
            portfolioRepository.save(portfolio);

            log.debug("Updated valuation for portfolio: {}", portfolio.getName());

        } catch (Exception e) {
            log.error("Error updating portfolio valuation: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to update all portfolio valuations
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void updateAllPortfolioValuations() {
        try {
            List<Portfolio> activePortfolios = portfolioRepository.findActivePortfolios();
            
            for (Portfolio portfolio : activePortfolios) {
                updatePortfolioValuation(portfolio.getId());
            }

            log.debug("Updated valuations for {} active portfolios", activePortfolios.size());

        } catch (Exception e) {
            log.error("Error in scheduled portfolio valuation update: {}", e.getMessage(), e);
        }
    }

    /**
     * Create portfolio snapshot
     */
    @Transactional
    public PortfolioSnapshot createPortfolioSnapshot(UUID portfolioId, PortfolioSnapshot.SnapshotType type) {
        try {
            Portfolio portfolio = getPortfolio(portfolioId);
            
            // Update valuation before snapshot
            updatePortfolioValuation(portfolioId);
            
            PortfolioSnapshot snapshot = PortfolioSnapshot.createFromPortfolio(portfolio, type);
            
            // Add to portfolio
            portfolio.getSnapshots().add(snapshot);
            portfolioRepository.save(portfolio);

            log.info("Created {} snapshot for portfolio: {}", type, portfolio.getName());

            return snapshot;

        } catch (Exception e) {
            log.error("Error creating portfolio snapshot: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Scheduled task to create daily snapshots
     */
    @Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
    public void createDailySnapshots() {
        try {
            List<Portfolio> activePortfolios = portfolioRepository.findActivePortfolios();
            
            for (Portfolio portfolio : activePortfolios) {
                createPortfolioSnapshot(portfolio.getId(), PortfolioSnapshot.SnapshotType.DAILY);
            }

            log.info("Created daily snapshots for {} portfolios", activePortfolios.size());

        } catch (Exception e) {
            log.error("Error creating daily snapshots: {}", e.getMessage(), e);
        }
    }

    /**
     * Close portfolio
     */
    @Transactional
    public void closePortfolio(UUID portfolioId, String reason) {
        try {
            Portfolio portfolio = getPortfolio(portfolioId);
            
            // Close all open positions
            List<Position> openPositions = positionRepository.findOpenPositionsByPortfolio(portfolio);
            for (Position position : openPositions) {
                positionService.closePosition(position.getId(), "Portfolio closure: " + reason);
            }
            
            // Update portfolio status
            portfolio.setStatus(Portfolio.PortfolioStatus.CLOSED);
            portfolioRepository.save(portfolio);

            log.info("Closed portfolio: {} - Reason: {}", portfolio.getName(), reason);

            // Publish portfolio closure event
            publishPortfolioEvent("PORTFOLIO_CLOSED", portfolio);

        } catch (Exception e) {
            log.error("Error closing portfolio: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get portfolio performance summary
     */
    public Map<String, Object> getPortfolioPerformanceSummary(UUID portfolioId) {
        try {
            Portfolio portfolio = getPortfolio(portfolioId);
            
            return Map.of(
                "portfolioId", portfolio.getId(),
                "name", portfolio.getName(),
                "totalValue", portfolio.getTotalValue(),
                "totalReturn", portfolio.getTotalReturn(),
                "totalPnl", portfolio.getTotalPnl(),
                "maxDrawdown", portfolio.getMaxDrawdown(),
                "sharpeRatio", portfolio.getSharpeRatio(),
                "winRate", portfolio.getWinRate(),
                "openPositions", portfolio.getOpenPositionsCount(),
                "lastUpdated", portfolio.getLastValuationAt()
            );

        } catch (Exception e) {
            log.error("Error getting portfolio performance summary: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Publish portfolio event to Kafka
     */
    private void publishPortfolioEvent(String eventType, Portfolio portfolio) {
        try {
            Map<String, Object> eventData = Map.of(
                "eventType", eventType,
                "portfolioId", portfolio.getId().toString(),
                "portfolioName", portfolio.getName(),
                "userId", portfolio.getUserId(),
                "totalValue", portfolio.getTotalValue(),
                "totalReturn", portfolio.getTotalReturn(),
                "timestamp", LocalDateTime.now()
            );

            kafkaTemplate.send("portfolio-events", portfolio.getId().toString(), eventData);
            log.debug("Published portfolio event: {} for portfolio: {}", eventType, portfolio.getName());

        } catch (Exception e) {
            log.error("Error publishing portfolio event: {}", e.getMessage(), e);
        }
    }
}

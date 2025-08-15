package com.cryptotrading.strategy.service;

import com.cryptotrading.strategy.model.StrategyExecution;
import com.cryptotrading.strategy.model.TradingStrategy;
import com.cryptotrading.strategy.repository.StrategyExecutionRepository;
import com.cryptotrading.strategy.repository.TradingStrategyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for executing trading strategies using Drools rules engine
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StrategyExecutionService {

    private final TradingStrategyRepository strategyRepository;
    private final StrategyExecutionRepository executionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MarketDataService marketDataService;
    private final RiskManagementService riskManagementService;

    // Cache for compiled Drools sessions
    private final Map<String, KieSession> strategySessionCache = new ConcurrentHashMap<>();

    /**
     * Listen to technical indicators from analysis service
     */
    @KafkaListener(topics = "technical-indicators", groupId = "strategy-execution-group")
    public void processIndicatorUpdate(Map<String, Object> indicatorData) {
        try {
            String symbol = (String) indicatorData.get("symbol");
            log.debug("Processing indicator update for symbol: {}", symbol);

            // Get active strategies for this symbol
            List<TradingStrategy> activeStrategies = strategyRepository
                    .findBySymbolAndStatus(symbol, TradingStrategy.StrategyStatus.LIVE);

            for (TradingStrategy strategy : activeStrategies) {
                evaluateStrategy(strategy, indicatorData);
            }

        } catch (Exception e) {
            log.error("Error processing indicator update: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to prediction updates from analysis service
     */
    @KafkaListener(topics = "price-predictions", groupId = "strategy-execution-group")
    public void processPredictionUpdate(Map<String, Object> predictionData) {
        try {
            String symbol = (String) predictionData.get("symbol");
            log.debug("Processing prediction update for symbol: {}", symbol);

            // Get prediction-based strategies for this symbol
            List<TradingStrategy> predictionStrategies = strategyRepository
                    .findBySymbolAndStatus(symbol, TradingStrategy.StrategyStatus.LIVE)
                    .stream()
                    .filter(s -> s.getType() == TradingStrategy.StrategyType.PREDICTION_BASED)
                    .toList();

            for (TradingStrategy strategy : predictionStrategies) {
                evaluateStrategyWithPredictions(strategy, predictionData);
            }

        } catch (Exception e) {
            log.error("Error processing prediction update: {}", e.getMessage(), e);
        }
    }

    /**
     * Evaluate strategy using Drools rules engine
     */
    @Transactional
    public void evaluateStrategy(TradingStrategy strategy, Map<String, Object> marketData) {
        try {
            // Get or create Drools session for this strategy
            KieSession session = getOrCreateSession(strategy);
            
            // Create market context
            MarketContext context = createMarketContext(strategy, marketData);
            
            // Insert facts into Drools session
            session.insert(context);
            session.insert(strategy);
            
            // Fire rules
            int rulesFired = session.fireAllRules();
            log.debug("Fired {} rules for strategy: {}", rulesFired, strategy.getName());
            
            // Check if any trading signals were generated
            processGeneratedSignals(strategy, context);
            
        } catch (Exception e) {
            log.error("Error evaluating strategy {}: {}", strategy.getName(), e.getMessage(), e);
        }
    }

    /**
     * Evaluate strategy with prediction data
     */
    @Transactional
    public void evaluateStrategyWithPredictions(TradingStrategy strategy, Map<String, Object> predictionData) {
        try {
            // Get current market data
            Map<String, Object> marketData = marketDataService.getCurrentMarketData(strategy.getSymbol());
            
            // Combine market data with predictions
            marketData.putAll(predictionData);
            
            // Evaluate using standard process
            evaluateStrategy(strategy, marketData);
            
        } catch (Exception e) {
            log.error("Error evaluating strategy {} with predictions: {}", strategy.getName(), e.getMessage(), e);
        }
    }

    /**
     * Get or create Drools session for strategy
     */
    private KieSession getOrCreateSession(TradingStrategy strategy) {
        String cacheKey = strategy.getId().toString();
        
        return strategySessionCache.computeIfAbsent(cacheKey, k -> {
            try {
                return createDroolsSession(strategy);
            } catch (Exception e) {
                log.error("Error creating Drools session for strategy {}: {}", strategy.getName(), e.getMessage());
                throw new RuntimeException("Failed to create Drools session", e);
            }
        });
    }

    /**
     * Create Drools session from strategy rules
     */
    private KieSession createDroolsSession(TradingStrategy strategy) {
        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        
        // Add entry rules
        if (strategy.getEntryRules() != null && !strategy.getEntryRules().isEmpty()) {
            builder.add(ResourceFactory.newByteArrayResource(strategy.getEntryRules().getBytes()),
                       ResourceType.DRL);
        }
        
        // Add exit rules
        if (strategy.getExitRules() != null && !strategy.getExitRules().isEmpty()) {
            builder.add(ResourceFactory.newByteArrayResource(strategy.getExitRules().getBytes()),
                       ResourceType.DRL);
        }
        
        if (builder.hasErrors()) {
            log.error("Drools compilation errors for strategy {}: {}", 
                     strategy.getName(), builder.getErrors().toString());
            throw new RuntimeException("Drools compilation failed");
        }
        
        InternalKnowledgeBase knowledgeBase = KnowledgeBaseFactory.newKnowledgeBase();
        knowledgeBase.addPackages(builder.getKnowledgePackages());
        
        return knowledgeBase.newKieSession();
    }

    /**
     * Create market context for Drools evaluation
     */
    private MarketContext createMarketContext(TradingStrategy strategy, Map<String, Object> marketData) {
        return MarketContext.builder()
                .symbol(strategy.getSymbol())
                .timestamp(LocalDateTime.now())
                .currentPrice(extractBigDecimal(marketData, "currentPrice"))
                .rsi(extractBigDecimal(marketData, "rsi"))
                .macd(extractBigDecimal(marketData, "macd"))
                .macdSignal(extractBigDecimal(marketData, "macdSignal"))
                .sma20(extractBigDecimal(marketData, "sma20"))
                .ema12(extractBigDecimal(marketData, "ema12"))
                .ema26(extractBigDecimal(marketData, "ema26"))
                .bollingerUpper(extractBigDecimal(marketData, "bollingerUpper"))
                .bollingerLower(extractBigDecimal(marketData, "bollingerLower"))
                .volume(extractBigDecimal(marketData, "volume"))
                .vwap(extractBigDecimal(marketData, "vwap"))
                .arimaPrediction(extractBigDecimal(marketData, "arimaPrediction"))
                .mlPrediction(extractBigDecimal(marketData, "mlPrediction"))
                .trendPrediction(extractBigDecimal(marketData, "trendPrediction"))
                .build();
    }

    /**
     * Extract BigDecimal from market data map
     */
    private BigDecimal extractBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Could not convert {} to BigDecimal: {}", key, value);
            return null;
        }
    }

    /**
     * Process trading signals generated by Drools rules
     */
    private void processGeneratedSignals(TradingStrategy strategy, MarketContext context) {
        // Check if context has generated any signals
        if (context.getGeneratedSignals() != null && !context.getGeneratedSignals().isEmpty()) {
            for (TradingSignal signal : context.getGeneratedSignals()) {
                processTradingSignal(strategy, signal, context);
            }
        }
    }

    /**
     * Process individual trading signal
     */
    @Transactional
    public void processTradingSignal(TradingStrategy strategy, TradingSignal signal, MarketContext context) {
        try {
            // Validate signal with risk management
            if (!riskManagementService.validateSignal(strategy, signal)) {
                log.info("Signal rejected by risk management for strategy: {}", strategy.getName());
                return;
            }

            // Create strategy execution
            StrategyExecution execution = StrategyExecution.builder()
                    .strategy(strategy)
                    .symbol(strategy.getSymbol())
                    .executionType(strategy.getStatus() == TradingStrategy.StrategyStatus.LIVE 
                                 ? StrategyExecution.ExecutionType.LIVE_TRADING 
                                 : StrategyExecution.ExecutionType.PAPER_TRADING)
                    .side(signal.getSide())
                    .status(StrategyExecution.ExecutionStatus.PENDING)
                    .quantity(signal.getQuantity())
                    .entryPrice(context.getCurrentPrice())
                    .stopLossPrice(signal.getStopLossPrice())
                    .takeProfitPrice(signal.getTakeProfitPrice())
                    .triggerReason(signal.getReason())
                    .marketConditions(serializeMarketConditions(context))
                    .exchange("BINANCE") // Default exchange
                    .entryTime(LocalDateTime.now())
                    .build();

            // Save execution
            execution = executionRepository.save(execution);

            // Send to execution service via Kafka
            publishExecutionOrder(execution);

            // Update strategy metrics
            strategy.setLastExecutedAt(LocalDateTime.now());
            strategyRepository.save(strategy);

            log.info("Generated trading signal for strategy {}: {} {} at {}", 
                    strategy.getName(), signal.getSide(), signal.getQuantity(), context.getCurrentPrice());

        } catch (Exception e) {
            log.error("Error processing trading signal for strategy {}: {}", strategy.getName(), e.getMessage(), e);
        }
    }

    /**
     * Publish execution order to Kafka
     */
    private void publishExecutionOrder(StrategyExecution execution) {
        try {
            kafkaTemplate.send("execution-orders", execution.getSymbol(), execution);
            log.debug("Published execution order to Kafka: {}", execution.getId());
        } catch (Exception e) {
            log.error("Error publishing execution order to Kafka: {}", e.getMessage(), e);
        }
    }

    /**
     * Serialize market conditions to JSON
     */
    private String serializeMarketConditions(MarketContext context) {
        // Implementation would serialize context to JSON
        // For now, return a simple string representation
        return String.format("Price: %s, RSI: %s, MACD: %s", 
                            context.getCurrentPrice(), context.getRsi(), context.getMacd());
    }

    /**
     * Clear strategy session cache (for strategy updates)
     */
    public void clearStrategyCache(String strategyId) {
        KieSession session = strategySessionCache.remove(strategyId);
        if (session != null) {
            session.dispose();
            log.info("Cleared Drools session cache for strategy: {}", strategyId);
        }
    }

    /**
     * Clear all strategy caches
     */
    public void clearAllCaches() {
        strategySessionCache.values().forEach(KieSession::dispose);
        strategySessionCache.clear();
        log.info("Cleared all Drools session caches");
    }
}

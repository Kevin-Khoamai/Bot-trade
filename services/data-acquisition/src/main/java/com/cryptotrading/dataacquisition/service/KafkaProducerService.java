package com.cryptotrading.dataacquisition.service;

import com.cryptotrading.dataacquisition.dto.MarketDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing market data to Kafka topics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.binance-trades}")
    private String binanceTradesTopic;

    @Value("${kafka.topics.coinbase-trades}")
    private String coinbaseTradesTopic;

    @Value("${kafka.topics.aggregated-data}")
    private String aggregatedDataTopic;

    @Value("${kafka.topics.price-updates}")
    private String priceUpdatesTopic;

    /**
     * Send market data to appropriate topic based on exchange
     */
    public void sendMarketData(MarketDataDto marketData) {
        String topic = getTopicForExchange(marketData.getExchange());
        String key = generateKey(marketData);
        
        sendMessage(topic, key, marketData);
    }

    /**
     * Send aggregated market data
     */
    public void sendAggregatedData(MarketDataDto aggregatedData) {
        String key = generateKey(aggregatedData);
        sendMessage(aggregatedDataTopic, key, aggregatedData);
    }

    /**
     * Send price update notification
     */
    public void sendPriceUpdate(String symbol, Object priceData) {
        String key = symbol;
        sendMessage(priceUpdatesTopic, key, priceData);
    }

    /**
     * Send message to specific topic
     */
    public void sendMessage(String topic, String key, Object message) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(topic, key, message);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Sent message=[{}] with key=[{}] to topic=[{}] with offset=[{}]",
                            message, key, topic, result.getRecordMetadata().offset());
                } else {
                    log.error("Unable to send message=[{}] with key=[{}] to topic=[{}] due to: {}",
                            message, key, topic, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error sending message to topic {}: {}", topic, e.getMessage());
        }
    }

    /**
     * Send message with custom partition
     */
    public void sendMessage(String topic, Integer partition, String key, Object message) {
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(topic, partition, key, message);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Sent message=[{}] with key=[{}] to topic=[{}] partition=[{}] with offset=[{}]",
                            message, key, topic, partition, result.getRecordMetadata().offset());
                } else {
                    log.error("Unable to send message=[{}] with key=[{}] to topic=[{}] partition=[{}] due to: {}",
                            message, key, topic, partition, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error sending message to topic {} partition {}: {}", topic, partition, e.getMessage());
        }
    }

    /**
     * Get appropriate topic for exchange
     */
    private String getTopicForExchange(String exchange) {
        return switch (exchange.toUpperCase()) {
            case "BINANCE" -> binanceTradesTopic;
            case "COINBASE" -> coinbaseTradesTopic;
            default -> aggregatedDataTopic;
        };
    }

    /**
     * Generate message key for partitioning
     */
    private String generateKey(MarketDataDto marketData) {
        return String.format("%s_%s", marketData.getExchange(), marketData.getSymbol());
    }

    /**
     * Send batch of market data
     */
    public void sendMarketDataBatch(Iterable<MarketDataDto> marketDataList) {
        for (MarketDataDto marketData : marketDataList) {
            sendMarketData(marketData);
        }
    }

    /**
     * Send message with headers
     */
    public void sendMessageWithHeaders(String topic, String key, Object message,
                                     java.util.Map<String, Object> headers) {
        try {
            org.springframework.messaging.Message<Object> kafkaMessage =
                org.springframework.messaging.support.MessageBuilder
                    .withPayload(message)
                    .setHeader(org.springframework.kafka.support.KafkaHeaders.TOPIC, topic)
                    .setHeader(org.springframework.kafka.support.KafkaHeaders.KEY, key)
                    .copyHeaders(headers)
                    .build();

            kafkaTemplate.send(kafkaMessage);
            log.debug("Sent message with headers to topic: {}", topic);
        } catch (Exception e) {
            log.error("Error sending message with headers to topic {}: {}", topic, e.getMessage());
        }
    }
}

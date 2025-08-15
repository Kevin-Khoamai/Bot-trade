package com.cryptotrading.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Analysis Service - Main Application
 * 
 * This service is responsible for:
 * - Consuming market data from Kafka streams
 * - Computing technical indicators (MA, RSI, MACD, etc.)
 * - Generating price predictions using ML models
 * - Providing real-time analysis via WebSocket
 * - Caching analysis results in Redis
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
public class AnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalysisApplication.class, args);
    }
}

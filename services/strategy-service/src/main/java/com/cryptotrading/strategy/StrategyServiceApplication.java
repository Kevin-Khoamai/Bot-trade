package com.cryptotrading.strategy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Trading Strategy Service
 * 
 * This service handles:
 * - Trading strategy definition and management
 * - Real-time strategy execution based on technical indicators
 * - Backtesting and paper trading
 * - Strategy performance analysis
 * - Integration with prediction models from Analysis Service
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
@EnableAsync
@EnableScheduling
public class StrategyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StrategyServiceApplication.class, args);
    }
}

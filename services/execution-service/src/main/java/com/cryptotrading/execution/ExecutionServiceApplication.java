package com.cryptotrading.execution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Trade Execution Service
 * 
 * This service handles:
 * - Real-time trade execution on cryptocurrency exchanges
 * - Order lifecycle management (placement, fills, cancellations)
 * - Exchange API integration (Binance, Coinbase, etc.)
 * - Pre-trade and post-trade risk controls
 * - Position tracking and P&L calculation
 * - Order routing and smart execution algorithms
 * - Real-time market data consumption for execution decisions
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
@EnableAsync
@EnableScheduling
public class ExecutionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutionServiceApplication.class, args);
    }
}

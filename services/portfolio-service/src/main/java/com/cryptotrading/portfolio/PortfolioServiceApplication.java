package com.cryptotrading.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Portfolio Management Service
 * 
 * This service handles:
 * - Real-time position tracking and management
 * - P&L calculation and performance analytics
 * - Portfolio risk monitoring and VaR calculations
 * - Asset allocation and rebalancing
 * - Performance attribution and benchmarking
 * - Real-time portfolio valuation
 * - Risk metrics calculation (Sharpe ratio, drawdown, etc.)
 * - Portfolio optimization and allocation strategies
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
@EnableAsync
@EnableScheduling
public class PortfolioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioServiceApplication.class, args);
    }
}

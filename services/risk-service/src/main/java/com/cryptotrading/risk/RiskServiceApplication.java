package com.cryptotrading.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Risk Management Service
 * 
 * This service handles:
 * - Real-time risk monitoring and assessment
 * - Value at Risk (VaR) calculations using Monte Carlo simulation
 * - Portfolio risk metrics and stress testing
 * - Risk limit monitoring and breach detection
 * - Real-time risk alerts and notifications
 * - Regulatory compliance and reporting
 * - Correlation analysis and concentration risk
 * - Market risk, credit risk, and operational risk management
 * - Risk dashboard and visualization
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
@EnableAsync
@EnableScheduling
public class RiskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskServiceApplication.class, args);
    }
}

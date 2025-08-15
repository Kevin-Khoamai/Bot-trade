package com.cryptotrading.dataacquisition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Data Acquisition Service - Main Application
 * 
 * This service is responsible for:
 * - Fetching real-time market data from Binance and Coinbase
 * - Storing data in PostgreSQL
 * - Caching data in Redis
 * - Streaming data via Kafka
 */
@SpringBootApplication
@EnableKafka
@EnableScheduling
public class DataAcquisitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataAcquisitionApplication.class, args);
    }
}

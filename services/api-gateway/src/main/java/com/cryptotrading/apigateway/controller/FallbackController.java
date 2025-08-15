package com.cryptotrading.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback controller for circuit breaker patterns
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    /**
     * Fallback for Data Service
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> dataServiceFallback() {
        log.warn("Data Service is currently unavailable - returning fallback response");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Service Unavailable",
                "message", "Data Service is temporarily unavailable. Please try again later.",
                "service", "data-service",
                "timestamp", Instant.now().toString(),
                "fallback", true,
                "suggestion", "Check service health or try again in a few moments"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * Fallback for Analysis Service
     */
    @GetMapping("/analysis")
    public ResponseEntity<Map<String, Object>> analysisServiceFallback() {
        log.warn("Analysis Service is currently unavailable - returning fallback response");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Service Unavailable",
                "message", "Analysis Service is temporarily unavailable. Cached data may be available.",
                "service", "analysis-service",
                "timestamp", Instant.now().toString(),
                "fallback", true,
                "suggestion", "Use cached analysis data or try again later"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * Fallback for Strategy Service
     */
    @GetMapping("/strategy")
    public ResponseEntity<Map<String, Object>> strategyServiceFallback() {
        log.warn("Strategy Service is currently unavailable - returning fallback response");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Service Unavailable",
                "message", "Strategy Service is temporarily unavailable. Trading strategies are paused.",
                "service", "strategy-service",
                "timestamp", Instant.now().toString(),
                "fallback", true,
                "suggestion", "Manual trading is still available. Automated strategies are paused."
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * Fallback for Execution Service
     */
    @GetMapping("/execution")
    public ResponseEntity<Map<String, Object>> executionServiceFallback() {
        log.error("Execution Service is currently unavailable - CRITICAL FALLBACK");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Critical Service Unavailable",
                "message", "Execution Service is temporarily unavailable. Trading is suspended.",
                "service", "execution-service",
                "timestamp", Instant.now().toString(),
                "fallback", true,
                "critical", true,
                "suggestion", "Contact support immediately. All trading operations are suspended."
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * Fallback for Portfolio Service
     */
    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> portfolioServiceFallback() {
        log.warn("Portfolio Service is currently unavailable - returning fallback response");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Service Unavailable",
                "message", "Portfolio Service is temporarily unavailable. Portfolio data may be stale.",
                "service", "portfolio-service",
                "timestamp", Instant.now().toString(),
                "fallback", true,
                "suggestion", "Portfolio calculations are paused. Last known data is displayed."
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * Fallback for Risk Service
     */
    @GetMapping("/risk")
    public ResponseEntity<Map<String, Object>> riskServiceFallback() {
        log.error("Risk Service is currently unavailable - CRITICAL FALLBACK");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Critical Service Unavailable",
                "message", "Risk Service is temporarily unavailable. Risk monitoring is suspended.",
                "service", "risk-service",
                "timestamp", Instant.now().toString(),
                "fallback", true,
                "critical", true,
                "suggestion", "Risk monitoring is disabled. Exercise extreme caution with trading."
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * Generic fallback for unknown services
     */
    @GetMapping("/generic")
    public ResponseEntity<Map<String, Object>> genericFallback() {
        log.warn("Generic service fallback triggered");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Service Unavailable",
                "message", "The requested service is temporarily unavailable.",
                "service", "unknown",
                "timestamp", Instant.now().toString(),
                "fallback", true,
                "suggestion", "Please try again later or contact support if the issue persists."
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * Health check endpoint for the gateway itself
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> healthResponse = Map.of(
                "status", "UP",
                "service", "api-gateway",
                "timestamp", Instant.now().toString(),
                "message", "API Gateway is operational"
        );
        
        return ResponseEntity.ok(healthResponse);
    }

    /**
     * Fallback for authentication failures
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<String, Object>> authFallback() {
        log.warn("Authentication service fallback triggered");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Authentication Service Unavailable",
                "message", "Authentication service is temporarily unavailable.",
                "service", "auth-service",
                "timestamp", Instant.now().toString(),
                "fallback", true,
                "suggestion", "Please try logging in again or contact support."
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
    }

    /**
     * Fallback for rate limiting service
     */
    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> rateLimitFallback() {
        log.warn("Rate limiting service fallback triggered");
        
        Map<String, Object> fallbackResponse = Map.of(
                "error", "Rate Limiting Service Unavailable",
                "message", "Rate limiting service is temporarily unavailable. Requests are allowed.",
                "service", "rate-limit-service",
                "timestamp", Instant.now().toString(),
                "fallback", true,
                "suggestion", "Rate limiting is temporarily disabled."
        );
        
        return ResponseEntity.ok(fallbackResponse);
    }
}

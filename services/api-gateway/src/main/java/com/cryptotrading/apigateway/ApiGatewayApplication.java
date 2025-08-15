package com.cryptotrading.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Main application class for API Gateway
 *
 * This service provides:
 * - Unified API access point for all microservices
 * - JWT-based authentication and authorization
 * - Rate limiting and throttling
 * - Circuit breaker pattern for resilience
 * - Request/response transformation
 * - Centralized logging and monitoring
 * - CORS handling for web clients
 * - WebSocket proxy for real-time data
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /**
     * CORS configuration for web clients
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(Arrays.asList("*"));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    /**
     * Custom route locator for dynamic routing
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Health check route
                .route("health-check", r -> r.path("/health")
                        .uri("http://localhost:8080"))

                // API documentation route
                .route("api-docs", r -> r.path("/api-docs/**")
                        .uri("http://localhost:8080"))

                // Static resources route
                .route("static-resources", r -> r.path("/static/**")
                        .uri("http://localhost:3000"))

                .build();
    }
}

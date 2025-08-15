package com.cryptotrading.apigateway.config;

import com.cryptotrading.apigateway.filter.JwtAuthenticationFilter;
import com.cryptotrading.apigateway.filter.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

/**
 * Gateway configuration for routing, filters, and rate limiting
 */
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    /**
     * Custom route locator with advanced routing rules
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Authentication routes (public)
                .route("auth-routes", r -> r
                        .path("/api/auth/**")
                        .uri("lb://auth-service")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(rateLimitingFilter.apply(new RateLimitingFilter.Config()))
                        )
                )
                
                // Data service routes
                .route("data-service-routes", r -> r
                        .path("/api/data/**")
                        .uri("lb://data-service")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitingFilter.apply(createRateLimitConfig(500, 1000)))
                                .circuitBreaker(config -> config
                                        .setName("data-service-cb")
                                        .setFallbackUri("forward:/fallback/data")
                                )
                        )
                )
                
                // Analysis service routes
                .route("analysis-service-routes", r -> r
                        .path("/api/analysis/**")
                        .uri("lb://analysis-service")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitingFilter.apply(createRateLimitConfig(100, 200)))
                                .circuitBreaker(config -> config
                                        .setName("analysis-service-cb")
                                        .setFallbackUri("forward:/fallback/analysis")
                                )
                        )
                )
                
                // Strategy service routes
                .route("strategy-service-routes", r -> r
                        .path("/api/strategy/**")
                        .uri("lb://strategy-service")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitingFilter.apply(createRateLimitConfig(50, 100)))
                                .circuitBreaker(config -> config
                                        .setName("strategy-service-cb")
                                        .setFallbackUri("forward:/fallback/strategy")
                                )
                        )
                )
                
                // Execution service routes (critical)
                .route("execution-service-routes", r -> r
                        .path("/api/execution/**")
                        .uri("lb://execution-service")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitingFilter.apply(createRateLimitConfig(200, 400)))
                                .circuitBreaker(config -> config
                                        .setName("execution-service-cb")
                                        .setFallbackUri("forward:/fallback/execution")
                                )
                        )
                )
                
                // Portfolio service routes
                .route("portfolio-service-routes", r -> r
                        .path("/api/portfolio/**")
                        .uri("lb://portfolio-service")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitingFilter.apply(createRateLimitConfig(100, 200)))
                                .circuitBreaker(config -> config
                                        .setName("portfolio-service-cb")
                                        .setFallbackUri("forward:/fallback/portfolio")
                                )
                        )
                )
                
                // Risk service routes (critical)
                .route("risk-service-routes", r -> r
                        .path("/api/risk/**")
                        .uri("lb://risk-service")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitingFilter.apply(createRateLimitConfig(50, 100)))
                                .circuitBreaker(config -> config
                                        .setName("risk-service-cb")
                                        .setFallbackUri("forward:/fallback/risk")
                                )
                        )
                )
                
                // WebSocket routes
                .route("websocket-routes", r -> r
                        .path("/ws/**")
                        .uri("lb://websocket-service")
                        .filters(f -> f
                                .filter(rateLimitingFilter.apply(createRateLimitConfig(1000, 2000)))
                        )
                )
                
                // Admin routes (restricted)
                .route("admin-routes", r -> r
                        .path("/api/admin/**")
                        .and()
                        .method(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
                        .uri("lb://admin-service")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitingFilter.apply(createRateLimitConfig(20, 40)))
                        )
                )
                
                // Health check routes (public)
                .route("health-routes", r -> r
                        .path("/health", "/actuator/**")
                        .uri("http://localhost:8080")
                )
                
                // API documentation routes (public)
                .route("docs-routes", r -> r
                        .path("/api-docs/**", "/swagger-ui/**", "/webjars/**")
                        .uri("http://localhost:8080")
                )
                
                .build();
    }

    /**
     * Key resolver for rate limiting based on user ID or IP
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from JWT token
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }
            
            // Fall back to IP address
            String clientIp = exchange.getRequest().getRemoteAddress() != null ?
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * Redis rate limiter for Spring Cloud Gateway
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    /**
     * Create rate limit configuration
     */
    private RateLimitingFilter.Config createRateLimitConfig(int requestsPerMinute, int burstCapacity) {
        RateLimitingFilter.Config config = new RateLimitingFilter.Config();
        config.setRequestsPerMinute(requestsPerMinute);
        config.setBurstCapacity(burstCapacity);
        return config;
    }

    /**
     * High-frequency data rate limiter
     */
    @Bean("highFrequencyRateLimiter")
    public RedisRateLimiter highFrequencyRateLimiter() {
        return new RedisRateLimiter(500, 1000, 1);
    }

    /**
     * Trading operations rate limiter
     */
    @Bean("tradingRateLimiter")
    public RedisRateLimiter tradingRateLimiter() {
        return new RedisRateLimiter(200, 400, 1);
    }

    /**
     * Analysis operations rate limiter
     */
    @Bean("analysisRateLimiter")
    public RedisRateLimiter analysisRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    /**
     * Admin operations rate limiter
     */
    @Bean("adminRateLimiter")
    public RedisRateLimiter adminRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }
}

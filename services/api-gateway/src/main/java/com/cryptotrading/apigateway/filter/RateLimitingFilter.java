package com.cryptotrading.apigateway.filter;

import com.cryptotrading.apigateway.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate Limiting Filter for API Gateway
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    private final RateLimitService rateLimitService;

    public RateLimitingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Extract client identifier (IP address or user ID)
            String clientId = getClientIdentifier(request);
            String path = request.getURI().getPath();
            String method = request.getMethod().name();
            
            // Check rate limit
            return rateLimitService.isAllowed(clientId, path, method)
                    .flatMap(allowed -> {
                        if (allowed.isAllowed()) {
                            // Add rate limit headers
                            ServerHttpResponse response = exchange.getResponse();
                            response.getHeaders().add("X-RateLimit-Limit", String.valueOf(allowed.getLimit()));
                            response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(allowed.getRemaining()));
                            response.getHeaders().add("X-RateLimit-Reset", String.valueOf(allowed.getResetTime()));
                            
                            log.debug("Rate limit check passed for client: {} on path: {}", clientId, path);
                            return chain.filter(exchange);
                        } else {
                            log.warn("Rate limit exceeded for client: {} on path: {}", clientId, path);
                            return handleRateLimitExceeded(exchange, allowed);
                        }
                    })
                    .onErrorResume(error -> {
                        log.error("Error checking rate limit for client: {} on path: {}", clientId, path, error);
                        // On error, allow the request to proceed (fail open)
                        return chain.filter(exchange);
                    });
        };
    }

    /**
     * Extract client identifier from request
     */
    private String getClientIdentifier(ServerHttpRequest request) {
        // Try to get user ID from headers (set by JWT filter)
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }
        
        // Fall back to IP address
        String clientIp = getClientIpAddress(request);
        return "ip:" + clientIp;
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * Handle rate limit exceeded
     */
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, RateLimitService.RateLimitResult result) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(result.getResetTime()));
        response.getHeaders().add("Retry-After", String.valueOf(result.getRetryAfter()));
        
        String body = """
                {
                    "error": "Rate Limit Exceeded",
                    "message": "Too many requests. Please try again later.",
                    "limit": %d,
                    "remaining": 0,
                    "resetTime": %d,
                    "retryAfter": %d,
                    "timestamp": "%s",
                    "path": "%s"
                }
                """.formatted(
                result.getLimit(),
                result.getResetTime(),
                result.getRetryAfter(),
                java.time.Instant.now().toString(),
                exchange.getRequest().getURI().getPath()
        );

        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Configuration class for the filter
     */
    public static class Config {
        private int requestsPerMinute = 100;
        private int burstCapacity = 200;
        private String tier = "default";

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public String getTier() {
            return tier;
        }

        public void setTier(String tier) {
            this.tier = tier;
        }
    }
}

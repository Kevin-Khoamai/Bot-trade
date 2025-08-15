package com.cryptotrading.apigateway.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Rate Limiting Service using Redis for distributed rate limiting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("#{${rate-limiting.tiers}}")
    private Map<String, RateLimitConfig> rateLimitTiers;

    @Value("${rate-limiting.default.requests-per-minute:100}")
    private int defaultRequestsPerMinute;

    @Value("${rate-limiting.default.burst-capacity:200}")
    private int defaultBurstCapacity;

    // Lua script for atomic rate limiting check and update
    private static final String RATE_LIMIT_SCRIPT = """
            local key = KEYS[1]
            local window = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local current_time = tonumber(ARGV[3])
            
            -- Get current count
            local current = redis.call('GET', key)
            if current == false then
                current = 0
            else
                current = tonumber(current)
            end
            
            -- Check if limit exceeded
            if current >= limit then
                local ttl = redis.call('TTL', key)
                return {0, current, limit, ttl > 0 and ttl or window}
            end
            
            -- Increment counter
            local new_count = current + 1
            redis.call('SET', key, new_count, 'EX', window)
            
            return {1, new_count, limit, window}
            """;

    private final RedisScript<List> rateLimitScript = RedisScript.of(RATE_LIMIT_SCRIPT, List.class);

    /**
     * Check if request is allowed based on rate limiting rules
     */
    public Mono<RateLimitResult> isAllowed(String clientId, String path, String method) {
        return Mono.fromCallable(() -> {
            // Determine rate limit configuration
            RateLimitConfig config = getRateLimitConfig(clientId, path, method);
            
            // Create Redis key
            String key = createRateLimitKey(clientId, path, method);
            
            // Current time
            long currentTime = Instant.now().getEpochSecond();
            
            return new RateLimitCheck(key, config, currentTime);
        })
        .flatMap(this::executeRateLimitCheck)
        .onErrorResume(error -> {
            log.error("Error in rate limiting for client: {}, path: {}", clientId, path, error);
            // On error, allow the request (fail open)
            return Mono.just(RateLimitResult.allowed(defaultRequestsPerMinute, defaultRequestsPerMinute - 1, 60));
        });
    }

    /**
     * Execute rate limit check using Redis Lua script
     */
    private Mono<RateLimitResult> executeRateLimitCheck(RateLimitCheck check) {
        return redisTemplate.execute(
                rateLimitScript,
                List.of(check.getKey()),
                List.of(
                        String.valueOf(check.getConfig().getWindowSeconds()),
                        String.valueOf(check.getConfig().getLimit()),
                        String.valueOf(check.getCurrentTime())
                )
        )
        .cast(List.class)
        .map(result -> {
            int allowed = ((Number) result.get(0)).intValue();
            int current = ((Number) result.get(1)).intValue();
            int limit = ((Number) result.get(2)).intValue();
            int ttl = ((Number) result.get(3)).intValue();
            
            if (allowed == 1) {
                return RateLimitResult.allowed(limit, limit - current, ttl);
            } else {
                return RateLimitResult.denied(limit, 0, ttl);
            }
        });
    }

    /**
     * Get rate limit configuration for client and endpoint
     */
    private RateLimitConfig getRateLimitConfig(String clientId, String path, String method) {
        // Check for user-specific tier
        if (clientId.startsWith("user:")) {
            String userTier = getUserTier(clientId);
            if (rateLimitTiers.containsKey(userTier)) {
                return rateLimitTiers.get(userTier);
            }
        }
        
        // Check for endpoint-specific limits
        RateLimitConfig endpointConfig = getEndpointSpecificConfig(path, method);
        if (endpointConfig != null) {
            return endpointConfig;
        }
        
        // Default configuration
        return new RateLimitConfig(defaultRequestsPerMinute, defaultBurstCapacity, 60);
    }

    /**
     * Get user tier from user service or cache
     */
    private String getUserTier(String clientId) {
        // This would typically call user service or check cache
        // For now, return default tier
        return "basic";
    }

    /**
     * Get endpoint-specific rate limit configuration
     */
    private RateLimitConfig getEndpointSpecificConfig(String path, String method) {
        // High-frequency endpoints
        if (path.startsWith("/api/data/prices") || path.startsWith("/api/market")) {
            return new RateLimitConfig(500, 1000, 60); // Higher limits for market data
        }
        
        // Trading endpoints
        if (path.startsWith("/api/execution") || path.startsWith("/api/orders")) {
            return new RateLimitConfig(200, 400, 60); // Moderate limits for trading
        }
        
        // Analysis endpoints
        if (path.startsWith("/api/analysis") || path.startsWith("/api/strategy")) {
            return new RateLimitConfig(100, 200, 60); // Lower limits for analysis
        }
        
        // Admin endpoints
        if (path.startsWith("/api/admin")) {
            return new RateLimitConfig(50, 100, 60); // Strict limits for admin
        }
        
        return null; // Use default
    }

    /**
     * Create Redis key for rate limiting
     */
    private String createRateLimitKey(String clientId, String path, String method) {
        // Create hierarchical key for different granularities
        String baseKey = "rate_limit:" + clientId;
        
        // Add path-specific key for endpoint-specific limits
        if (isHighFrequencyEndpoint(path)) {
            return baseKey + ":hf:" + method;
        } else if (isTradingEndpoint(path)) {
            return baseKey + ":trading:" + method;
        } else {
            return baseKey + ":general:" + method;
        }
    }

    /**
     * Check if endpoint is high frequency
     */
    private boolean isHighFrequencyEndpoint(String path) {
        return path.startsWith("/api/data/prices") || 
               path.startsWith("/api/market") ||
               path.startsWith("/ws");
    }

    /**
     * Check if endpoint is trading related
     */
    private boolean isTradingEndpoint(String path) {
        return path.startsWith("/api/execution") || 
               path.startsWith("/api/orders") ||
               path.startsWith("/api/portfolio");
    }

    /**
     * Get current rate limit status for client
     */
    public Mono<RateLimitStatus> getRateLimitStatus(String clientId) {
        String key = "rate_limit:" + clientId + ":*";
        
        return redisTemplate.keys(key)
                .flatMap(redisKey -> 
                    redisTemplate.opsForValue().get(redisKey)
                            .map(value -> new RateLimitKeyStatus(redisKey, Integer.parseInt(value)))
                )
                .collectList()
                .map(keyStatuses -> new RateLimitStatus(clientId, keyStatuses));
    }

    /**
     * Reset rate limit for client
     */
    public Mono<Void> resetRateLimit(String clientId) {
        String pattern = "rate_limit:" + clientId + ":*";
        
        return redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .then()
                .doOnSuccess(v -> log.info("Reset rate limit for client: {}", clientId));
    }

    // Data classes
    @Data
    public static class RateLimitConfig {
        private final int limit;
        private final int burstCapacity;
        private final int windowSeconds;
    }

    @Data
    private static class RateLimitCheck {
        private final String key;
        private final RateLimitConfig config;
        private final long currentTime;
    }

    @Data
    public static class RateLimitResult {
        private final boolean allowed;
        private final int limit;
        private final int remaining;
        private final long resetTime;
        private final long retryAfter;

        public static RateLimitResult allowed(int limit, int remaining, long resetTime) {
            return new RateLimitResult(true, limit, remaining, resetTime, 0);
        }

        public static RateLimitResult denied(int limit, int remaining, long retryAfter) {
            return new RateLimitResult(false, limit, remaining, 
                    Instant.now().getEpochSecond() + retryAfter, retryAfter);
        }
    }

    @Data
    public static class RateLimitStatus {
        private final String clientId;
        private final List<RateLimitKeyStatus> keyStatuses;
    }

    @Data
    public static class RateLimitKeyStatus {
        private final String key;
        private final int currentCount;
    }
}

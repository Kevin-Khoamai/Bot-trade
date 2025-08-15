package com.cryptotrading.apigateway.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * JWT Service for token validation and extraction
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final long jwtExpiration;
    private final long refreshExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long jwtExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.jwtExpiration = jwtExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from JWT token
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extract user roles from JWT token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    /**
     * Extract expiration date from JWT token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from JWT token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from JWT token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /**
     * Check if JWT token is expired
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Validate JWT token
     */
    public Boolean isTokenValid(String token) {
        try {
            // Parse the token to validate signature and structure
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            
            // Check if token is not expired
            return !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate JWT token for user
     */
    public String generateToken(String username, String userId, List<String> roles) {
        return createToken(username, userId, roles, jwtExpiration);
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(String username, String userId, List<String> roles) {
        return createToken(username, userId, roles, refreshExpiration);
    }

    /**
     * Create JWT token with specified expiration
     */
    private String createToken(String username, String userId, List<String> roles, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("tokenType", expiration == jwtExpiration ? "access" : "refresh")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validate token against username
     */
    public Boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * Get token type (access or refresh)
     */
    public String getTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    /**
     * Check if token is a refresh token
     */
    public Boolean isRefreshToken(String token) {
        try {
            String tokenType = getTokenType(token);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get remaining time until token expiration in milliseconds
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Extract issuer from token
     */
    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    /**
     * Extract audience from token
     */
    public String extractAudience(String token) {
        return extractClaim(token, Claims::getAudience);
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String token, String role) {
        try {
            List<String> roles = extractRoles(token);
            return roles != null && roles.contains(role);
        } catch (Exception e) {
            log.error("Error checking user role: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if user has any of the specified roles
     */
    public boolean hasAnyRole(String token, List<String> requiredRoles) {
        try {
            List<String> userRoles = extractRoles(token);
            if (userRoles == null || requiredRoles == null) {
                return false;
            }
            return userRoles.stream().anyMatch(requiredRoles::contains);
        } catch (Exception e) {
            log.error("Error checking user roles: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get token claims as string for logging
     */
    public String getTokenInfo(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return String.format("User: %s, UserId: %s, Roles: %s, Expires: %s",
                    claims.getSubject(),
                    claims.get("userId"),
                    claims.get("roles"),
                    claims.getExpiration());
        } catch (Exception e) {
            return "Invalid token";
        }
    }
}

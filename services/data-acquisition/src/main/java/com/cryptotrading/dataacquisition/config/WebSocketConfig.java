package com.cryptotrading.dataacquisition.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Configuration for WebSocket clients
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig {

    /**
     * Bean for WebSocket client
     */
    @Bean
    public StandardWebSocketClient webSocketClient() {
        return new StandardWebSocketClient();
    }
}

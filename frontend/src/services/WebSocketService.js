import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscriptions = new Map();
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 1000;
  }

  connect() {
    return new Promise((resolve, reject) => {
      try {
        // Create STOMP client with SockJS
        this.client = new Client({
          webSocketFactory: () => new SockJS('/ws'),
          connectHeaders: {},
          debug: (str) => {
            console.log('STOMP Debug:', str);
          },
          reconnectDelay: this.reconnectDelay,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
        });

        // Connection success handler
        this.client.onConnect = (frame) => {
          console.log('WebSocket Connected:', frame);
          this.connected = true;
          this.reconnectAttempts = 0;
          resolve(frame);
        };

        // Connection error handler
        this.client.onStompError = (frame) => {
          console.error('STOMP Error:', frame);
          this.connected = false;
          reject(new Error(`STOMP Error: ${frame.headers['message']}`));
        };

        // WebSocket error handler
        this.client.onWebSocketError = (error) => {
          console.error('WebSocket Error:', error);
          this.connected = false;
        };

        // Disconnection handler
        this.client.onDisconnect = () => {
          console.log('WebSocket Disconnected');
          this.connected = false;
          this.handleReconnect();
        };

        // Activate the client
        this.client.activate();

      } catch (error) {
        console.error('Error creating WebSocket connection:', error);
        reject(error);
      }
    });
  }

  disconnect() {
    if (this.client && this.connected) {
      this.client.deactivate();
      this.connected = false;
      this.subscriptions.clear();
    }
  }

  handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      setTimeout(() => {
        this.connect().catch(error => {
          console.error('Reconnection failed:', error);
        });
      }, this.reconnectDelay * this.reconnectAttempts);
    } else {
      console.error('Max reconnection attempts reached');
    }
  }

  // Subscribe to price updates
  subscribeToPrices(callback) {
    if (!this.connected) {
      console.warn('WebSocket not connected');
      return null;
    }

    const subscription = this.client.subscribe('/topic/prices', (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing price update:', error);
      }
    });

    this.subscriptions.set('prices', subscription);
    
    // Send subscription message
    this.client.publish({
      destination: '/app/subscribe.prices',
      body: JSON.stringify({ type: 'prices' })
    });

    return subscription;
  }

  // Subscribe to trade updates
  subscribeToTrades(callback) {
    if (!this.connected) {
      console.warn('WebSocket not connected');
      return null;
    }

    const subscription = this.client.subscribe('/topic/trades', (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing trade update:', error);
      }
    });

    this.subscriptions.set('trades', subscription);
    
    // Send subscription message
    this.client.publish({
      destination: '/app/subscribe.trades',
      body: JSON.stringify({ type: 'trades' })
    });

    return subscription;
  }

  // Subscribe to specific symbol
  subscribeToSymbol(symbol, callback) {
    if (!this.connected) {
      console.warn('WebSocket not connected');
      return null;
    }

    const subscription = this.client.subscribe(`/topic/symbol/${symbol}`, (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error(`Error parsing symbol update for ${symbol}:`, error);
      }
    });

    this.subscriptions.set(`symbol-${symbol}`, subscription);
    
    // Send subscription message
    this.client.publish({
      destination: '/app/subscribe.symbol',
      body: JSON.stringify({ symbol: symbol })
    });

    return subscription;
  }

  // Subscribe to system status
  subscribeToSystemStatus(callback) {
    if (!this.connected) {
      console.warn('WebSocket not connected');
      return null;
    }

    const subscription = this.client.subscribe('/topic/system', (message) => {
      try {
        const data = JSON.parse(message.body);
        callback(data);
      } catch (error) {
        console.error('Error parsing system status:', error);
      }
    });

    this.subscriptions.set('system', subscription);
    return subscription;
  }

  // Unsubscribe from a topic
  unsubscribe(subscriptionKey) {
    const subscription = this.subscriptions.get(subscriptionKey);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(subscriptionKey);
      
      // Send unsubscription message
      this.client.publish({
        destination: '/app/unsubscribe',
        body: JSON.stringify({ type: subscriptionKey })
      });
    }
  }

  // Check connection status
  isConnected() {
    return this.connected && this.client && this.client.connected;
  }

  // Get connection state
  getConnectionState() {
    if (!this.client) return 'DISCONNECTED';
    return this.client.state;
  }
}

// Create singleton instance
const webSocketService = new WebSocketService();
export default webSocketService;

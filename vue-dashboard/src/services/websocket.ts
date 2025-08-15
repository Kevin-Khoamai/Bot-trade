import type { WebSocketMessage } from '../types'

export class WebSocketService {
  private ws: WebSocket | null = null
  private isConnected = false
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  private reconnectTimer: number | null = null

  // Event callbacks
  private onConnectCallback?: () => void
  private onDisconnectCallback?: () => void
  private onErrorCallback?: (error: any) => void
  private onMessageCallback?: (message: WebSocketMessage) => void

  constructor() {
    // Don't auto-connect, wait for explicit connect() call
  }

  private setupWebSocket() {
    try {
      // Use native WebSocket instead of SockJS for now
      this.ws = new WebSocket('ws://localhost:8082/ws-native')

      this.ws.onopen = () => {
        console.log('WebSocket Connected')
        this.isConnected = true
        this.reconnectAttempts = 0
        this.onConnectCallback?.()

        // Send a simple ping to test connection
        this.sendMessage({ type: 'ping' })
      }

      this.ws.onclose = () => {
        console.log('WebSocket Disconnected')
        this.isConnected = false
        this.onDisconnectCallback?.()
        this.handleReconnect()
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket Error:', error)
        this.onErrorCallback?.(error)
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.onMessageCallback?.({
            type: data.type || 'UNKNOWN',
            data: data.data || data,
            timestamp: new Date().toISOString()
          })
        } catch (error) {
          console.error('Error parsing WebSocket message:', error)
        }
      }

    } catch (error) {
      console.error('Error setting up WebSocket:', error)
      this.onErrorCallback?.(error)
    }
  }

  private sendMessage(message: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    }
  }

  private handleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts && !this.reconnectTimer) {
      this.reconnectAttempts++
      console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

      this.reconnectTimer = window.setTimeout(() => {
        this.reconnectTimer = null
        this.connect()
      }, this.reconnectDelay * this.reconnectAttempts)
    } else if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached')
    }
  }

  // Public methods
  connect() {
    if (!this.isConnected) {
      console.log('Connecting to WebSocket...')
      this.setupWebSocket()
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.ws) {
      console.log('Disconnecting from WebSocket...')
      this.ws.close()
      this.ws = null
    }
    this.isConnected = false
  }

  // Event handlers
  onConnect(callback: () => void) {
    this.onConnectCallback = callback
  }

  onDisconnect(callback: () => void) {
    this.onDisconnectCallback = callback
  }

  onError(callback: (error: any) => void) {
    this.onErrorCallback = callback
  }

  onMessage(callback: (message: WebSocketMessage) => void) {
    this.onMessageCallback = callback
  }

  // Getters
  get connected() {
    return this.isConnected
  }
}

// Export singleton instance
export const websocketService = new WebSocketService()
export default websocketService

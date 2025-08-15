import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import ReconnectingWebSocket from 'reconnecting-websocket'
import { useToast } from 'vue-toastification'

interface WebSocketMessage {
  type: string
  data: any
  timestamp: string
}

interface MarketData {
  symbol: string
  price: number
  change24h: number
  volume24h: number
  timestamp: string
}

interface OrderUpdate {
  orderId: string
  status: string
  symbol: string
  side: string
  quantity: number
  price: number
  timestamp: string
}

interface PortfolioUpdate {
  portfolioId: string
  totalValue: number
  totalPnl: number
  dailyPnl: number
  positions: any[]
  timestamp: string
}

interface RiskAlert {
  id: string
  portfolioId: string
  alertType: string
  severity: string
  message: string
  timestamp: string
}

export const useWebSocketStore = defineStore('websocket', () => {
  const toast = useToast()
  
  // State
  const ws = ref<ReconnectingWebSocket | null>(null)
  const isConnected = ref(false)
  const connectionAttempts = ref(0)
  const lastMessage = ref<WebSocketMessage | null>(null)
  
  // Market data
  const marketData = ref<Map<string, MarketData>>(new Map())
  const priceUpdates = ref<MarketData[]>([])
  
  // Order updates
  const orderUpdates = ref<OrderUpdate[]>([])
  const activeOrders = ref<Map<string, OrderUpdate>>(new Map())
  
  // Portfolio updates
  const portfolioData = ref<PortfolioUpdate | null>(null)
  const portfolioHistory = ref<PortfolioUpdate[]>([])
  
  // Risk alerts
  const riskAlerts = ref<RiskAlert[]>([])
  const unreadAlerts = ref(0)
  
  // System health
  const systemHealth = ref({
    status: 'unknown',
    services: {},
    lastUpdate: null as string | null
  })

  // Computed
  const connectionStatus = computed(() => {
    if (isConnected.value) {
      return 'Connected'
    } else if (connectionAttempts.value > 0) {
      return 'Reconnecting...'
    } else {
      return 'Disconnected'
    }
  })

  const latestPrices = computed(() => {
    return Array.from(marketData.value.values())
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      .slice(0, 20)
  })

  const recentOrderUpdates = computed(() => {
    return orderUpdates.value
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      .slice(0, 10)
  })

  const criticalAlerts = computed(() => {
    return riskAlerts.value.filter(alert => alert.severity === 'CRITICAL')
  })

  // Actions
  const connect = () => {
    if (ws.value?.readyState === WebSocket.OPEN) {
      return
    }

    const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'
    
    ws.value = new ReconnectingWebSocket(wsUrl, [], {
      maxReconnectionDelay: 10000,
      minReconnectionDelay: 1000,
      reconnectionDelayGrowFactor: 1.3,
      connectionTimeout: 4000,
      maxRetries: Infinity,
      debug: import.meta.env.DEV
    })

    ws.value.addEventListener('open', handleOpen)
    ws.value.addEventListener('message', handleMessage)
    ws.value.addEventListener('close', handleClose)
    ws.value.addEventListener('error', handleError)
  }

  const disconnect = () => {
    if (ws.value) {
      ws.value.close()
      ws.value = null
    }
    isConnected.value = false
  }

  const subscribe = (channel: string, symbols?: string[]) => {
    if (ws.value?.readyState === WebSocket.OPEN) {
      const message = {
        action: 'subscribe',
        channel,
        symbols
      }
      ws.value.send(JSON.stringify(message))
    }
  }

  const unsubscribe = (channel: string, symbols?: string[]) => {
    if (ws.value?.readyState === WebSocket.OPEN) {
      const message = {
        action: 'unsubscribe',
        channel,
        symbols
      }
      ws.value.send(JSON.stringify(message))
    }
  }

  // Event handlers
  const handleOpen = () => {
    isConnected.value = true
    connectionAttempts.value = 0
    
    toast.success('WebSocket connected')
    
    // Subscribe to default channels
    subscribe('market-data', ['BTCUSDT', 'ETHUSDT', 'ADAUSDT'])
    subscribe('order-updates')
    subscribe('portfolio-updates')
    subscribe('risk-alerts')
    subscribe('system-health')
  }

  const handleMessage = (event: MessageEvent) => {
    try {
      const message: WebSocketMessage = JSON.parse(event.data)
      lastMessage.value = message
      
      switch (message.type) {
        case 'market-data':
          handleMarketData(message.data)
          break
        case 'order-update':
          handleOrderUpdate(message.data)
          break
        case 'portfolio-update':
          handlePortfolioUpdate(message.data)
          break
        case 'risk-alert':
          handleRiskAlert(message.data)
          break
        case 'system-health':
          handleSystemHealth(message.data)
          break
        default:
          console.log('Unknown message type:', message.type)
      }
    } catch (error) {
      console.error('Error parsing WebSocket message:', error)
    }
  }

  const handleClose = () => {
    isConnected.value = false
    connectionAttempts.value++
    
    if (connectionAttempts.value === 1) {
      toast.warning('WebSocket disconnected. Attempting to reconnect...')
    }
  }

  const handleError = (error: Event) => {
    console.error('WebSocket error:', error)
    
    if (connectionAttempts.value > 5) {
      toast.error('WebSocket connection failed. Please check your connection.')
    }
  }

  // Data handlers
  const handleMarketData = (data: MarketData) => {
    marketData.value.set(data.symbol, data)
    priceUpdates.value.unshift(data)
    
    // Keep only last 1000 price updates
    if (priceUpdates.value.length > 1000) {
      priceUpdates.value = priceUpdates.value.slice(0, 1000)
    }
  }

  const handleOrderUpdate = (data: OrderUpdate) => {
    orderUpdates.value.unshift(data)
    activeOrders.value.set(data.orderId, data)
    
    // Keep only last 100 order updates
    if (orderUpdates.value.length > 100) {
      orderUpdates.value = orderUpdates.value.slice(0, 100)
    }
    
    // Show toast for important order updates
    if (data.status === 'FILLED') {
      toast.success(`Order filled: ${data.side} ${data.quantity} ${data.symbol}`)
    } else if (data.status === 'REJECTED') {
      toast.error(`Order rejected: ${data.side} ${data.quantity} ${data.symbol}`)
    }
  }

  const handlePortfolioUpdate = (data: PortfolioUpdate) => {
    portfolioData.value = data
    portfolioHistory.value.unshift(data)
    
    // Keep only last 1000 portfolio updates
    if (portfolioHistory.value.length > 1000) {
      portfolioHistory.value = portfolioHistory.value.slice(0, 1000)
    }
  }

  const handleRiskAlert = (data: RiskAlert) => {
    riskAlerts.value.unshift(data)
    unreadAlerts.value++
    
    // Keep only last 50 alerts
    if (riskAlerts.value.length > 50) {
      riskAlerts.value = riskAlerts.value.slice(0, 50)
    }
    
    // Show toast based on severity
    const message = `Risk Alert: ${data.message}`
    switch (data.severity) {
      case 'CRITICAL':
        toast.error(message)
        break
      case 'HIGH':
        toast.warning(message)
        break
      default:
        toast.info(message)
    }
  }

  const handleSystemHealth = (data: any) => {
    systemHealth.value = {
      ...data,
      lastUpdate: new Date().toISOString()
    }
  }

  const markAlertsAsRead = () => {
    unreadAlerts.value = 0
  }

  const clearAlerts = () => {
    riskAlerts.value = []
    unreadAlerts.value = 0
  }

  const getMarketDataForSymbol = (symbol: string) => {
    return marketData.value.get(symbol)
  }

  const getPriceHistory = (symbol: string, limit = 100) => {
    return priceUpdates.value
      .filter(update => update.symbol === symbol)
      .slice(0, limit)
  }

  return {
    // State
    isConnected,
    connectionAttempts,
    lastMessage,
    marketData,
    priceUpdates,
    orderUpdates,
    activeOrders,
    portfolioData,
    portfolioHistory,
    riskAlerts,
    unreadAlerts,
    systemHealth,
    
    // Computed
    connectionStatus,
    latestPrices,
    recentOrderUpdates,
    criticalAlerts,
    
    // Actions
    connect,
    disconnect,
    subscribe,
    unsubscribe,
    markAlertsAsRead,
    clearAlerts,
    getMarketDataForSymbol,
    getPriceHistory
  }
})

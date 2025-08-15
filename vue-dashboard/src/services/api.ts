import axios from 'axios'
import type { PriceData, MarketSummary, TradingSymbol, ApiResponse } from '../types'

// API Configuration
const API_BASE_URL = 'http://localhost:8082'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  }
})

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`)
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    console.log(`API Response: ${response.status} ${response.config.url}`)
    return response
  },
  (error) => {
    console.error('API Error:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

// API Methods
export const api = {
  // Health check
  async getHealth() {
    const response = await apiClient.get('/actuator/health')
    return response.data
  },

  // Market data
  async getSymbols(): Promise<TradingSymbol[]> {
    const response = await apiClient.get('/api/symbols')
    if (!Array.isArray(response.data)) {
      throw new Error('Invalid symbols data format from API')
    }

    // Convert simple symbol strings to TradingSymbol objects
    return response.data.map((symbol: string) => ({
      symbol,
      baseAsset: symbol.split('-')[0] || symbol.split('USDT')[0],
      quoteAsset: symbol.includes('-') ? symbol.split('-')[1] : 'USDT',
      status: 'TRADING',
      minQty: 0.001,
      maxQty: 1000000,
      stepSize: 0.001,
      tickSize: 0.01
    }))
  },

  async getMarketSummary(): Promise<MarketSummary> {
    const response = await apiClient.get('/api/market/summary')
    if (!response.data) {
      throw new Error('Invalid market summary data from API')
    }
    return response.data
  },

  // Price data
  async getCurrentPrices(): Promise<PriceData[]> {
    const response = await apiClient.get('/api/prices/current')

    // Handle case where API returns empty object instead of array
    if (response.data && typeof response.data === 'object' && !Array.isArray(response.data)) {
      // If it's an empty object, return empty array
      if (Object.keys(response.data).length === 0) {
        return []
      }
      // If it's an object with price data, convert to array
      return Object.entries(response.data).map(([symbol, data]: [string, any]) => ({
        symbol,
        price: data.price || 0,
        volume: data.volume || 0,
        changePercent24h: data.changePercent24h || 0,
        high24h: data.high24h || 0,
        low24h: data.low24h || 0,
        timestamp: data.timestamp || Date.now()
      }))
    }

    if (!Array.isArray(response.data)) {
      throw new Error('Invalid current prices data format from API')
    }
    return response.data
  },

  async getLatestPrices(): Promise<PriceData[]> {
    const response = await apiClient.get('/api/prices/latest')
    if (!Array.isArray(response.data)) {
      throw new Error('Invalid latest prices data format from API')
    }
    return response.data
  },

  async getPriceHistory(symbol: string, interval: string = '1h', limit: number = 100): Promise<CandlestickData[]> {
    const response = await apiClient.get('/api/prices/history', {
      params: { symbol, interval, limit }
    })
    if (!Array.isArray(response.data)) {
      throw new Error('Invalid price history data format from API')
    }
    return response.data
  },

  // Portfolio (placeholder for future implementation)
  async getPortfolio() {
    // This will be implemented when portfolio service is ready
    return {
      totalValue: 0,
      totalPnl: 0,
      totalPnlPercent: 0,
      assets: []
    }
  }
}

// Mock data generation for demonstration
function generateMockCandlestickData(symbol: string, interval: string, limit: number) {
  const data = []
  const basePrice = getBasePriceForSymbol(symbol)
  let currentPrice = basePrice
  const now = Date.now()
  const intervalMs = getIntervalMs(interval)

  for (let i = limit; i >= 0; i--) {
    const timestamp = now - (i * intervalMs)
    const volatility = basePrice * 0.02 // 2% volatility
    const change = (Math.random() - 0.5) * volatility

    const open = currentPrice
    const close = currentPrice + change
    const high = Math.max(open, close) + Math.random() * volatility * 0.5
    const low = Math.min(open, close) - Math.random() * volatility * 0.5
    const volume = Math.random() * 1000000

    data.push({
      timestamp,
      open,
      high,
      low,
      close,
      volume
    })

    currentPrice = close
  }

  return data
}

function getBasePriceForSymbol(symbol: string): number {
  const prices: Record<string, number> = {
    'BTC-USD': 50000,
    'BTCUSDT': 50000,
    'ETH-USD': 3000,
    'ETHUSDT': 3000,
    'ADA-USD': 0.5,
    'ADAUSDT': 0.5
  }
  return prices[symbol] || 100
}

function getIntervalMs(interval: string): number {
  const intervals: Record<string, number> = {
    '1m': 60 * 1000,
    '5m': 5 * 60 * 1000,
    '15m': 15 * 60 * 1000,
    '1h': 60 * 60 * 1000,
    '4h': 4 * 60 * 60 * 1000,
    '1d': 24 * 60 * 60 * 1000
  }
  return intervals[interval] || intervals['1h']
}



export default api

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { PriceData, MarketSummary, TradingSymbol } from '../types'
import { api } from '../services/api'
import { websocketService } from '../services/websocket'

export const useMarketStore = defineStore('market', () => {
  // State
  const symbols = ref<TradingSymbol[]>([])
  const currentPrices = ref<PriceData[]>([])
  const marketSummary = ref<MarketSummary | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const isConnected = ref(false)

  // Computed
  const topGainers = computed(() => {
    if (!Array.isArray(currentPrices.value)) return []
    return currentPrices.value
      .filter(price => price.changePercent24h > 0)
      .sort((a, b) => b.changePercent24h - a.changePercent24h)
      .slice(0, 5)
  })

  const topLosers = computed(() => {
    if (!Array.isArray(currentPrices.value)) return []
    return currentPrices.value
      .filter(price => price.changePercent24h < 0)
      .sort((a, b) => a.changePercent24h - b.changePercent24h)
      .slice(0, 5)
  })

  const totalMarketValue = computed(() => {
    if (!Array.isArray(currentPrices.value)) return 0
    return currentPrices.value.reduce((total, price) => total + (price.price * price.volume), 0)
  })

  // Actions
  async function fetchSymbols() {
    try {
      isLoading.value = true
      error.value = null
      symbols.value = await api.getSymbols()
    } catch (err) {
      error.value = 'Failed to fetch symbols from database'
      console.error('Error fetching symbols from database:', err)
      symbols.value = [] // Clear symbols on error
    } finally {
      isLoading.value = false
    }
  }

  async function fetchCurrentPrices() {
    try {
      isLoading.value = true
      error.value = null
      const prices = await api.getCurrentPrices()
      currentPrices.value = prices

      // Show info message if no price data is available
      if (prices.length === 0) {
        console.info('No price data available in database yet. Please ensure data acquisition service is running.')
      }
    } catch (err) {
      error.value = 'Failed to fetch current prices from database'
      console.error('Error fetching current prices from database:', err)
      currentPrices.value = [] // Clear prices on error
    } finally {
      isLoading.value = false
    }
  }

  async function fetchMarketSummary() {
    try {
      isLoading.value = true
      error.value = null
      marketSummary.value = await api.getMarketSummary()
    } catch (err) {
      error.value = 'Failed to fetch market summary from database'
      console.error('Error fetching market summary from database:', err)
      marketSummary.value = null // Clear summary on error
    } finally {
      isLoading.value = false
    }
  }

  function updatePriceData(newPriceData: PriceData[]) {
    // Ensure currentPrices is an array
    if (!Array.isArray(currentPrices.value)) {
      currentPrices.value = []
    }

    // Update existing prices or add new ones
    newPriceData.forEach(newPrice => {
      const existingIndex = currentPrices.value.findIndex(p => p.symbol === newPrice.symbol)
      if (existingIndex >= 0) {
        currentPrices.value[existingIndex] = newPrice
      } else {
        currentPrices.value.push(newPrice)
      }
    })
  }

  function updateMarketSummary(newSummary: MarketSummary) {
    marketSummary.value = newSummary
  }

  function connectWebSocket() {
    try {
      websocketService.onConnect(() => {
        isConnected.value = true
        console.log('Market store: WebSocket connected')
      })

      websocketService.onDisconnect(() => {
        isConnected.value = false
        console.log('Market store: WebSocket disconnected')
      })

      websocketService.onError((error) => {
        console.warn('Market store: WebSocket error (this is normal if backend WebSocket is not available):', error)
        isConnected.value = false
      })

      websocketService.onMessage((message) => {
        switch (message.type) {
          case 'PRICE_UPDATE':
            if (Array.isArray(message.data)) {
              updatePriceData(message.data)
            } else {
              updatePriceData([message.data])
            }
            break
          case 'MARKET_SUMMARY':
            updateMarketSummary(message.data)
            break
          default:
            console.log('Unknown message type:', message.type)
        }
      })

      websocketService.connect()
    } catch (error) {
      console.warn('WebSocket connection failed (this is normal if backend WebSocket is not available):', error)
      isConnected.value = false
    }
  }

  function disconnectWebSocket() {
    websocketService.disconnect()
    isConnected.value = false
  }

  async function initialize() {
    // Fetch initial data
    await Promise.all([
      fetchSymbols(),
      fetchCurrentPrices(),
      fetchMarketSummary()
    ])

    // Connect to WebSocket for real-time updates
    connectWebSocket()
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    symbols,
    currentPrices,
    marketSummary,
    isLoading,
    error,
    isConnected,
    
    // Computed
    topGainers,
    topLosers,
    totalMarketValue,
    
    // Actions
    fetchSymbols,
    fetchCurrentPrices,
    fetchMarketSummary,
    updatePriceData,
    updateMarketSummary,
    connectWebSocket,
    disconnectWebSocket,
    initialize,
    clearError
  }
})

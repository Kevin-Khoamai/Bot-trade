import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Portfolio, PortfolioAsset } from '../types'
import { api } from '../services/api'
import { useMarketStore } from './market'

export const usePortfolioStore = defineStore('portfolio', () => {
  // State
  const portfolio = ref<Portfolio | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Get market store for current prices
  const marketStore = useMarketStore()

  // Computed
  const totalValue = computed(() => {
    if (!portfolio.value) return 0
    return portfolio.value.assets.reduce((total, asset) => total + asset.value, 0)
  })

  const totalPnl = computed(() => {
    if (!portfolio.value) return 0
    return portfolio.value.assets.reduce((total, asset) => total + asset.pnl, 0)
  })

  const totalPnlPercent = computed(() => {
    if (!portfolio.value || totalValue.value === 0) return 0
    const totalCost = totalValue.value - totalPnl.value
    return totalCost > 0 ? (totalPnl.value / totalCost) * 100 : 0
  })

  const topPerformers = computed(() => {
    if (!portfolio.value) return []
    return [...portfolio.value.assets]
      .sort((a, b) => b.pnlPercent - a.pnlPercent)
      .slice(0, 3)
  })

  const worstPerformers = computed(() => {
    if (!portfolio.value) return []
    return [...portfolio.value.assets]
      .sort((a, b) => a.pnlPercent - b.pnlPercent)
      .slice(0, 3)
  })

  const assetAllocation = computed(() => {
    if (!portfolio.value || totalValue.value === 0) return []
    return portfolio.value.assets.map(asset => ({
      symbol: asset.symbol,
      value: asset.value,
      percentage: (asset.value / totalValue.value) * 100
    }))
  })

  // Actions
  async function fetchPortfolio() {
    try {
      isLoading.value = true
      error.value = null
      
      // Get portfolio data from database
      const portfolioData = await api.getPortfolio()
      portfolio.value = portfolioData
      
      // Update with current market prices
      updatePortfolioWithCurrentPrices()
      
    } catch (err) {
      console.error('Error fetching portfolio from database:', err)
      error.value = err instanceof Error ? err.message : 'Failed to fetch portfolio from database'
      portfolio.value = null
    } finally {
      isLoading.value = false
    }
  }

  function updatePortfolioWithCurrentPrices() {
    if (!portfolio.value || !marketStore.currentPrices.length) return

    portfolio.value.assets.forEach(asset => {
      const currentPrice = marketStore.currentPrices.find(
        price => price.symbol === asset.symbol
      )
      
      if (currentPrice) {
        asset.currentPrice = currentPrice.price
        asset.value = asset.quantity * currentPrice.price
        asset.pnl = asset.value - (asset.quantity * asset.averagePrice)
        asset.pnlPercent = asset.averagePrice > 0 
          ? ((asset.currentPrice - asset.averagePrice) / asset.averagePrice) * 100 
          : 0
      }
    })

    // Update portfolio totals
    portfolio.value.totalValue = totalValue.value
    portfolio.value.totalPnl = totalPnl.value
    portfolio.value.totalPnlPercent = totalPnlPercent.value
  }



  function addAsset(asset: PortfolioAsset) {
    if (!portfolio.value) {
      portfolio.value = {
        totalValue: 0,
        totalPnl: 0,
        totalPnlPercent: 0,
        assets: []
      }
    }
    
    const existingAssetIndex = portfolio.value.assets.findIndex(
      a => a.symbol === asset.symbol
    )
    
    if (existingAssetIndex >= 0) {
      // Update existing asset
      const existingAsset = portfolio.value.assets[existingAssetIndex]
      const totalQuantity = existingAsset.quantity + asset.quantity
      const totalCost = (existingAsset.quantity * existingAsset.averagePrice) + 
                       (asset.quantity * asset.averagePrice)
      
      existingAsset.quantity = totalQuantity
      existingAsset.averagePrice = totalCost / totalQuantity
      existingAsset.value = existingAsset.quantity * existingAsset.currentPrice
      existingAsset.pnl = existingAsset.value - totalCost
      existingAsset.pnlPercent = totalCost > 0 ? (existingAsset.pnl / totalCost) * 100 : 0
    } else {
      // Add new asset
      portfolio.value.assets.push(asset)
    }
    
    updatePortfolioWithCurrentPrices()
  }

  function removeAsset(symbol: string) {
    if (!portfolio.value) return
    
    portfolio.value.assets = portfolio.value.assets.filter(
      asset => asset.symbol !== symbol
    )
    
    updatePortfolioWithCurrentPrices()
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    portfolio,
    isLoading,
    error,
    
    // Computed
    totalValue,
    totalPnl,
    totalPnlPercent,
    topPerformers,
    worstPerformers,
    assetAllocation,
    
    // Actions
    fetchPortfolio,
    updatePortfolioWithCurrentPrices,
    addAsset,
    removeAsset,
    clearError
  }
})

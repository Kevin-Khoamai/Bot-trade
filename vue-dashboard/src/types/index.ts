// Crypto Trading Types

export interface PriceData {
  id: string
  symbol: string
  price: number
  volume: number
  change24h: number
  changePercent24h: number
  timestamp: string
  source: string
}

export interface MarketSummary {
  totalMarketCap: number
  totalVolume24h: number
  btcDominance: number
  activeCryptocurrencies: number
  markets: number
  timestamp: string
}

export interface CandlestickData {
  timestamp: number
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export interface ChartData {
  symbol: string
  interval: string
  data: CandlestickData[]
}

export interface TradingSymbol {
  symbol: string
  baseAsset: string
  quoteAsset: string
  status: string
  minPrice: number
  maxPrice: number
  tickSize: number
  minQty: number
  maxQty: number
  stepSize: number
}

export interface Portfolio {
  totalValue: number
  totalPnl: number
  totalPnlPercent: number
  assets: PortfolioAsset[]
}

export interface PortfolioAsset {
  symbol: string
  quantity: number
  averagePrice: number
  currentPrice: number
  value: number
  pnl: number
  pnlPercent: number
}

export interface WebSocketMessage {
  type: 'PRICE_UPDATE' | 'MARKET_SUMMARY' | 'TRADE_EXECUTION'
  data: any
  timestamp: string
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  timestamp: string
}

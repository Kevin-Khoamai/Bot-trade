import axios from 'axios';

class ApiService {
  constructor() {
    this.baseURL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
    this.api = axios.create({
      baseURL: this.baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor
    this.api.interceptors.request.use(
      (config) => {
        console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`);
        return config;
      },
      (error) => {
        console.error('API Request Error:', error);
        return Promise.reject(error);
      }
    );

    // Response interceptor
    this.api.interceptors.response.use(
      (response) => {
        console.log(`API Response: ${response.status} ${response.config.url}`);
        return response;
      },
      (error) => {
        console.error('API Response Error:', error.response?.status, error.response?.data);
        return Promise.reject(error);
      }
    );
  }

  // Health check
  async checkHealth() {
    try {
      const response = await this.api.get('/actuator/health');
      return response.data;
    } catch (error) {
      throw new Error(`Health check failed: ${error.message}`);
    }
  }

  // Get current prices for all symbols
  async getCurrentPrices() {
    try {
      const response = await this.api.get('/api/prices/current');
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get current prices: ${error.message}`);
    }
  }

  // Get current price for specific symbol
  async getCurrentPrice(symbol) {
    try {
      const response = await this.api.get(`/api/prices/current/${symbol}`);
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get current price for ${symbol}: ${error.message}`);
    }
  }

  // Get historical prices
  async getHistoricalPrices(symbol, startTime, endTime, interval = '1h') {
    try {
      const params = {
        symbol,
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        interval
      };
      const response = await this.api.get('/api/prices/history', { params });
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get historical prices for ${symbol}: ${error.message}`);
    }
  }

  // Get OHLCV data for charts
  async getOHLCVData(symbol, interval = '1h', limit = 100) {
    try {
      const params = { symbol, interval, limit };
      const response = await this.api.get('/api/prices/ohlcv', { params });
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get OHLCV data for ${symbol}: ${error.message}`);
    }
  }

  // Get recent trades
  async getRecentTrades(symbol, limit = 50) {
    try {
      const params = { symbol, limit };
      const response = await this.api.get('/api/trades/recent', { params });
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get recent trades for ${symbol}: ${error.message}`);
    }
  }

  // Get all recent trades
  async getAllRecentTrades(limit = 100) {
    try {
      const params = { limit };
      const response = await this.api.get('/api/trades/recent/all', { params });
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get all recent trades: ${error.message}`);
    }
  }

  // Get price statistics
  async getPriceStatistics(symbol, timeRange = '24h') {
    try {
      const params = { symbol, timeRange };
      const response = await this.api.get('/api/prices/statistics', { params });
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get price statistics for ${symbol}: ${error.message}`);
    }
  }

  // Get available symbols
  async getAvailableSymbols() {
    try {
      const response = await this.api.get('/api/symbols');
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get available symbols: ${error.message}`);
    }
  }

  // Get available exchanges
  async getAvailableExchanges() {
    try {
      const response = await this.api.get('/api/exchanges');
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get available exchanges: ${error.message}`);
    }
  }

  // Get market overview
  async getMarketOverview() {
    try {
      const response = await this.api.get('/api/market/overview');
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get market overview: ${error.message}`);
    }
  }

  // Get market summary
  async getMarketSummary() {
    try {
      const response = await this.api.get('/api/market/summary');
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get market summary: ${error.message}`);
    }
  }

  // Get system metrics
  async getSystemMetrics() {
    try {
      const response = await this.api.get('/actuator/metrics');
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get system metrics: ${error.message}`);
    }
  }

  // Get specific metric
  async getMetric(metricName) {
    try {
      const response = await this.api.get(`/actuator/metrics/${metricName}`);
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get metric ${metricName}: ${error.message}`);
    }
  }
}

// Create singleton instance
const apiService = new ApiService();
export default apiService;

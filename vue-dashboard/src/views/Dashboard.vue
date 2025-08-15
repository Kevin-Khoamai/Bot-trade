<template>
  <v-container fluid class="pa-4">
    <!-- Header -->
    <v-row class="mb-4">
      <v-col>
        <h1 class="text-h4 font-weight-bold">Crypto Trading Dashboard</h1>
        <p class="text-subtitle-1 text-medium-emphasis">
          Real-time cryptocurrency market data and trading insights
        </p>
      </v-col>
      <v-col cols="auto">
        <v-chip
          :color="marketStore.isConnected ? 'success' : 'error'"
          variant="flat"
          size="small"
        >
          <v-icon start>
            {{ marketStore.isConnected ? 'mdi-wifi' : 'mdi-wifi-off' }}
          </v-icon>
          {{ marketStore.isConnected ? 'Connected' : 'Disconnected' }}
        </v-chip>
      </v-col>
    </v-row>

    <!-- Error Alert -->
    <v-alert
      v-if="marketStore.error"
      type="error"
      variant="tonal"
      closable
      class="mb-4"
      @click:close="marketStore.clearError"
    >
      {{ marketStore.error }}
    </v-alert>

    <!-- Market Summary Cards -->
    <v-row class="mb-6">
      <v-col cols="12" md="3">
        <MarketSummaryCard
          title="Total Market Cap"
          :value="marketStore.marketSummary?.totalMarketCap"
          format="currency"
          icon="mdi-chart-line"
          color="primary"
        />
      </v-col>
      <v-col cols="12" md="3">
        <MarketSummaryCard
          title="24h Volume"
          :value="marketStore.marketSummary?.totalVolume24h"
          format="currency"
          icon="mdi-swap-horizontal"
          color="info"
        />
      </v-col>
      <v-col cols="12" md="3">
        <MarketSummaryCard
          title="BTC Dominance"
          :value="marketStore.marketSummary?.btcDominance"
          format="percentage"
          icon="mdi-bitcoin"
          color="warning"
        />
      </v-col>
      <v-col cols="12" md="3">
        <MarketSummaryCard
          title="Active Cryptos"
          :value="marketStore.marketSummary?.activeCryptocurrencies"
          format="number"
          icon="mdi-currency-usd"
          color="success"
        />
      </v-col>
    </v-row>

    <!-- Chart Section -->
    <v-row class="mb-6">
      <v-col cols="12">
        <SymbolSelector
          v-model="selectedSymbol"
          @symbol-change="onSymbolChange"
        />
        <CandlestickChart
          :symbol="selectedSymbol"
          :height="500"
        />
      </v-col>
    </v-row>

    <!-- Portfolio Overview -->
    <v-row class="mb-6">
      <v-col cols="12">
        <PortfolioSummary />
      </v-col>
    </v-row>

    <!-- Main Content -->
    <v-row>
      <!-- Price Table -->
      <v-col cols="12" lg="8">
        <PriceTable
          :prices="marketStore.currentPrices"
          :loading="marketStore.isLoading"
        />
      </v-col>

      <!-- Side Panel -->
      <v-col cols="12" lg="4">
        <v-row>
          <!-- Top Gainers -->
          <v-col cols="12">
            <TopMoversCard
              title="Top Gainers"
              :prices="marketStore.topGainers"
              type="gainers"
            />
          </v-col>

          <!-- Top Losers -->
          <v-col cols="12">
            <TopMoversCard
              title="Top Losers"
              :prices="marketStore.topLosers"
              type="losers"
            />
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useMarketStore } from '../stores/market'
import { usePortfolioStore } from '../stores/portfolio'
import MarketSummaryCard from '../components/MarketSummaryCard.vue'
import PriceTable from '../components/PriceTable.vue'
import TopMoversCard from '../components/TopMoversCard.vue'
import CandlestickChart from '../components/CandlestickChart.vue'
import SymbolSelector from '../components/SymbolSelector.vue'
import PortfolioSummary from '../components/PortfolioSummary.vue'

const marketStore = useMarketStore()
const portfolioStore = usePortfolioStore()
const selectedSymbol = ref('BTC-USD')

function onSymbolChange(symbol: string) {
  selectedSymbol.value = symbol
  console.log('Selected symbol changed to:', symbol)
}

onMounted(async () => {
  await marketStore.initialize()
  await portfolioStore.fetchPortfolio()
})

onUnmounted(() => {
  marketStore.disconnectWebSocket()
})
</script>

<style scoped>
.v-container {
  max-width: 1400px;
}
</style>

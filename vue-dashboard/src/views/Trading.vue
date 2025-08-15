<template>
  <v-container fluid class="pa-4">
    <!-- Header -->
    <v-row class="mb-4">
      <v-col>
        <h1 class="text-h4 font-weight-bold">Trading Interface</h1>
        <p class="text-subtitle-1 text-medium-emphasis">
          Advanced charting and market analysis
        </p>
      </v-col>
    </v-row>

    <!-- Symbol Selector -->
    <v-row class="mb-4">
      <v-col cols="12">
        <SymbolSelector
          v-model="selectedSymbol"
          @symbol-change="onSymbolChange"
        />
      </v-col>
    </v-row>

    <!-- Main Trading Layout -->
    <v-row>
      <!-- Chart Section -->
      <v-col cols="12" lg="9">
        <CandlestickChart
          :symbol="selectedSymbol"
          :height="600"
        />
      </v-col>

      <!-- Trading Panel -->
      <v-col cols="12" lg="3">
        <v-row>
          <!-- Order Book Placeholder -->
          <v-col cols="12">
            <v-card elevation="2" class="mb-4">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-format-list-numbered</v-icon>
                Order Book
              </v-card-title>
              <v-card-text class="text-center pa-8">
                <v-icon size="48" class="mb-4 text-medium-emphasis">mdi-chart-bar</v-icon>
                <div class="text-h6 mb-2">Order Book</div>
                <div class="text-body-2 text-medium-emphasis">
                  Coming with trading service
                </div>
              </v-card-text>
            </v-card>
          </v-col>

          <!-- Trade Form Placeholder -->
          <v-col cols="12">
            <v-card elevation="2">
              <v-card-title class="d-flex align-center">
                <v-icon class="mr-2">mdi-swap-horizontal</v-icon>
                Place Order
              </v-card-title>
              <v-card-text class="text-center pa-8">
                <v-icon size="48" class="mb-4 text-medium-emphasis">mdi-plus-circle</v-icon>
                <div class="text-h6 mb-2">Trading Form</div>
                <div class="text-body-2 text-medium-emphasis">
                  Coming with trading service
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-col>
    </v-row>

    <!-- Market Data Row -->
    <v-row class="mt-4">
      <v-col cols="12">
        <PriceTable
          :prices="marketStore.currentPrices.slice(0, 10)"
          :loading="marketStore.isLoading"
        />
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useMarketStore } from '../stores/market'
import CandlestickChart from '../components/CandlestickChart.vue'
import SymbolSelector from '../components/SymbolSelector.vue'
import PriceTable from '../components/PriceTable.vue'

const marketStore = useMarketStore()
const selectedSymbol = ref('BTC-USD')

function onSymbolChange(symbol: string) {
  selectedSymbol.value = symbol
  console.log('Trading view - Selected symbol changed to:', symbol)
}

onMounted(async () => {
  // Initialize market data if not already done
  if (marketStore.symbols.length === 0) {
    await marketStore.initialize()
  }
})

onUnmounted(() => {
  // Don't disconnect WebSocket here as it might be used by other views
})
</script>

<style scoped>
.v-container {
  max-width: 1600px;
}
</style>

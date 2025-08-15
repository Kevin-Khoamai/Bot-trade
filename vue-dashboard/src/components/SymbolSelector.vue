<template>
  <v-card elevation="2" class="mb-4">
    <v-card-text class="d-flex align-center">
      <v-icon class="mr-3">mdi-chart-candlestick</v-icon>
      
      <div class="flex-grow-1">
        <v-select
          v-model="selectedSymbol"
          :items="symbolItems"
          label="Select Trading Pair"
          variant="outlined"
          density="compact"
          hide-details
          @update:model-value="onSymbolChange"
        >
          <template #item="{ props, item }">
            <v-list-item v-bind="props">
              <template #prepend>
                <v-avatar size="24" class="mr-2">
                  <v-img
                    :src="`https://cryptoicons.org/api/icon/${item.raw.baseAsset?.toLowerCase()}/32`"
                    :alt="item.raw.baseAsset"
                  >
                    <template #error>
                      <v-icon size="16">mdi-currency-usd</v-icon>
                    </template>
                  </v-img>
                </v-avatar>
              </template>
              <v-list-item-title>{{ item.title }}</v-list-item-title>
              <v-list-item-subtitle>{{ item.raw.baseAsset }}/{{ item.raw.quoteAsset }}</v-list-item-subtitle>
            </v-list-item>
          </template>
          
          <template #selection="{ item }">
            <div class="d-flex align-center">
              <v-avatar size="20" class="mr-2">
                <v-img
                  :src="`https://cryptoicons.org/api/icon/${item.raw.baseAsset?.toLowerCase()}/32`"
                  :alt="item.raw.baseAsset"
                >
                  <template #error>
                    <v-icon size="12">mdi-currency-usd</v-icon>
                  </template>
                </v-img>
              </v-avatar>
              {{ item.title }}
            </div>
          </template>
        </v-select>
      </div>

      <!-- Current price display -->
      <div v-if="currentPrice" class="ml-4 text-right">
        <div class="text-h6 font-weight-bold">
          {{ formatCurrency(currentPrice.price) }}
        </div>
        <v-chip
          :color="currentPrice.changePercent24h >= 0 ? 'success' : 'error'"
          variant="flat"
          size="small"
        >
          <v-icon
            :icon="currentPrice.changePercent24h >= 0 ? 'mdi-trending-up' : 'mdi-trending-down'"
            size="14"
            class="mr-1"
          />
          {{ formatPercentage(currentPrice.changePercent24h) }}
        </v-chip>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useMarketStore } from '../stores/market'
import type { TradingSymbol, PriceData } from '../types'

interface Props {
  modelValue?: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'symbolChange', symbol: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const marketStore = useMarketStore()
const selectedSymbol = ref(props.modelValue || '')

// Computed properties
const symbolItems = computed(() => {
  if (!Array.isArray(marketStore.symbols)) return []
  return marketStore.symbols.map((symbol: TradingSymbol) => ({
    title: symbol.symbol,
    value: symbol.symbol,
    baseAsset: symbol.baseAsset,
    quoteAsset: symbol.quoteAsset,
    status: symbol.status
  }))
})

const currentPrice = computed(() => {
  if (!selectedSymbol.value || !Array.isArray(marketStore.currentPrices)) return null
  return marketStore.currentPrices.find((price: PriceData) =>
    price.symbol === selectedSymbol.value
  )
})

// Methods
function onSymbolChange(symbol: string) {
  emit('update:modelValue', symbol)
  emit('symbolChange', symbol)
}

// Formatting functions
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 8
  }).format(value)
}

function formatPercentage(value: number): string {
  const sign = value >= 0 ? '+' : ''
  return `${sign}${value.toFixed(2)}%`
}

// Watchers
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    selectedSymbol.value = newValue
  }
})

// Set default symbol when symbols are loaded
watch(() => marketStore.symbols, (symbols) => {
  if (Array.isArray(symbols) && symbols.length > 0 && !selectedSymbol.value) {
    const defaultSymbol = symbols.find(s => s.symbol === 'BTC-USD') || symbols[0]
    if (defaultSymbol) {
      selectedSymbol.value = defaultSymbol.symbol
      onSymbolChange(defaultSymbol.symbol)
    }
  }
}, { immediate: true })
</script>

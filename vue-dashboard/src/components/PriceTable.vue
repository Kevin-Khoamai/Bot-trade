<template>
  <v-card elevation="2">
    <v-card-title class="d-flex align-center">
      <v-icon class="mr-2">mdi-chart-line</v-icon>
      Market Prices
      <v-spacer />
      <v-text-field
        v-model="search"
        density="compact"
        label="Search symbols..."
        prepend-inner-icon="mdi-magnify"
        variant="outlined"
        hide-details
        single-line
        style="max-width: 300px;"
      />
    </v-card-title>

    <v-data-table
      :headers="headers"
      :items="filteredPrices"
      :loading="loading"
      :items-per-page="10"
      class="elevation-0"
      item-key="symbol"
    >
      <!-- Symbol column -->
      <template #item.symbol="{ item }">
        <div class="d-flex align-center">
          <v-avatar size="24" class="mr-2">
            <v-img
              :src="`https://cryptoicons.org/api/icon/${item.symbol.toLowerCase()}/32`"
              :alt="item.symbol"
            >
              <template #error>
                <v-icon size="16">mdi-currency-usd</v-icon>
              </template>
            </v-img>
          </v-avatar>
          <div>
            <div class="font-weight-medium">{{ item.symbol }}</div>
            <div class="text-caption text-medium-emphasis">{{ item.source }}</div>
          </div>
        </div>
      </template>

      <!-- Price column -->
      <template #item.price="{ item }">
        <div class="font-weight-medium">
          {{ formatCurrency(item.price) }}
        </div>
      </template>

      <!-- Volume column -->
      <template #item.volume="{ item }">
        <div class="text-body-2">
          {{ formatVolume(item.volume) }}
        </div>
      </template>

      <!-- 24h Change column -->
      <template #item.change24h="{ item }">
        <v-chip
          :color="item.changePercent24h >= 0 ? 'success' : 'error'"
          variant="flat"
          size="small"
        >
          <v-icon
            :icon="item.changePercent24h >= 0 ? 'mdi-trending-up' : 'mdi-trending-down'"
            size="16"
            class="mr-1"
          />
          {{ formatPercentage(item.changePercent24h) }}
        </v-chip>
      </template>

      <!-- Timestamp column -->
      <template #item.timestamp="{ item }">
        <div class="text-caption">
          {{ formatTimestamp(item.timestamp) }}
        </div>
      </template>

      <!-- Loading state -->
      <template #loading>
        <v-skeleton-loader type="table-row@10" />
      </template>

      <!-- No data state -->
      <template #no-data>
        <div class="text-center pa-4">
          <v-icon size="48" class="mb-2 text-medium-emphasis">mdi-database-search</v-icon>
          <div class="text-h6 text-medium-emphasis">No Price Data Available</div>
          <div class="text-body-2 text-medium-emphasis mb-4">
            Price data will appear here once the data acquisition service populates the database.
          </div>
          <v-chip color="info" variant="outlined" size="small">
            <v-icon start size="16">mdi-information</v-icon>
            Connected to Database - Waiting for Price Data
          </v-chip>
        </div>
      </template>
    </v-data-table>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { PriceData } from '../types'

interface Props {
  prices: PriceData[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const search = ref('')

const headers = [
  { title: 'Symbol', key: 'symbol', sortable: true },
  { title: 'Price', key: 'price', sortable: true, align: 'end' as const },
  { title: 'Volume', key: 'volume', sortable: true, align: 'end' as const },
  { title: '24h Change', key: 'change24h', sortable: true, align: 'center' as const },
  { title: 'Last Updated', key: 'timestamp', sortable: true, align: 'end' as const }
]

const filteredPrices = computed(() => {
  if (!search.value) return props.prices
  
  const searchTerm = search.value.toLowerCase()
  return props.prices.filter(price => 
    price.symbol.toLowerCase().includes(searchTerm)
  )
})

// Formatting functions
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 8
  }).format(value)
}

function formatVolume(value: number): string {
  return new Intl.NumberFormat('en-US', {
    notation: 'compact',
    maximumFractionDigits: 2
  }).format(value)
}

function formatPercentage(value: number): string {
  const sign = value >= 0 ? '+' : ''
  return `${sign}${value.toFixed(2)}%`
}

function formatTimestamp(timestamp: string): string {
  return new Date(timestamp).toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

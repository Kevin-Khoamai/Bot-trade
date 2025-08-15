<template>
  <v-card elevation="2" class="h-100">
    <v-card-title class="d-flex align-center">
      <v-icon 
        :icon="type === 'gainers' ? 'mdi-trending-up' : 'mdi-trending-down'"
        :color="type === 'gainers' ? 'success' : 'error'"
        class="mr-2"
      />
      {{ title }}
    </v-card-title>

    <v-card-text class="pa-0">
      <v-list density="compact">
        <template v-if="prices.length > 0">
          <v-list-item
            v-for="(price, index) in prices"
            :key="price.symbol"
            class="px-4"
          >
            <template #prepend>
              <v-avatar size="32" class="mr-3">
                <v-img
                  :src="`https://cryptoicons.org/api/icon/${price.symbol.toLowerCase()}/32`"
                  :alt="price.symbol"
                >
                  <template #error>
                    <v-icon size="16">mdi-currency-usd</v-icon>
                  </template>
                </v-img>
              </v-avatar>
            </template>

            <v-list-item-title class="font-weight-medium">
              {{ price.symbol }}
            </v-list-item-title>

            <v-list-item-subtitle>
              {{ formatCurrency(price.price) }}
            </v-list-item-subtitle>

            <template #append>
              <v-chip
                :color="type === 'gainers' ? 'success' : 'error'"
                variant="flat"
                size="small"
              >
                <v-icon
                  :icon="type === 'gainers' ? 'mdi-trending-up' : 'mdi-trending-down'"
                  size="14"
                  class="mr-1"
                />
                {{ formatPercentage(price.changePercent24h) }}
              </v-chip>
            </template>
          </v-list-item>
        </template>

        <template v-else>
          <v-list-item class="text-center pa-4">
            <v-list-item-title class="text-medium-emphasis">
              No data available
            </v-list-item-title>
          </v-list-item>
        </template>
      </v-list>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import type { PriceData } from '../types'

interface Props {
  title: string
  prices: PriceData[]
  type: 'gainers' | 'losers'
}

defineProps<Props>()

// Formatting functions
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 6
  }).format(value)
}

function formatPercentage(value: number): string {
  const sign = value >= 0 ? '+' : ''
  return `${sign}${value.toFixed(2)}%`
}
</script>

<template>
  <v-card elevation="2">
    <v-card-title class="d-flex align-center">
      <v-icon class="mr-2">mdi-wallet</v-icon>
      Portfolio Summary
      <v-spacer />
      <v-btn
        icon
        size="small"
        @click="portfolioStore.fetchPortfolio"
        :loading="portfolioStore.isLoading"
      >
        <v-icon>mdi-refresh</v-icon>
      </v-btn>
    </v-card-title>

    <!-- Loading State -->
    <v-card-text v-if="portfolioStore.isLoading" class="text-center pa-8">
      <v-progress-circular indeterminate size="64" color="primary" />
      <div class="text-h6 mt-4">Loading Portfolio...</div>
    </v-card-text>

    <!-- Error State -->
    <v-card-text v-else-if="portfolioStore.error" class="text-center pa-8">
      <v-icon size="48" class="mb-4 text-error">mdi-alert-circle</v-icon>
      <div class="text-h6 mb-2">Failed to Load Portfolio</div>
      <div class="text-body-2 text-medium-emphasis mb-4">{{ portfolioStore.error }}</div>
      <v-btn color="primary" @click="portfolioStore.fetchPortfolio">
        <v-icon start>mdi-refresh</v-icon>
        Retry
      </v-btn>
    </v-card-text>

    <!-- Portfolio Content -->
    <div v-else-if="portfolioStore.portfolio">
      <!-- Summary Cards -->
      <v-card-text>
        <v-row>
          <v-col cols="12" md="4">
            <v-card variant="outlined" class="text-center pa-4">
              <div class="text-caption text-medium-emphasis">Total Value</div>
              <div class="text-h5 font-weight-bold">
                {{ formatCurrency(portfolioStore.totalValue) }}
              </div>
            </v-card>
          </v-col>
          <v-col cols="12" md="4">
            <v-card variant="outlined" class="text-center pa-4">
              <div class="text-caption text-medium-emphasis">Total P&L</div>
              <div 
                class="text-h5 font-weight-bold"
                :class="portfolioStore.totalPnl >= 0 ? 'text-success' : 'text-error'"
              >
                {{ formatCurrency(portfolioStore.totalPnl) }}
              </div>
            </v-card>
          </v-col>
          <v-col cols="12" md="4">
            <v-card variant="outlined" class="text-center pa-4">
              <div class="text-caption text-medium-emphasis">Total P&L %</div>
              <div 
                class="text-h5 font-weight-bold"
                :class="portfolioStore.totalPnlPercent >= 0 ? 'text-success' : 'text-error'"
              >
                {{ formatPercentage(portfolioStore.totalPnlPercent) }}
              </div>
            </v-card>
          </v-col>
        </v-row>
      </v-card-text>

      <v-divider />

      <!-- Asset Allocation Chart -->
      <v-card-text>
        <div class="text-h6 mb-4">Asset Allocation</div>
        <PortfolioChart :data="portfolioStore.assetAllocation" />
      </v-card-text>

      <v-divider />

      <!-- Holdings Table -->
      <v-card-text>
        <div class="text-h6 mb-4">Holdings</div>
        <PortfolioTable 
          :assets="portfolioStore.portfolio.assets"
          @remove-asset="portfolioStore.removeAsset"
        />
      </v-card-text>

      <v-divider />

      <!-- Performance Summary -->
      <v-card-text>
        <v-row>
          <v-col cols="12" md="6">
            <div class="text-h6 mb-3">Top Performers</div>
            <v-list density="compact">
              <v-list-item
                v-for="asset in portfolioStore.topPerformers"
                :key="asset.symbol"
                class="px-0"
              >
                <template #prepend>
                  <v-avatar size="24" class="mr-3">
                    <v-img
                      :src="`https://cryptoicons.org/api/icon/${asset.symbol.split('-')[0].toLowerCase()}/32`"
                      :alt="asset.symbol"
                    >
                      <template #error>
                        <v-icon size="12">mdi-currency-usd</v-icon>
                      </template>
                    </v-img>
                  </v-avatar>
                </template>
                
                <v-list-item-title>{{ asset.symbol }}</v-list-item-title>
                
                <template #append>
                  <v-chip
                    color="success"
                    variant="flat"
                    size="small"
                  >
                    +{{ formatPercentage(asset.pnlPercent) }}
                  </v-chip>
                </template>
              </v-list-item>
            </v-list>
          </v-col>
          
          <v-col cols="12" md="6">
            <div class="text-h6 mb-3">Worst Performers</div>
            <v-list density="compact">
              <v-list-item
                v-for="asset in portfolioStore.worstPerformers"
                :key="asset.symbol"
                class="px-0"
              >
                <template #prepend>
                  <v-avatar size="24" class="mr-3">
                    <v-img
                      :src="`https://cryptoicons.org/api/icon/${asset.symbol.split('-')[0].toLowerCase()}/32`"
                      :alt="asset.symbol"
                    >
                      <template #error>
                        <v-icon size="12">mdi-currency-usd</v-icon>
                      </template>
                    </v-img>
                  </v-avatar>
                </template>
                
                <v-list-item-title>{{ asset.symbol }}</v-list-item-title>
                
                <template #append>
                  <v-chip
                    :color="asset.pnlPercent >= 0 ? 'success' : 'error'"
                    variant="flat"
                    size="small"
                  >
                    {{ formatPercentage(asset.pnlPercent) }}
                  </v-chip>
                </template>
              </v-list-item>
            </v-list>
          </v-col>
        </v-row>
      </v-card-text>
    </div>

    <!-- Empty State -->
    <v-card-text v-else class="text-center pa-8">
      <v-icon size="64" class="mb-4 text-medium-emphasis">mdi-wallet-outline</v-icon>
      <div class="text-h6 mb-2">No Portfolio Data</div>
      <div class="text-body-2 text-medium-emphasis mb-4">
        Start trading to build your portfolio
      </div>
      <v-btn color="primary" @click="portfolioStore.fetchPortfolio">
        <v-icon start>mdi-refresh</v-icon>
        Load Portfolio
      </v-btn>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { usePortfolioStore } from '../stores/portfolio'
import PortfolioChart from './PortfolioChart.vue'
import PortfolioTable from './PortfolioTable.vue'

const portfolioStore = usePortfolioStore()

// Formatting functions
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)
}

function formatPercentage(value: number): string {
  return `${value.toFixed(2)}%`
}

onMounted(() => {
  portfolioStore.fetchPortfolio()
})
</script>

<template>
  <v-container fluid class="pa-4">
    <!-- Header -->
    <v-row class="mb-4">
      <v-col>
        <h1 class="text-h4 font-weight-bold">Portfolio</h1>
        <p class="text-subtitle-1 text-medium-emphasis">
          Track your cryptocurrency investments and performance
        </p>
      </v-col>
      <v-col cols="auto">
        <v-btn
          color="primary"
          @click="addAssetDialog = true"
        >
          <v-icon start>mdi-plus</v-icon>
          Add Asset
        </v-btn>
      </v-col>
    </v-row>

    <!-- Portfolio Summary -->
    <v-row>
      <v-col cols="12">
        <PortfolioSummary />
      </v-col>
    </v-row>

    <!-- Quick Actions -->
    <v-row class="mt-4">
      <v-col cols="12">
        <v-card elevation="2">
          <v-card-title>Quick Actions</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" sm="6" md="3">
                <v-btn
                  block
                  variant="outlined"
                  @click="addAssetDialog = true"
                >
                  <v-icon start>mdi-plus</v-icon>
                  Add Asset
                </v-btn>
              </v-col>
              <v-col cols="12" sm="6" md="3">
                <v-btn
                  block
                  variant="outlined"
                  @click="portfolioStore.fetchPortfolio"
                >
                  <v-icon start>mdi-refresh</v-icon>
                  Refresh
                </v-btn>
              </v-col>
              <v-col cols="12" sm="6" md="3">
                <v-btn
                  block
                  variant="outlined"
                  disabled
                >
                  <v-icon start>mdi-download</v-icon>
                  Export
                </v-btn>
              </v-col>
              <v-col cols="12" sm="6" md="3">
                <v-btn
                  block
                  variant="outlined"
                  disabled
                >
                  <v-icon start>mdi-chart-line</v-icon>
                  Analytics
                </v-btn>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>

  <!-- Add Asset Dialog -->
  <AddAssetDialog
    v-model="addAssetDialog"
    @asset-added="onAssetAdded"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { usePortfolioStore } from '../stores/portfolio'
import { useMarketStore } from '../stores/market'
import PortfolioSummary from '../components/PortfolioSummary.vue'
import AddAssetDialog from '../components/AddAssetDialog.vue'
import type { PortfolioAsset } from '../types'

const portfolioStore = usePortfolioStore()
const marketStore = useMarketStore()
const addAssetDialog = ref(false)

function onAssetAdded(asset: PortfolioAsset) {
  portfolioStore.addAsset(asset)
  addAssetDialog.value = false
}

onMounted(async () => {
  // Initialize market data if not already done
  if (marketStore.symbols.length === 0) {
    await marketStore.initialize()
  }

  // Fetch portfolio data
  await portfolioStore.fetchPortfolio()
})
</script>

<style scoped>
.v-container {
  max-width: 1400px;
}
</style>

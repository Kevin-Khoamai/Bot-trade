<template>
  <v-data-table
    :headers="headers"
    :items="assets"
    class="elevation-0"
    item-key="symbol"
    :items-per-page="10"
  >
    <!-- Symbol column -->
    <template #item.symbol="{ item }">
      <div class="d-flex align-center">
        <v-avatar size="32" class="mr-3">
          <v-img
            :src="`https://cryptoicons.org/api/icon/${item.symbol.split('-')[0].toLowerCase()}/32`"
            :alt="item.symbol"
          >
            <template #error>
              <v-icon size="16">mdi-currency-usd</v-icon>
            </template>
          </v-img>
        </v-avatar>
        <div>
          <div class="font-weight-medium">{{ item.symbol }}</div>
          <div class="text-caption text-medium-emphasis">
            {{ item.symbol.split('-')[0] }}
          </div>
        </div>
      </div>
    </template>

    <!-- Quantity column -->
    <template #item.quantity="{ item }">
      <div class="font-weight-medium">
        {{ formatQuantity(item.quantity) }}
      </div>
    </template>

    <!-- Average Price column -->
    <template #item.averagePrice="{ item }">
      <div class="text-body-2">
        {{ formatCurrency(item.averagePrice) }}
      </div>
    </template>

    <!-- Current Price column -->
    <template #item.currentPrice="{ item }">
      <div class="font-weight-medium">
        {{ formatCurrency(item.currentPrice) }}
      </div>
    </template>

    <!-- Value column -->
    <template #item.value="{ item }">
      <div class="font-weight-bold">
        {{ formatCurrency(item.value) }}
      </div>
    </template>

    <!-- P&L column -->
    <template #item.pnl="{ item }">
      <div :class="item.pnl >= 0 ? 'text-success' : 'text-error'">
        <div class="font-weight-medium">
          {{ formatCurrency(item.pnl) }}
        </div>
        <div class="text-caption">
          ({{ formatPercentage(item.pnlPercent) }})
        </div>
      </div>
    </template>

    <!-- Actions column -->
    <template #item.actions="{ item }">
      <v-menu>
        <template #activator="{ props }">
          <v-btn
            icon
            size="small"
            variant="text"
            v-bind="props"
          >
            <v-icon>mdi-dots-vertical</v-icon>
          </v-btn>
        </template>
        
        <v-list density="compact">
          <v-list-item @click="viewDetails(item)">
            <template #prepend>
              <v-icon>mdi-eye</v-icon>
            </template>
            <v-list-item-title>View Details</v-list-item-title>
          </v-list-item>
          
          <v-list-item @click="editAsset(item)">
            <template #prepend>
              <v-icon>mdi-pencil</v-icon>
            </template>
            <v-list-item-title>Edit</v-list-item-title>
          </v-list-item>
          
          <v-divider />
          
          <v-list-item 
            @click="confirmRemove(item)"
            class="text-error"
          >
            <template #prepend>
              <v-icon color="error">mdi-delete</v-icon>
            </template>
            <v-list-item-title>Remove</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </template>

    <!-- No data state -->
    <template #no-data>
      <div class="text-center pa-4">
        <v-icon size="48" class="mb-2 text-medium-emphasis">mdi-wallet-outline</v-icon>
        <div class="text-h6 text-medium-emphasis">No Holdings</div>
        <div class="text-body-2 text-medium-emphasis">
          Your portfolio is empty
        </div>
      </div>
    </template>
  </v-data-table>

  <!-- Remove Confirmation Dialog -->
  <v-dialog v-model="removeDialog" max-width="400">
    <v-card>
      <v-card-title class="text-h6">
        Remove Asset
      </v-card-title>
      
      <v-card-text>
        Are you sure you want to remove <strong>{{ assetToRemove?.symbol }}</strong> from your portfolio?
        This action cannot be undone.
      </v-card-text>
      
      <v-card-actions>
        <v-spacer />
        <v-btn @click="removeDialog = false">
          Cancel
        </v-btn>
        <v-btn 
          color="error" 
          @click="removeAsset"
        >
          Remove
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { PortfolioAsset } from '../types'

interface Props {
  assets: PortfolioAsset[]
}

interface Emits {
  (e: 'removeAsset', symbol: string): void
  (e: 'editAsset', asset: PortfolioAsset): void
  (e: 'viewDetails', asset: PortfolioAsset): void
}

defineProps<Props>()
const emit = defineEmits<Emits>()

// State
const removeDialog = ref(false)
const assetToRemove = ref<PortfolioAsset | null>(null)

// Table headers
const headers = [
  { title: 'Asset', key: 'symbol', sortable: true },
  { title: 'Quantity', key: 'quantity', sortable: true, align: 'end' as const },
  { title: 'Avg Price', key: 'averagePrice', sortable: true, align: 'end' as const },
  { title: 'Current Price', key: 'currentPrice', sortable: true, align: 'end' as const },
  { title: 'Value', key: 'value', sortable: true, align: 'end' as const },
  { title: 'P&L', key: 'pnl', sortable: true, align: 'end' as const },
  { title: 'Actions', key: 'actions', sortable: false, align: 'center' as const }
]

// Methods
function confirmRemove(asset: PortfolioAsset) {
  assetToRemove.value = asset
  removeDialog.value = true
}

function removeAsset() {
  if (assetToRemove.value) {
    emit('removeAsset', assetToRemove.value.symbol)
  }
  removeDialog.value = false
  assetToRemove.value = null
}

function editAsset(asset: PortfolioAsset) {
  emit('editAsset', asset)
}

function viewDetails(asset: PortfolioAsset) {
  emit('viewDetails', asset)
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

function formatQuantity(value: number): string {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 8
  }).format(value)
}

function formatPercentage(value: number): string {
  const sign = value >= 0 ? '+' : ''
  return `${sign}${value.toFixed(2)}%`
}
</script>

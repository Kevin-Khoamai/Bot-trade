<template>
  <v-dialog v-model="dialog" max-width="500" persistent>
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2">mdi-plus-circle</v-icon>
        Add Asset to Portfolio
      </v-card-title>

      <v-card-text>
        <v-form ref="form" v-model="valid" @submit.prevent="addAsset">
          <v-row>
            <!-- Symbol Selection -->
            <v-col cols="12">
              <v-select
                v-model="formData.symbol"
                :items="symbolItems"
                label="Trading Pair"
                variant="outlined"
                :rules="[rules.required]"
                required
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
            </v-col>

            <!-- Quantity -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="formData.quantity"
                label="Quantity"
                variant="outlined"
                type="number"
                step="any"
                min="0"
                :rules="[rules.required, rules.positive]"
                required
              />
            </v-col>

            <!-- Average Price -->
            <v-col cols="12" sm="6">
              <v-text-field
                v-model.number="formData.averagePrice"
                label="Average Price (USD)"
                variant="outlined"
                type="number"
                step="any"
                min="0"
                :rules="[rules.required, rules.positive]"
                required
              />
            </v-col>

            <!-- Current Price (Auto-filled) -->
            <v-col cols="12">
              <v-text-field
                :model-value="currentPrice"
                label="Current Price (USD)"
                variant="outlined"
                readonly
                :loading="loadingCurrentPrice"
              >
                <template #append-inner>
                  <v-btn
                    icon
                    size="small"
                    @click="updateCurrentPrice"
                    :loading="loadingCurrentPrice"
                  >
                    <v-icon>mdi-refresh</v-icon>
                  </v-btn>
                </template>
              </v-text-field>
            </v-col>

            <!-- Calculated Values -->
            <v-col cols="12">
              <v-card variant="outlined" class="pa-3">
                <div class="text-subtitle-2 mb-2">Calculated Values</div>
                <v-row dense>
                  <v-col cols="6">
                    <div class="text-caption text-medium-emphasis">Total Value</div>
                    <div class="text-h6">{{ formatCurrency(totalValue) }}</div>
                  </v-col>
                  <v-col cols="6">
                    <div class="text-caption text-medium-emphasis">P&L</div>
                    <div 
                      class="text-h6"
                      :class="pnl >= 0 ? 'text-success' : 'text-error'"
                    >
                      {{ formatCurrency(pnl) }}
                    </div>
                  </v-col>
                </v-row>
              </v-card>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>

      <v-card-actions>
        <v-spacer />
        <v-btn @click="closeDialog">
          Cancel
        </v-btn>
        <v-btn
          color="primary"
          @click="addAsset"
          :disabled="!valid"
          :loading="loading"
        >
          Add Asset
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useMarketStore } from '../stores/market'
import type { PortfolioAsset, TradingSymbol, PriceData } from '../types'

interface Props {
  modelValue: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'assetAdded', asset: PortfolioAsset): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const marketStore = useMarketStore()

// State
const form = ref()
const valid = ref(false)
const loading = ref(false)
const loadingCurrentPrice = ref(false)

const formData = ref({
  symbol: '',
  quantity: 0,
  averagePrice: 0
})

// Computed
const dialog = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const symbolItems = computed(() => {
  return marketStore.symbols.map((symbol: TradingSymbol) => ({
    title: symbol.symbol,
    value: symbol.symbol,
    baseAsset: symbol.baseAsset,
    quoteAsset: symbol.quoteAsset
  }))
})

const currentPrice = computed(() => {
  if (!formData.value.symbol) return 0
  const price = marketStore.currentPrices.find((p: PriceData) => 
    p.symbol === formData.value.symbol
  )
  return price?.price || 0
})

const totalValue = computed(() => {
  return formData.value.quantity * currentPrice.value
})

const pnl = computed(() => {
  const cost = formData.value.quantity * formData.value.averagePrice
  return totalValue.value - cost
})

// Validation rules
const rules = {
  required: (value: any) => !!value || 'This field is required',
  positive: (value: number) => value > 0 || 'Value must be positive'
}

// Methods
async function addAsset() {
  if (!valid.value) return

  loading.value = true
  
  try {
    const asset: PortfolioAsset = {
      symbol: formData.value.symbol,
      quantity: formData.value.quantity,
      averagePrice: formData.value.averagePrice,
      currentPrice: currentPrice.value,
      value: totalValue.value,
      pnl: pnl.value,
      pnlPercent: formData.value.averagePrice > 0 
        ? ((currentPrice.value - formData.value.averagePrice) / formData.value.averagePrice) * 100 
        : 0
    }

    emit('assetAdded', asset)
    resetForm()
    
  } catch (error) {
    console.error('Error adding asset:', error)
  } finally {
    loading.value = false
  }
}

function closeDialog() {
  resetForm()
  emit('update:modelValue', false)
}

function resetForm() {
  formData.value = {
    symbol: '',
    quantity: 0,
    averagePrice: 0
  }
  form.value?.resetValidation()
}

async function updateCurrentPrice() {
  if (!formData.value.symbol) return
  
  loadingCurrentPrice.value = true
  try {
    await marketStore.fetchCurrentPrices()
  } catch (error) {
    console.error('Error updating current price:', error)
  } finally {
    loadingCurrentPrice.value = false
  }
}

// Watch for symbol changes to auto-update current price
watch(() => formData.value.symbol, (newSymbol) => {
  if (newSymbol) {
    updateCurrentPrice()
  }
})

// Formatting functions
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value)
}
</script>

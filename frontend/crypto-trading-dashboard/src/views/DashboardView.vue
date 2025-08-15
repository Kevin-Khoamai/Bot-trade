<template>
  <div class="dashboard">
    <!-- Summary Cards -->
    <v-row class="mb-4">
      <v-col cols="12" sm="6" md="3">
        <v-card>
          <v-card-text>
            <div class="d-flex align-center">
              <v-icon color="primary" size="40" class="mr-3">mdi-wallet</v-icon>
              <div>
                <div class="text-h6">{{ formatCurrency(portfolioValue) }}</div>
                <div class="text-caption text-medium-emphasis">Portfolio Value</div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      
      <v-col cols="12" sm="6" md="3">
        <v-card>
          <v-card-text>
            <div class="d-flex align-center">
              <v-icon :color="dailyPnlColor" size="40" class="mr-3">
                {{ dailyPnl >= 0 ? 'mdi-trending-up' : 'mdi-trending-down' }}
              </v-icon>
              <div>
                <div class="text-h6" :class="dailyPnlColor + '--text'">
                  {{ formatCurrency(dailyPnl) }}
                </div>
                <div class="text-caption text-medium-emphasis">Daily P&L</div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      
      <v-col cols="12" sm="6" md="3">
        <v-card>
          <v-card-text>
            <div class="d-flex align-center">
              <v-icon color="info" size="40" class="mr-3">mdi-swap-horizontal</v-icon>
              <div>
                <div class="text-h6">{{ activeOrdersCount }}</div>
                <div class="text-caption text-medium-emphasis">Active Orders</div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      
      <v-col cols="12" sm="6" md="3">
        <v-card>
          <v-card-text>
            <div class="d-flex align-center">
              <v-icon :color="riskLevelColor" size="40" class="mr-3">mdi-shield-alert</v-icon>
              <div>
                <div class="text-h6" :class="riskLevelColor + '--text'">{{ riskLevel }}</div>
                <div class="text-caption text-medium-emphasis">Risk Level</div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Charts Row -->
    <v-row class="mb-4">
      <!-- Portfolio Performance Chart -->
      <v-col cols="12" md="8">
        <v-card>
          <v-card-title>
            <span>Portfolio Performance</span>
            <v-spacer></v-spacer>
            <v-btn-toggle v-model="portfolioTimeframe" mandatory>
              <v-btn value="1D" size="small">1D</v-btn>
              <v-btn value="1W" size="small">1W</v-btn>
              <v-btn value="1M" size="small">1M</v-btn>
              <v-btn value="3M" size="small">3M</v-btn>
            </v-btn-toggle>
          </v-card-title>
          <v-card-text>
            <div style="height: 300px;">
              <apexchart
                type="line"
                height="300"
                :options="portfolioChartOptions"
                :series="portfolioChartSeries"
              ></apexchart>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      
      <!-- Asset Allocation -->
      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>Asset Allocation</v-card-title>
          <v-card-text>
            <div style="height: 300px;">
              <apexchart
                type="donut"
                height="300"
                :options="allocationChartOptions"
                :series="allocationChartSeries"
              ></apexchart>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Market Data and Recent Activity -->
    <v-row>
      <!-- Top Cryptocurrencies -->
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>Top Cryptocurrencies</v-card-title>
          <v-card-text>
            <v-data-table
              :headers="marketHeaders"
              :items="topCryptos"
              :items-per-page="10"
              hide-default-footer
              density="compact"
            >
              <template v-slot:item.symbol="{ item }">
                <div class="d-flex align-center">
                  <v-avatar size="24" class="mr-2">
                    <img :src="getCryptoIcon(item.symbol)" :alt="item.symbol">
                  </v-avatar>
                  {{ item.symbol }}
                </div>
              </template>
              
              <template v-slot:item.price="{ item }">
                <span class="font-weight-medium">{{ formatCurrency(item.price) }}</span>
              </template>
              
              <template v-slot:item.change24h="{ item }">
                <v-chip
                  :color="item.change24h >= 0 ? 'success' : 'error'"
                  size="small"
                  variant="outlined"
                >
                  {{ item.change24h >= 0 ? '+' : '' }}{{ item.change24h.toFixed(2) }}%
                </v-chip>
              </template>
              
              <template v-slot:item.volume24h="{ item }">
                <span class="text-caption">{{ formatVolume(item.volume24h) }}</span>
              </template>
            </v-data-table>
          </v-card-text>
        </v-card>
      </v-col>
      
      <!-- Recent Activity -->
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>Recent Activity</v-card-title>
          <v-card-text>
            <v-timeline density="compact">
              <v-timeline-item
                v-for="activity in recentActivity"
                :key="activity.id"
                :dot-color="activity.color"
                size="small"
              >
                <template v-slot:icon>
                  <v-icon size="16">{{ activity.icon }}</v-icon>
                </template>
                
                <div class="d-flex justify-space-between align-center">
                  <div>
                    <div class="text-body-2">{{ activity.title }}</div>
                    <div class="text-caption text-medium-emphasis">{{ activity.description }}</div>
                  </div>
                  <div class="text-caption text-medium-emphasis">
                    {{ formatTime(activity.timestamp) }}
                  </div>
                </div>
              </v-timeline-item>
            </v-timeline>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Risk Alerts (if any) -->
    <v-row v-if="criticalAlerts.length > 0" class="mt-4">
      <v-col cols="12">
        <v-alert
          type="error"
          variant="outlined"
          closable
          @click:close="dismissAlerts"
        >
          <v-alert-title>Critical Risk Alerts</v-alert-title>
          <div v-for="alert in criticalAlerts" :key="alert.id" class="mt-2">
            <strong>{{ alert.alertType }}:</strong> {{ alert.message }}
          </div>
        </v-alert>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useWebSocketStore } from '@/stores/websocket'
import { formatCurrency, formatVolume, formatTime } from '@/utils/formatters'
import numeral from 'numeral'

const websocketStore = useWebSocketStore()

// Reactive data
const portfolioTimeframe = ref('1D')
const loading = ref(true)

// Computed properties
const portfolioValue = computed(() => websocketStore.portfolioData?.totalValue || 0)
const dailyPnl = computed(() => websocketStore.portfolioData?.dailyPnl || 0)
const activeOrdersCount = computed(() => websocketStore.activeOrders.size)
const criticalAlerts = computed(() => websocketStore.criticalAlerts)

const dailyPnlColor = computed(() => dailyPnl.value >= 0 ? 'success' : 'error')
const riskLevel = computed(() => 'Medium') // Would come from risk service
const riskLevelColor = computed(() => {
  switch (riskLevel.value) {
    case 'Low': return 'success'
    case 'Medium': return 'warning'
    case 'High': return 'error'
    default: return 'info'
  }
})

const topCryptos = computed(() => websocketStore.latestPrices.slice(0, 10))

// Chart configurations
const portfolioChartOptions = ref({
  chart: {
    type: 'line',
    toolbar: { show: false },
    background: 'transparent'
  },
  theme: { mode: 'dark' },
  stroke: { curve: 'smooth', width: 2 },
  xaxis: { type: 'datetime' },
  yaxis: { labels: { formatter: (val: number) => formatCurrency(val) } },
  tooltip: {
    y: { formatter: (val: number) => formatCurrency(val) }
  },
  colors: ['#1976D2']
})

const portfolioChartSeries = computed(() => [{
  name: 'Portfolio Value',
  data: websocketStore.portfolioHistory.slice(0, 100).map(p => ({
    x: new Date(p.timestamp).getTime(),
    y: p.totalValue
  })).reverse()
}])

const allocationChartOptions = ref({
  chart: { type: 'donut' },
  theme: { mode: 'dark' },
  legend: { position: 'bottom' },
  colors: ['#1976D2', '#424242', '#82B1FF', '#FF5252', '#4CAF50']
})

const allocationChartSeries = computed(() => [40, 25, 20, 10, 5])

// Table headers
const marketHeaders = [
  { title: 'Symbol', key: 'symbol', sortable: false },
  { title: 'Price', key: 'price', align: 'end' },
  { title: '24h Change', key: 'change24h', align: 'center' },
  { title: 'Volume', key: 'volume24h', align: 'end' }
]

// Recent activity
const recentActivity = computed(() => [
  {
    id: 1,
    title: 'Order Filled',
    description: 'BUY 0.5 BTC at $45,000',
    timestamp: new Date().toISOString(),
    icon: 'mdi-check-circle',
    color: 'success'
  },
  {
    id: 2,
    title: 'Risk Alert',
    description: 'Portfolio drawdown exceeded 5%',
    timestamp: new Date(Date.now() - 300000).toISOString(),
    icon: 'mdi-alert',
    color: 'warning'
  },
  {
    id: 3,
    title: 'Strategy Executed',
    description: 'MACD crossover signal triggered',
    timestamp: new Date(Date.now() - 600000).toISOString(),
    icon: 'mdi-robot',
    color: 'info'
  }
])

// Methods
const getCryptoIcon = (symbol: string) => {
  return `/crypto-icons/${symbol.toLowerCase()}.png`
}

const dismissAlerts = () => {
  websocketStore.markAlertsAsRead()
}

// Watchers
watch(portfolioTimeframe, (newTimeframe) => {
  // Update chart data based on timeframe
  console.log('Timeframe changed to:', newTimeframe)
})

// Lifecycle
onMounted(() => {
  loading.value = false
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.v-card {
  height: 100%;
}

.v-data-table {
  background: transparent;
}
</style>

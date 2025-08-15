<template>
  <v-card elevation="2" class="chart-container">
    <v-card-title class="d-flex align-center">
      <v-icon class="mr-2">mdi-chart-candlestick</v-icon>
      {{ symbol }} Price Chart
      <v-spacer />
      
      <!-- Chart controls -->
      <div class="d-flex align-center ga-2">
        <!-- Volume toggle -->
        <v-btn
          :color="showVolume ? 'primary' : 'default'"
          :variant="showVolume ? 'flat' : 'outlined'"
          size="small"
          @click="toggleVolume"
        >
          <v-icon size="16">mdi-chart-bar</v-icon>
          Volume
        </v-btn>

        <!-- Time interval selector -->
        <v-btn-toggle
          v-model="selectedInterval"
          variant="outlined"
          size="small"
          mandatory
          @update:model-value="onIntervalChange"
        >
          <v-btn value="1m" size="small">1m</v-btn>
          <v-btn value="5m" size="small">5m</v-btn>
          <v-btn value="15m" size="small">15m</v-btn>
          <v-btn value="1h" size="small">1h</v-btn>
          <v-btn value="4h" size="small">4h</v-btn>
          <v-btn value="1d" size="small">1d</v-btn>
        </v-btn-toggle>
      </div>
    </v-card-title>

    <v-card-text class="pa-0">
      <!-- Chart container -->
      <div
        ref="chartContainer"
        class="chart-wrapper"
        :style="{ height: height + 'px' }"
      />
      
      <!-- Loading overlay -->
      <v-overlay
        v-model="loading"
        contained
        class="d-flex align-center justify-center"
      >
        <v-progress-circular
          indeterminate
          size="64"
          color="primary"
        />
      </v-overlay>

      <!-- Error state -->
      <div
        v-if="error && !loading"
        class="d-flex flex-column align-center justify-center pa-8"
        :style="{ height: height + 'px' }"
      >
        <v-icon size="48" class="mb-4 text-medium-emphasis">mdi-chart-line-variant</v-icon>
        <div class="text-h6 text-medium-emphasis mb-2">Failed to load chart data</div>
        <div class="text-body-2 text-medium-emphasis mb-4">{{ error }}</div>
        <v-btn color="primary" @click="loadChartData">
          <v-icon start>mdi-refresh</v-icon>
          Retry
        </v-btn>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { createChart, ColorType } from 'lightweight-charts'
import { api } from '../services/api'
import type { CandlestickData as AppCandlestickData } from '../types'

interface Props {
  symbol: string
  height?: number
}

const props = withDefaults(defineProps<Props>(), {
  height: 400
})

// Reactive state
const chartContainer = ref<HTMLElement>()
const selectedInterval = ref('1h')
const loading = ref(false)
const error = ref<string | null>(null)
const showVolume = ref(true)

// Chart instances
let chart: any = null
let candlestickSeries: any = null
let volumeSeries: any = null
let ma7Series: any = null
let ma25Series: any = null

// Initialize chart
function initChart() {
  if (!chartContainer.value) return

  // Create the chart
  chart = createChart(chartContainer.value, {
    layout: {
      background: { type: ColorType.Solid, color: 'transparent' },
      textColor: '#d1d4dc',
    },
    width: chartContainer.value.clientWidth,
    height: props.height,
    grid: {
      vertLines: { color: 'rgba(42, 46, 57, 0.5)' },
      horzLines: { color: 'rgba(42, 46, 57, 0.5)' },
    },
    crosshair: {
      mode: 1,
    },
    rightPriceScale: {
      borderColor: 'rgba(197, 203, 206, 0.8)',
      textColor: '#d1d4dc',
    },
    timeScale: {
      borderColor: 'rgba(197, 203, 206, 0.8)',
      textColor: '#d1d4dc',
      timeVisible: true,
      secondsVisible: false,
    },
  })

  // Add candlestick series
  candlestickSeries = chart.addCandlestickSeries({
    upColor: '#26a69a',
    downColor: '#ef5350',
    borderVisible: false,
    wickUpColor: '#26a69a',
    wickDownColor: '#ef5350',
  })

  // Add volume series (initially hidden if showVolume is false)
  volumeSeries = chart.addHistogramSeries({
    color: '#26a69a',
    priceFormat: {
      type: 'volume',
    },
    priceScaleId: 'volume',
    scaleMargins: {
      top: 0.85, // Volume takes only bottom 15% of chart
      bottom: 0,
    },
    visible: showVolume.value,
  })

  // Create separate price scale for volume
  chart.priceScale('volume').applyOptions({
    scaleMargins: {
      top: 0.85,
      bottom: 0,
    },
  })

  // Add moving averages
  ma7Series = chart.addLineSeries({
    color: '#2196F3',
    lineWidth: 1,
    title: 'MA(7)',
  })

  ma25Series = chart.addLineSeries({
    color: '#FF6D00',
    lineWidth: 1,
    title: 'MA(25)',
  })

  // Handle resize
  const resizeObserver = new ResizeObserver(entries => {
    if (chart && entries.length > 0) {
      const { width } = entries[0].contentRect
      chart.applyOptions({ width })
    }
  })

  resizeObserver.observe(chartContainer.value)
}

// Load chart data from API
async function loadChartData() {
  if (!props.symbol || !candlestickSeries || !volumeSeries) return

  try {
    loading.value = true
    error.value = null

    // Get historical price data
    const historyData = await api.getPriceHistory(props.symbol, selectedInterval.value, 200)

    if (!historyData || !Array.isArray(historyData)) {
      throw new Error('Invalid chart data received')
    }

    // Convert to lightweight-charts format
    const candlestickData = historyData.map((item: AppCandlestickData) => ({
      time: Math.floor(item.timestamp / 1000),
      open: item.open,
      high: item.high,
      low: item.low,
      close: item.close,
    }))

    const volumeData = historyData.map((item: AppCandlestickData) => ({
      time: Math.floor(item.timestamp / 1000),
      value: item.volume,
      color: item.close >= item.open ? 'rgba(38, 166, 154, 0.5)' : 'rgba(239, 83, 80, 0.5)',
    }))

    // Calculate moving averages
    const ma7Data = calculateMA(candlestickData, 7)
    const ma25Data = calculateMA(candlestickData, 25)

    // Update series
    candlestickSeries.setData(candlestickData)
    if (showVolume.value) {
      volumeSeries.setData(volumeData)
    }
    ma7Series.setData(ma7Data)
    ma25Series.setData(ma25Data)

    // Fit content
    chart.timeScale().fitContent()

  } catch (err) {
    console.error('Error loading chart data from database:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load chart data from database'
  } finally {
    loading.value = false
  }
}

// Handle interval change
function onIntervalChange() {
  loadChartData()
}

// Toggle volume display
function toggleVolume() {
  showVolume.value = !showVolume.value
  if (volumeSeries) {
    volumeSeries.applyOptions({
      visible: showVolume.value
    })
  }
}

// Calculate moving average
function calculateMA(data: any[], period: number) {
  const result = []
  for (let i = period - 1; i < data.length; i++) {
    const sum = data.slice(i - period + 1, i + 1).reduce((acc, item) => acc + item.close, 0)
    result.push({
      time: data[i].time,
      value: sum / period
    })
  }
  return result
}

// Watchers
watch(() => props.symbol, () => {
  loadChartData()
})

// Lifecycle
onMounted(async () => {
  await nextTick()
  initChart()
  
  // Try to load real data, fallback to mock data
  try {
    await loadChartData()
  } catch {
    console.log('Loading mock data for demonstration...')
    await loadMockData()
  }
})

onUnmounted(() => {
  if (chart) {
    chart.remove()
    chart = null
    candlestickSeries = null
    volumeSeries = null
    ma7Series = null
    ma25Series = null
  }
})
</script>

<style scoped>
.chart-container {
  position: relative;
  overflow: hidden;
}

.chart-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

/* Ensure chart renders properly */
:deep(.tv-lightweight-charts) {
  width: 100% !important;
  height: 100% !important;
}
</style>

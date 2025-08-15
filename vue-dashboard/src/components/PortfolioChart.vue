<template>
  <div class="portfolio-chart">
    <canvas ref="chartCanvas" :width="width" :height="height"></canvas>
    
    <!-- Legend -->
    <div class="chart-legend mt-4">
      <v-row>
        <v-col
          v-for="(item, index) in data"
          :key="item.symbol"
          cols="12"
          sm="6"
          md="4"
        >
          <div class="d-flex align-center">
            <div
              class="legend-color mr-2"
              :style="{ backgroundColor: colors[index % colors.length] }"
            ></div>
            <div class="flex-grow-1">
              <div class="text-body-2 font-weight-medium">{{ item.symbol }}</div>
              <div class="text-caption text-medium-emphasis">
                {{ formatCurrency(item.value) }} ({{ formatPercentage(item.percentage) }})
              </div>
            </div>
          </div>
        </v-col>
      </v-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'

interface ChartData {
  symbol: string
  value: number
  percentage: number
}

interface Props {
  data: ChartData[]
  width?: number
  height?: number
}

const props = withDefaults(defineProps<Props>(), {
  width: 300,
  height: 300
})

const chartCanvas = ref<HTMLCanvasElement>()

// Chart colors
const colors = [
  '#1976D2', // Blue
  '#388E3C', // Green
  '#F57C00', // Orange
  '#7B1FA2', // Purple
  '#C62828', // Red
  '#00796B', // Teal
  '#5D4037', // Brown
  '#455A64'  // Blue Grey
]

function drawChart() {
  if (!chartCanvas.value || !props.data.length) return

  const canvas = chartCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // Clear canvas
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // Calculate center and radius
  const centerX = canvas.width / 2
  const centerY = canvas.height / 2
  const radius = Math.min(centerX, centerY) - 20

  // Draw pie chart
  let currentAngle = -Math.PI / 2 // Start from top

  props.data.forEach((item, index) => {
    const sliceAngle = (item.percentage / 100) * 2 * Math.PI
    const color = colors[index % colors.length]

    // Draw slice
    ctx.beginPath()
    ctx.moveTo(centerX, centerY)
    ctx.arc(centerX, centerY, radius, currentAngle, currentAngle + sliceAngle)
    ctx.closePath()
    ctx.fillStyle = color
    ctx.fill()

    // Draw border
    ctx.strokeStyle = '#ffffff'
    ctx.lineWidth = 2
    ctx.stroke()

    // Draw percentage label if slice is large enough
    if (item.percentage > 5) {
      const labelAngle = currentAngle + sliceAngle / 2
      const labelX = centerX + Math.cos(labelAngle) * (radius * 0.7)
      const labelY = centerY + Math.sin(labelAngle) * (radius * 0.7)

      ctx.fillStyle = '#ffffff'
      ctx.font = 'bold 12px Roboto'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(`${item.percentage.toFixed(1)}%`, labelX, labelY)
    }

    currentAngle += sliceAngle
  })

  // Draw center circle for donut effect
  ctx.beginPath()
  ctx.arc(centerX, centerY, radius * 0.4, 0, 2 * Math.PI)
  ctx.fillStyle = '#1E1E1E' // Match dark theme background
  ctx.fill()

  // Draw total value in center
  const totalValue = props.data.reduce((sum, item) => sum + item.value, 0)
  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 16px Roboto'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('Total', centerX, centerY - 10)
  
  ctx.font = '14px Roboto'
  ctx.fillText(formatCurrency(totalValue), centerX, centerY + 10)
}

// Formatting functions
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(value)
}

function formatPercentage(value: number): string {
  return `${value.toFixed(1)}%`
}

// Watch for data changes
watch(() => props.data, async () => {
  await nextTick()
  drawChart()
}, { deep: true })

onMounted(async () => {
  await nextTick()
  drawChart()
})
</script>

<style scoped>
.portfolio-chart {
  text-align: center;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 2px;
  flex-shrink: 0;
}

canvas {
  max-width: 100%;
  height: auto;
}
</style>

<template>
  <div class="system-health">
    <!-- Overall System Status -->
    <v-row class="mb-4">
      <v-col cols="12">
        <v-card>
          <v-card-title>
            <v-icon :color="overallStatusColor" class="mr-2">{{ overallStatusIcon }}</v-icon>
            System Status: {{ overallStatus }}
          </v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="3">
                <v-card variant="outlined">
                  <v-card-text class="text-center">
                    <v-icon size="48" :color="systemUptime >= 99 ? 'success' : 'warning'">
                      mdi-clock-outline
                    </v-icon>
                    <div class="text-h6 mt-2">{{ systemUptime }}%</div>
                    <div class="text-caption">Uptime</div>
                  </v-card-text>
                </v-card>
              </v-col>
              <v-col cols="12" md="3">
                <v-card variant="outlined">
                  <v-card-text class="text-center">
                    <v-icon size="48" :color="responseTime < 100 ? 'success' : responseTime < 500 ? 'warning' : 'error'">
                      mdi-speedometer
                    </v-icon>
                    <div class="text-h6 mt-2">{{ responseTime }}ms</div>
                    <div class="text-caption">Avg Response Time</div>
                  </v-card-text>
                </v-card>
              </v-col>
              <v-col cols="12" md="3">
                <v-card variant="outlined">
                  <v-card-text class="text-center">
                    <v-icon size="48" :color="errorRate < 1 ? 'success' : errorRate < 5 ? 'warning' : 'error'">
                      mdi-alert-circle-outline
                    </v-icon>
                    <div class="text-h6 mt-2">{{ errorRate }}%</div>
                    <div class="text-caption">Error Rate</div>
                  </v-card-text>
                </v-card>
              </v-col>
              <v-col cols="12" md="3">
                <v-card variant="outlined">
                  <v-card-text class="text-center">
                    <v-icon size="48" :color="throughput > 1000 ? 'success' : throughput > 500 ? 'warning' : 'error'">
                      mdi-chart-line
                    </v-icon>
                    <div class="text-h6 mt-2">{{ formatNumber(throughput) }}</div>
                    <div class="text-caption">Requests/min</div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Service Status Grid -->
    <v-row class="mb-4">
      <v-col cols="12">
        <v-card>
          <v-card-title>Service Health</v-card-title>
          <v-card-text>
            <v-row>
              <v-col
                v-for="service in services"
                :key="service.name"
                cols="12"
                sm="6"
                md="4"
                lg="3"
              >
                <v-card
                  :color="service.status === 'healthy' ? 'success' : service.status === 'warning' ? 'warning' : 'error'"
                  variant="outlined"
                  class="service-card"
                >
                  <v-card-text>
                    <div class="d-flex align-center mb-2">
                      <v-icon :color="service.status === 'healthy' ? 'success' : service.status === 'warning' ? 'warning' : 'error'" class="mr-2">
                        {{ service.icon }}
                      </v-icon>
                      <span class="font-weight-medium">{{ service.name }}</span>
                    </div>
                    
                    <div class="text-caption mb-2">
                      Status: <span class="font-weight-medium">{{ service.status.toUpperCase() }}</span>
                    </div>
                    
                    <div class="text-caption mb-1">
                      CPU: {{ service.metrics.cpu }}%
                    </div>
                    <v-progress-linear
                      :model-value="service.metrics.cpu"
                      :color="service.metrics.cpu < 70 ? 'success' : service.metrics.cpu < 90 ? 'warning' : 'error'"
                      height="4"
                      class="mb-2"
                    ></v-progress-linear>
                    
                    <div class="text-caption mb-1">
                      Memory: {{ service.metrics.memory }}%
                    </div>
                    <v-progress-linear
                      :model-value="service.metrics.memory"
                      :color="service.metrics.memory < 70 ? 'success' : service.metrics.memory < 90 ? 'warning' : 'error'"
                      height="4"
                      class="mb-2"
                    ></v-progress-linear>
                    
                    <div class="text-caption">
                      Response: {{ service.metrics.responseTime }}ms
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Performance Charts -->
    <v-row class="mb-4">
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>System Performance</v-card-title>
          <v-card-text>
            <div style="height: 300px;">
              <apexchart
                type="line"
                height="300"
                :options="performanceChartOptions"
                :series="performanceChartSeries"
              ></apexchart>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>Error Rate Trends</v-card-title>
          <v-card-text>
            <div style="height: 300px;">
              <apexchart
                type="area"
                height="300"
                :options="errorChartOptions"
                :series="errorChartSeries"
              ></apexchart>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Recent Events and Logs -->
    <v-row>
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>Recent System Events</v-card-title>
          <v-card-text>
            <v-timeline density="compact">
              <v-timeline-item
                v-for="event in recentEvents"
                :key="event.id"
                :dot-color="event.severity === 'error' ? 'error' : event.severity === 'warning' ? 'warning' : 'info'"
                size="small"
              >
                <template v-slot:icon>
                  <v-icon size="16">{{ event.icon }}</v-icon>
                </template>
                
                <div>
                  <div class="text-body-2">{{ event.message }}</div>
                  <div class="text-caption text-medium-emphasis">
                    {{ event.service }} • {{ formatTime(event.timestamp) }}
                  </div>
                </div>
              </v-timeline-item>
            </v-timeline>
          </v-card-text>
        </v-card>
      </v-col>
      
      <v-col cols="12" md="6">
        <v-card>
          <v-card-title>
            System Alerts
            <v-spacer></v-spacer>
            <v-btn
              size="small"
              variant="outlined"
              @click="clearAlerts"
            >
              Clear All
            </v-btn>
          </v-card-title>
          <v-card-text>
            <v-alert
              v-for="alert in systemAlerts"
              :key="alert.id"
              :type="alert.type"
              variant="outlined"
              class="mb-2"
              closable
              @click:close="dismissAlert(alert.id)"
            >
              <v-alert-title>{{ alert.title }}</v-alert-title>
              {{ alert.message }}
              <div class="text-caption mt-1">{{ formatTime(alert.timestamp) }}</div>
            </v-alert>
            
            <div v-if="systemAlerts.length === 0" class="text-center text-medium-emphasis">
              <v-icon size="48" class="mb-2">mdi-check-circle-outline</v-icon>
              <div>No active alerts</div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useWebSocketStore } from '@/stores/websocket'
import { formatTime, formatNumber } from '@/utils/formatters'

const websocketStore = useWebSocketStore()

// Reactive data
const refreshInterval = ref<NodeJS.Timeout | null>(null)

// Mock data (would come from real monitoring service)
const systemUptime = ref(99.8)
const responseTime = ref(45)
const errorRate = ref(0.2)
const throughput = ref(1250)

const services = ref([
  {
    name: 'Data Service',
    status: 'healthy',
    icon: 'mdi-database',
    metrics: { cpu: 35, memory: 45, responseTime: 23 }
  },
  {
    name: 'Analysis Service',
    status: 'healthy',
    icon: 'mdi-chart-line',
    metrics: { cpu: 55, memory: 62, responseTime: 67 }
  },
  {
    name: 'Strategy Service',
    status: 'warning',
    icon: 'mdi-robot',
    metrics: { cpu: 78, memory: 71, responseTime: 145 }
  },
  {
    name: 'Execution Service',
    status: 'healthy',
    icon: 'mdi-swap-horizontal',
    metrics: { cpu: 42, memory: 38, responseTime: 34 }
  },
  {
    name: 'Portfolio Service',
    status: 'healthy',
    icon: 'mdi-briefcase',
    metrics: { cpu: 28, memory: 41, responseTime: 28 }
  },
  {
    name: 'Risk Service',
    status: 'healthy',
    icon: 'mdi-shield-alert',
    metrics: { cpu: 33, memory: 29, responseTime: 52 }
  }
])

const recentEvents = ref([
  {
    id: 1,
    message: 'Service restarted successfully',
    service: 'Strategy Service',
    severity: 'info',
    icon: 'mdi-restart',
    timestamp: new Date().toISOString()
  },
  {
    id: 2,
    message: 'High CPU usage detected',
    service: 'Analysis Service',
    severity: 'warning',
    icon: 'mdi-alert',
    timestamp: new Date(Date.now() - 300000).toISOString()
  },
  {
    id: 3,
    message: 'Database connection restored',
    service: 'Data Service',
    severity: 'info',
    icon: 'mdi-database-check',
    timestamp: new Date(Date.now() - 600000).toISOString()
  }
])

const systemAlerts = ref([
  {
    id: 1,
    type: 'warning',
    title: 'High Memory Usage',
    message: 'Strategy Service memory usage is above 70%',
    timestamp: new Date().toISOString()
  }
])

// Computed properties
const overallStatus = computed(() => {
  const healthyServices = services.value.filter(s => s.status === 'healthy').length
  const totalServices = services.value.length
  
  if (healthyServices === totalServices) return 'All Systems Operational'
  if (healthyServices / totalServices > 0.8) return 'Minor Issues'
  return 'Service Degradation'
})

const overallStatusColor = computed(() => {
  switch (overallStatus.value) {
    case 'All Systems Operational': return 'success'
    case 'Minor Issues': return 'warning'
    default: return 'error'
  }
})

const overallStatusIcon = computed(() => {
  switch (overallStatus.value) {
    case 'All Systems Operational': return 'mdi-check-circle'
    case 'Minor Issues': return 'mdi-alert-circle'
    default: return 'mdi-close-circle'
  }
})

// Chart configurations
const performanceChartOptions = ref({
  chart: {
    type: 'line',
    toolbar: { show: false },
    background: 'transparent'
  },
  theme: { mode: 'dark' },
  stroke: { curve: 'smooth', width: 2 },
  xaxis: { type: 'datetime' },
  yaxis: { 
    title: { text: 'Response Time (ms)' },
    min: 0
  },
  colors: ['#1976D2', '#4CAF50', '#FF9800']
})

const performanceChartSeries = ref([
  {
    name: 'Response Time',
    data: generateTimeSeriesData(50, 100, 24)
  },
  {
    name: 'CPU Usage',
    data: generateTimeSeriesData(30, 80, 24)
  },
  {
    name: 'Memory Usage',
    data: generateTimeSeriesData(40, 70, 24)
  }
])

const errorChartOptions = ref({
  chart: {
    type: 'area',
    toolbar: { show: false },
    background: 'transparent'
  },
  theme: { mode: 'dark' },
  stroke: { curve: 'smooth', width: 2 },
  fill: { type: 'gradient' },
  xaxis: { type: 'datetime' },
  yaxis: { 
    title: { text: 'Error Rate (%)' },
    min: 0,
    max: 5
  },
  colors: ['#FF5252']
})

const errorChartSeries = ref([
  {
    name: 'Error Rate',
    data: generateTimeSeriesData(0, 2, 24)
  }
])

// Methods
function generateTimeSeriesData(min: number, max: number, points: number) {
  const data = []
  const now = new Date()
  
  for (let i = points - 1; i >= 0; i--) {
    const timestamp = new Date(now.getTime() - i * 60 * 60 * 1000) // Hourly data
    const value = Math.random() * (max - min) + min
    data.push({
      x: timestamp.getTime(),
      y: Math.round(value * 100) / 100
    })
  }
  
  return data
}

const dismissAlert = (alertId: number) => {
  const index = systemAlerts.value.findIndex(alert => alert.id === alertId)
  if (index > -1) {
    systemAlerts.value.splice(index, 1)
  }
}

const clearAlerts = () => {
  systemAlerts.value = []
}

const refreshData = () => {
  // Simulate data updates
  responseTime.value = Math.round(Math.random() * 100 + 20)
  errorRate.value = Math.round(Math.random() * 2 * 100) / 100
  throughput.value = Math.round(Math.random() * 500 + 1000)
  
  // Update service metrics
  services.value.forEach(service => {
    service.metrics.cpu = Math.round(Math.random() * 40 + 20)
    service.metrics.memory = Math.round(Math.random() * 50 + 20)
    service.metrics.responseTime = Math.round(Math.random() * 100 + 20)
  })
}

// Lifecycle
onMounted(() => {
  refreshInterval.value = setInterval(refreshData, 5000) // Refresh every 5 seconds
})

onUnmounted(() => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
  }
})
</script>

<style scoped>
.system-health {
  padding: 0;
}

.service-card {
  height: 100%;
  transition: transform 0.2s;
}

.service-card:hover {
  transform: translateY(-2px);
}

.v-progress-linear {
  border-radius: 2px;
}
</style>

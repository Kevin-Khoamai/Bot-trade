<template>
  <v-app>
    <!-- Navigation Drawer -->
    <v-navigation-drawer
      v-model="drawer"
      :rail="rail"
      permanent
      @click="rail = false"
    >
      <v-list-item
        prepend-avatar="/logo.png"
        :title="rail ? '' : 'CryptoTrading Pro'"
        :subtitle="rail ? '' : 'Real-time Dashboard'"
        nav
      >
        <template v-slot:append>
          <v-btn
            variant="text"
            icon="mdi-chevron-left"
            @click.stop="rail = !rail"
          ></v-btn>
        </template>
      </v-list-item>

      <v-divider></v-divider>

      <v-list density="compact" nav>
        <v-list-item
          v-for="item in navigationItems"
          :key="item.title"
          :prepend-icon="item.icon"
          :title="item.title"
          :to="item.route"
          :value="item.title"
        >
          <v-badge
            v-if="item.badge"
            :content="item.badge"
            color="error"
            inline
          ></v-badge>
        </v-list-item>
      </v-list>

      <template v-slot:append>
        <div class="pa-2">
          <v-btn
            block
            variant="outlined"
            prepend-icon="mdi-logout"
            @click="logout"
          >
            Logout
          </v-btn>
        </div>
      </template>
    </v-navigation-drawer>

    <!-- App Bar -->
    <v-app-bar>
      <v-app-bar-nav-icon @click="drawer = !drawer"></v-app-bar-nav-icon>
      
      <v-toolbar-title>{{ currentPageTitle }}</v-toolbar-title>

      <v-spacer></v-spacer>

      <!-- Connection Status -->
      <v-chip
        :color="connectionStatus.color"
        :prepend-icon="connectionStatus.icon"
        variant="outlined"
        class="mr-2"
      >
        {{ connectionStatus.text }}
      </v-chip>

      <!-- Theme Toggle -->
      <v-btn
        icon
        @click="toggleTheme"
        class="mr-2"
      >
        <v-icon>{{ theme.global.name.value === 'dark' ? 'mdi-weather-sunny' : 'mdi-weather-night' }}</v-icon>
      </v-btn>

      <!-- Notifications -->
      <v-menu>
        <template v-slot:activator="{ props }">
          <v-btn
            icon
            v-bind="props"
            class="mr-2"
          >
            <v-badge
              :content="unreadNotifications"
              :model-value="unreadNotifications > 0"
              color="error"
            >
              <v-icon>mdi-bell</v-icon>
            </v-badge>
          </v-btn>
        </template>
        
        <v-card min-width="300">
          <v-card-title>Notifications</v-card-title>
          <v-divider></v-divider>
          <v-list>
            <v-list-item
              v-for="notification in recentNotifications"
              :key="notification.id"
              :subtitle="notification.message"
              :title="notification.title"
            >
              <template v-slot:prepend>
                <v-icon :color="notification.severity">{{ notification.icon }}</v-icon>
              </template>
            </v-list-item>
          </v-list>
        </v-card>
      </v-menu>

      <!-- User Menu -->
      <v-menu>
        <template v-slot:activator="{ props }">
          <v-btn
            icon
            v-bind="props"
          >
            <v-avatar size="32">
              <v-img src="/user-avatar.png" alt="User"></v-img>
            </v-avatar>
          </v-btn>
        </template>
        
        <v-card min-width="200">
          <v-list>
            <v-list-item
              prepend-icon="mdi-account"
              title="Profile"
              @click="goToProfile"
            ></v-list-item>
            <v-list-item
              prepend-icon="mdi-cog"
              title="Settings"
              @click="goToSettings"
            ></v-list-item>
            <v-divider></v-divider>
            <v-list-item
              prepend-icon="mdi-logout"
              title="Logout"
              @click="logout"
            ></v-list-item>
          </v-list>
        </v-card>
      </v-menu>
    </v-app-bar>

    <!-- Main Content -->
    <v-main>
      <v-container fluid>
        <router-view />
      </v-container>
    </v-main>

    <!-- Footer -->
    <v-footer app>
      <v-row justify="center" no-gutters>
        <v-col cols="12" class="text-center">
          <span>&copy; {{ new Date().getFullYear() }} CryptoTrading Pro. All rights reserved.</span>
        </v-col>
      </v-row>
    </v-footer>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTheme } from 'vuetify'
import { useWebSocketStore } from '@/stores/websocket'
import { useNotificationStore } from '@/stores/notifications'

const router = useRouter()
const route = useRoute()
const theme = useTheme()
const websocketStore = useWebSocketStore()
const notificationStore = useNotificationStore()

// Navigation state
const drawer = ref(true)
const rail = ref(false)

// Navigation items
const navigationItems = ref([
  { title: 'Dashboard', icon: 'mdi-view-dashboard', route: '/' },
  { title: 'Market Data', icon: 'mdi-chart-line', route: '/market' },
  { title: 'Trading', icon: 'mdi-swap-horizontal', route: '/trading' },
  { title: 'Portfolio', icon: 'mdi-briefcase', route: '/portfolio' },
  { title: 'Risk Management', icon: 'mdi-shield-alert', route: '/risk' },
  { title: 'Orders', icon: 'mdi-format-list-bulleted', route: '/orders' },
  { title: 'Analytics', icon: 'mdi-chart-box', route: '/analytics' },
  { title: 'System Health', icon: 'mdi-heart-pulse', route: '/health' },
  { title: 'Settings', icon: 'mdi-cog', route: '/settings' }
])

// Computed properties
const currentPageTitle = computed(() => {
  const currentItem = navigationItems.value.find(item => item.route === route.path)
  return currentItem?.title || 'Dashboard'
})

const connectionStatus = computed(() => {
  if (websocketStore.isConnected) {
    return {
      color: 'success',
      icon: 'mdi-wifi',
      text: 'Connected'
    }
  } else {
    return {
      color: 'error',
      icon: 'mdi-wifi-off',
      text: 'Disconnected'
    }
  }
})

const unreadNotifications = computed(() => notificationStore.unreadCount)
const recentNotifications = computed(() => notificationStore.recent)

// Methods
const toggleTheme = () => {
  theme.global.name.value = theme.global.name.value === 'dark' ? 'light' : 'dark'
}

const logout = () => {
  // Implement logout logic
  router.push('/login')
}

const goToProfile = () => {
  router.push('/profile')
}

const goToSettings = () => {
  router.push('/settings')
}

// Lifecycle
onMounted(() => {
  // Initialize WebSocket connection
  websocketStore.connect()
  
  // Load initial notifications
  notificationStore.loadNotifications()
})
</script>

<style scoped>
.v-navigation-drawer {
  border-right: 1px solid rgba(255, 255, 255, 0.12);
}

.v-app-bar {
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}
</style>

<template>
  <v-container fluid class="pa-4">
    <v-row>
      <v-col>
        <h1 class="text-h4 font-weight-bold mb-4">Settings</h1>
        
        <v-card elevation="2">
          <v-card-title>Application Settings</v-card-title>
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6">
                <v-text-field
                  label="API Gateway URL"
                  :model-value="apiUrl"
                  readonly
                  variant="outlined"
                  prepend-inner-icon="mdi-server"
                />
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field
                  label="WebSocket URL"
                  :model-value="wsUrl"
                  readonly
                  variant="outlined"
                  prepend-inner-icon="mdi-wifi"
                />
              </v-col>
            </v-row>
            
            <v-divider class="my-4" />
            
            <h3 class="text-h6 mb-3">Connection Status</h3>
            <v-row>
              <v-col cols="12" md="6">
                <v-card variant="outlined">
                  <v-card-text class="text-center">
                    <v-icon size="32" color="success" class="mb-2">mdi-check-circle</v-icon>
                    <div class="text-subtitle-1 font-weight-medium">API Gateway</div>
                    <div class="text-caption">Connected</div>
                  </v-card-text>
                </v-card>
              </v-col>
              <v-col cols="12" md="6">
                <v-card variant="outlined">
                  <v-card-text class="text-center">
                    <v-icon 
                      size="32" 
                      :color="marketStore.isConnected ? 'success' : 'error'" 
                      class="mb-2"
                    >
                      {{ marketStore.isConnected ? 'mdi-check-circle' : 'mdi-close-circle' }}
                    </v-icon>
                    <div class="text-subtitle-1 font-weight-medium">WebSocket</div>
                    <div class="text-caption">
                      {{ marketStore.isConnected ? 'Connected' : 'Disconnected' }}
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { useMarketStore } from '../stores/market'

const marketStore = useMarketStore()

const apiUrl = 'http://localhost:8082'
const wsUrl = 'ws://localhost:8082/ws'
</script>

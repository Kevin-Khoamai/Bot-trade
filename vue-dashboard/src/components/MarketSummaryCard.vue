<template>
  <v-card class="h-100" elevation="2">
    <v-card-text class="d-flex align-center">
      <div class="flex-grow-1">
        <div class="text-caption text-medium-emphasis mb-1">
          {{ title }}
        </div>
        <div class="text-h5 font-weight-bold">
          {{ formattedValue }}
        </div>
      </div>
      <v-avatar :color="color" size="48" class="ml-3">
        <v-icon :icon="icon" size="24" color="white" />
      </v-avatar>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title: string
  value?: number
  format: 'currency' | 'percentage' | 'number'
  icon: string
  color: string
}

const props = defineProps<Props>()

const formattedValue = computed(() => {
  if (props.value === undefined || props.value === null) {
    return '--'
  }

  switch (props.format) {
    case 'currency':
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        notation: 'compact',
        maximumFractionDigits: 2
      }).format(props.value)
    
    case 'percentage':
      return new Intl.NumberFormat('en-US', {
        style: 'percent',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(props.value / 100)
    
    case 'number':
      return new Intl.NumberFormat('en-US', {
        notation: 'compact',
        maximumFractionDigits: 0
      }).format(props.value)
    
    default:
      return props.value.toString()
  }
})
</script>

<template>
  <RouterLink class="app-logo" :class="{ 'app-logo--compact': compact }" :to="to" :aria-label="brand.name">
    <span class="app-logo__icon" aria-hidden="true">
      <img v-if="!imageFailed" :src="brand.iconPath" :alt="brand.name" @error="imageFailed = true" />
      <span v-else>{{ brand.fallbackText }}</span>
    </span>
    <span v-if="!compact" class="app-logo__copy">
      <strong>{{ brand.name }}</strong>
      <small>{{ subtitle || brand.subtitle }}</small>
    </span>
  </RouterLink>
</template>

<script setup>
import { ref } from 'vue'

import { brandConfig as brand } from '@/config/brand'

defineProps({
  to: {
    type: [String, Object],
    default: '/',
  },
  compact: {
    type: Boolean,
    default: false,
  },
  subtitle: {
    type: String,
    default: '',
  },
})

const imageFailed = ref(false)
</script>

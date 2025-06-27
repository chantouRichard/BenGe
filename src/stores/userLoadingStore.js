import { defineStore } from 'pinia'
import { ref } from 'vue'

export const userLoadingStore = defineStore('loading',() => {
    const loading = ref(false)
    const aiAssistantLoading = ref(false)
    const show = () => {
        loading.value = true
    }

    const hide = () => {
        loading.value = false
    }

    const showAiAssistantLoading = () => {
        aiAssistantLoading.value = true
    }

    const hideAiAssistantLoading = () => {
        aiAssistantLoading.value = false
    }

    return {
        loading,
        aiAssistantLoading,
        show,
        hide,
        showAiAssistantLoading,
        hideAiAssistantLoading
    }
})
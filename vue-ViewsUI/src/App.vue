<template>
  <div id="app">
    <DocumentManager v-if="currentView === 'manager'" />
    <DocumentEditor
      v-else-if="currentView === 'editor'"
      :document-id="documentId"
      :mode="mode"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import DocumentManager from './components/DocumentManager.vue'
import DocumentEditor from './components/DocumentEditor.vue'

const currentView = ref('manager')
const documentId = ref('')
const mode = ref('edit')

// 解析 URL hash 来确定显示哪个视图
const parseHash = () => {
  const hash = window.location.hash.slice(1) // 去掉 #
  
  if (hash.startsWith('/editor/')) {
    // 提取文档ID和模式
    const parts = hash.replace('/editor/', '').split('?')
    documentId.value = parts[0]
    
    // 解析查询参数
    if (parts[1]) {
      const params = new URLSearchParams(parts[1])
      mode.value = params.get('mode') || 'edit'
    }
    
    currentView.value = 'editor'
  } else {
    currentView.value = 'manager'
  }
}

// 监听 hash 变化
const handleHashChange = () => {
  parseHash()
}

onMounted(() => {
  parseHash()
  window.addEventListener('hashchange', handleHashChange)
})

onUnmounted(() => {
  window.removeEventListener('hashchange', handleHashChange)
})
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}
</style>

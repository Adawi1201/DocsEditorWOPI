<template>
  <div class="editor-container">
    <div class="editor-header">
      <button @click="goBack" class="back-btn">← 返回文档列表</button>
      <h2>正在编辑: {{ fileName }}</h2>
      <div class="status" :class="{ saving: isSaving }">
        {{ isSaving ? '保存中...' : '已保存' }}
      </div>
    </div>
    
    <!-- Collabora编辑器iframe -->
    <div class="iframe-wrapper">
      <iframe
        v-if="editorUrl"
        ref="editorFrame"
        :src="editorUrl"
        class="collabora-frame"
        allowfullscreen
        @load="onFrameLoad"
      ></iframe>
      <div v-else class="loading-editor">
        正在加载编辑器...
      </div>
    </div>

    <!-- 事件日志（调试用） -->
    <div v-if="showDebug" class="debug-panel">
      <h4>调试信息</h4>
      <div class="debug-content">
        <p v-for="(log, index) in debugLogs" :key="index">{{ log }}</p>
      </div>
    </div>
    
    <button @click="showDebug = !showDebug" class="debug-toggle">
      {{ showDebug ? '隐藏调试' : '显示调试' }}
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const editorFrame = ref(null)
const editorUrl = ref('')
const fileName = ref('')
const isSaving = ref(false)
const showDebug = ref(false)
const debugLogs = ref([])

// 返回文档列表
const goBack = () => {
  router.push('/')
}

// 添加调试日志
const addLog = (message) => {
  const time = new Date().toLocaleTimeString()
  debugLogs.value.push(`[${time}] ${message}`)
  // 保持日志数量不超过50条
  if (debugLogs.value.length > 50) {
    debugLogs.value.shift()
  }
}

// 处理iframe加载完成
const onFrameLoad = () => {
  addLog('编辑器iframe加载完成')
}

// 处理来自Collabora的消息
const handleMessage = (event) => {
  addLog(`收到消息: ${JSON.stringify(event.data)}`)
  
  // 检查消息类型
  if (event.data) {
    switch (event.data.type) {
      case 'doc':
        addLog('文档已加载')
        break
      case 'saved':
        addLog('文档已保存')
        isSaving.value = false
        break
      case 'save_failed':
        addLog(`保存失败: ${event.data.message}`)
        isSaving.value = false
        break
      case 'close':
        addLog('编辑器关闭')
        // 可以选择返回列表或保持当前页面
        break
      case 'error':
        addLog(`错误: ${event.data.message}`)
        break
      default:
        // 其他消息类型
        if (typeof event.data === 'string' && event.data.startsWith(' LOOL')) {
          addLog(`LOOL消息: ${event.data}`)
        }
    }
  }
}

// 监听postMessage事件
onMounted(() => {
  // 获取从路由传递的URL和文件名
  const url = route.query.url
  const fileNameParam = route.query.fileName
  const docId = route.params.id
  
  if (url) {
    editorUrl.value = url
    
    // 优先使用传递的文件名参数
    if (fileNameParam) {
      fileName.value = fileNameParam
    } else {
      // 从URL中提取文件名作为回退
      try {
        const urlObj = new URL(url)
        const pathParts = urlObj.pathname.split('/')
        fileName.value = decodeURIComponent(pathParts[pathParts.length - 1] || '文档')
      } catch {
        fileName.value = '文档'
      }
    }
    addLog(`编辑器URL: ${editorUrl.value}`)
    addLog(`文件名: ${fileName.value}`)
  } else {
    // 如果URL不存在，可能是因为刷新了页面，需要重新获取
    addLog('未获取到编辑器URL，返回列表')
    router.push('/')
  }
  
  // 监听来自iframe的消息
  window.addEventListener('message', handleMessage)
  addLog('已开始监听postMessage事件')
})

// 清理事件监听
onUnmounted(() => {
  window.removeEventListener('message', handleMessage)
  addLog('已移除postMessage监听')
})
</script>

<style scoped>
.editor-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.editor-header {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
}

.back-btn {
  padding: 8px 15px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 20px;
}

.back-btn:hover {
  background: #66b1ff;
}

.editor-header h2 {
  flex: 1;
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.status {
  padding: 5px 15px;
  border-radius: 4px;
  font-size: 14px;
  background: #67c23a;
  color: white;
}

.status.saving {
  background: #e6a23c;
}

.iframe-wrapper {
  flex: 1;
  position: relative;
  background: #e4e7ed;
}

.collabora-frame {
  width: 100%;
  height: 100%;
  border: none;
}

.loading-editor {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 18px;
  color: #909399;
}

.debug-toggle {
  position: fixed;
  bottom: 20px;
  right: 20px;
  padding: 8px 15px;
  background: #909399;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  z-index: 100;
}

.debug-panel {
  position: fixed;
  bottom: 60px;
  right: 20px;
  width: 300px;
  max-height: 200px;
  background: rgba(0, 0, 0, 0.8);
  color: #0f0;
  padding: 10px;
  border-radius: 5px;
  overflow-y: auto;
  z-index: 100;
}

.debug-panel h4 {
  margin: 0 0 10px 0;
  color: #fff;
  font-size: 14px;
}

.debug-content {
  font-size: 12px;
  font-family: monospace;
}

.debug-content p {
  margin: 3px 0;
}
</style>
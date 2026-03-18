<template>
  <div class="document-editor">
    <div v-if="loading" class="loading">
      加载中...
    </div>
    
    <div v-else-if="error" class="error">
      {{ error }}
    </div>
    
    <div v-else class="editor-container">
      <iframe
        ref="iframe"
        :src="editorUrl"
        class="collabora-iframe"
        allow="fullscreen"
      ></iframe>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import API_CONFIG from '../config.js'

export default {
  name: 'DocumentEditor',
  props: {
    documentId: {
      type: String,
      required: true
    },
    mode: {
      type: String,
      default: 'edit', // 'view' or 'edit'
      validator: (value) => ['view', 'edit'].includes(value)
    }
  },
  setup(props) {
    const loading = ref(true)
    const error = ref(null)
    const iframe = ref(null)
    const editorUrl = ref('')
    const accessToken = ref('')
    
    // Collabora Online 服务器地址
    const COLLABORA_URL = API_CONFIG.COLLABORA_URL
    
    // 计算编辑器 URL - 改为异步初始化
    const initEditorUrl = async () => {
      const docId = props.documentId
      console.log('Document ID:', docId)
      
      // 生成 access token
      const token = await generateToken(docId)
      accessToken.value = token
      console.log('Access token:', token)
      
      // 构建 WOPI URL - 使用 host.docker.internal 让 Collabora 容器能够访问
      const wopiHost = 'http://host.docker.internal:8000'
      const wopiSrc = encodeURIComponent(
        `${wopiHost}/wopi/files/${docId}?access_token=${token}`
      )
      
      console.log('WOPI Src:', wopiSrc)
      
      // 使用已知的 Collabora URL 模板
      const urlTemplate = `${COLLABORA_URL}/browser/4fd2181/cool.html?`
      console.log('Using Collabora URL template:', urlTemplate)
      
      // 构建完整的编辑器 URL
      let url = `${urlTemplate}WOPISrc=${wopiSrc}&lang=zh-CN`
      
      // 确保编辑模式
      if (props.mode === 'edit') {
        url += '&permission=edit'
      }
      
      console.log('Generated editor URL:', url)
      editorUrl.value = url
      
      // 验证 Collabora 服务器是否可访问
      try {
        const response = await fetch(`${COLLABORA_URL}/browser/4fd2181/cool.html`, {
          method: 'HEAD',
          mode: 'no-cors'  // 不要求 CORS
        })
        console.log('Collabora server reachable:', response.ok || 'no-cors mode')
      } catch (e) {
        console.error('Collabora server not reachable:', e)
        error.value = `无法连接到 Collabora 服务器 (${COLLABORA_URL})。请确保 Docker 容器正在运行。`
        loading.value = false
        return
      }
    }
    
    // 计算 SHA256 哈希的辅助函数
    async function sha256(message) {
      const msgBuffer = new TextEncoder().encode(message)
      const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer)
      const hashArray = Array.from(new Uint8Array(hashBuffer))
      const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
      return hashHex
    }
    
    // 生成访问令牌（与后端保持一致，使用 SHA256）
    const generateToken = async (docId) => {
      const str = `${docId}:admin:secret`
      const token = await sha256(str)
      console.log('Generated access token:', token)  // 调试日志
      return token
    }
    
    // 验证文档是否存在
    const validateDocument = async (docId) => {
      try {
        console.log('Validating document with ID:', docId);  // 调试日志
        
        // 生成 token 用于验证
        const token = await generateToken(docId)
        
        // 尝试获取文档信息以确保文档存在
        const docInfoResponse = await fetch(`${API_CONFIG.BACKEND_BASE_URL}/wopi/files/${encodeURIComponent(docId)}?access_token=${token}`, {
          method: 'GET',
          headers: {
            'Accept': 'application/json',
          }
        });
        
        const isValid = docInfoResponse.ok;
        console.log('Document validation result:', isValid);  // 调试日志
        
        if (!isValid) {
          console.error('文档验证失败，状态码:', docInfoResponse.status);
          
          // 尝试不编码直接访问（兼容性处理）
          const docInfoResponse2 = await fetch(`${API_CONFIG.BACKEND_BASE_URL}/wopi/files/${docId}?access_token=${token}`, {
            method: 'GET',
            headers: {
              'Accept': 'application/json',
            }
          });
          
          const isValid2 = docInfoResponse2.ok;
          console.log('备用验证结果:', isValid2);  // 调试日志
          
          return isValid2;
        }
        
        return isValid;
      } catch (error) {
        console.error('Error validating document:', error);
        return false;
      }
    }
    
    // 处理来自 iframe 的消息
    const handleMessage = (event) => {
      console.log('Received message from iframe:', event)  // 调试日志
      console.log('Event origin:', event.origin)  // 调试日志
      console.log('Expected origin:', COLLABORA_URL)  // 调试日志
      
      // 验证消息来源
      if (!event.origin.startsWith(COLLABORA_URL)) {
        console.log('Message origin does not match expected Collabora URL')  // 调试日志
        return
      }
      
      try {
        const message = JSON.parse(event.data)
        console.log('Parsed message:', message)  // 调试日志
        
        switch (message.messageId) {
          case 'FileSave':
            console.log('文件已保存')
            break
          case 'editorReady':
            console.log('Editor is ready')  // 调试日志
            loading.value = false
            break
          case 'close':
            // 处理关闭编辑器
            console.log('Editor closed')  // 调试日志
            break
          case 'appLoaded':
            console.log('App loaded')  // 调试日志
            break
          default:
            console.log('Unknown message ID:', message.messageId)  // 调试日志
        }
      } catch (e) {
        console.error('Failed to parse message:', e)  // 调试日志
        // 忽略无法解析的消息
      }
    }
    
    onMounted(async () => {
      console.log('DocumentEditor component mounted')
      console.log('Document ID:', props.documentId)
      console.log('Mode:', props.mode)
      
      // 生成 token 并构建编辑器 URL
      await initEditorUrl()
      console.log('Editor URL initialized:', editorUrl.value)
      
      // 验证文档是否可访问（使用 token）
      try {
        const token = await generateToken(props.documentId)
        const response = await fetch(`${API_CONFIG.BACKEND_BASE_URL}/wopi/files/${props.documentId}?access_token=${token}`, {
          method: 'GET',
          headers: { 'Accept': 'application/json' }
        })
        console.log('WOPI endpoint status:', response.status)
        
        if (!response.ok) {
          const errorText = await response.text()
          console.error('WOPI error:', errorText)
          error.value = `文档加载失败 (${response.status}): ${errorText}`
          return
        }
        
        const data = await response.json()
        console.log('CheckFileInfo response:', data)
        
      } catch (err) {
        console.error('Error connecting to WOPI:', err)
        error.value = '无法连接到文档服务'
        return
      }
      
      // 标记加载完成，显示 iframe
      loading.value = false
      console.log('Loading complete, showing iframe')
      
      // 监听来自 Collabora 的消息
      window.addEventListener('message', handleMessage)
      
      // 不设置超时错误，因为 Collabora 加载需要时间
      // iframe 会显示加载状态，直到 Collabora 准备就绪
      
      // 额外的超时用于检查 iframe 是否已加载
      setTimeout(() => {
        const iframeEl = iframe.value
        if (iframeEl) {
          console.log('iframe src:', iframeEl.src)  // 调试日志
          console.log('iframe readyState:', iframeEl.contentDocument ? 'loaded' : 'not loaded')  // 调试日志
        }
      }, 5000)
    })
    
    onUnmounted(() => {
      window.removeEventListener('message', handleMessage)
    })
    
    return {
      loading,
      error,
      iframe,
      editorUrl
    }
  }
}
</script>

<style scoped>
.document-editor {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.loading,
.error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 16px;
  color: #666;
}

.loading::after {
  content: '';
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error {
  color: #e74c3c;
}

.editor-container {
  flex: 1;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.collabora-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>

<template>
  <div class="document-manager">
    <h2>文档管理器</h2>
    
    <div class="create-section">
      <h3>创建新文档</h3>
      <input 
        v-model="newDocName" 
        placeholder="输入文档名称" 
        @keyup.enter="createDocument"
      />
      <select v-model="newDocType">
        <option value="word">Word 文档</option>
        <option value="excel">Excel 表格</option>
      </select>
      <button @click="createDocument">创建文档</button>
    </div>
    
    <div class="documents-list">
      <h3>现有文档</h3>
      <div v-if="documents.length === 0" class="empty-state">
        暂无文档，点击上方创建新文档
      </div>
      <div v-else class="document-grid">
        <div 
          v-for="doc in documents" 
          :key="doc.id" 
          class="document-card"
        >
          <h4>{{ doc.filename }}</h4>
          <p>ID: {{ doc.id }}</p>
          <p>类型: {{ doc.document_type }}</p>
          <div class="actions">
            <button @click="openEditor(doc.id, 'edit')">编辑</button>
            <button @click="openEditor(doc.id, 'view')">预览</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import API_CONFIG from '../config.js'

// 获取CSRF令牌
function getCsrfToken() {
  const name = 'csrftoken'
  let cookieValue = null
  if (document.cookie && document.cookie !== '') {
    const cookies = document.cookie.split(';')
    for (let i = 0; i < cookies.length; i++) {
      const cookie = cookies[i].trim()
      if (cookie.substring(0, name.length + 1) === (name + '=')) {
        cookieValue = decodeURIComponent(cookie.substring(name.length + 1))
        break
      }
    }
  }
  return cookieValue
}

export default {
  name: 'DocumentManager',
  setup() {
    const documents = ref([])
    const newDocName = ref('')
    const newDocType = ref('word')
    
    // 获取CSRF令牌（先访问一次后端以获取cookie）
    const initCsrfToken = async () => {
      try {
        await fetch(`${API_CONFIG.BACKEND_BASE_URL}/wopi/documents/`, {
          method: 'GET',
          credentials: 'include'  // 包含cookie
        })
      } catch (error) {
        console.error('初始化CSRF令牌失败:', error)
      }
    }
    
    // 获取文档列表
    const fetchDocuments = async () => {
      try {
        const response = await fetch(`${API_CONFIG.BACKEND_BASE_URL}/wopi/documents/`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
          credentials: 'include'  // 包含cookie
        })
        
        if (response.ok) {
          const result = await response.json();
          documents.value = result.documents || [];
        } else {
          console.error('获取文档列表失败:', response.status)
          documents.value = []
        }
      } catch (error) {
        console.error('获取文档列表时出错:', error)
        documents.value = []
      }
    }
    
    // 创建文档
    const createDocument = async () => {
      if (!newDocName.value.trim()) {
        alert('请输入文档名称')
        return
      }
      
      const csrfToken = getCsrfToken()
      
      try {
        const response = await fetch(`${API_CONFIG.BACKEND_BASE_URL}/wopi/documents/create/`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-CSRFToken': csrfToken
          },
          credentials: 'include',  // 包含cookie
          body: JSON.stringify({
            filename: newDocName.value.trim(),
            document_type: newDocType.value
          })
        })
        
        const result = await response.json()
        
        if (result.success) {
          // 创建成功后跳转到编辑器页面
          const docId = result.document.id
          window.location.hash = `#/editor/${docId}?mode=edit`
          
          // 刷新文档列表
          fetchDocuments()
        } else {
          alert(`创建文档失败: ${result.error || '未知错误'}`)
        }
      } catch (error) {
        console.error('创建文档时出错:', error)
        alert('创建文档失败，请检查网络连接')
      }
      
      // 清空输入框
      newDocName.value = ''
    }
    
    // 打开编辑器
    const openEditor = (docId, mode) => {
      window.location.hash = `#/editor/${docId}?mode=${mode}`
    }
    
    onMounted(async () => {
      await initCsrfToken()
      fetchDocuments()
    })
    
    return {
      documents,
      newDocName,
      newDocType,
      createDocument,
      openEditor
    }
  }
}
</script>

<style scoped>
.document-manager {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.create-section {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.create-section input, .create-section select {
  padding: 8px;
  margin-right: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.create-section button {
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.create-section button:hover {
  background: #0056b3;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
}

.document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.document-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  background: white;
}

.document-card h4 {
  margin-top: 0;
  color: #333;
}

.actions {
  margin-top: 15px;
}

.actions button {
  padding: 6px 12px;
  margin-right: 10px;
  border: 1px solid #007bff;
  background: white;
  color: #007bff;
  border-radius: 4px;
  cursor: pointer;
}

.actions button:hover {
  background: #007bff;
  color: white;
}
</style>
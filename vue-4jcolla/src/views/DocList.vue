<template>
  <div class="doc-list">
    <div class="header">
      <h1>📄 在线文档编辑系统</h1>
    </div>

    <!-- 上传区域 -->
    <div class="upload-section">
      <div class="upload-box">
        <input 
          type="file" 
          id="fileInput" 
          @change="handleFileSelect" 
          accept=".docx,.xlsx,.pptx,.odt,.ods,.odp,.txt"
          hidden
        />
        <label for="fileInput" class="upload-btn">
          📁 选择文件上传
        </label>
        <span class="upload-hint">
          支持 .docx, .xlsx, .pptx, .odt, .ods, .odp, .txt 等格式
        </span>
      </div>
      <div v-if="selectedFile" class="selected-file">
        已选择: {{ selectedFile.name }}
        <button @click="uploadFile" class="confirm-btn">确认上传</button>
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="docs-section">
      <h2>我的文档</h2>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="docs.length === 0" class="empty">
        暂无文档，请先上传文件
      </div>
      <div v-else class="doc-grid">
        <div
          v-for="doc in docs"
          :key="doc.id"
          class="doc-card"
        >
          <div class="doc-icon">{{ getDocIcon(doc.documentType) }}</div>
          <div class="doc-info">
            <h3>{{ doc.filename }}</h3>
            <p>类型: {{ doc.documentType || '未知' }}</p>
            <p>大小: {{ formatSize(doc.fileSize) }}</p>
            <p>上传时间: {{ formatTime(doc.createdAt) }}</p>
          </div>
          <div class="doc-actions">
            <button @click="editDoc(doc.id)" class="edit-btn">
              ✏️ 编辑
            </button>
            <button @click="removeDoc(doc.id)" class="delete-btn">
              🗑️ 删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 消息提示 -->
    <div v-if="message" :class="['message', message.type]">
      {{ message.text }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDocList, uploadDoc, deleteDoc, getEditUrl } from '@/api'

const router = useRouter()

const docs = ref([])
const loading = ref(false)
const selectedFile = ref(null)
const message = ref(null)

// 加载文档列表
const loadDocs = async () => {
  loading.value = true
  try {
    docs.value = await getDocList()
  } catch (error) {
    showMessage('加载文档列表失败', 'error')
  } finally {
    loading.value = false
  }
}

// 选择文件
const handleFileSelect = (event) => {
  const file = event.target.files[0]
  if (file) {
    selectedFile.value = file
  }
}

// 上传文件
const uploadFile = async () => {
  if (!selectedFile.value) return
  
  try {
    const result = await uploadDoc(selectedFile.value)
    if (result.success) {
      showMessage('文件上传成功', 'success')
      selectedFile.value = null
      await loadDocs()
    } else {
      showMessage(result.message || '上传失败', 'error')
    }
  } catch (error) {
    showMessage('上传失败: ' + error.message, 'error')
  }
}

// 编辑文档
const editDoc = async (id) => {
  try {
    const result = await getEditUrl(id)
    if (result.success) {
      router.push({
        name: 'Editor',
        params: { id },
        query: {
          url: result.editUrl,
          fileName: result.fileName
        }
      })
    } else {
      showMessage('获取编辑链接失败', 'error')
    }
  } catch (error) {
    showMessage('打开编辑器失败', 'error')
  }
}

// 删除文档
const removeDoc = async (id) => {
  if (!confirm('确定要删除这个文档吗？')) return
  
  try {
    const result = await deleteDoc(id)
    if (result.success) {
      showMessage('文档已删除', 'success')
      await loadDocs()
    } else {
      showMessage(result.message || '删除失败', 'error')
    }
  } catch (error) {
    showMessage('删除失败', 'error')
  }
}

// 格式化文件大小
const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(2)} ${units[unitIndex]}`
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 根据文档类型获取图标
const getDocIcon = (documentType) => {
  if (!documentType) return '📄'
  const type = documentType.toLowerCase()
  if (type.includes('word') || type.includes('document')) return '📝'
  if (type.includes('spreadsheet') || type.includes('excel')) return '📊'
  if (type.includes('presentation') || type.includes('powerpoint')) return '📽️'
  if (type.includes('text') || type.includes('plain')) return '📃'
  return '📄'
}

// 显示消息
const showMessage = (text, type = 'info') => {
  message.value = { text, type }
  setTimeout(() => {
    message.value = null
  }, 3000)
}

onMounted(() => {
  loadDocs()
})
</script>

<style scoped>
.doc-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header h1 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

.upload-section {
  background: #f5f5f5;
  padding: 30px;
  border-radius: 10px;
  margin-bottom: 30px;
  text-align: center;
}

.upload-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.upload-btn {
  display: inline-block;
  padding: 12px 30px;
  background: #409eff;
  color: white;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.upload-btn:hover {
  background: #66b1ff;
}

.upload-hint {
  color: #909399;
  font-size: 14px;
}

.selected-file {
  margin-top: 15px;
  padding: 10px;
  background: white;
  border-radius: 5px;
  display: inline-block;
}

.confirm-btn {
  margin-left: 10px;
  padding: 5px 15px;
  background: #67c23a;
  color: white;
  border: none;
  border-radius: 3px;
  cursor: pointer;
}

.docs-section h2 {
  margin-bottom: 20px;
  color: #333;
}

.loading, .empty {
  text-align: center;
  padding: 50px;
  color: #909399;
}

.doc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.doc-card {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  transition: box-shadow 0.3s;
}

.doc-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.doc-icon {
  font-size: 40px;
  text-align: center;
  margin-bottom: 10px;
}

.doc-info h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #303133;
}

.doc-info p {
  margin: 5px 0;
  color: #606266;
  font-size: 14px;
}

.doc-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.edit-btn, .delete-btn {
  flex: 1;
  padding: 8px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.edit-btn {
  background: #409eff;
  color: white;
}

.edit-btn:hover {
  background: #66b1ff;
}

.delete-btn {
  background: #f56c6c;
  color: white;
}

.delete-btn:hover {
  background: #f78989;
}

.message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 15px 20px;
  border-radius: 5px;
  color: white;
  z-index: 1000;
  animation: slideIn 0.3s;
}

.message.success {
  background: #67c23a;
}

.message.error {
  background: #f56c6c;
}

.message.info {
  background: #409eff;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
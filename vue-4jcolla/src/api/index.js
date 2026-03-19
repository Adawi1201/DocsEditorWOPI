import axios from 'axios'

// 创建axios实例
// 使用代理路径，避免跨域问题
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器
api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

/**
 * 获取文档列表
 */
export function getDocList() {
  return api.get('/docs')
}

/**
 * 上传文档
 * @param {File} file - 要上传的文件
 */
export function uploadDoc(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/docs/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取文档编辑URL
 * @param {Number} id - 文档ID
 */
export function getEditUrl(id) {
  return api.get(`/docs/${id}/edit`)
}

/**
 * 删除文档
 * @param {Number} id - 文档ID
 */
export function deleteDoc(id) {
  return api.delete(`/docs/${id}`)
}

/**
 * 获取单个文档信息
 * @param {Number} id - 文档ID
 */
export function getDoc(id) {
  return api.get(`/docs/${id}`)
}

export default api
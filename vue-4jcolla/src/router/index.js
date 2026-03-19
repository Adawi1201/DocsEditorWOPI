import { createRouter, createWebHistory } from 'vue-router'
import DocList from '../views/DocList.vue'
import Editor from '../views/Editor.vue'

const routes = [
  {
    path: '/',
    name: 'DocList',
    component: DocList
  },
  {
    path: '/docs/:id/edit',
    name: 'Editor',
    component: Editor,
    props: true
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
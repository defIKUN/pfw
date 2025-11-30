import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  const isAuthenticated = ref(!!token.value)

  const login = async (usernameInput, passwordInput) => {
    try {
      const response = await api.post('/login', { username: usernameInput, password: passwordInput })
      const body = response.data || {}
      if (body.code !== 200 || !body.data) {
        return { success: false, message: body.message || '用户名或密码错误' }
      }
      const { token: newToken, username: newUsername, role: newRole } = body.data

      if (!newToken) {
        return { success: false, message: '登录失败：未获取到令牌' }
      }
      
      token.value = newToken
      username.value = newUsername || usernameInput
      role.value = newRole || ''
      isAuthenticated.value = true

      localStorage.setItem('token', newToken)
      localStorage.setItem('username', username.value)
      localStorage.setItem('role', role.value)

      return { success: true }
    } catch (error) {
      return { success: false, message: error.response?.data?.message || '登录失败' }
    }
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    role.value = ''
    isAuthenticated.value = false

    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  return {
    token,
    username,
    role,
    isAuthenticated,
    login,
    logout
  }
})


import { defineStore } from 'pinia'
import { ref } from 'vue'

/* 用户状态管理 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const realName = ref(localStorage.getItem('realName') || '')
  const role = ref(localStorage.getItem('role') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')

  /* 设置用户信息 */
  function setUser(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    realName.value = data.realName
    role.value = data.role
    avatar.value = data.avatar || ''
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('realName', data.realName)
    localStorage.setItem('role', data.role)
    localStorage.setItem('avatar', data.avatar || '')
  }

  /* 清除用户信息 */
  function clearUser() {
    token.value = ''
    userId.value = ''
    username.value = ''
    realName.value = ''
    role.value = ''
    avatar.value = ''
    localStorage.clear()
  }

  return { token, userId, username, realName, role, avatar, setUser, clearUser }
})

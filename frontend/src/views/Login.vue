<template>
  <div class="login-container">
    <!-- 左侧装饰区 -->
    <div class="login-left">
      <div class="left-content">
        <div class="brand-icon">
          <el-icon :size="48" color="#fff"><DataAnalysis /></el-icon>
        </div>
        <h1 class="brand-title">学生综合素质评价系统</h1>
        <p class="brand-desc">基于大数据分析与K-means聚类算法，全面、客观、动态地评价学生综合素质</p>
        <div class="features">
          <div class="feature-item">
            <el-icon :size="20"><TrendCharts /></el-icon>
            <span>五维度评价体系</span>
          </div>
          <div class="feature-item">
            <el-icon :size="20"><DataLine /></el-icon>
            <span>智能聚类分析</span>
          </div>
          <div class="feature-item">
            <el-icon :size="20"><PieChart /></el-icon>
            <span>可视化数据看板</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-right">
      <div class="login-form-wrapper">
        <h2 class="form-title">欢迎登录</h2>
        <p class="form-subtitle">请输入您的账号和密码</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">登 录</el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>还没有账号？</span>
          <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
        </div>

        <!-- 快捷登录 -->
        <div class="quick-login">
          <el-divider>快捷登录（演示账号）</el-divider>
          <div class="quick-btns">
            <el-button @click="quickLogin('admin')" type="danger" plain>
              <el-icon><UserFilled /></el-icon>管理员
            </el-button>
            <el-button @click="quickLogin('teacher1')" plain>
              <el-icon><Reading /></el-icon>教师
            </el-button>
            <el-button @click="quickLogin('student1')" type="success" plain>
              <el-icon><Notebook /></el-icon>学生
            </el-button>
            <el-button @click="quickLogin('parent1')" type="warning" plain>
              <el-icon><House /></el-icon>家长
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/* 登录 */
async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const res = await request.post('/api/auth/login', form)
    userStore.setUser(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

/* 快捷登录 */
function quickLogin(username) {
  form.username = username
  form.password = '123456'
  handleLogin()
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
}
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #0f766e 0%, #0e7490 50%, #0369a1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
}
.left-content {
  max-width: 420px;
  color: #fff;
}
.brand-icon {
  width: 72px;
  height: 72px;
  background: rgba(255,255,255,0.15);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 12px;
}
.brand-desc {
  font-size: 15px;
  line-height: 1.6;
  opacity: 0.85;
  margin-bottom: 40px;
}
.features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  padding: 12px 16px;
  background: rgba(255,255,255,0.1);
  border-radius: 10px;
  backdrop-filter: blur(4px);
}
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}
.login-form-wrapper {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
}
.form-title {
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}
.form-subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 32px;
}
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  background: linear-gradient(135deg, #0f766e, #0369a1);
  border: none;
}
.login-btn:hover {
  background: linear-gradient(135deg, #0d9488, #0284c7);
}
.form-footer {
  text-align: center;
  font-size: 14px;
  color: #94a3b8;
  margin-top: 16px;
}
.quick-login {
  margin-top: 20px;
}
.quick-btns {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}
.quick-btns .el-button {
  flex: 1;
  min-width: 80px;
}
</style>

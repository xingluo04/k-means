<template>
  <div class="register-container">
    <!-- 左侧装饰区 -->
    <div class="register-left">
      <div class="left-content">
        <div class="brand-icon">
          <el-icon :size="48" color="#fff"><DataAnalysis /></el-icon>
        </div>
        <h1 class="brand-title">加入我们</h1>
        <p class="brand-desc">注册账号，开启学生综合素质评价之旅</p>
      </div>
    </div>

    <!-- 右侧注册区 -->
    <div class="register-right">
      <div class="register-form-wrapper">
        <h2 class="form-title">用户注册</h2>
        <p class="form-subtitle">请填写以下信息完成注册</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" label-width="0">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" prefix-icon="Postcard" />
          </el-form-item>
          <el-form-item prop="role">
            <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
              <el-option label="学生" value="student" />
              <el-option label="教师" value="teacher" />
              <el-option label="家长" value="parent" />
            </el-select>
          </el-form-item>
          <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号（选填）" prefix-icon="Phone" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="register-btn" @click="handleRegister" :loading="loading">注 册</el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span>已有账号？</span>
          <el-link type="primary" @click="$router.push('/login')">返回登录</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  role: '',
  phone: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function handleRegister() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await request.post('/api/auth/register', form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  height: 100vh;
  display: flex;
}
.register-left {
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
  opacity: 0.85;
}
.register-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}
.register-form-wrapper {
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
.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  background: linear-gradient(135deg, #0f766e, #0369a1);
  border: none;
}
.form-footer {
  text-align: center;
  font-size: 14px;
  color: #94a3b8;
  margin-top: 16px;
}
</style>

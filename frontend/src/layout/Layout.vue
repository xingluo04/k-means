<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon :size="24" color="#fff"><DataAnalysis /></el-icon>
        <span v-show="!isCollapse" class="logo-text">综合素质评价</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        router
        background-color="#1e293b"
        text-color="#94a3b8"
        active-text-color="#38bdf8"
        class="side-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>

        <!-- 管理员和教师菜单 -->
        <template v-if="role === 'admin'">
          <el-sub-menu index="system">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/user">用户管理</el-menu-item>
            <el-menu-item index="/class">班级管理</el-menu-item>
            <el-menu-item index="/student">学生管理</el-menu-item>
            <el-menu-item index="/notice">通知管理</el-menu-item>
            <el-menu-item index="/log">操作日志</el-menu-item>
          </el-sub-menu>
        </template>

        <template v-if="role === 'admin' || role === 'teacher'">
          <template v-if="role === 'teacher'">
            <el-menu-item index="/class">
              <el-icon><School /></el-icon>
              <template #title>班级管理</template>
            </el-menu-item>
            <el-menu-item index="/student">
              <el-icon><User /></el-icon>
              <template #title>学生管理</template>
            </el-menu-item>
          </template>
          <el-sub-menu index="evaluation">
            <template #title>
              <el-icon><EditPen /></el-icon>
              <span>评价管理</span>
            </template>
            <el-menu-item index="/moral">德育测评</el-menu-item>
            <el-menu-item index="/academic">智育测评</el-menu-item>
            <el-menu-item index="/physical">体育测评</el-menu-item>
            <el-menu-item index="/art">美育测评</el-menu-item>
            <el-menu-item index="/practice">劳动教育测评</el-menu-item>
            <el-menu-item index="/comprehensive">综合评价</el-menu-item>
            <el-menu-item index="/import">数据导入</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/cluster">
            <el-icon><TrendCharts /></el-icon>
            <template #title>聚类分析</template>
          </el-menu-item>
        </template>

        <!-- 学生菜单 -->
        <template v-if="role === 'student'">
          <el-menu-item index="/my-evaluation">
            <el-icon><Document /></el-icon>
            <template #title>我的评价</template>
          </el-menu-item>
        </template>

        <!-- 家长菜单 -->
        <template v-if="role === 'parent'">
          <el-menu-item index="/child-evaluation">
            <el-icon><View /></el-icon>
            <template #title>孩子评价</template>
          </el-menu-item>
        </template>

        <el-menu-item index="/notice-list">
          <el-icon><Bell /></el-icon>
          <template #title>系统通知</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" :size="20">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="avatar || undefined">
                {{ realName?.charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ realName }}</span>
              <el-tag size="small" :type="roleTagType">{{ roleLabel }}</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

const role = computed(() => userStore.role)
const realName = computed(() => userStore.realName)
const avatar = computed(() => userStore.avatar)

const roleLabel = computed(() => {
  const map = { admin: '管理员', teacher: '教师', student: '学生', parent: '家长' }
  return map[role.value] || ''
})

const roleTagType = computed(() => {
  const map = { admin: 'danger', teacher: '', student: 'success', parent: 'warning' }
  return map[role.value] || ''
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.clearUser()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background: #1e293b;
  transition: width 0.3s;
  overflow: hidden;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid #334155;
}
.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.side-menu {
  border-right: none;
  height: calc(100vh - 56px);
  overflow-y: auto;
}
.side-menu::-webkit-scrollbar {
  width: 0;
}
.header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.collapse-btn {
  cursor: pointer;
  color: #64748b;
}
.collapse-btn:hover {
  color: #0ea5e9;
}
.header-right {
  display: flex;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.user-name {
  font-size: 14px;
  color: #334155;
}
.main {
  background: #f1f5f9;
  padding: 20px;
  overflow-y: auto;
}
</style>

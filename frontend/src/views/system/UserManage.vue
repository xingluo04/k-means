<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索用户名/姓名" clearable style="width: 200px" @clear="loadData" />
      <el-select v-model="query.role" placeholder="角色筛选" clearable style="width: 140px" @change="loadData">
        <el-option label="管理员" value="admin" />
        <el-option label="教师" value="teacher" />
        <el-option label="学生" value="student" />
        <el-option label="家长" value="parent" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openDialog()">新增用户</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="roleType(row.role)" size="small">{{ roleLabel(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '未知' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="resetPwd(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="loadData" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="rawPassword" v-if="!isEdit">
          <el-input v-model="form.rawPassword" placeholder="默认123456" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="教师" value="teacher" />
            <el-option label="学生" value="student" />
            <el-option label="家长" value="parent" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const query = reactive({ current: 1, size: 10, keyword: '', role: '' })
const form = reactive({ id: null, username: '', rawPassword: '', realName: '', role: '', phone: '', email: '', gender: 1 })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const roleLabel = (r) => ({ admin: '管理员', teacher: '教师', student: '学生', parent: '家长' }[r] || r)
const roleType = (r) => ({ admin: 'danger', teacher: '', student: 'success', parent: 'warning' }[r] || '')

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/api/user/page', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row) {
  isEdit.value = !!row
  Object.assign(form, row ? { ...row, rawPassword: '' } : { id: null, username: '', rawPassword: '', realName: '', role: '', phone: '', email: '', gender: 1 })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) {
    await request.put('/api/user', form)
  } else {
    await request.post('/api/user', form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' })
  await request.delete(`/api/user/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

async function resetPwd(row) {
  await ElMessageBox.confirm('确定重置密码为123456？', '提示', { type: 'warning' })
  await request.put(`/api/user/resetPwd/${row.id}`)
  ElMessage.success('密码已重置')
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await request.put(`/api/user/status/${row.id}/${newStatus}`)
  ElMessage.success('状态已更新')
  loadData()
}
</script>

<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索班级名称" clearable style="width: 200px" @clear="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openDialog()">新增班级</el-button>
    </div>

    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="className" label="班级名称" min-width="200" />
      <el-table-column prop="grade" label="年级" width="120" />
      <el-table-column prop="department" label="院系" min-width="200" />
      <el-table-column prop="teacherName" label="班主任" width="120" />
      <el-table-column prop="studentCount" label="学生人数" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="loadData" />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑班级' : '新增班级'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="班级名称" prop="className"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="年级" prop="grade"><el-input v-model="form.grade" /></el-form-item>
        <el-form-item label="院系"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="form.teacherId" placeholder="请选择" clearable style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName" :value="t.id" />
          </el-select>
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
const teachers = ref([])

const query = reactive({ current: 1, size: 10, keyword: '' })
const form = reactive({ id: null, className: '', grade: '', department: '', teacherId: null })
const rules = { className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }] }

onMounted(() => { loadData(); loadTeachers() })

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/api/class/page', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function loadTeachers() {
  const res = await request.get('/api/user/page', { params: { current: 1, size: 100, role: 'teacher' } })
  teachers.value = res.data.records
}

function openDialog(row) {
  isEdit.value = !!row
  Object.assign(form, row || { id: null, className: '', grade: '', department: '', teacherId: null })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) { await request.put('/api/class', form) }
  else { await request.post('/api/class', form) }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该班级？', '提示', { type: 'warning' })
  await request.delete(`/api/class/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

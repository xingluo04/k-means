<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索姓名/学号" clearable style="width: 200px" @clear="loadData" />
      <el-select v-model="query.classId" placeholder="班级筛选" clearable style="width: 200px" @change="loadData">
        <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openDialog()">新增学生</el-button>
    </div>

    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="studentNo" label="学号" width="140" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="80">
        <template #default="{ row }">{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '未知' }}</template>
      </el-table-column>
      <el-table-column prop="className" label="班级" min-width="200" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="parentName" label="家长" width="100" />
      <el-table-column prop="enrollmentYear" label="入学年份" width="100" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="loadData" />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑学生' : '新增学生'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="学号" prop="studentNo"><el-input v-model="form.studentNo" :disabled="isEdit" /></el-form-item>
        <el-form-item label="关联用户" prop="userId">
          <el-select v-model="form.userId" placeholder="选择学生用户" filterable style="width: 100%">
            <el-option v-for="u in studentUsers" :key="u.id" :label="`${u.realName}(${u.username})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-select v-model="form.classId" placeholder="选择班级" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="家长">
          <el-select v-model="form.parentId" placeholder="选择家长" clearable filterable style="width: 100%">
            <el-option v-for="p in parentUsers" :key="p.id" :label="`${p.realName}(${p.username})`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入学年份"><el-input-number v-model="form.enrollmentYear" :min="2015" :max="2030" /></el-form-item>
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
const classes = ref([])
const studentUsers = ref([])
const parentUsers = ref([])

const query = reactive({ current: 1, size: 10, keyword: '', classId: null })
const form = reactive({ id: null, studentNo: '', userId: null, classId: null, parentId: null, enrollmentYear: 2023 })
const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  userId: [{ required: true, message: '请选择关联用户', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }]
}

onMounted(() => { loadData(); loadClasses(); loadUsers() })

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/api/student/page', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function loadClasses() {
  const res = await request.get('/api/class/list')
  classes.value = res.data
}

async function loadUsers() {
  const sRes = await request.get('/api/user/page', { params: { current: 1, size: 200, role: 'student' } })
  studentUsers.value = sRes.data.records
  const pRes = await request.get('/api/user/page', { params: { current: 1, size: 200, role: 'parent' } })
  parentUsers.value = pRes.data.records
}

function openDialog(row) {
  isEdit.value = !!row
  Object.assign(form, row || { id: null, studentNo: '', userId: null, classId: null, parentId: null, enrollmentYear: 2023 })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (isEdit.value) { await request.put('/api/student', form) }
  else { await request.post('/api/student', form) }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该学生信息？', '提示', { type: 'warning' })
  await request.delete(`/api/student/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

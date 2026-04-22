<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索通知标题" clearable style="width: 200px" @clear="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openDialog()">发布通知</el-button>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="250" />
      <el-table-column prop="type" label="类型" width="120"><template #default="{ row }"><el-tag :type="row.type === 1 ? 'info' : 'warning'">{{ row.type === 1 ? '系统通知' : '评价通知' }}</el-tag></template></el-table-column>
      <el-table-column prop="publisherName" label="发布人" width="100" />
      <el-table-column prop="status" label="状态" width="80"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag></template></el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right"><template #default="{ row }"><el-button size="small" @click="openDialog(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="loadData" />
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑通知' : '发布通知'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型"><el-select v-model="form.type" style="width: 100%"><el-option label="系统通知" :value="1" /><el-option label="评价通知" :value="2" /></el-select></el-form-item>
        <el-form-item label="内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :label="0">草稿</el-radio><el-radio :label="1">发布</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
const loading = ref(false), tableData = ref([]), total = ref(0), dialogVisible = ref(false), isEdit = ref(false), formRef = ref()
const query = reactive({ current: 1, size: 10, keyword: '' })
const form = reactive({ id: null, title: '', content: '', type: 1, status: 1, publisherId: null })
const rules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }], content: [{ required: true, message: '请输入内容', trigger: 'blur' }] }
onMounted(() => loadData())
async function loadData() { loading.value = true; try { const res = await request.get('/api/notice/page', { params: query }); tableData.value = res.data.records; total.value = res.data.total } finally { loading.value = false } }
function openDialog(row) { isEdit.value = !!row; Object.assign(form, row || { id: null, title: '', content: '', type: 1, status: 1 }); dialogVisible.value = true }
async function handleSubmit() { await formRef.value?.validate(); form.publisherId = Number(localStorage.getItem('userId')); if (isEdit.value) await request.put('/api/notice', form); else await request.post('/api/notice', form); ElMessage.success('操作成功'); dialogVisible.value = false; loadData() }
async function handleDelete(row) { await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }); await request.delete(`/api/notice/${row.id}`); ElMessage.success('删除成功'); loadData() }
</script>
<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

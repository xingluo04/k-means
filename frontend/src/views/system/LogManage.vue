<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索用户名/操作" clearable style="width: 200px" @clear="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="danger" @click="handleClear">清空日志</el-button>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="username" label="操作用户" width="120" />
      <el-table-column prop="operation" label="操作内容" min-width="200" />
      <el-table-column prop="method" label="请求方法" min-width="200" />
      <el-table-column prop="ip" label="IP地址" width="140" />
      <el-table-column prop="createTime" label="操作时间" width="170" />
    </el-table>
    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="loadData" />
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
const loading = ref(false), tableData = ref([]), total = ref(0)
const query = reactive({ current: 1, size: 10, keyword: '' })
onMounted(() => loadData())
async function loadData() { loading.value = true; try { const res = await request.get('/api/log/page', { params: query }); tableData.value = res.data.records; total.value = res.data.total } finally { loading.value = false } }
async function handleClear() { await ElMessageBox.confirm('确定清空所有日志？', '提示', { type: 'warning' }); await request.delete('/api/log/clear'); ElMessage.success('已清空'); loadData() }
</script>
<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

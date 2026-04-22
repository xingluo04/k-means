<template>
  <div class="page-container">
    <div class="notice-list">
      <div v-for="item in notices" :key="item.id" class="notice-item" @click="viewNotice(item)">
        <div class="notice-header">
          <el-tag :type="item.type === 1 ? 'info' : 'warning'" size="small">{{ item.type === 1 ? '系统通知' : '评价通知' }}</el-tag>
          <span class="notice-time">{{ item.createTime }}</span>
        </div>
        <h3 class="notice-title">{{ item.title }}</h3>
      </div>
      <el-empty v-if="notices.length === 0" description="暂无通知" />
    </div>
    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="10" v-model:current-page="current" @current-change="loadData" />
    <el-dialog v-model="detailVisible" :title="currentNotice?.title" width="600px">
      <div style="white-space: pre-wrap; line-height: 1.8; color: #334155;">{{ currentNotice?.content }}</div>
      <div style="margin-top: 16px; color: #94a3b8; font-size: 13px;">发布时间：{{ currentNotice?.createTime }}</div>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
const notices = ref([]), total = ref(0), current = ref(1), detailVisible = ref(false), currentNotice = ref(null)
onMounted(() => loadData())
async function loadData() { const res = await request.get('/api/notice/page', { params: { current: current.value, size: 10 } }); notices.value = res.data.records; total.value = res.data.total }
function viewNotice(item) { currentNotice.value = item; detailVisible.value = true }
</script>
<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.notice-item { padding: 16px; border-bottom: 1px solid #f1f5f9; cursor: pointer; transition: background 0.2s; }
.notice-item:hover { background: #f8fafc; }
.notice-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.notice-time { font-size: 13px; color: #94a3b8; }
.notice-title { font-size: 15px; color: #1e293b; font-weight: 500; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

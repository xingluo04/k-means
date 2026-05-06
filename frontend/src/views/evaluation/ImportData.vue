<template>
  <div class="page-container">
    <el-card class="upload-card">
      <template #header><span>导入综测数据</span></template>
      <el-form label-width="100px">
        <el-form-item label="选择学年">
          <el-select v-model="academicYear" style="width: 200px">
            <el-option label="2023-2024" value="2023-2024" />
            <el-option label="2024-2025" value="2024-2025" />
          </el-select>
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xls,.xlsx"
            :on-change="handleFileChange"
            :on-exceed="() => ElMessage.warning('一次只能上传一个文件')"
            drag
          >
            <el-icon><UploadFilled /></el-icon>
            <div class="upload-text">将Excel文件拖到此处，或点击选择</div>
            <div class="upload-hint">支持 .xls / .xlsx 格式</div>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="importing" :disabled="!file" @click="handleImport">
            开始导入
          </el-button>
          <el-button @click="resetForm">清空</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" class="result-card">
      <template #header><span>导入结果</span></template>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="stat-card stat-success">
            <div class="stat-num">{{ result.success }}</div>
            <div class="stat-label">导入成功</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-card stat-fail">
            <div class="stat-num">{{ result.fail }}</div>
            <div class="stat-label">导入失败</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="stat-card">
            <div class="stat-num">{{ (result.success || 0) + (result.fail || 0) }}</div>
            <div class="stat-label">总处理数</div>
          </div>
        </el-col>
      </el-row>
      <div v-if="result.errors && result.errors.length > 0" style="margin-top: 16px">
        <el-alert
          v-for="(err, i) in result.errors.slice(0, 20)"
          :key="i"
          :title="err"
          type="error"
          :closable="false"
          style="margin-bottom: 4px"
        />
        <div v-if="result.errors.length > 20" style="color: #94a3b8; margin-top: 8px">
          还有 {{ result.errors.length - 20 }} 条错误未显示...
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import request from '../../utils/request'

const academicYear = ref('2024-2025')
const file = ref(null)
const importing = ref(false)
const result = ref(null)
const uploadRef = ref()

function handleFileChange(uploadFile) {
  file.value = uploadFile.raw
}

function resetForm() {
  file.value = null
  result.value = null
  uploadRef.value?.clearFiles()
}

async function handleImport() {
  if (!file.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  importing.value = true
  result.value = null
  try {
    const formData = new FormData()
    formData.append('file', file.value)
    const res = await request.post(`/api/import/excel?academicYear=${academicYear.value}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    result.value = res.data
    ElMessage.success(`导入完成：成功 ${res.data.success} 条，失败 ${res.data.fail} 条`)
  } catch (e) {
    ElMessage.error('导入失败：' + (e.message || '未知错误'))
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.page-container { max-width: 800px; margin: 0 auto; }
.upload-card { margin-bottom: 20px; }
.upload-text { font-size: 14px; color: #334155; margin-top: 8px; }
.upload-hint { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.result-card { margin-top: 0; }
.stat-card { text-align: center; padding: 20px; background: #f8fafc; border-radius: 10px; }
.stat-num { font-size: 32px; font-weight: 700; color: #0ea5e9; }
.stat-success .stat-num { color: #10b981; }
.stat-fail .stat-num { color: #ef4444; }
.stat-label { font-size: 13px; color: #94a3b8; margin-top: 4px; }
</style>

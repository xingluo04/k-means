<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="query.academicYear" placeholder="选择学年" clearable style="width:160px" @change="loadData"><el-option label="2023-2024" value="2023-2024" /><el-option label="2024-2025" value="2024-2025" /></el-select>
      <el-select v-model="query.studentId" placeholder="选择学生" clearable filterable style="width:200px" @change="loadData"><el-option v-for="s in students" :key="s.id" :label="`${s.realName}(${s.studentNo})`" :value="s.id" /></el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openDialog()">录入评价</el-button>
    </div>
    <el-table :data="tableData" stripe v-loading="loading" size="small">
      <el-table-column prop="studentName" label="姓名" width="80" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="academicYear" label="学年" width="95" />
      <el-table-column prop="weightedAvgScore" label="加权均分" width="90" />
      <el-table-column prop="academicBonus" label="奖励分" width="80" />
      <el-table-column prop="academicOther" label="其他分" width="80" />
      <el-table-column prop="academicBonusSubtotal" label="奖励分小计" width="95" />
      <el-table-column prop="academicDeduction" label="扣分" width="70"><template #default="{ row }"><span style="color:#ef4444">-{{ row.academicDeduction }}</span></template></el-table-column>
      <el-table-column prop="totalScore" label="总分" width="75"><template #default="{ row }"><el-tag type="success">{{ row.totalScore }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><el-button size="small" @click="openDialog(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="loadData" />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑智育评价' : '录入智育评价'" width="680px" top="3vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" size="default">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="学生" prop="studentId"><el-select v-model="form.studentId" filterable style="width:100%" :disabled="isEdit"><el-option v-for="s in students" :key="s.id" :label="`${s.realName}(${s.studentNo})`" :value="s.id" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="学年" prop="academicYear"><el-select v-model="form.academicYear" style="width:100%"><el-option label="2023-2024" value="2023-2024" /><el-option label="2024-2025" value="2024-2025" /></el-select></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">学业成绩</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="学分加权平均成绩"><el-input-number v-model="form.weightedAvgScore" :min="0" :max="100" :precision="2" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">奖励分（满分10）</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="智育奖励分(10)"><el-input-number v-model="form.academicBonus" :min="0" :max="10" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="智育其他分"><el-input-number v-model="form.academicOther" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">扣分事项</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="智育表现扣分"><el-input-number v-model="form.academicDeduction" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
const loading = ref(false), tableData = ref([]), total = ref(0), dialogVisible = ref(false), isEdit = ref(false), formRef = ref(), students = ref([])
const query = reactive({ current: 1, size: 10, academicYear: '', studentId: null })
const defaultForm = { id: null, studentId: null, academicYear: '2024-2025', weightedAvgScore: 85, academicBonus: 10, academicOther: 0, academicDeduction: 0, remark: '' }
const form = reactive({ ...defaultForm })
const rules = { studentId: [{ required: true, message: '请选择学生', trigger: 'change' }], academicYear: [{ required: true, message: '请选择学年', trigger: 'change' }] }
onMounted(() => { loadData(); loadStudents() })
async function loadData() { loading.value = true; try { const res = await request.get('/api/evaluation/academic/page', { params: query }); tableData.value = res.data.records; total.value = res.data.total } finally { loading.value = false } }
async function loadStudents() { const res = await request.get('/api/student/page', { params: { current: 1, size: 200 } }); students.value = res.data.records }
function openDialog(row) { isEdit.value = !!row; Object.assign(form, row ? { ...row } : { ...defaultForm }); dialogVisible.value = true }
async function handleSubmit() { await formRef.value?.validate(); form.evaluatorId = Number(localStorage.getItem('userId')); await request.post('/api/evaluation/academic', form); ElMessage.success('操作成功'); dialogVisible.value = false; loadData() }
async function handleDelete(row) { await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }); await request.delete(`/api/evaluation/academic/${row.id}`); ElMessage.success('删除成功'); loadData() }
</script>
<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

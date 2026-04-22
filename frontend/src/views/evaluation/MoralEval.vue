<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="query.semester" placeholder="选择学期" clearable style="width: 140px" @change="loadData">
        <el-option label="2024-1" value="2024-1" /><el-option label="2024-2" value="2024-2" />
      </el-select>
      <el-select v-model="query.studentId" placeholder="选择学生" clearable filterable style="width: 200px" @change="loadData">
        <el-option v-for="s in students" :key="s.id" :label="`${s.realName}(${s.studentNo})`" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openDialog()">录入评价</el-button>
    </div>

    <el-table :data="tableData" stripe v-loading="loading" size="small">
      <el-table-column prop="studentName" label="姓名" width="80" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="semester" label="学期" width="75" />
      <el-table-column prop="moralActivityScore" label="德育实践" width="85" />
      <el-table-column prop="politicalThoughtScore" label="政治思想" width="85" />
      <el-table-column prop="integrityScore" label="诚信" width="70" />
      <el-table-column prop="disciplineScore" label="纪律" width="70" />
      <el-table-column prop="honorScore" label="荣誉" width="70" />
      <el-table-column prop="socialWorkScore" label="社会工作" width="85" />
      <el-table-column prop="outstandingScore" label="突出事例" width="85" />
      <el-table-column prop="deductionScore" label="扣分" width="70">
        <template #default="{ row }"><span style="color:#ef4444">-{{ row.deductionScore }}</span></template>
      </el-table-column>
      <el-table-column prop="totalScore" label="总分" width="75">
        <template #default="{ row }"><el-tag type="success">{{ row.totalScore }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="loadData" />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑品德评价' : '录入品德评价'" width="680px" top="3vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="default">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学生" prop="studentId">
              <el-select v-model="form.studentId" filterable style="width:100%" :disabled="isEdit">
                <el-option v-for="s in students" :key="s.id" :label="`${s.realName}(${s.studentNo})`" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学期" prop="semester">
              <el-select v-model="form.semester" style="width:100%"><el-option label="2024-1" value="2024-1" /><el-option label="2024-2" value="2024-2" /></el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">德育实践</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="活动参与次数"><el-input-number v-model="form.moralActivityCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="德育实践得分"><el-input-number v-model="form.moralActivityScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">品德表现</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="政治思想"><el-input-number v-model="form.politicalThoughtScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="诚信原则"><el-input-number v-model="form.integrityScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="学习态度"><el-input-number v-model="form.learningAttitudeScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="纪律作风"><el-input-number v-model="form.disciplineScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="集体观念"><el-input-number v-model="form.collectiveScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="文明礼貌"><el-input-number v-model="form.civilityScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">荣誉与奖励</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="个人荣誉数"><el-input-number v-model="form.personalHonorCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="集体荣誉数"><el-input-number v-model="form.collectiveHonorCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="荣誉得分"><el-input-number v-model="form.honorScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="军训等级"><el-select v-model="form.militaryTrainingLevel" clearable style="width:100%"><el-option label="优秀" value="优秀" /><el-option label="良好" value="良好" /><el-option label="合格" value="合格" /><el-option label="不合格" value="不合格" /></el-select></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">社会工作</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="干部职务"><el-input v-model="form.cadreDuty" placeholder="如：班长" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="任职时长(月)"><el-input-number v-model="form.cadreDuration" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="社会工作得分"><el-input-number v-model="form.socialWorkScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">突出事例</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="突出事例数"><el-input-number v-model="form.outstandingEventCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="献血次数"><el-input-number v-model="form.bloodDonationCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="突出事例得分"><el-input-number v-model="form.outstandingScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">扣分事项</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="通报批评次数"><el-input-number v-model="form.criticismCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="纪律处分"><el-select v-model="form.disciplinePunishment" clearable style="width:100%"><el-option label="无" value="" /><el-option label="警告" value="警告" /><el-option label="严重警告" value="严重警告" /><el-option label="记过" value="记过" /><el-option label="留校察看" value="留校察看" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="缺席活动次数"><el-input-number v-model="form.absentActivityCount" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="扣分合计"><el-input-number v-model="form.deductionScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
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
const query = reactive({ current: 1, size: 10, semester: '', studentId: null })

const defaultForm = { id: null, studentId: null, semester: '2024-2', moralActivityCount: 0, moralActivityScore: 70, politicalThoughtScore: 80, integrityScore: 85, learningAttitudeScore: 75, disciplineScore: 80, collectiveScore: 70, civilityScore: 80, personalHonorCount: 0, collectiveHonorCount: 0, honorScore: 50, militaryTrainingLevel: '良好', cadreDuty: '', cadreDuration: 0, socialWorkScore: 50, outstandingEventCount: 0, bloodDonationCount: 0, outstandingScore: 30, criticismCount: 0, disciplinePunishment: '', absentActivityCount: 0, deductionScore: 0, remark: '' }
const form = reactive({ ...defaultForm })
const rules = { studentId: [{ required: true, message: '请选择学生', trigger: 'change' }], semester: [{ required: true, message: '请选择学期', trigger: 'change' }] }

onMounted(() => { loadData(); loadStudents() })
async function loadData() { loading.value = true; try { const res = await request.get('/api/evaluation/moral/page', { params: query }); tableData.value = res.data.records; total.value = res.data.total } finally { loading.value = false } }
async function loadStudents() { const res = await request.get('/api/student/page', { params: { current: 1, size: 200 } }); students.value = res.data.records }
function openDialog(row) { isEdit.value = !!row; Object.assign(form, row ? { ...row } : { ...defaultForm, evaluatorId: Number(localStorage.getItem('userId')) }); dialogVisible.value = true }
async function handleSubmit() { await formRef.value?.validate(); form.evaluatorId = Number(localStorage.getItem('userId')); await request.post('/api/evaluation/moral', form); ElMessage.success('操作成功'); dialogVisible.value = false; loadData() }
async function handleDelete(row) { await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }); await request.delete(`/api/evaluation/moral/${row.id}`); ElMessage.success('删除成功'); loadData() }
</script>

<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

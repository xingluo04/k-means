<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="query.semester" placeholder="选择学期" clearable style="width:140px" @change="loadData"><el-option label="2024-1" value="2024-1" /><el-option label="2024-2" value="2024-2" /></el-select>
      <el-select v-model="query.studentId" placeholder="选择学生" clearable filterable style="width:200px" @change="loadData"><el-option v-for="s in students" :key="s.id" :label="`${s.realName}(${s.studentNo})`" :value="s.id" /></el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="openDialog()">录入评价</el-button>
    </div>
    <el-table :data="tableData" stripe v-loading="loading" size="small">
      <el-table-column prop="studentName" label="姓名" width="80" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="semester" label="学期" width="75" />
      <el-table-column prop="artCourseCount" label="美育课程数" width="95" />
      <el-table-column prop="artCourseAvgScore" label="课程均分" width="85" />
      <el-table-column prop="artGroupMember" label="艺术团" width="75"><template #default="{ row }">{{ row.artGroupMember ? '是' : '否' }}</template></el-table-column>
      <el-table-column prop="artActivityCount" label="活动次数" width="85" />
      <el-table-column prop="artActivityScore" label="活动得分" width="85" />
      <el-table-column prop="artCompetitionCount" label="竞赛次数" width="85" />
      <el-table-column prop="artCompetitionCategory" label="竞赛类别" width="140" />
      <el-table-column prop="artCompetitionScore" label="竞赛得分" width="85" />
      <el-table-column prop="artDeduction" label="扣分" width="70"><template #default="{ row }"><span style="color:#ef4444">-{{ row.artDeduction }}</span></template></el-table-column>
      <el-table-column prop="totalScore" label="总分" width="75"><template #default="{ row }"><el-tag type="success">{{ row.totalScore }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><el-button size="small" @click="openDialog(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="loadData" />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑艺术评价' : '录入艺术评价'" width="680px" top="3vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" size="default">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="学生" prop="studentId"><el-select v-model="form.studentId" filterable style="width:100%" :disabled="isEdit"><el-option v-for="s in students" :key="s.id" :label="`${s.realName}(${s.studentNo})`" :value="s.id" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="学期" prop="semester"><el-select v-model="form.semester" style="width:100%"><el-option label="2024-1" value="2024-1" /><el-option label="2024-2" value="2024-2" /></el-select></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">美育课程与活动</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="美育课程数"><el-input-number v-model="form.artCourseCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="课程均分"><el-input-number v-model="form.artCourseAvgScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="艺术团成员"><el-select v-model="form.artGroupMember" style="width:100%"><el-option label="否" :value="0" /><el-option label="是" :value="1" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="活动参与次数"><el-input-number v-model="form.artActivityCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="活动得分"><el-input-number v-model="form.artActivityScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">文艺竞赛</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="竞赛参赛次数"><el-input-number v-model="form.artCompetitionCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="竞赛类别"><el-input v-model="form.artCompetitionCategory" placeholder="美术/书法/音乐等" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="最高级别"><el-select v-model="form.artHighestLevel" clearable style="width:100%"><el-option label="国家级" value="国家级" /><el-option label="省级" value="省级" /><el-option label="校级" value="校级" /><el-option label="院级" value="院级" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="竞赛得分"><el-input-number v-model="form.artCompetitionScore" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">扣分事项</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="缺席美育活动"><el-input-number v-model="form.absentArtCount" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="扣分合计"><el-input-number v-model="form.artDeduction" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
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
const defaultForm = { id: null, studentId: null, semester: '2024-2', artCourseCount: 1, artCourseAvgScore: 75, artGroupMember: 0, artActivityCount: 2, artActivityScore: 60, artCompetitionCount: 0, artCompetitionCategory: '', artHighestLevel: '', artCompetitionScore: 25, absentArtCount: 0, artDeduction: 0, remark: '' }
const form = reactive({ ...defaultForm })
const rules = { studentId: [{ required: true, message: '请选择学生', trigger: 'change' }], semester: [{ required: true, message: '请选择学期', trigger: 'change' }] }
onMounted(() => { loadData(); loadStudents() })
async function loadData() { loading.value = true; try { const res = await request.get('/api/evaluation/art/page', { params: query }); tableData.value = res.data.records; total.value = res.data.total } finally { loading.value = false } }
async function loadStudents() { const res = await request.get('/api/student/page', { params: { current: 1, size: 200 } }); students.value = res.data.records }
function openDialog(row) { isEdit.value = !!row; Object.assign(form, row ? { ...row } : { ...defaultForm }); dialogVisible.value = true }
async function handleSubmit() { await formRef.value?.validate(); form.evaluatorId = Number(localStorage.getItem('userId')); await request.post('/api/evaluation/art', form); ElMessage.success('操作成功'); dialogVisible.value = false; loadData() }
async function handleDelete(row) { await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }); await request.delete(`/api/evaluation/art/${row.id}`); ElMessage.success('删除成功'); loadData() }
</script>
<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>

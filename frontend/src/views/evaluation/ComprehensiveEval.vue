<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="query.academicYear" placeholder="选择学年" clearable style="width: 160px" @change="loadData"><el-option label="2023-2024" value="2023-2024" /><el-option label="2024-2025" value="2024-2025" /></el-select>
      <el-select v-model="query.classId" placeholder="选择班级" clearable style="width: 160px" @change="loadData">
        <el-option v-for="c in classList" :key="c.id" :label="c.className" :value="c.id" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="搜索姓名/学号" clearable style="width: 180px" @clear="loadData" />
      <el-select v-model="query.clusterLabel" placeholder="聚类类别" clearable style="width: 160px" @change="loadData">
        <el-option label="全面优秀型" :value="0" /><el-option label="均衡发展型" :value="1" /><el-option label="学术艺术型" :value="2" /><el-option label="体能突出型" :value="3" /><el-option label="待提升型" :value="4" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
    <el-table :data="tableData" stripe v-loading="loading">
      <el-table-column prop="studentName" label="学生姓名" width="100" />
      <el-table-column prop="studentNo" label="学号" width="130" />
      <el-table-column prop="className" label="班级" width="200" />
      <el-table-column prop="academicYear" label="学年" width="105" />
      <el-table-column prop="moralScore" label="德育" width="70" />
      <el-table-column prop="academicScore" label="智育" width="70" />
      <el-table-column prop="physicalScore" label="体育" width="70" />
      <el-table-column prop="artScore" label="美育" width="70" />
      <el-table-column prop="practiceScore" label="劳动教育" width="80" />
      <el-table-column prop="totalScore" label="综合总分" width="90"><template #default="{ row }"><el-tag type="success" effect="dark">{{ row.totalScore }}</el-tag></template></el-table-column>
      <el-table-column prop="clusterName" label="聚类类别" width="120"><template #default="{ row }"><el-tag v-if="row.clusterName" :type="clusterType(row.clusterLabel)">{{ row.clusterName }}</el-tag><span v-else>未分类</span></template></el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button size="small" @click="viewGrowth(row)">成长轨迹</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pagination" background layout="total, prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.current" @current-change="loadData" />

    <el-dialog v-model="detailVisible" title="综合素质评价详情" width="700px">
      <template #header="{ close, titleId, titleClass }">
        <div class="dialog-header">
          <span :id="titleId" :class="titleClass">综合素质评价详情</span>
          <el-button type="success" size="small" @click="printReport" style="margin-left: 12px">导出报告</el-button>
        </div>
      </template>
      <div v-if="detailData" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生姓名">{{ detailData.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detailData.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="学年">{{ detailData.academicYear }}</el-descriptions-item>
          <el-descriptions-item label="聚类类别"><el-tag :type="clusterType(detailData.clusterLabel)">{{ detailData.clusterName || '未分类' }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="德育发展">{{ detailData.moralScore }}分</el-descriptions-item>
          <el-descriptions-item label="智育发展">{{ detailData.academicScore }}分</el-descriptions-item>
          <el-descriptions-item label="体育发展">{{ detailData.physicalScore }}分</el-descriptions-item>
          <el-descriptions-item label="美育发展">{{ detailData.artScore }}分</el-descriptions-item>
          <el-descriptions-item label="劳动教育发展">{{ detailData.practiceScore }}分</el-descriptions-item>
          <el-descriptions-item label="综合总分"><el-tag type="success" effect="dark" size="large">{{ detailData.totalScore }}分</el-tag></el-descriptions-item>
        </el-descriptions>
        <div ref="detailRadarRef" style="height: 300px; margin-top: 16px"></div>
        <div v-if="detailSuggestion" class="suggestion-area">
          <h4>个性化发展建议</h4>
          <div class="suggestion-header">
            <el-tag v-if="detailSuggestion.clusterName" type="primary" size="large">{{ detailSuggestion.clusterName }}</el-tag>
            <el-tag :type="severityTagType(detailSuggestion.severityLevel)" size="large" style="margin-left:8px">
              {{ severityLabel(detailSuggestion.severityLevel) }}
            </el-tag>
            <el-tag v-if="detailSuggestion.typicality" type="info" size="large" style="margin-left:8px">
              {{ detailSuggestion.typicality.label }}
            </el-tag>
            <p class="summary-text">{{ detailSuggestion.summary }}</p>
          </div>
          <el-row :gutter="12" style="margin-top:12px">
            <el-col :span="12" v-if="detailSuggestion.strengths && detailSuggestion.strengths.length > 0">
              <div class="dim-card dim-strength">
                <h5>优势维度</h5>
                <div v-for="s in detailSuggestion.strengths" :key="s.dimKey" class="dim-item">
                  <div class="dim-header">
                    <span class="dim-label">{{ s.dimLabel }}</span>
                    <el-tag size="small" :type="levelTagType(s.level)">{{ s.levelLabel }}</el-tag>
                  </div>
                  <el-progress :percentage="s.score" :color="levelColor(s.level)" :stroke-width="14">
                    <span class="progress-text">{{ s.score }}分</span>
                  </el-progress>
                  <p class="dim-rank" v-if="s.rank > 0">排名 {{ s.rank }}/{{ s.total }}</p>
                  <p class="dim-sug">{{ s.suggestion }}</p>
                </div>
              </div>
            </el-col>
            <el-col :span="12" v-if="detailSuggestion.weaknesses && detailSuggestion.weaknesses.length > 0">
              <div class="dim-card dim-weakness">
                <h5>待提升维度</h5>
                <div v-for="w in detailSuggestion.weaknesses" :key="w.dimKey" class="dim-item">
                  <div class="dim-header">
                    <span class="dim-label">{{ w.dimLabel }}</span>
                    <el-tag size="small" :type="levelTagType(w.level)">{{ w.levelLabel }}</el-tag>
                  </div>
                  <el-progress :percentage="w.score" :color="levelColor(w.level)" :stroke-width="14">
                    <span class="progress-text">{{ w.score }}分</span>
                  </el-progress>
                  <p class="dim-rank">排名 {{ w.rank }}/{{ w.total }}</p>
                  <p class="dim-sug">{{ w.suggestion }}</p>
                </div>
              </div>
            </el-col>
          </el-row>
          <div v-if="detailSuggestion.actionItems && detailSuggestion.actionItems.length > 0" class="action-box">
            <h5>行动建议</h5>
            <ul><li v-for="(item, i) in detailSuggestion.actionItems" :key="i">{{ item }}</li></ul>
          </div>
<div v-if="detailSuggestion.trend" class="trend-box">
            <h5>成长趋势（{{ detailSuggestion.trend.prevYear }} 对比）</h5>
            <p class="trend-highlight">{{ detailSuggestion.trend.highlight }}</p>
            <div class="trend-changes">
              <span v-for="dc in detailSuggestion.trend.dimChanges" :key="dc.dimKey" class="trend-chip"
                    :class="dc.improved ? 'trend-up' : 'trend-down'">
                {{ dc.dimLabel }} {{ dc.improved ? '↑' : '↓' }}{{ Math.abs(dc.change) }}
              </span>
            </div>
          </div>
          <div v-if="detailSuggestion.percentileRanks && detailSuggestion.percentileRanks.length > 0" class="rank-box">
            <h5>年级相对位置</h5>
            <div class="rank-chips">
              <span v-for="r in detailSuggestion.percentileRanks" :key="r.dimKey" class="rank-chip">
                {{ r.dimLabel }}：排名 {{ r.rank }}/{{ r.total }}
              </span>
            </div>
          </div>
          <div v-if="detailSuggestion.typicality" class="typicality-box">
            <span class="typicality-label">群体位置：</span>
            <el-tag size="small">{{ detailSuggestion.typicality.label }}</el-tag>
            <span style="margin-left:8px;color:#64748b">{{ detailSuggestion.typicality.description }}</span>
          </div>
        </div>
        <div v-else-if="detailData.suggestion" class="suggestion-box">
          <h4>个性化发展建议</h4>
          <p>{{ detailData.suggestion }}</p>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="growthVisible" title="成长轨迹" width="800px">
      <div ref="growthChartRef" style="height: 400px"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const loading = ref(false), tableData = ref([]), total = ref(0), classList = ref([])
const detailVisible = ref(false), growthVisible = ref(false), detailData = ref(null)
const detailRadarRef = ref(), growthChartRef = ref()
const query = reactive({ current: 1, size: 10, academicYear: '', classId: null, keyword: '', clusterLabel: null })

const clusterType = (label) => ({ 0: 'success', 1: '', 2: 'warning', 3: 'info', 4: 'danger' }[label] || '')
const detailSuggestion = computed(() => {
  if (!detailData.value?.suggestion) return null
  return parseSuggestion(detailData.value.suggestion)
})

function parseSuggestion(raw) {
  try { return JSON.parse(raw) }
  catch { return null }
}
function severityTagType(level) {
  return { critical: 'danger', warning: 'warning', normal: '', good: 'success', excellent: 'success' }[level] || ''
}
function severityLabel(level) {
  return { critical: '需紧急提升', warning: '建议重点关注', normal: '可进一步提升', good: '保持优势', excellent: '榜样标杆' }[level] || level
}
function levelTagType(level) {
  return { critical: 'danger', warning: 'warning', normal: 'info', good: 'success', excellent: 'success' }[level] || ''
}
function levelColor(level) {
  return { critical: '#ef4444', warning: '#f59e0b', normal: '#0ea5e9', good: '#10b981', excellent: '#6366f1' }[level] || '#0ea5e9'
}

onMounted(() => { loadClasses(); loadData() })

async function loadClasses() {
  try {
    const res = await request.get('/api/class/list')
    classList.value = res.data || []
  } catch { /* 忽略 */ }
}

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/api/evaluation/comprehensive/page', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function viewDetail(row) {
  detailData.value = row
  detailVisible.value = true
  nextTick(() => {
    const chart = echarts.init(detailRadarRef.value)
    chart.setOption({
      radar: { indicator: [{ name: '德育', max: 100 }, { name: '智育', max: 100 }, { name: '体育', max: 100 }, { name: '美育', max: 100 }, { name: '劳动教育', max: 100 }] },
      series: [{ type: 'radar', data: [{ value: [row.moralScore, row.academicScore, row.physicalScore, row.artScore, row.practiceScore], name: row.studentName, areaStyle: { color: 'rgba(14,165,233,0.2)' }, lineStyle: { color: '#0ea5e9' }, itemStyle: { color: '#0ea5e9' } }] }]
    })
  })
}

function printReport() {
  if (!detailData.value) return
  const { studentId, academicYear } = detailData.value
  window.open(`/report?studentId=${studentId}&academicYear=${academicYear}`, '_blank')
}

async function viewGrowth(row) {
  growthVisible.value = true
  const res = await request.get(`/api/evaluation/comprehensive/growth/${row.studentId}`)
  nextTick(() => {
    const chart = echarts.init(growthChartRef.value)
    const data = res.data
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['德育', '智育', '体育', '美育', '劳动教育', '综合'] },
      xAxis: { type: 'category', data: data.map(d => d.academicYear) },
      yAxis: { type: 'value', min: 40, max: 100 },
      series: [
        { name: '德育', type: 'line', data: data.map(d => d.moralScore), smooth: true },
        { name: '智育', type: 'line', data: data.map(d => d.academicScore), smooth: true },
        { name: '体育', type: 'line', data: data.map(d => d.physicalScore), smooth: true },
        { name: '美育', type: 'line', data: data.map(d => d.artScore), smooth: true },
        { name: '劳动教育', type: 'line', data: data.map(d => d.practiceScore), smooth: true },
        { name: '综合', type: 'line', data: data.map(d => d.totalScore), smooth: true, lineStyle: { width: 3 } }
      ]
    })
  })
}
</script>

<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.dialog-header { display: flex; align-items: center; width: 100%; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.suggestion-box { margin-top: 16px; padding: 16px; background: #f0f9ff; border-radius: 8px; border-left: 4px solid #0ea5e9; }
.suggestion-box h4 { margin-bottom: 8px; color: #0369a1; }
.suggestion-box p { color: #334155; line-height: 1.6; }

.suggestion-area { margin-top: 16px; }
.suggestion-area h4 { margin-bottom: 12px; color: #0369a1; font-size: 15px; }
.suggestion-header { margin-bottom: 8px; }
.suggestion-header .summary-text { color: #334155; line-height: 1.6; margin-top: 8px; font-size: 14px; }
.dim-card { background: #f8fafc; border-radius: 10px; padding: 16px; height: 100%; }
.dim-card h5 { font-size: 14px; margin-bottom: 10px; color: #1e293b; }
.dim-card.dim-strength { border-left: 4px solid #10b981; }
.dim-card.dim-weakness { border-left: 4px solid #f59e0b; }
.dim-item { margin-bottom: 12px; }
.dim-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.dim-label { font-size: 13px; font-weight: 600; color: #334155; }
.dim-rank { font-size: 11px; color: #94a3b8; margin: 2px 0; }
.dim-sug { font-size: 12px; color: #64748b; margin: 4px 0; }
.progress-text { font-size: 11px; padding-left: 4px; }
.action-box { margin-top: 12px; padding: 16px; background: #eff6ff; border-radius: 10px; border-left: 4px solid #0ea5e9; }
.action-box h5 { font-size: 14px; margin-bottom: 8px; color: #0369a1; }
.action-box ul { margin: 0; padding-left: 20px; color: #334155; font-size: 13px; line-height: 1.8; }
.trend-box { margin-top: 12px; padding: 16px; background: #f0fdf4; border-radius: 10px; border-left: 4px solid #10b981; }
.trend-box h5 { font-size: 14px; margin-bottom: 8px; color: #15803d; }
.trend-highlight { color: #334155; font-size: 13px; line-height: 1.6; }
.trend-changes { margin-top: 8px; display: flex; gap: 8px; flex-wrap: wrap; }
.trend-chip { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.trend-up { background: #dcfce7; color: #15803d; }
.trend-down { background: #fef2f2; color: #dc2626; }
.rank-box { margin-top: 12px; padding: 16px; background: #faf5ff; border-radius: 10px; border-left: 4px solid #a855f7; }
.rank-box h5 { font-size: 14px; margin-bottom: 8px; color: #7c3aed; }
.rank-chips { display: flex; gap: 8px; flex-wrap: wrap; }
.rank-chip { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; background: #f3e8ff; color: #6b21a8; }
.typicality-box { margin-top: 12px; padding: 10px 16px; background: #f8fafc; border-radius: 8px; font-size: 13px; }
.typicality-label { color: #64748b; }
</style>

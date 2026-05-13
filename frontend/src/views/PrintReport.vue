<template>
  <div class="report-container" v-if="reportData">
    <div class="no-print" style="text-align: center; padding: 20px">
      <el-button type="primary" @click="handlePrint" :icon="Printer">打印报告</el-button>
      <el-button @click="handleClose">关闭</el-button>
    </div>

    <div class="report-header">
      <h1>学生综合素质评价报告</h1>
      <div class="report-meta">
        <span>学年：{{ reportData.academicYear || academicYear }}</span>
        <span>生成日期：{{ new Date().toLocaleDateString('zh-CN') }}</span>
      </div>
    </div>

    <div class="report-section">
      <h3>基本信息</h3>
      <table class="info-table">
        <tr><td class="label">学号</td><td>{{ reportData.studentNo || '-' }}</td></tr>
        <tr><td class="label">姓名</td><td>{{ reportData.studentName || '-' }}</td></tr>
        <tr><td class="label">班级</td><td>{{ reportData.className || '-' }}</td></tr>
      </table>
    </div>

    <div class="report-section" v-if="scores">
      <h3>五维评价得分</h3>
      <table class="score-table">
        <thead>
          <tr><th>维度</th><th>得分</th><th>年级排名</th><th>百分位</th><th>等级</th></tr>
        </thead>
        <tbody>
          <tr v-for="s in scores" :key="s.dimKey">
            <td>{{ s.dimLabel }}</td>
            <td class="score-val">{{ s.score }}</td>
            <td>{{ s.rank }}/{{ s.total }}</td>
            <td>{{ s.percentile ? s.percentile.toFixed(1) + '%' : '-' }}</td>
            <td><span :class="'level-' + s.level">{{ s.levelLabel }}</span></td>
          </tr>
        </tbody>
      </table>
      <div style="margin-top:12px"><strong>综合总分：{{ reportData.totalScore || '-' }}</strong></div>
    </div>

    <div class="report-section" v-if="suggestion">
      <h3>聚类分析结果</h3>
      <p><strong>所属群体：</strong>{{ suggestion.clusterName || '-' }}</p>
      <p><strong>综合评语：</strong>{{ suggestion.summary || '-' }}</p>

      <div v-if="suggestion.strengths && suggestion.strengths.length > 0" style="margin-top:12px">
        <h4>优势维度</h4>
        <ul>
          <li v-for="s in suggestion.strengths" :key="s.dimKey">
            {{ s.dimLabel }}：{{ s.score }}分（{{ s.levelLabel }}） — {{ s.suggestion }}
          </li>
        </ul>
      </div>

      <div v-if="suggestion.weaknesses && suggestion.weaknesses.length > 0" style="margin-top:12px">
        <h4>需提升维度</h4>
        <ul>
          <li v-for="w in suggestion.weaknesses" :key="w.dimKey">
            {{ w.dimLabel }}：{{ w.score }}分（{{ w.levelLabel }}） — {{ w.suggestion }}
          </li>
        </ul>
      </div>

      <div v-if="suggestion.actionItems && suggestion.actionItems.length > 0" style="margin-top:12px">
        <h4>行动建议</h4>
        <ol>
          <li v-for="(item, i) in suggestion.actionItems" :key="i">{{ item }}</li>
        </ol>
      </div>

      <div v-if="suggestion.trend && suggestion.trend.highlight" style="margin-top:12px">
        <h4>成长趋势</h4>
        <p>{{ suggestion.trend.highlight }}</p>
        <p v-if="suggestion.trend.totalChange !== undefined">
          总分变化：{{ suggestion.trend.totalChange >= 0 ? '+' : '' }}{{ suggestion.trend.totalChange }}分（较{{ suggestion.trend.prevYear }}学年）
        </p>
      </div>

      <div v-if="suggestion.typicality" style="margin-top:12px">
        <h4>群体典型性</h4>
        <p>{{ suggestion.typicality.description || '-' }}（典型度比率：{{ suggestion.typicality.ratio }}）</p>
      </div>
    </div>

    <div class="report-footer">
      <p>本报告由学生综合素质评价系统自动生成，仅供参考。</p>
    </div>
  </div>
  <div v-else-if="loading" style="text-align:center; padding: 40px">
    <el-icon :size="40" class="is-loading"><Loading /></el-icon>
    <p>正在加载报告数据...</p>
  </div>
  <div v-else-if="loadError" style="text-align:center; padding: 40px">
    <p style="color: #ef4444">加载报告数据失败，请检查学生ID或学年参数是否正确。</p>
    <el-button type="primary" @click="window.close()">关闭</el-button>
  </div>
  <div v-else style="text-align:center; padding: 40px">
    <p>未找到相关数据。</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Printer, Loading } from '@element-plus/icons-vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const academicYear = ref(route.query.academicYear || '2024-2025')
const studentId = ref(Number(route.query.studentId))
const reportData = ref(null)
const loading = ref(true)
const loadError = ref(false)

const scores = computed(() => {
  if (!reportData.value) return null
  const dimKeys = ['moral', 'academic', 'physical', 'art', 'practice']
  const dimLabels = ['德育', '智育', '体育', '美育', '劳动教育']
  const scoreValues = [
    reportData.value.moralScore, reportData.value.academicScore,
    reportData.value.physicalScore, reportData.value.artScore, reportData.value.practiceScore
  ]
  return dimKeys.map((k, i) => {
    const pr = reportData.value.percentileRanks
    return {
      dimKey: k,
      dimLabel: dimLabels[i],
      score: scoreValues[i],
      rank: pr && pr[i] ? pr[i].rank : '-',
      total: pr && pr[i] ? pr[i].total : '-',
      percentile: pr && pr[i] ? pr[i].percentile : 0,
      level: getLevel(scoreValues[i]),
      levelLabel: getLevelLabel(getLevel(scoreValues[i]))
    }
  })
})

const suggestion = computed(() => {
  if (!reportData.value || !reportData.value.suggestion) return null
  try {
    return typeof reportData.value.suggestion === 'string'
      ? JSON.parse(reportData.value.suggestion)
      : reportData.value.suggestion
  } catch (e) {
    return null
  }
})

function getLevel(score) {
  if (score >= 90) return 'excellent'
  if (score >= 75) return 'good'
  if (score >= 60) return 'normal'
  if (score >= 40) return 'warning'
  return 'critical'
}

function getLevelLabel(level) {
  const map = { excellent: '榜样标杆', good: '保持优势', normal: '可进一步提升', warning: '建议重点关注', critical: '需紧急提升' }
  return map[level] || '一般'
}

onMounted(async () => {
  if (!studentId.value) {
    loadError.value = true
    loading.value = false
    return
  }
  try {
    const res = await request.get('/api/evaluation/comprehensive/detail', {
      params: { studentId: studentId.value, academicYear: academicYear.value }
    })
    reportData.value = res.data
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
})

function handlePrint() {
  window.print()
}

function handleClose() {
  window.close()
}
</script>

<style scoped>
.report-container {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  padding: 20px;
  font-family: 'SimSun', 'Microsoft YaHei', serif;
  color: #333;
}
.no-print { display: flex; gap: 10px; justify-content: center; }

.report-header { text-align: center; margin-bottom: 24px; border-bottom: 2px solid #1e40af; padding-bottom: 16px; }
.report-header h1 { font-size: 22px; color: #1e40af; margin: 0 0 8px 0; }
.report-meta { font-size: 13px; color: #666; display: flex; gap: 24px; justify-content: center; }

.report-section { margin: 20px 0; }
.report-section h3 { font-size: 16px; color: #1e40af; border-left: 4px solid #1e40af; padding-left: 10px; margin-bottom: 10px; }
.report-section h4 { font-size: 14px; color: #334155; margin: 8px 0 4px 0; }

.info-table { width: 100%; border-collapse: collapse; }
.info-table td { padding: 6px 12px; border: 1px solid #e2e8f0; }
.info-table .label { background: #f1f5f9; width: 80px; font-weight: bold; text-align: center; }

.score-table { width: 100%; border-collapse: collapse; margin-top: 8px; }
.score-table th, .score-table td { padding: 8px 12px; border: 1px solid #e2e8f0; text-align: center; }
.score-table th { background: #f1f5f9; font-weight: bold; }
.score-table .score-val { font-weight: bold; font-size: 16px; color: #0ea5e9; }
.score-table .gap { color: #ef4444; font-weight: bold; }

.level-excellent { color: #10b981; font-weight: bold; }
.level-good { color: #0ea5e9; }
.level-normal { color: #f59e0b; }
.level-warning { color: #ef4444; }
.level-critical { color: #dc2626; font-weight: bold; }

.report-section ul, .report-section ol { margin: 4px 0; padding-left: 20px; }
.report-section li { line-height: 1.8; }

.report-footer { margin-top: 30px; padding-top: 12px; border-top: 1px solid #e2e8f0; text-align: center; font-size: 12px; color: #94a3b8; }

@media print {
  .no-print { display: none !important; }
  .report-container { padding: 0; max-width: 100%; }
  body { background: #fff; }
}
</style>

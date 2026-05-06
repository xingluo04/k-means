<template>
  <div class="page-container">
    <div v-if="children.length === 0" class="empty-tip">
      <el-empty description="暂无关联的学生信息" />
    </div>
    <template v-else>
      <el-tabs v-model="activeChild" @tab-change="loadChildEval">
        <el-tab-pane v-for="child in children" :key="child.id" :label="child.realName || `学生${child.studentNo}`" :name="String(child.id)" />
      </el-tabs>

      <div v-if="evaluations.length > 0">
        <div class="eval-card">
          <el-row :gutter="20">
            <el-col :span="12">
              <h3>{{ latest?.academicYear }} 学年综合素质评价</h3>
              <el-descriptions :column="1" border style="margin-top: 12px">
                <el-descriptions-item label="德育发展">{{ latest?.moralScore }}分</el-descriptions-item>
                <el-descriptions-item label="智育发展">{{ latest?.academicScore }}分</el-descriptions-item>
                <el-descriptions-item label="体育发展">{{ latest?.physicalScore }}分</el-descriptions-item>
                <el-descriptions-item label="美育发展">{{ latest?.artScore }}分</el-descriptions-item>
                <el-descriptions-item label="劳动教育发展">{{ latest?.practiceScore }}分</el-descriptions-item>
                <el-descriptions-item label="综合总分"><el-tag type="success" effect="dark" size="large">{{ latest?.totalScore }}分</el-tag></el-descriptions-item>
                <el-descriptions-item label="聚类类别"><el-tag>{{ latest?.clusterName || '未分类' }}</el-tag></el-descriptions-item>
              </el-descriptions>
            </el-col>
            <el-col :span="12">
              <div ref="radarRef" style="height: 300px"></div>
            </el-col>
          </el-row>
          <div v-if="suggestionData" class="suggestion-area">
            <h4>个性化发展建议</h4>
            <div class="suggestion-header">
              <el-tag v-if="suggestionData.clusterName" type="primary" size="large">{{ suggestionData.clusterName }}</el-tag>
              <el-tag :type="severityTagType(suggestionData.severityLevel)" size="large" style="margin-left:8px">
                {{ severityLabel(suggestionData.severityLevel) }}
              </el-tag>
              <el-tag v-if="suggestionData.typicality" type="info" size="large" style="margin-left:8px">
                {{ suggestionData.typicality.label }}
              </el-tag>
              <p class="summary-text">{{ suggestionData.summary }}</p>
            </div>
            <el-row :gutter="12" style="margin-top:12px">
              <el-col :span="12" v-if="suggestionData.strengths && suggestionData.strengths.length > 0">
                <div class="dim-card dim-strength">
                  <h5>优势维度</h5>
                  <div v-for="s in suggestionData.strengths" :key="s.dimKey" class="dim-item">
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
              <el-col :span="12" v-if="suggestionData.weaknesses && suggestionData.weaknesses.length > 0">
                <div class="dim-card dim-weakness">
                  <h5>待提升维度</h5>
                  <div v-for="w in suggestionData.weaknesses" :key="w.dimKey" class="dim-item">
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
            <div v-if="suggestionData.actionItems && suggestionData.actionItems.length > 0" class="action-box">
              <h5>行动建议</h5>
              <ul><li v-for="(item, i) in suggestionData.actionItems" :key="i">{{ item }}</li></ul>
            </div>
            <div v-if="suggestionData.promotion" class="promotion-box">
              <h5>提升路径</h5>
              <p>{{ suggestionData.promotion.description }}</p>
              <template v-if="suggestionData.promotion.gaps && suggestionData.promotion.gaps.length > 0">
                <div v-for="g in suggestionData.promotion.gaps" :key="g.dimKey" style="margin-top:8px">
                  <span class="dim-label">{{ g.dimLabel }}：</span>
                  <span>当前 {{ g.currentScore }} → 目标 {{ g.targetScore }}（差 {{ g.gap }} 分）</span>
                  <el-progress :percentage="Math.round(Math.min(100, g.currentScore / g.targetScore * 100))" :stroke-width="10" :color="'#f59e0b'" />
                </div>
              </template>
            </div>
            <div v-if="suggestionData.trend" class="trend-box">
              <h5>成长趋势（{{ suggestionData.trend.prevYear }} 对比）</h5>
              <p class="trend-highlight">{{ suggestionData.trend.highlight }}</p>
              <div class="trend-changes">
                <span v-for="dc in suggestionData.trend.dimChanges" :key="dc.dimKey" class="trend-chip"
                      :class="dc.improved ? 'trend-up' : 'trend-down'">
                  {{ dc.dimLabel }} {{ dc.improved ? '↑' : '↓' }}{{ Math.abs(dc.change) }}
                </span>
              </div>
            </div>
            <div v-if="suggestionData.percentileRanks && suggestionData.percentileRanks.length > 0" class="rank-box">
              <h5>年级相对位置</h5>
              <div class="rank-chips">
                <span v-for="r in suggestionData.percentileRanks" :key="r.dimKey" class="rank-chip">
                  {{ r.dimLabel }}：排名 {{ r.rank }}/{{ r.total }}
                </span>
              </div>
            </div>
            <div v-if="suggestionData.typicality" class="typicality-box">
              <span class="typicality-label">群体位置：</span>
              <el-tag size="small">{{ suggestionData.typicality.label }}</el-tag>
              <span style="margin-left:8px;color:#64748b">{{ suggestionData.typicality.description }}</span>
            </div>
          </div>
          <div v-else-if="latest?.suggestion" class="suggestion-box">
            <h4>个性化发展建议</h4>
            <p>{{ latest.suggestion }}</p>
          </div>
        </div>
        <div class="eval-card" style="margin-top: 16px">
          <h3>成长轨迹</h3>
          <div ref="growthRef" style="height: 350px; margin-top: 12px"></div>
        </div>
      </div>
      <el-empty v-else description="暂无评价数据" />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const children = ref([])
const activeChild = ref('')
const evaluations = ref([])
const radarRef = ref(), growthRef = ref()
const latest = computed(() => evaluations.value.length > 0 ? evaluations.value[evaluations.value.length - 1] : null)
const suggestionData = computed(() => {
  if (!latest.value?.suggestion) return null
  return parseSuggestion(latest.value.suggestion)
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

onMounted(async () => {
  const res = await request.get('/api/student/children')
  children.value = res.data
  if (children.value.length > 0) {
    activeChild.value = String(children.value[0].id)
    loadChildEval()
  }
})

async function loadChildEval() {
  if (!activeChild.value) return
  const res = await request.get(`/api/evaluation/comprehensive/growth/${activeChild.value}`)
  evaluations.value = res.data
  if (evaluations.value.length > 0) nextTick(() => renderCharts())
}

function renderCharts() {
  const l = latest.value
  if (l && radarRef.value) {
    const radar = echarts.init(radarRef.value)
    radar.setOption({
      radar: { indicator: [{ name: '德育', max: 100 }, { name: '智育', max: 100 }, { name: '体育', max: 100 }, { name: '美育', max: 100 }, { name: '劳动教育', max: 100 }] },
      series: [{ type: 'radar', data: [{ value: [l.moralScore, l.academicScore, l.physicalScore, l.artScore, l.practiceScore], areaStyle: { color: 'rgba(14,165,233,0.2)' }, lineStyle: { color: '#0ea5e9' }, itemStyle: { color: '#0ea5e9' } }] }]
    })
  }
  if (growthRef.value) {
    const growth = echarts.init(growthRef.value)
    growth.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['德育', '智育', '体育', '美育', '劳动教育', '综合'] },
      xAxis: { type: 'category', data: evaluations.value.map(d => d.academicYear) },
      yAxis: { type: 'value', min: 40, max: 100 },
      series: [
        { name: '德育', type: 'line', data: evaluations.value.map(d => d.moralScore), smooth: true },
        { name: '智育', type: 'line', data: evaluations.value.map(d => d.academicScore), smooth: true },
        { name: '体育', type: 'line', data: evaluations.value.map(d => d.physicalScore), smooth: true },
        { name: '美育', type: 'line', data: evaluations.value.map(d => d.artScore), smooth: true },
        { name: '劳动教育', type: 'line', data: evaluations.value.map(d => d.practiceScore), smooth: true },
        { name: '综合', type: 'line', data: evaluations.value.map(d => d.totalScore), smooth: true, lineStyle: { width: 3 } }
      ]
    })
  }
}
</script>

<style scoped>
.eval-card { background: #fff; border-radius: 10px; padding: 24px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.eval-card h3 { font-size: 16px; color: #1e293b; }
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
.promotion-box { margin-top: 12px; padding: 16px; background: #fefce8; border-radius: 10px; border-left: 4px solid #eab308; }
.promotion-box h5 { font-size: 14px; margin-bottom: 8px; color: #a16207; }
.promotion-box p { color: #334155; font-size: 13px; }
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

<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="academicYear" placeholder="选择学年" style="width: 160px">
        <el-option label="2023-2024" value="2023-2024" />
        <el-option label="2024-2025" value="2024-2025" />
      </el-select>
      <el-input-number v-model="clusterCount" :min="2" :max="10" placeholder="聚类数" style="width: 140px" />
      <el-button type="primary" @click="executeCluster" :loading="executing">执行聚类分析</el-button>
      <el-button @click="recommendK" :loading="recommending">推荐K值</el-button>
      <el-button @click="loadResults">查看结果</el-button>
    </div>

    <el-alert
      v-if="lowDiscriminationDims.length > 0"
      :title="'以下维度区分度较低(CV&lt;0.10)，对聚类贡献有限：' + lowDiscriminationDims.join('、')"
      type="warning" show-icon :closable="false" style="margin-bottom:16px"
    />

    <el-row :gutter="16" v-if="results.length > 0" class="result-row">
      <el-col :span="5">
        <div class="info-card">
          <div class="info-label">轮廓系数</div>
          <div class="info-value">{{ silhouetteScore }}</div>
          <div class="info-desc">越接近1越好</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="info-card">
          <div class="info-label">DBI指数</div>
          <div class="info-value">{{ dbiScore }}</div>
          <div class="info-desc">越低越好</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="info-card">
          <div class="info-label">CHI指数</div>
          <div class="info-value">{{ chiScore }}</div>
          <div class="info-desc">越高越好</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="info-card">
          <div class="info-label">聚类数量</div>
          <div class="info-value">{{ results.length }}</div>
        </div>
      </el-col>
      <el-col :span="4">
        <div class="info-card">
          <div class="info-label">学生总数</div>
          <div class="info-value">{{ results.reduce((s, r) => s + r.studentCount, 0) }}</div>
        </div>
      </el-col>
    </el-row>

    <el-table :data="results" stripe v-if="results.length > 0" style="margin-top: 16px">
      <el-table-column prop="clusterName" label="聚类名称" width="140">
        <template #default="{ row }"><el-tag :type="clusterType(row.clusterLabel)">{{ row.clusterName }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="studentCount" label="学生数" width="90" />
      <el-table-column prop="avgMoral" label="平均德育" width="100" />
      <el-table-column prop="avgAcademic" label="平均智育" width="100" />
      <el-table-column prop="avgPhysical" label="平均体育" width="100" />
      <el-table-column prop="avgArt" label="平均美育" width="100" />
      <el-table-column prop="avgPractice" label="平均劳动教育" width="110" />
      <el-table-column prop="description" label="聚类描述" min-width="300" />
    </el-table>

    <el-row :gutter="16" style="margin-top: 16px" v-if="results.length > 0">
      <el-col :span="12">
        <div class="chart-card">
          <h3>聚类分布饼图</h3>
          <div ref="pieRef" style="height: 340px"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <h3>各聚类维度对比雷达图</h3>
          <div ref="radarRef" style="height: 340px"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px" v-if="kMetricsData">
      <el-col :span="24">
        <div class="chart-card">
          <h3>K值优化曲线（肘部法则）</h3>
          <div ref="elbowRef" style="height: 340px"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const academicYear = ref('2024-2025')
const clusterCount = ref(5)
const executing = ref(false)
const recommending = ref(false)
const results = ref([])
const silhouetteScore = ref('-')
const dbiScore = ref('-')
const chiScore = ref('-')
const lowDiscriminationDims = ref([])
const kMetricsData = ref(null)
const pieRef = ref(), radarRef = ref(), elbowRef = ref()

const clusterType = (label) => ({ 0: 'success', 1: '', 2: 'warning', 3: 'info', 4: 'danger' }[label] || '')
const colors = ['#10b981', '#0ea5e9', '#f59e0b', '#64748b', '#ef4444']

async function executeCluster() {
  executing.value = true
  try {
    const res = await request.post(`/api/cluster/execute?academicYear=${academicYear.value}&clusterCount=${clusterCount.value}`)
    const data = res.data
    results.value = data.clusters
    silhouetteScore.value = data.silhouetteScore
    dbiScore.value = data.daviesBouldinIndex ?? '-'
    chiScore.value = data.calinskiHarabaszIndex ?? '-'
    if (data.descriptiveStats) {
      lowDiscriminationDims.value = data.descriptiveStats.dimensions
        .filter(d => d.lowDiscrimination)
        .map(d => d.label)
    }
    kMetricsData.value = null
    ElMessage.success('聚类分析完成')
    nextTick(() => renderCharts())
  } catch (e) {
    ElMessage.error(e.message || '聚类分析失败')
  } finally { executing.value = false }
}

async function recommendK() {
  recommending.value = true
  try {
    const res = await request.get('/api/cluster/optimal-k', {
      params: { academicYear: academicYear.value, minK: 2, maxK: 10 }
    })
    const data = res.data
    clusterCount.value = data.optimalK
    kMetricsData.value = {
      silhouette: data.silhouetteByK,
      dbi: data.dbiByK,
      chi: data.chiByK,
      optimalK: data.optimalK
    }
    ElMessage.success(`推荐K=${data.optimalK} (三指标投票)`)
    nextTick(() => renderElbowChart())
  } catch (e) {
    ElMessage.error(e.message || '推荐失败')
  } finally { recommending.value = false }
}

async function loadResults() {
  const res = await request.get('/api/cluster/results', { params: { academicYear: academicYear.value } })
  results.value = res.data
  if (results.value.length > 0) {
    silhouetteScore.value = results.value[0]?.silhouetteScore ?? '-'
    dbiScore.value = results.value[0]?.daviesBouldinIndex ?? '-'
    chiScore.value = results.value[0]?.calinskiHarabaszIndex ?? '-'
    nextTick(() => renderCharts())
  } else {
    ElMessage.info('暂无聚类结果')
  }
}

function renderCharts() {
  const pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    color: colors,
    series: [{ type: 'pie', radius: ['40%', '70%'], itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }, label: { formatter: '{b}\n{c}人' }, data: results.value.map(r => ({ name: r.clusterName, value: r.studentCount })) }]
  })

  const radarChart = echarts.init(radarRef.value)
  radarChart.setOption({
    tooltip: {},
    legend: { data: results.value.map(r => r.clusterName), bottom: 0 },
    color: colors,
    radar: { indicator: [{ name: '德育', max: 100 }, { name: '智育', max: 100 }, { name: '体育', max: 100 }, { name: '美育', max: 100 }, { name: '劳动教育', max: 100 }] },
    series: [{ type: 'radar', data: results.value.map(r => ({ value: [r.avgMoral, r.avgAcademic, r.avgPhysical, r.avgArt, r.avgPractice], name: r.clusterName })) }]
  })
}

function renderElbowChart() {
  if (!elbowRef.value || !kMetricsData.value) return
  const chart = echarts.init(elbowRef.value)
  const ks = kMetricsData.value.silhouette.map(d => d.k)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['轮廓系数', 'DBI', 'CHI (缩放)'], bottom: 0 },
    grid: { left: 50, right: 50, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: ks, name: 'K值' },
    yAxis: [
      { type: 'value', name: '轮廓系数 / DBI' },
      { type: 'value', name: 'CHI' }
    ],
    series: [
      { name: '轮廓系数', type: 'line', data: kMetricsData.value.silhouette.map(d => d.value),
        markPoint: { data: [{ coord: [kMetricsData.value.optimalK, kMetricsData.value.silhouette.find(d => d.k === kMetricsData.value.optimalK)?.value || 0], name: '最优K', symbol: 'pin', symbolSize: 40 }] } },
      { name: 'DBI', type: 'line', data: kMetricsData.value.dbi.map(d => d.value) },
      { name: 'CHI (缩放)', type: 'line', yAxisIndex: 1, data: kMetricsData.value.chi.map(d => d.value) }
    ]
  })
}
</script>

<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; }
.result-row { margin-top: 16px; }
.info-card { background: #f8fafc; border-radius: 10px; padding: 20px; text-align: center; }
.info-label { font-size: 13px; color: #94a3b8; }
.info-value { font-size: 28px; font-weight: 700; color: #0ea5e9; margin: 8px 0; }
.info-desc { font-size: 12px; color: #cbd5e1; }
.chart-card { background: #f8fafc; border-radius: 10px; padding: 20px; }
.chart-card h3 { font-size: 14px; color: #334155; margin-bottom: 12px; }
</style>

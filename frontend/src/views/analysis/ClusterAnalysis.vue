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
      :title="'以下维度区分度较低(CV<0.10)，对聚类贡献有限：' + lowDiscriminationDims.join('、')"
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

    <!-- ANOVA显著性检验结果 -->
    <div class="chart-card" style="margin-top: 16px" v-if="anovaResult && anovaResult.dimensions">
      <h3>聚类差异显著性检验（单因素ANOVA）</h3>
      <el-table :data="anovaResult.dimensions" stripe size="small" style="margin-top: 12px">
        <el-table-column prop="dimension" label="维度" width="100" />
        <el-table-column prop="fValue" label="F值" width="120" />
        <el-table-column prop="dfBetween" label="组间df" width="90" />
        <el-table-column prop="dfWithin" label="组内df" width="90" />
        <el-table-column prop="pValue" label="p值" width="120" />
        <el-table-column prop="etaSquared" label="效应量η²" width="110" />
        <el-table-column label="显著性" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.highlySignificant" type="danger" effect="dark">极显著(p&lt;0.01)</el-tag>
            <el-tag v-else-if="row.significant" type="warning">显著(p&lt;0.05)</el-tag>
            <el-tag v-else type="info">不显著</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="info-desc" style="margin-top:8px">n={{ anovaResult.totalStudents }}, k={{ anovaResult.clusterCount }}。η²≥0.14为大效应，η²≥0.06为中等效应，η²≥0.01为小效应。</div>
    </div>

    <el-row :gutter="16" style="margin-top: 16px" v-if="pcaPoints && pcaPoints.length > 0">
      <el-col :span="12">
        <div class="chart-card">
          <h3>PCA降维散点图（聚类分布）</h3>
          <div ref="scatterRef" style="height: 400px"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <h3>维度相关性热力图</h3>
          <div ref="heatmapRef" style="height: 400px"></div>
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
const pcaPoints = ref(null)
const correlationMatrix = ref(null)
const anovaResult = ref(null)
const pieRef = ref(), radarRef = ref(), scatterRef = ref(), heatmapRef = ref(), elbowRef = ref()

const clusterType = (label) => ({ 0: 'success', 1: '', 2: 'warning', 3: 'info', 4: 'danger' }[label] || '')
const colors = ['#10b981', '#0ea5e9', '#f59e0b', '#64748b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#6366f1']

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
    pcaPoints.value = data.pcaPoints || null
    correlationMatrix.value = data.correlationMatrix || null
    anovaResult.value = data.anovaResult || null
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
      gap: data.gapByK || [],
      gapSE: data.gapSEByK || [],
      optimalK: data.optimalK,
      gapOptimalK: data.gapOptimalK
    }
    const gapK = data.gapOptimalK ? `，Gap Statistic推荐K=${data.gapOptimalK}` : ''
    ElMessage.success(`三指标投票推荐K=${data.optimalK}${gapK}`)
    nextTick(() => renderElbowChart())
  } catch (e) {
    ElMessage.error(e.message || '推荐失败')
  } finally { recommending.value = false }
}

async function loadResults() {
  const res = await request.get('/api/cluster/results', { params: { academicYear: academicYear.value } })
  const data = res.data
  if (!data.clusters || data.clusters.length === 0) {
    results.value = []
    ElMessage.info('暂无聚类结果')
    return
  }
  results.value = data.clusters
  silhouetteScore.value = data.silhouetteScore ?? data.clusters[0]?.silhouetteScore ?? '-'
  dbiScore.value = data.daviesBouldinIndex ?? data.clusters[0]?.daviesBouldinIndex ?? '-'
  chiScore.value = data.calinskiHarabaszIndex ?? data.clusters[0]?.calinskiHarabaszIndex ?? '-'
  pcaPoints.value = data.pcaPoints || null
  correlationMatrix.value = data.correlationMatrix || null
  anovaResult.value = data.anovaResult || null
  if (data.descriptiveStats) {
    lowDiscriminationDims.value = data.descriptiveStats.dimensions
      .filter(d => d.lowDiscrimination)
      .map(d => d.label)
  }
  kMetricsData.value = null
  nextTick(() => renderCharts())
}

function renderCharts() {
  renderPieChart()
  renderRadarChart()
  if (pcaPoints.value && pcaPoints.value.length > 0) {
    renderScatterChart()
  }
  if (correlationMatrix.value) {
    renderHeatmapChart()
  }
}

function renderPieChart() {
  const pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    color: colors,
    series: [{ type: 'pie', radius: ['40%', '70%'], itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }, label: { formatter: '{b}\n{c}人' }, data: results.value.map(r => ({ name: r.clusterName, value: r.studentCount })) }]
  })
}

function renderRadarChart() {
  const radarChart = echarts.init(radarRef.value)
  radarChart.setOption({
    tooltip: {},
    legend: { data: results.value.map(r => r.clusterName), bottom: 0 },
    color: colors,
    radar: { indicator: [{ name: '德育', max: 100 }, { name: '智育', max: 100 }, { name: '体育', max: 100 }, { name: '美育', max: 100 }, { name: '劳动教育', max: 100 }] },
    series: [{ type: 'radar', data: results.value.map(r => ({ value: [r.avgMoral, r.avgAcademic, r.avgPhysical, r.avgArt, r.avgPractice], name: r.clusterName })) }]
  })
}

function renderScatterChart() {
  const chart = echarts.init(scatterRef.value)
  const clusterLabels = [...new Set(pcaPoints.value.map(p => p.cluster))]
  const series = clusterLabels.map((label, i) => ({
    name: results.value.find(r => r.clusterLabel === label)?.clusterName || `聚类${label}`,
    type: 'scatter',
    data: pcaPoints.value.filter(p => p.cluster === label).map(p => [p.x, p.y]),
    symbolSize: 8,
    itemStyle: { color: colors[i % colors.length], opacity: 0.75 },
    emphasis: { itemStyle: { borderColor: '#333', borderWidth: 1 } }
  }))
  chart.setOption({
    tooltip: { trigger: 'item', formatter: p => `${p.seriesName}<br/>PC1: ${p.value[0]}<br/>PC2: ${p.value[1]}` },
    legend: { data: series.map(s => s.name), bottom: 0 },
    grid: { left: 50, right: 30, top: 20, bottom: 40 },
    xAxis: { type: 'value', name: '主成分1', nameLocation: 'center', nameGap: 25, splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { type: 'value', name: '主成分2', nameLocation: 'center', nameGap: 35, splitLine: { lineStyle: { type: 'dashed' } } },
    series
  })
}

function renderHeatmapChart() {
  const chart = echarts.init(heatmapRef.value)
  const dimLabels = ['德育', '智育', '体育', '美育', '劳动教育']
  const data = []
  for (let i = 0; i < correlationMatrix.value.length; i++) {
    for (let j = 0; j < correlationMatrix.value[i].length; j++) {
      data.push({ value: [j, i, correlationMatrix.value[i][j]] })
    }
  }
  chart.setOption({
    tooltip: { position: 'top', formatter: p => `${dimLabels[p.value[1]]} - ${dimLabels[p.value[0]]}<br/>相关系数: ${p.value[2].toFixed(3)}` },
    grid: { left: 80, right: 20, top: 20, bottom: 80 },
    xAxis: { type: 'category', data: dimLabels, axisLabel: { rotate: 0 }, position: 'top', axisLine: { onZero: false } },
    yAxis: { type: 'category', data: dimLabels, inverse: true },
    visualMap: { min: -1, max: 1, calculable: true, orient: 'horizontal', left: 'center', bottom: 0,
      inRange: { color: ['#4393c3', '#f7f7f7', '#d73027'] } },
    series: [{
      type: 'heatmap', data, label: { show: true, formatter: p => p.value[2].toFixed(3), fontSize: 13 },
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.3)' } }
    }]
  })
}

function renderElbowChart() {
  if (!elbowRef.value || !kMetricsData.value) return
  const chart = echarts.init(elbowRef.value)
  const ks = kMetricsData.value.silhouette.map(d => d.k)
  const series = [
    { name: '轮廓系数', type: 'line', data: kMetricsData.value.silhouette.map(d => d.value),
      markPoint: { data: [{ coord: [kMetricsData.value.optimalK, kMetricsData.value.silhouette.find(d => d.k === kMetricsData.value.optimalK)?.value || 0], name: '最优K', symbol: 'pin', symbolSize: 40 }] } },
    { name: 'DBI', type: 'line', data: kMetricsData.value.dbi.map(d => d.value) },
    { name: 'CHI (缩放)', type: 'line', yAxisIndex: 1, data: kMetricsData.value.chi.map(d => d.value) }
  ]
  if (kMetricsData.value.gap && kMetricsData.value.gap.length > 0) {
    series.push({
      name: 'Gap Statistic',
      type: 'line',
      yAxisIndex: 2,
      data: kMetricsData.value.gap.map(d => d.value),
      markPoint: {
        data: kMetricsData.value.gapOptimalK ? [{ coord: [kMetricsData.value.gapOptimalK, kMetricsData.value.gap.find(d => d.k === kMetricsData.value.gapOptimalK)?.value || 0], name: `Gap K=${kMetricsData.value.gapOptimalK}`, symbol: 'roundRect', symbolSize: 50 }] : []
      },
      lineStyle: { type: 'dashed', width: 2 },
      itemStyle: { color: '#8b5cf6' }
    })
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: series.map(s => s.name), bottom: 0 },
    grid: { left: 50, right: 80, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: ks, name: 'K值' },
    yAxis: [
      { type: 'value', name: '轮廓系数 / DBI' },
      { type: 'value', name: 'CHI' },
      { type: 'value', name: 'Gap值', show: kMetricsData.value.gap && kMetricsData.value.gap.length > 0 }
    ],
    series
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

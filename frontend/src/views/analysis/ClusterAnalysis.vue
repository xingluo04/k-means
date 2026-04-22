<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="semester" placeholder="选择学期" style="width: 140px"><el-option label="2024-1" value="2024-1" /><el-option label="2024-2" value="2024-2" /></el-select>
      <el-input-number v-model="clusterCount" :min="2" :max="10" placeholder="聚类数" style="width: 140px" />
      <el-button type="primary" @click="executeCluster" :loading="executing">执行聚类分析</el-button>
      <el-button @click="loadResults">查看结果</el-button>
    </div>

    <!-- 聚类结果概览 -->
    <el-row :gutter="16" v-if="results.length > 0" class="result-row">
      <el-col :span="8">
        <div class="info-card">
          <div class="info-label">轮廓系数</div>
          <div class="info-value">{{ results[0]?.silhouetteScore }}</div>
          <div class="info-desc">越接近1表示聚类效果越好</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="info-card">
          <div class="info-label">聚类数量</div>
          <div class="info-value">{{ results.length }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="info-card">
          <div class="info-label">学生总数</div>
          <div class="info-value">{{ results.reduce((s, r) => s + r.studentCount, 0) }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 聚类结果表格 -->
    <el-table :data="results" stripe v-if="results.length > 0" style="margin-top: 16px">
      <el-table-column prop="clusterName" label="聚类名称" width="140">
        <template #default="{ row }"><el-tag :type="clusterType(row.clusterLabel)">{{ row.clusterName }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="studentCount" label="学生数" width="90" />
      <el-table-column prop="avgMoral" label="平均品德" width="100" />
      <el-table-column prop="avgAcademic" label="平均学业" width="100" />
      <el-table-column prop="avgPhysical" label="平均体能" width="100" />
      <el-table-column prop="avgArt" label="平均艺术" width="100" />
      <el-table-column prop="avgPractice" label="平均实践" width="100" />
      <el-table-column prop="description" label="聚类描述" min-width="300" />
    </el-table>

    <!-- 图表 -->
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
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const semester = ref('2024-2')
const clusterCount = ref(5)
const executing = ref(false)
const results = ref([])
const pieRef = ref(), radarRef = ref()

const clusterType = (label) => ({ 0: 'success', 1: '', 2: 'warning', 3: 'info', 4: 'danger' }[label] || '')
const colors = ['#10b981', '#0ea5e9', '#f59e0b', '#64748b', '#ef4444']

async function executeCluster() {
  executing.value = true
  try {
    const res = await request.post(`/api/cluster/execute?semester=${semester.value}&clusterCount=${clusterCount.value}`)
    results.value = res.data.clusters
    ElMessage.success('聚类分析完成')
    nextTick(() => renderCharts())
  } catch (e) {
    ElMessage.error(e.message || '聚类分析失败')
  } finally { executing.value = false }
}

async function loadResults() {
  const res = await request.get('/api/cluster/results', { params: { semester: semester.value } })
  results.value = res.data
  if (results.value.length > 0) nextTick(() => renderCharts())
  else ElMessage.info('暂无聚类结果')
}

function renderCharts() {
  /* 饼图 */
  const pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    color: colors,
    series: [{ type: 'pie', radius: ['40%', '70%'], itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 }, label: { formatter: '{b}\n{c}人' }, data: results.value.map(r => ({ name: r.clusterName, value: r.studentCount })) }]
  })

  /* 雷达图 */
  const radarChart = echarts.init(radarRef.value)
  radarChart.setOption({
    tooltip: {},
    legend: { data: results.value.map(r => r.clusterName), bottom: 0 },
    color: colors,
    radar: { indicator: [{ name: '品德', max: 100 }, { name: '学业', max: 100 }, { name: '体能', max: 100 }, { name: '艺术', max: 100 }, { name: '实践', max: 100 }] },
    series: [{ type: 'radar', data: results.value.map(r => ({ value: [r.avgMoral, r.avgAcademic, r.avgPhysical, r.avgArt, r.avgPractice], name: r.clusterName })) }]
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

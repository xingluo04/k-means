<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="item in statCards" :key="item.label">
        <div class="stat-card" :style="{ borderLeft: `4px solid ${item.color}` }">
          <div class="stat-info">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
          <el-icon :size="36" :color="item.color"><component :is="item.icon" /></el-icon>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <div class="chart-card">
          <h3 class="chart-title">各维度平均得分（雷达图）</h3>
          <div ref="radarRef" class="chart-body"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <h3 class="chart-title">学生聚类分布</h3>
          <div ref="pieRef" class="chart-body"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <div class="chart-card">
          <h3 class="chart-title">综合成绩分布</h3>
          <div ref="barRef" class="chart-body"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, markRaw } from 'vue'
import * as echarts from 'echarts'
import request from '../utils/request'
import { UserFilled, Reading, Notebook, OfficeBuilding } from '@element-plus/icons-vue'

const radarRef = ref()
const pieRef = ref()
const barRef = ref()

const statCards = ref([
  { label: '学生总数', value: 0, icon: markRaw(Notebook), color: '#0ea5e9' },
  { label: '教师总数', value: 0, icon: markRaw(Reading), color: '#10b981' },
  { label: '班级总数', value: 0, icon: markRaw(OfficeBuilding), color: '#f59e0b' },
  { label: '评价记录', value: 0, icon: markRaw(UserFilled), color: '#ef4444' }
])

onMounted(async () => {
  await loadOverview()
  await loadCharts()
})

async function loadOverview() {
  try {
    const res = await request.get('/api/dashboard/overview')
    statCards.value[0].value = res.data.studentCount
    statCards.value[1].value = res.data.teacherCount
    statCards.value[2].value = res.data.classCount
    statCards.value[3].value = res.data.evaluationCount
  } catch (e) { /* 忽略 */ }
}

async function loadCharts() {
  /* 雷达图 */
  try {
    const dimRes = await request.get('/api/dashboard/dimension-avg', { params: { semester: '2024-2' } })
    if (dimRes.data && dimRes.data.dimensions) {
      const radarChart = echarts.init(radarRef.value)
      radarChart.setOption({
        tooltip: {},
        radar: {
          indicator: dimRes.data.dimensions.map(d => ({ name: d, max: 100 })),
          shape: 'polygon',
          splitArea: { areaStyle: { color: ['#f0f9ff', '#e0f2fe', '#bae6fd', '#7dd3fc', '#38bdf8'].reverse() } }
        },
        series: [{
          type: 'radar',
          data: [{
            value: dimRes.data.values,
            name: '平均得分',
            areaStyle: { color: 'rgba(14, 165, 233, 0.2)' },
            lineStyle: { color: '#0ea5e9' },
            itemStyle: { color: '#0ea5e9' }
          }]
        }]
      })
      window.addEventListener('resize', () => radarChart.resize())
    }
  } catch (e) { /* 忽略 */ }

  /* 饼图 */
  try {
    const clusterRes = await request.get('/api/dashboard/cluster-distribution', { params: { semester: '2024-2' } })
    if (clusterRes.data && clusterRes.data.length > 0) {
      const pieChart = echarts.init(pieRef.value)
      pieChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
        color: ['#0ea5e9', '#10b981', '#f59e0b', '#ef4444', '#64748b'],
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}\n{c}人' },
          data: clusterRes.data
        }]
      })
      window.addEventListener('resize', () => pieChart.resize())
    }
  } catch (e) { /* 忽略 */ }

  /* 柱状图 */
  try {
    const scoreRes = await request.get('/api/dashboard/score-distribution', { params: { semester: '2024-2' } })
    if (scoreRes.data && scoreRes.data.labels) {
      const barChart = echarts.init(barRef.value)
      barChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: scoreRes.data.labels },
        yAxis: { type: 'value', name: '人数' },
        series: [{
          type: 'bar',
          data: scoreRes.data.values,
          barWidth: '40%',
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#0ea5e9' },
              { offset: 1, color: '#0369a1' }
            ])
          }
        }]
      })
      window.addEventListener('resize', () => barChart.resize())
    }
  } catch (e) { /* 忽略 */ }
}
</script>

<style scoped>
.stat-row { margin-bottom: 16px; }
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.stat-value { font-size: 28px; font-weight: 700; color: #1e293b; }
.stat-label { font-size: 13px; color: #94a3b8; margin-top: 4px; }
.chart-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.chart-title { font-size: 15px; font-weight: 600; color: #334155; margin-bottom: 12px; }
.chart-body { height: 340px; }
</style>

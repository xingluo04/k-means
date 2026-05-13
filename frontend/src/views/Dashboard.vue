<template>
  <div class="dashboard">
    <!-- 学年选择 -->
    <div class="search-bar" style="margin-bottom: 16px">
      <el-select v-model="academicYear" placeholder="选择学年" style="width: 160px" @change="onYearChange">
        <el-option label="2023-2024" value="2023-2024" />
        <el-option label="2024-2025" value="2024-2025" />
      </el-select>
    </div>

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

    <!-- 班级聚类对比 -->
    <el-row :gutter="16" style="margin-top: 16px" v-if="classComparison">
      <el-col :span="24">
        <div class="chart-card">
          <h3 class="chart-title">各班级聚类分布对比</h3>
          <div ref="classBarRef" class="chart-body"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, markRaw, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { UserFilled, Reading, Notebook, OfficeBuilding } from '@element-plus/icons-vue'

const academicYear = ref('2024-2025')
const radarRef = ref()
const pieRef = ref()
const barRef = ref()
const classBarRef = ref()
const classComparison = ref(null)

const statCards = ref([
  { label: '学生总数', value: 0, icon: markRaw(Notebook), color: '#0ea5e9' },
  { label: '教师总数', value: 0, icon: markRaw(Reading), color: '#10b981' },
  { label: '班级总数', value: 0, icon: markRaw(OfficeBuilding), color: '#f59e0b' },
  { label: '评价记录', value: 0, icon: markRaw(UserFilled), color: '#ef4444' }
])

const resizeHandlers = []

function onResize(handler) {
  resizeHandlers.push(handler)
  window.addEventListener('resize', handler)
}

onMounted(async () => {
  await loadOverview()
  await loadCharts()
  await loadClassComparison()
})

onBeforeUnmount(() => {
  resizeHandlers.forEach(h => window.removeEventListener('resize', h))
})

async function onYearChange() {
  await loadCharts()
  await loadClassComparison()
}

async function loadOverview() {
  try {
    const res = await request.get('/api/dashboard/overview')
    statCards.value[0].value = res.data.studentCount
    statCards.value[1].value = res.data.teacherCount
    statCards.value[2].value = res.data.classCount
    statCards.value[3].value = res.data.evaluationCount
  } catch (e) {
    ElMessage.warning('加载概览数据失败')
  }
}

async function loadCharts() {
  /* 雷达图 */
  try {
    const dimRes = await request.get('/api/dashboard/dimension-avg', { params: { academicYear: academicYear.value } })
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
      onResize(() => radarChart.resize())
    }
  } catch (e) {
    ElMessage.warning('加载维度平均数据失败')
  }

  /* 饼图 */
  try {
    const clusterRes = await request.get('/api/dashboard/cluster-distribution', { params: { academicYear: academicYear.value } })
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
      onResize(() => pieChart.resize())
    }
  } catch (e) {
    ElMessage.warning('加载聚类分布数据失败')
  }

  /* 柱状图 */
  try {
    const scoreRes = await request.get('/api/dashboard/score-distribution', { params: { academicYear: academicYear.value } })
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
      onResize(() => barChart.resize())
    }
  } catch (e) {
    ElMessage.warning('加载成绩分布数据失败')
  }
}

async function loadClassComparison() {
  try {
    const res = await request.get('/api/dashboard/class-comparison', { params: { academicYear: academicYear.value } })
    if (res.data && res.data.classes && res.data.classes.length > 0) {
      classComparison.value = res.data
      nextTick(() => renderClassComparison())
    } else {
      classComparison.value = null
    }
  } catch (e) {
    classComparison.value = null
  }
}

function renderClassComparison() {
  if (!classBarRef.value || !classComparison.value) return
  const chart = echarts.init(classBarRef.value)
  const colors = ['#10b981', '#0ea5e9', '#f59e0b', '#64748b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#6366f1']
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: classComparison.value.clusterNames, bottom: 0 },
    color: colors,
    grid: { left: 60, right: 30, bottom: 50, top: 20 },
    xAxis: { type: 'category', data: classComparison.value.classes, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '学生人数' },
    series: classComparison.value.series.map((s, i) => ({
      name: s.name,
      type: 'bar',
      stack: 'total',
      data: s.data,
      itemStyle: { color: colors[i % colors.length] }
    }))
  })
  onResize(() => chart.resize())
}
</script>

<style scoped>
.dashboard { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; }
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

<template>
  <div class="page-container">
    <div v-if="evaluations.length === 0" class="empty-tip">
      <el-empty description="暂无评价数据" />
    </div>
    <template v-else>
      <!-- 最新学期评价卡片 -->
      <div class="eval-card" v-if="latest">
        <el-row :gutter="20">
          <el-col :span="12">
            <h3>{{ latest.semester }} 学期综合素质评价</h3>
            <el-descriptions :column="1" border style="margin-top: 12px">
              <el-descriptions-item label="品德发展">{{ latest.moralScore }}分</el-descriptions-item>
              <el-descriptions-item label="学业发展">{{ latest.academicScore }}分</el-descriptions-item>
              <el-descriptions-item label="体能发展">{{ latest.physicalScore }}分</el-descriptions-item>
              <el-descriptions-item label="艺术素养">{{ latest.artScore }}分</el-descriptions-item>
              <el-descriptions-item label="实践创新">{{ latest.practiceScore }}分</el-descriptions-item>
              <el-descriptions-item label="综合总分"><el-tag type="success" effect="dark" size="large">{{ latest.totalScore }}分</el-tag></el-descriptions-item>
              <el-descriptions-item label="聚类类别"><el-tag>{{ latest.clusterName || '未分类' }}</el-tag></el-descriptions-item>
            </el-descriptions>
          </el-col>
          <el-col :span="12">
            <div ref="radarRef" style="height: 300px"></div>
          </el-col>
        </el-row>
        <div v-if="latest.suggestion" class="suggestion-box">
          <h4>个性化发展建议</h4>
          <p>{{ latest.suggestion }}</p>
        </div>
      </div>

      <!-- 成长轨迹 -->
      <div class="eval-card" style="margin-top: 16px">
        <h3>成长轨迹</h3>
        <div ref="growthRef" style="height: 350px; margin-top: 12px"></div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const evaluations = ref([])
const radarRef = ref(), growthRef = ref()
const latest = computed(() => evaluations.value.length > 0 ? evaluations.value[evaluations.value.length - 1] : null)

onMounted(async () => {
  try {
    const res = await request.get('/api/evaluation/comprehensive/my')
    evaluations.value = res.data
    if (evaluations.value.length > 0) nextTick(() => renderCharts())
  } catch (e) { /* 忽略 */ }
})

function renderCharts() {
  const l = latest.value
  if (l) {
    const radar = echarts.init(radarRef.value)
    radar.setOption({
      radar: { indicator: [{ name: '品德', max: 100 }, { name: '学业', max: 100 }, { name: '体能', max: 100 }, { name: '艺术', max: 100 }, { name: '实践', max: 100 }] },
      series: [{ type: 'radar', data: [{ value: [l.moralScore, l.academicScore, l.physicalScore, l.artScore, l.practiceScore], areaStyle: { color: 'rgba(14,165,233,0.2)' }, lineStyle: { color: '#0ea5e9' }, itemStyle: { color: '#0ea5e9' } }] }]
    })
  }
  const growth = echarts.init(growthRef.value)
  growth.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['品德', '学业', '体能', '艺术', '实践', '综合'] },
    xAxis: { type: 'category', data: evaluations.value.map(d => d.semester) },
    yAxis: { type: 'value', min: 40, max: 100 },
    series: [
      { name: '品德', type: 'line', data: evaluations.value.map(d => d.moralScore), smooth: true },
      { name: '学业', type: 'line', data: evaluations.value.map(d => d.academicScore), smooth: true },
      { name: '体能', type: 'line', data: evaluations.value.map(d => d.physicalScore), smooth: true },
      { name: '艺术', type: 'line', data: evaluations.value.map(d => d.artScore), smooth: true },
      { name: '实践', type: 'line', data: evaluations.value.map(d => d.practiceScore), smooth: true },
      { name: '综合', type: 'line', data: evaluations.value.map(d => d.totalScore), smooth: true, lineStyle: { width: 3 } }
    ]
  })
}
</script>

<style scoped>
.page-container { }
.eval-card { background: #fff; border-radius: 10px; padding: 24px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.eval-card h3 { font-size: 16px; color: #1e293b; }
.suggestion-box { margin-top: 16px; padding: 16px; background: #f0f9ff; border-radius: 8px; border-left: 4px solid #0ea5e9; }
.suggestion-box h4 { margin-bottom: 8px; color: #0369a1; }
.suggestion-box p { color: #334155; line-height: 1.6; }
</style>

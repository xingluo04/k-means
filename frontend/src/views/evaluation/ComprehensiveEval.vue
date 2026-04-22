<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="query.semester" placeholder="选择学期" clearable style="width: 140px" @change="loadData"><el-option label="2024-1" value="2024-1" /><el-option label="2024-2" value="2024-2" /></el-select>
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
      <el-table-column prop="semester" label="学期" width="90" />
      <el-table-column prop="moralScore" label="品德" width="70" />
      <el-table-column prop="academicScore" label="学业" width="70" />
      <el-table-column prop="physicalScore" label="体能" width="70" />
      <el-table-column prop="artScore" label="艺术" width="70" />
      <el-table-column prop="practiceScore" label="实践" width="70" />
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="综合素质评价详情" width="700px">
      <div v-if="detailData" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生姓名">{{ detailData.studentName }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ detailData.studentNo }}</el-descriptions-item>
          <el-descriptions-item label="学期">{{ detailData.semester }}</el-descriptions-item>
          <el-descriptions-item label="聚类类别"><el-tag :type="clusterType(detailData.clusterLabel)">{{ detailData.clusterName || '未分类' }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="品德发展">{{ detailData.moralScore }}分</el-descriptions-item>
          <el-descriptions-item label="学业发展">{{ detailData.academicScore }}分</el-descriptions-item>
          <el-descriptions-item label="体能发展">{{ detailData.physicalScore }}分</el-descriptions-item>
          <el-descriptions-item label="艺术素养">{{ detailData.artScore }}分</el-descriptions-item>
          <el-descriptions-item label="实践创新">{{ detailData.practiceScore }}分</el-descriptions-item>
          <el-descriptions-item label="综合总分"><el-tag type="success" effect="dark" size="large">{{ detailData.totalScore }}分</el-tag></el-descriptions-item>
        </el-descriptions>
        <div ref="detailRadarRef" style="height: 300px; margin-top: 16px"></div>
        <div v-if="detailData.suggestion" class="suggestion-box">
          <h4>个性化发展建议</h4>
          <p>{{ detailData.suggestion }}</p>
        </div>
      </div>
    </el-dialog>

    <!-- 成长轨迹弹窗 -->
    <el-dialog v-model="growthVisible" title="成长轨迹" width="800px">
      <div ref="growthChartRef" style="height: 400px"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const loading = ref(false), tableData = ref([]), total = ref(0)
const detailVisible = ref(false), growthVisible = ref(false), detailData = ref(null)
const detailRadarRef = ref(), growthChartRef = ref()
const query = reactive({ current: 1, size: 10, semester: '', keyword: '', clusterLabel: null })

const clusterType = (label) => ({ 0: 'success', 1: '', 2: 'warning', 3: 'info', 4: 'danger' }[label] || '')

onMounted(() => loadData())

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
      radar: { indicator: [{ name: '品德', max: 100 }, { name: '学业', max: 100 }, { name: '体能', max: 100 }, { name: '艺术', max: 100 }, { name: '实践', max: 100 }] },
      series: [{ type: 'radar', data: [{ value: [row.moralScore, row.academicScore, row.physicalScore, row.artScore, row.practiceScore], name: row.studentName, areaStyle: { color: 'rgba(14,165,233,0.2)' }, lineStyle: { color: '#0ea5e9' }, itemStyle: { color: '#0ea5e9' } }] }]
    })
  })
}

async function viewGrowth(row) {
  growthVisible.value = true
  const res = await request.get(`/api/evaluation/comprehensive/growth/${row.studentId}`)
  nextTick(() => {
    const chart = echarts.init(growthChartRef.value)
    const data = res.data
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['品德', '学业', '体能', '艺术', '实践', '综合'] },
      xAxis: { type: 'category', data: data.map(d => d.semester) },
      yAxis: { type: 'value', min: 40, max: 100 },
      series: [
        { name: '品德', type: 'line', data: data.map(d => d.moralScore), smooth: true },
        { name: '学业', type: 'line', data: data.map(d => d.academicScore), smooth: true },
        { name: '体能', type: 'line', data: data.map(d => d.physicalScore), smooth: true },
        { name: '艺术', type: 'line', data: data.map(d => d.artScore), smooth: true },
        { name: '实践', type: 'line', data: data.map(d => d.practiceScore), smooth: true },
        { name: '综合', type: 'line', data: data.map(d => d.totalScore), smooth: true, lineStyle: { width: 3 } }
      ]
    })
  })
}
</script>

<style scoped>
.page-container { background: #fff; border-radius: 10px; padding: 20px; }
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.suggestion-box { margin-top: 16px; padding: 16px; background: #f0f9ff; border-radius: 8px; border-left: 4px solid #0ea5e9; }
.suggestion-box h4 { margin-bottom: 8px; color: #0369a1; }
.suggestion-box p { color: #334155; line-height: 1.6; }
</style>

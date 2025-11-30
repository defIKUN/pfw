<template>
  <div class="dashboard">
    <el-row :gutter="20" class="statistics-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409eff;">
              <el-icon><Picture /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.todayImageCount || 0 }}</div>
              <div class="stat-label">今日识别图片数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67c23a;">
              <el-icon><VideoPlay /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.todayVideoCount || 0 }}</div>
              <div class="stat-label">今日识别视频数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #e6a23c;">
              <el-icon><Box /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalDetections || 0 }}</div>
              <div class="stat-label">识别漂浮物总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>历史识别数量趋势</span>
              <el-select v-model="days" @change="loadCharts" style="width: 120px">
                <el-option label="最近7天" :value="7" />
                <el-option label="最近30天" :value="30" />
                <el-option label="最近90天" :value="90" />
              </el-select>
            </div>
          </template>
          <v-chart :option="trendOption" style="height: 300px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>漂浮物类别比例</span>
          </template>
          <v-chart :option="pieOption" style="height: 300px" />
        </el-card>
      </el-col>
    </el-row>

    <el-card class="latest-card">
      <template #header>
        <span>最新识别结果</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="12">
          <h3>最新图片识别</h3>
          <el-empty v-if="latestImages.length === 0" description="暂无数据" />
          <div v-else class="image-list">
            <div
              v-for="image in latestImages"
              :key="image.id"
              class="image-item"
              @click="viewDetail(image.id, 0)"
            >
              <img :src="getImageUrl(image.imagePath)" alt="识别结果" />
              <div class="image-info">
                <div>{{ formatTime(image.createTime) }}</div>
                <el-tag :type="getStatusType(image.status)">{{ getStatusText(image.status) }}</el-tag>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="12">
          <h3>最新视频识别</h3>
          <el-empty v-if="latestVideos.length === 0" description="暂无数据" />
          <div v-else class="video-list">
            <div
              v-for="video in latestVideos"
              :key="video.id"
              class="video-item"
              @click="viewDetail(video.id, 1)"
            >
              <div class="video-thumb">
                <video
                  v-if="videoSrc(video)"
                  :src="videoSrc(video)"
                  muted
                  loop
                  autoplay
                  playsinline
                />
                <div class="video-placeholder" v-else>
                <el-icon><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="video-info">
                <div>{{ formatTime(video.createTime) }}</div>
                <el-tag :type="getStatusType(video.status)">{{ getStatusText(video.status) }}</el-tag>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import api from '@/api'
import { Picture, VideoPlay, Box } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()

const statistics = ref({})
const days = ref(30)
const trendData = ref([])
const pieData = ref([])
const latestImages = ref([])
const latestVideos = ref([])

const trendOption = computed(() => ({
  tooltip: {
    trigger: 'axis'
  },
  xAxis: {
    type: 'category',
    data: trendData.value.map(item => item.date)
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '识别数量',
      type: 'line',
      data: trendData.value.map(item => item.totalCount),
      smooth: true
    }
  ]
}))

const pieOption = computed(() => ({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    orient: 'vertical',
    left: 'left'
  },
  series: [
    {
      type: 'pie',
      radius: '50%',
      data: pieData.value,
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
}))

const loadStatistics = async () => {
  try {
    const response = await api.get('/dashboard/statistics')
    if (response.data && response.data.code === 200) {
      statistics.value = response.data.data || {}
    } else {
      console.error('加载统计信息失败：', response.data?.message || '未知错误')
      ElMessage.warning('加载统计信息失败，使用默认值')
      statistics.value = { todayImageCount: 0, todayVideoCount: 0, totalDetections: 0 }
    }
  } catch (error) {
    console.error('加载统计信息失败', error)
    const errorMsg = error.response?.data?.message || error.message || '网络错误，请检查后端服务是否运行'
    ElMessage.error(`加载统计信息失败：${errorMsg}`)
    statistics.value = { todayImageCount: 0, todayVideoCount: 0, totalDetections: 0 }
  }
}

const loadCharts = async () => {
  try {
    const response = await api.get('/dashboard/charts', { params: { days: days.value } })
    if (response.data && response.data.code === 200) {
      const data = response.data.data || {}
      trendData.value = data.trendData || []
      pieData.value = data.pieData || []
    } else {
      console.error('加载图表数据失败：', response.data?.message || '未知错误')
      trendData.value = []
      pieData.value = []
    }
  } catch (error) {
    console.error('加载图表数据失败', error)
    trendData.value = []
    pieData.value = []
  }
}

const loadLatest = async () => {
  try {
    const response = await api.get('/dashboard/latest')
    if (response.data && response.data.code === 200) {
      const data = response.data.data || {}
      latestImages.value = data.images || []
      latestVideos.value = data.videos || []
    } else {
      console.error('加载最新结果失败：', response.data?.message || '未知错误')
      latestImages.value = []
      latestVideos.value = []
    }
  } catch (error) {
    console.error('加载最新结果失败', error)
    latestImages.value = []
    latestVideos.value = []
  }
}

const getImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  // 兼容以 /uploads 开头或相对 uploads 目录
  const p = path.replace(/^\.\//, '')
  return p.startsWith('uploads') ? `/${p}` : `/${p}`
}

const getFileUrl = (p) => {
  if (!p) return ''
  if (p.startsWith('http')) return p
  const fixed = p.replace(/^\.\//, '/')
  // 确保以 /uploads 开头，vite 代理会转发到后端
  return fixed.startsWith('/uploads') ? fixed : `/uploads/${fixed.replace(/^\/?uploads\/?/, '')}`
}

const videoSrc = (video) => {
  return getFileUrl(video?.resultPath || video?.videoPath)
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const getStatusText = (status) => {
  const statusMap = {
    0: '未开始',
    1: '进行中',
    2: '已完成',
    3: '失败'
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status) => {
  const typeMap = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger'
  }
  return typeMap[status] || 'info'
}

const viewDetail = (id, taskType) => {
  router.push({
    name: 'Recognize',
    query: { taskId: id, taskType }
  })
}

onMounted(() => {
  loadStatistics()
  loadCharts()
  loadLatest()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.statistics-row {
  margin-bottom: 20px;
}

.stat-card {
  height: 120px;
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  margin-right: 20px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.charts-row {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.latest-card {
  margin-top: 20px;
}

.image-list,
.video-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
}

.image-item,
.video-item {
  cursor: pointer;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  transition: all 0.3s;
}

.image-item:hover,
.video-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.image-item img {
  width: 100%;
  height: 120px;
  object-fit: cover;
}

.video-thumb {
  width: 100%;
  height: 120px;
  background-color: #000;
  overflow: hidden;
}
.video-thumb video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.video-placeholder {
  width: 100%;
  height: 120px;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: #909399;
}

.image-info,
.video-info {
  padding: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}
</style>


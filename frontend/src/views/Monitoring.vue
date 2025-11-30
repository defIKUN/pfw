<template>
  <div class="monitor-page">
    <el-card class="hero-card" shadow="never">
      <div class="hero-left">
        <h2>多地区监控调度中心</h2>
        <p>利用识别完成的视频模拟各河段实时监控，快速定位高风险水域</p>
        <div class="hero-stats">
          <div class="stat">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">已配置地区</div>
          </div>
          <div class="stat">
            <div class="stat-value">{{ stats.online }}</div>
            <div class="stat-label">在线监控</div>
          </div>
          <div class="stat">
            <div class="stat-value">{{ stats.offline }}</div>
            <div class="stat-label">离线监控</div>
          </div>
        </div>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增地区监控
        </el-button>
        <el-button @click="loadMonitors" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="success" @click="openMap()">
          <el-icon><Location /></el-icon>
          监控地图
        </el-button>
      </div>
    </el-card>

    <el-empty v-if="!loading && monitors.length === 0" description="暂无监控地区，点击右上角新增" />

    <el-row v-else :gutter="16" class="monitor-grid">
      <el-col v-for="item in monitors" :key="item.id" :xl="6" :lg="8" :md="12" :sm="24">
        <el-card class="monitor-card" shadow="hover">
          <div class="card-cover" @click="openDetail(item)">
            <video
              v-if="item.resultPath"
              :src="fileUrl(item.resultPath)"
              muted
              loop
              autoplay
              playsinline
            />
            <div class="cover-placeholder" v-else>
              <el-icon><VideoCamera /></el-icon>
              <span>暂无视频</span>
            </div>
            <el-tag class="status-tag" :type="statusTag(item.status)">{{ statusText(item.status) }}</el-tag>
          </div>
          <div class="card-body">
            <div class="card-title">{{ item.name }}</div>
            <div class="card-region">
              <el-icon><Location /></el-icon>
              <span>{{ item.region }}</span>
            </div>
            <div class="card-desc">{{ item.description || '暂无描述' }}</div>
            <div class="card-summary" v-if="item.summaryMap && Object.keys(item.summaryMap).length">
              <el-tag
                v-for="(count, name) in item.summaryMap"
                :key="name"
                size="small"
                class="mr-1"
                type="success"
              >
                {{ name }} × {{ count }}
              </el-tag>
            </div>
            <div class="card-footer">
              <span class="last-detect">
                <el-icon><Clock /></el-icon>
                {{ formatTime(item.lastDetectTime) || '暂无检测记录' }}
              </span>
              <div class="actions">
                <el-button size="small" type="primary" text @click="openDetail(item)">查看监控</el-button>
                <el-popconfirm title="确认删除该监控？" confirm-button-text="删除" cancel-button-text="取消" @confirm="removeMonitor(item)">
                  <template #reference>
                    <el-button size="small" type="danger" text @click.stop>删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增监控 -->
    <el-dialog v-model="createVisible" title="新增地区监控" width="520px" destroy-on-close>
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="地区名称" prop="name">
          <el-input v-model="createForm.name" placeholder="如：秦淮河-燕子矶段" />
        </el-form-item>
        <el-form-item label="所属区域" prop="region">
          <el-input v-model="createForm.region" placeholder="行政区 / 河段" />
        </el-form-item>
        <el-form-item label="地名搜索">
          <div style="display:flex; gap:8px; width:100%;">
            <el-input v-model="placeQuery" placeholder="输入地名（如：中山市博爱路）" style="flex:2" />
            <el-input v-model="placeCity" placeholder="城市(可选，如：中山市)" style="flex:1" />
            <el-button type="primary" @click="searchByPlace">搜索</el-button>
          </div>
          <div class="form-tip">支持高德地理编码。也可使用下方“地图选择”或手动填写坐标。</div>
        </el-form-item>
        <el-form-item label="位置坐标">
          <div style="display:flex; gap:8px; width:100%;">
            <el-input v-model="createForm.latitude" placeholder="纬度" style="flex:1" />
            <el-input v-model="createForm.longitude" placeholder="经度" style="flex:1" />
            <el-button type="primary" text @click="openMapPick">地图选择</el-button>
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="2"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="监控状态">
          <el-radio-group v-model="createForm.status">
            <el-radio :label="1">在线</el-radio>
            <el-radio :label="0">离线</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="绑定识别任务" prop="videoTaskId">
          <el-select
            v-model="createForm.videoTaskId"
            filterable
            placeholder="选择已完成的视频识别任务"
            :loading="taskLoading"
            @visible-change="handleTaskPanel"
          >
            <el-option
              v-for="task in availableTasks"
              :key="task.id"
              :label="taskOptionLabel(task)"
              :value="task.id"
            />
          </el-select>
          <div class="form-tip">
            选择一个已完成并有结果的视频任务作为该地区监控的视频源
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 监控详情 -->
    <el-drawer v-model="detailVisible" :title="detailTitle" size="60%">
      <template v-if="detail">
        <div class="detail-layout">
          <div class="detail-video">
            <video
              v-if="detailVideoUrl"
              :src="detailVideoUrl"
              controls
              autoplay
              loop
              playsinline
            />
            <el-empty v-else description="当前监控暂无视频" />
          </div>
          <div class="detail-meta">
            <div class="meta-item">
              <span class="meta-label">所属地区</span>
              <span>{{ detail.monitor?.region || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">当前状态</span>
              <el-tag :type="statusTag(detail.monitor?.status)">{{ statusText(detail.monitor?.status) }}</el-tag>
            </div>
            <div class="meta-item">
              <span class="meta-label">最近检测</span>
              <span>{{ formatTime(detail.monitor?.lastDetectTime) || '暂无' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">绑定任务</span>
              <span v-if="detail.task">#{{ detail.task.id }}（{{ getTaskStatus(detail.task.status) }}）</span>
              <span v-else>未绑定</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">监控说明</span>
              <span>{{ detail.monitor?.description || '暂无' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">导航</span>
              <span><el-button size="small" type="primary" plain @click="navigateToMonitor(detail.monitor)">导航到此</el-button></span>
            </div>
          </div>
        </div>
        <div class="detail-summary">
          <h4>识别汇总</h4>
          <div class="summary-tags" v-if="detail.summary && Object.keys(detail.summary).length">
            <el-tag
              v-for="(count, name) in detail.summary"
              :key="name"
              type="success"
              effect="dark"
            >
              {{ name }} × {{ count }}
            </el-tag>
          </div>
          <el-text v-else type="info">暂未产生识别结果</el-text>
        </div>
        <el-table :data="detail.detections || []" height="260" size="small" stripe>
          <el-table-column prop="objectClass" label="类别" width="160" />
          <el-table-column
            prop="confidence"
            label="置信度"
            width="120"
            :formatter="(_, __, cellValue) => (cellValue ? (cellValue * 100).toFixed(2) + '%' : '-')"
          />
          <el-table-column prop="createTime" label="时间" :formatter="(_, __, cellValue) => formatTime(cellValue)" />
        </el-table>
      </template>
      <template v-else>
        <el-skeleton :rows="8" animated />
      </template>
    </el-drawer>

    <!-- 地图弹窗：浏览与选点复用 -->
    <MonitorMap
      v-model="mapVisible"
      :allow-pick="mapPickMode"
      :monitors="monitors"
      :center="[22.52, 113.38]"
      :zoom="11"
      :amap-key="amapKey" :tile-url="tileUrl || '/api/map/amap/{z}/{x}/{y}.png'" :route-path-gcj="routePathGcj"
      @picked="onMapPicked"
      @delete="removeMonitor"
      @view="openDetail"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, VideoCamera, Location, Clock } from '@element-plus/icons-vue'
import api from '@/api'
import MonitorMap from '@/components/MonitorMap.vue'

// 坐标系转换：WGS84 <-> GCJ-02（与地图组件一致）
const pi = Math.PI
const a = 6378245.0
const ee = 0.00669342162296594323
const outOfChina = (lat, lng) => (lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271)
function transformLat(x, y) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0
  return ret
}
function transformLng(x, y) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0
  return ret
}
function wgs84ToGcj02(lat, lng) {
  if (outOfChina(lat, lng)) return [lat, lng]
  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = lat / 180.0 * pi
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi)
  dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi)
  const mgLat = lat + dLat
  const mgLng = lng + dLng
  return [mgLat, mgLng]
}
function gcj02ToWgs84(lat, lng) {
  if (outOfChina(lat, lng)) return [lat, lng]
  const [mgLat, mgLng] = wgs84ToGcj02(lat, lng)
  const dLat = mgLat - lat
  const dLng = mgLng - lng
  return [lat - dLat, lng - dLng]
}

const monitors = ref([])
const loading = ref(false)
const detail = ref(null)
const detailVisible = ref(false)
const createVisible = ref(false)
const createFormRef = ref(null)
const createSubmitting = ref(false)

const createForm = reactive({
  name: '',
  region: '',
  latitude: null,
  longitude: null,
  description: '',
  status: 1,
  videoTaskId: null
})

const createRules = {
  name: [{ required: true, message: '请输入地区名称', trigger: 'blur' }],
  region: [{ required: true, message: '请输入所属区域', trigger: 'blur' }],
  videoTaskId: [{ required: true, message: '请选择已完成的视频任务', trigger: 'change' }]
}

const availableTasks = ref([])
const taskLoading = ref(false)

// 地名搜索
const placeQuery = ref('')
const placeCity = ref('中山市')

// 路径展示（GCJ-02 坐标点数组）
const routePathGcj = ref([])

// 地图相关
const mapVisible = ref(false)
const mapPickMode = ref(false)
// 高德地图 Web 瓦片 Key（优先读取环境变量）
const amapKey = import.meta.env.VITE_AMAP_KEY || 'fd57f50c370599504187ca0c4a673328'
// 自定义瓦片服务（可选）：如内网代理 http://your-host/tiles/{z}/{x}/{y}.png
const tileUrl = import.meta.env.VITE_TILE_URL || ''

const stats = computed(() => {
  const total = monitors.value.length
  const online = monitors.value.filter(item => item.status === 1).length
  return {
    total,
    online,
    offline: total - online
  }
})

const detailTitle = computed(() => detail.value?.monitor?.name || '监控详情')

const detailVideoUrl = computed(() => {
  const monitor = detail.value?.monitor
  if (!monitor) return ''
  return fileUrl(monitor.resultPath || monitor.videoPath)
})

function statusText(status) {
  if (status === 1) return '在线'
  if (status === 0) return '离线'
  return '未知'
}

function statusTag(status) {
  return status === 1 ? 'success' : 'info'
}

function parseSummary(summary) {
  if (!summary) return {}
  if (typeof summary === 'object') return summary
  try {
    return JSON.parse(summary)
  } catch (e) {
    return {}
  }
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

function fileUrl(p) {
  if (!p) return ''
  if (p.startsWith('http')) return p
  return p.replace(/^\.\//, '/').replace(/^uploads/, '/uploads')
}

function getTaskStatus(status) {
  switch (status) {
    case 0:
      return '未开始'
    case 1:
      return '进行中'
    case 2:
      return '已完成'
    case 3:
      return '失败'
    default:
      return '未知'
  }
}

async function loadMonitors() {
  loading.value = true
  try {
    const { data } = await api.get('/monitor/sources')
    if (data.code === 200) {
      monitors.value = (data.data || []).map(item => ({
        ...item,
        latitude: item.latitude !== undefined && item.latitude !== null && item.latitude !== '' ? Number(item.latitude) : null,
        longitude: item.longitude !== undefined && item.longitude !== null && item.longitude !== '' ? Number(item.longitude) : null,
        summaryMap: parseSummary(item.summary)
      }))
    }
  } catch (e) {
    ElMessage.error('加载监控列表失败')
  } finally {
    loading.value = false
  }
}

async function openDetail(monitor) {
  try {
    const { data } = await api.get(`/monitor/sources/${monitor.id}`)
    if (data.code === 200) {
      detail.value = data.data
      detailVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取监控详情失败')
  }
}

function resetCreateForm() {
  createForm.name = ''
  createForm.region = ''
  createForm.latitude = null
  createForm.longitude = null
  createForm.description = ''
  createForm.status = 1
  createForm.videoTaskId = null
}

function openCreate() {
  resetCreateForm()
  createVisible.value = true
  loadTaskOptions()
}

function openMap() {
  mapPickMode.value = false
  mapVisible.value = true
}

function openMapPick() {
  mapPickMode.value = true
  mapVisible.value = true
}

function onMapPicked(pos) {
  if (!pos) return
  createForm.latitude = Number(pos.latitude.toFixed(6))
  createForm.longitude = Number(pos.longitude.toFixed(6))
  // 若未打开新增弹窗，则打开且不重置表单
  if (!createVisible.value) {
    createVisible.value = true
    loadTaskOptions()
  }
  ElMessage.success('已选择地图位置')
}

// 地名搜索 -> 经纬度（WGS84）
async function searchByPlace() {
  const query = (placeQuery.value || '').trim()
  if (!query) {
    ElMessage.warning('请输入地名')
    return
  }
  try {
    const { data } = await api.get('/map/geocode', { params: { address: query, city: placeCity.value || undefined } })
    const resp = typeof data === 'string' ? JSON.parse(data) : data
    if (String(resp.status) === '1' && Array.isArray(resp.geocodes) && resp.geocodes.length) {
      const loc = resp.geocodes[0].location // GCJ-02: "lng,lat"
      const [lng, lat] = loc.split(',').map(Number)
      const [wLat, wLng] = gcj02ToWgs84(lat, lng)
      createForm.latitude = Number(wLat.toFixed(6))
      createForm.longitude = Number(wLng.toFixed(6))
      ElMessage.success('已定位地名并回填坐标')
      if (!createVisible.value) {
        createVisible.value = true
        loadTaskOptions()
      }
    } else {
      ElMessage.error(resp.info || '未找到该地名，请尝试更精确的名称')
    }
  } catch (e) {
    ElMessage.error('地名搜索失败')
  }
}

// 浏览器定位（WGS84）
function getBrowserLocation() {
  return new Promise((resolve) => {
    if (!navigator.geolocation) return resolve(null)
    navigator.geolocation.getCurrentPosition(
      pos => {
        resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude })
      },
      () => resolve(null),
      { enableHighAccuracy: true, timeout: 6000 }
    )
  })
}

// 导航到监控：绘制路线
async function navigateToMonitor(monitor) {
  if (!monitor || monitor.latitude == null || monitor.longitude == null) {
    ElMessage.warning('该监控没有坐标，无法导航')
    return
  }
  try {
    // 获取起点（WGS84），若失败使用中山市中心
    const cur = await getBrowserLocation()
    const originWgs = cur || { lat: 22.52, lng: 113.38 }
    const [oLat, oLng] = wgs84ToGcj02(originWgs.lat, originWgs.lng)
    const [dLat, dLng] = wgs84ToGcj02(Number(monitor.latitude), Number(monitor.longitude))
    const origin = `${oLng},${oLat}`
    const destination = `${dLng},${dLat}`

    const { data } = await api.get('/map/direction', { params: { origin, destination } })
    const resp = typeof data === 'string' ? JSON.parse(data) : data
    const path = (resp?.route?.paths?.[0]?.steps || [])
      .flatMap(step => String(step.polyline || '').split(';'))
      .map(p => p.split(',').map(Number))
      .filter(arr => arr.length === 2)
      .map(([lng, lat]) => [lat, lng]) // 转成 [lat,lng] 供 Leaflet

    if (!path.length) {
      ElMessage.error('未获取到可用路线')
      return
    }
    routePathGcj.value = path
    mapPickMode.value = false
    mapVisible.value = true
    ElMessage.success('已生成路线，已在地图中展示')
  } catch (e) {
    ElMessage.error('导航规划失败')
  }
}

async function handleCreate() {
  if (!createFormRef.value) return
  try {
    await createFormRef.value.validate()
  } catch (e) {
    return
  }
  createSubmitting.value = true
  try {
    const payload = {
      name: createForm.name,
      region: createForm.region,
      latitude: createForm.latitude ? Number(createForm.latitude) : null,
      longitude: createForm.longitude ? Number(createForm.longitude) : null,
      description: createForm.description,
      status: createForm.status,
      videoTaskId: createForm.videoTaskId
    }
    const { data } = await api.post('/monitor/sources', payload)
    if (data.code === 200) {
      ElMessage.success('新增监控成功')
      createVisible.value = false
      loadMonitors()
    } else {
      ElMessage.error(data.message || '新增失败')
    }
  } catch (e) {
    ElMessage.error('新增监控失败')
  } finally {
    createSubmitting.value = false
  }
}

async function loadTaskOptions() {
  taskLoading.value = true
  try {
    const { data } = await api.get('/tasks', { params: { page: 1, size: 50, taskType: 1, status: 2 } })
    if (data.code === 200) {
      availableTasks.value = data.data.records || []
    }
  } catch (e) {
    // ignore
  } finally {
    taskLoading.value = false
  }
}

function handleTaskPanel(visible) {
  if (visible && availableTasks.value.length === 0) {
    loadTaskOptions()
  }
}

function taskOptionLabel(task) {
  const time = task.createTime ? new Date(task.createTime).toLocaleString('zh-CN') : ''
  return `#${task.id} · ${time}`
}

async function removeMonitor(item) {
  if (!item || !item.id) return
  try {
    const { data } = await api.delete(`/monitor/sources/${item.id}`)
    if (data.code === 200) {
      ElMessage.success('删除成功')
      // 从本地列表移除，避免再发一次全量请求
      monitors.value = monitors.value.filter(m => m.id !== item.id)
      // 如果当前抽屉展示的是被删除的监控，顺便关闭
      if (detailVisible.value && detail.value?.monitor?.id === item.id) {
        detailVisible.value = false
      }
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadMonitors()
})
</script>

<style scoped>
.monitor-page {
  padding: 12px;
}
.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.hero-left h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.hero-left p {
  margin: 6px 0 16px;
  color: #606266;
}
.hero-stats {
  display: flex;
  gap: 24px;
}
.stat {
  min-width: 120px;
}
.stat-value {
  font-size: 30px;
  font-weight: 600;
  color: #409eff;
}
.stat-label {
  color: #909399;
  font-size: 13px;
}
.hero-actions {
  display: flex;
  gap: 12px;
}
.monitor-grid {
  margin-top: 8px;
}
.monitor-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}
.monitor-card:hover {
  transform: translateY(-4px);
}
.card-cover {
  position: relative;
  height: 180px;
  background: #000;
  border-radius: 6px;
  overflow: hidden;
}
.card-cover video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  gap: 6px;
}
.status-tag {
  position: absolute;
  top: 12px;
  left: 12px;
}
.card-body {
  padding-top: 12px;
}
.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}
.card-region {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 13px;
  margin-bottom: 6px;
}
.card-desc {
  color: #909399;
  font-size: 13px;
  min-height: 36px;
}
.card-summary {
  margin: 8px 0;
}
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #606266;
  font-size: 12px;
}
.last-detect {
  display: flex;
  align-items: center;
  gap: 4px;
}
.detail-layout {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.detail-video {
  flex: 2;
}
.detail-video video {
  width: 100%;
  height: 320px;
  border-radius: 8px;
  object-fit: contain;
  background: #000;
}
.detail-meta {
  flex: 1;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.meta-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
}
.meta-label {
  color: #909399;
}
.detail-summary {
  margin-bottom: 12px;
}
.summary-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
@media (max-width: 1024px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
    margin-top: 12px;
  }
  .detail-layout {
    flex-direction: column;
  }
}
</style>

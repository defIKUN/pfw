<template>
  <el-dialog v-model="visibleInner" :title="dialogTitle" width="70%" :destroy-on-close="false" @opened="onOpened" @closed="onClosed">
    <div class="map-wrapper">
      <div :key="visibleInner ? 1 : 0" ref="mapEl" class="map-container"></div>
      <!-- 统计面板：右上角 -->
      <div class="stats-panel">
        <el-card shadow="hover" class="stats-card">
          <div class="stat-row">
            <span class="stat-label">告警点</span>
            <span class="stat-value alarm">{{ alarmCount }}</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">正常点</span>
            <span class="stat-value normal">{{ normalCount }}</span>
          </div>
        </el-card>
      </div>
    </div>
    <template #footer>
      <div class="map-legend">
        <span class="legend-item"><span class="dot green"></span> 无告警</span>
        <span class="legend-item"><span class="dot red"></span> 有告警</span>
      </div>
      <div class="spacer"></div>
      <el-button @click="visibleInner = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import 'leaflet/dist/leaflet.css'
import * as L from 'leaflet'

// --- 坐标系转换：WGS84 <-> GCJ-02（高德）---
const pi = Math.PI
const a = 6378245.0
const ee = 0.00669342162296594323

function outOfChina(lat, lng) {
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
}
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
// 输入/输出均为 [lat, lng]
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

// 动态加载高德 JS API
function loadAmapSdk(key) {
  return new Promise((resolve, reject) => {
    if (window.AMap) return resolve(window.AMap)
    const s = document.createElement('script')
    s.src = `https://webapi.amap.com/maps?v=2.0&key=${key}`
    s.async = true
    s.onerror = () => reject(new Error('AMap SDK load error'))
    s.onload = () => window.AMap ? resolve(window.AMap) : reject(new Error('AMap not available'))
    document.head.appendChild(s)
    setTimeout(() => { if (!window.AMap) reject(new Error('AMap SDK timeout')) }, 8000)
  })
}

let amap = null
let amapMarkers = []
let tempAmapMarker = null

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  monitors: { type: Array, default: () => [] },
  allowPick: { type: Boolean, default: false },
  center: { type: Array, default: () => [22.52, 113.38] }, // 中山市大致中心（WGS84）
  zoom: { type: Number, default: 11 },
  amapKey: { type: String, default: '' }, // 高德 Web 瓦片 KEY（可为空）
  tileUrl: { type: String, default: '' }, // 自定义内网/本地瓦片地址模板，如 http://host/tiles/{z}/{x}/{y}.png
  routePathGcj: { type: Array, default: () => [] } // 路径规划坐标（GCJ-02，数组：[lat,lng]
})

const emits = defineEmits(['update:modelValue', 'picked', 'delete', 'view'])

const visibleInner = ref(props.modelValue)
watch(() => props.modelValue, v => visibleInner.value = v)
watch(visibleInner, v => emits('update:modelValue', v))

const dialogTitle = computed(() => props.allowPick ? '监控地图（选择位置）' : '监控地图')

// 统计面板数据
const alarmCount = computed(() => {
  return (props.monitors || []).filter(m => {
    if (m.latitude == null || m.longitude == null) return false
    const color = buildStatusColor(m)
    return color === 'red'
  }).length
})

const normalCount = computed(() => {
  return (props.monitors || []).filter(m => {
    if (m.latitude == null || m.longitude == null) return false
    const color = buildStatusColor(m)
    return color === 'green'
  }).length
})

let map = null
let markersLayer = null
const mapEl = ref(null)
let tempMarker = null
let routeLayer = null
// 仅首次打开或需要时自适应范围，用户交互后不再自动 fit
let shouldAutoFit = false

function buildStatusColor(m) {
  const sum = m.summaryMap || m.summary || {}
  const hasPositive = Object.values(sum || {}).some(v => Number(v) > 0)
  return hasPositive ? 'red' : 'green'
}

function getDetectionCount(m) {
  const sum = m.summaryMap || m.summary || {}
  const total = Object.values(sum || {}).reduce((acc, v) => acc + Number(v || 0), 0)
  return total
}

function getFileUrl(p) {
  if (!p) return ''
  if (p.startsWith('http')) return p
  return p.replace(/^\.\//, '/').replace(/^uploads/, '/uploads')
}

function markerIcon(color, count, name, videoUrl) {
  // 高德地图POI样式：橙色圆形标记，带数字，下方有名称标签
  // 告警点融合监控视频画面
  const bgColor = color === 'red' ? '#ff6b35' : '#67c23a' // 橙色告警，绿色正常
  const displayCount = count > 0 ? count : 0
  const shortName = (name || '监控点').length > 8 ? (name || '监控点').substring(0, 8) + '...' : (name || '监控点')
  // 根据数字位数调整字体大小
  const fontSize = displayCount >= 100 ? '14px' : displayCount >= 10 ? '16px' : '18px'
  
  // 告警点且有视频时，将视频融合到标记中
  const hasVideo = color === 'red' && videoUrl
  const videoElement = hasVideo 
    ? `<video class="mm-video-bg" autoplay muted loop playsinline><source src="${videoUrl}" type="video/mp4"></video>`
    : ''
  const videoOverlay = hasVideo ? '<div class="mm-video-overlay"></div>' : ''
  
  const html = `
    <div class="mm-poi-marker">
      <div class="mm-rank-icon ${hasVideo ? 'has-video' : ''}" style="background-color: ${bgColor};">
        ${videoElement}
        ${videoOverlay}
        <span class="rank-num" style="font-size: ${fontSize};">${displayCount}</span>
      </div>
      <div class="mm-poi-label">${shortName}</div>
    </div>
  `
  return L.divIcon({
    html,
    className: 'mm-poi-div-icon',
    iconSize: [90, 70],
    iconAnchor: [45, 65],
    popupAnchor: [0, -65]
  })
}

function renderMarkers() {
  if (!map) return
  if (markersLayer) {
    markersLayer.clearLayers()
  } else {
    markersLayer = L.layerGroup().addTo(map)
  }
  const latlngs = []
  ;(props.monitors || []).forEach((m, index) => {
    if (m.latitude == null || m.longitude == null) return
    const color = buildStatusColor(m)
    const count = getDetectionCount(m)
    // 获取视频URL（告警点显示视频）
    const videoUrl = color === 'red' ? getFileUrl(m.resultPath || m.videoPath) : null
    // 后端/数据库使用 WGS84，展示在高德瓦片上需要转 GCJ-02
    const [gLat, gLng] = wgs84ToGcj02(m.latitude, m.longitude)
    latlngs.push([gLat, gLng])
    const mk = L.marker([gLat, gLng], { icon: markerIcon(color, count, m.name, videoUrl) })
    // 点击标记直接打开详情
    mk.on('click', () => emits('view', m))
    const popup = document.createElement('div')
    popup.className = 'mm-popup'
    popup.innerHTML = `
      <div class="title">${m.name || '未命名监控'}</div>
      <div class="sub">${m.region || ''}</div>
      <div class="sub">检测数量：${count}</div>
      <div class="btns">
        <button class="btn view">查看</button>
        <button class="btn del">删除</button>
      </div>
    `
    popup.querySelector('.view').addEventListener('click', () => emits('view', m))
    popup.querySelector('.del').addEventListener('click', () => emits('delete', m))
    mk.bindPopup(popup)
    mk.addTo(markersLayer)
    // 告警区域高亮：橙色点绘制半径圈
    if (color === 'red') {
      const circle = L.circle([gLat, gLng], { radius: 400, color: '#ff6b35', weight: 2, fillColor: '#ff6b35', fillOpacity: 0.15 })
      circle.addTo(markersLayer)
    }
  })
  // 若有标记且当前没有路线，自动缩放到标记范围
  if (latlngs.length && !routeLayer) {
    try { map.fitBounds(L.latLngBounds(latlngs), { padding: [20,20] }) } catch(_){}
  }
}

function renderRoute() {
  if (!map) return
  if (routeLayer) {
    map.removeLayer(routeLayer)
    routeLayer = null
  }
  const pts = (props.routePathGcj || []).filter(p => Array.isArray(p) && p.length === 2)
  if (pts.length >= 2) {
    routeLayer = L.polyline(pts, { color: '#409eff', weight: 4, opacity: 0.8 })
    routeLayer.addTo(map)
    const bounds = L.latLngBounds(pts)
    map.fitBounds(bounds, { padding: [20,20] })
    shouldAutoFit = false
  }
}

function initMap() {
  if (map) return
  // 将传入中心点（WGS84）转换为 GCJ-02（高德）
  const [cLat, cLng] = wgs84ToGcj02(props.center[0], props.center[1])
  map = L.map(mapEl.value, {
    zoomControl: false, // 先禁用默认控件，稍后手动添加
    scrollWheelZoom: true, // 启用鼠标滚轮缩放
    doubleClickZoom: true, // 启用双击缩放
    boxZoom: true, // 启用框选缩放
    keyboard: true, // 启用键盘缩放
    dragging: true, // 启用拖拽
    zoomAnimation: true, // 启用缩放动画
    zoomAnimationThreshold: 4 // 缩放动画阈值
  }).setView([cLat, cLng], props.zoom)
  
  // 手动添加缩放控件到左上角，确保可见
  L.control.zoom({
    position: 'topleft'
  }).addTo(map)
  
  // 确保地图容器可以接收滚轮事件
  map.getContainer().style.pointerEvents = 'auto'
  
  // 防止对话框阻止地图事件
  map.on('click', (e) => {
    e.originalEvent?.stopPropagation()
  })
  // 使用高德地图瓦片（矢量样式7），带多源与降级回退（OSM）
  const subdomains = ['01','02','03','04']
  const keyParam = props.amapKey ? `&key=${props.amapKey}` : ''
  const providers = props.tileUrl
    ? [props.tileUrl]
    : [
      `https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=7&x={x}&y={y}&z={z}${keyParam}`,
      `https://webst0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=7&x={x}&y={y}&z={z}${keyParam}`,
      `https://wprd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=7&x={x}&y={y}&z={z}${keyParam}`
    ]
  let tileLayer = null
  let providerIndex = 0
  const fallbackOSM = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
  const blankTile = 'data:image/svg+xml;charset=UTF-8,%3Csvg xmlns%3D%22http%3A//www.w3.org/2000/svg%22 width%3D%22256%22 height%3D%22256%22%3E%3Crect width%3D%22256%22 height%3D%22256%22 fill%3D%22%23f0f3f6%22/%3E%3C/svg%3E'

  function useProvider(index) {
    if (tileLayer) {
      map.removeLayer(tileLayer)
      tileLayer.off('tileerror')
    }
    if (index < providers.length) {
      tileLayer = L.tileLayer(providers[index], { maxZoom: 19, subdomains, attribution: '© 高德地图', errorTileUrl: blankTile })
      // 瓦片加载成功后再触发一次尺寸与重绘，避免白屏
      tileLayer.on('load', () => { if (map) { map.invalidateSize() } })
      // 若出现 DNS 解析失败或网络错误，切换到下一个提供方
      let switched = false
      tileLayer.on('tileerror', () => {
        if (switched) return
        switched = true
        providerIndex++
        // 稍作延时避免递归抖动
        setTimeout(() => useProvider(providerIndex), 0)
      })
      tileLayer.addTo(map)
    } else {
      // 全部高德域名不可用，降级 OSM，保证功能可用
      tileLayer = L.tileLayer(fallbackOSM, { maxZoom: 19, attribution: '© OpenStreetMap contributors' })
      tileLayer.addTo(map)
    }
  }
  useProvider(providerIndex)

  renderMarkers()
  renderRoute()

  map.on('click', (e) => {
    if (!props.allowPick) return
    const { lat, lng } = e.latlng // 地图坐标为 GCJ-02
    if (tempMarker) {
      tempMarker.setLatLng([lat, lng])
    } else {
      tempMarker = L.marker([lat, lng], { icon: markerIcon('green') }).addTo(map)
    }
    const [wLat, wLng] = gcj02ToWgs84(lat, lng) // 存库使用 WGS84
    const container = document.createElement('div')
    container.innerHTML = `
      <div class="mm-popup">
        <div class="title">新监控位置</div>
        <div class="sub">${wLat.toFixed(6)}, ${wLng.toFixed(6)}（WGS84）</div>
        <div class="btns">
          <button class="btn primary">在此新增</button>
        </div>
      </div>
    `
    container.querySelector('.primary').addEventListener('click', () => {
      emits('picked', { latitude: wLat, longitude: wLng })
      visibleInner.value = false
    })
    tempMarker.bindPopup(container).openPopup()
  })
}

watch(() => props.monitors, () => renderMarkers(), { deep: true })
watch(() => props.routePathGcj, () => renderRoute(), { deep: true })

onMounted(() => {
  if (visibleInner.value) {
    setTimeout(initMap, 0)
  }
})

watch(visibleInner, async v => {
  if (v) {
    await nextTick()
    setTimeout(() => {
      initMap()
      setTimeout(() => { if (map) { map.invalidateSize(); renderMarkers(); renderRoute() } }, 50)
      setTimeout(() => map && map.invalidateSize(), 200)
    }, 0)
  }
})

async function waitForContainer() {
  for (let i=0;i<20;i++) { // 最长等待 ~1s
    const el = mapEl.value
    if (el && el.clientWidth > 50 && el.clientHeight > 50) return true
    await new Promise(r=>setTimeout(r,50))
  }
  return false
}

async function onOpened() {
  // 等待容器完成布局后再初始化，避免白屏
  await nextTick()
  await waitForContainer()
  initMap()
  // 首次打开允许自动自适应
  shouldAutoFit = true
  const doResize = () => { if (map) { map.invalidateSize(); renderMarkers(); renderRoute() } }
  setTimeout(doResize, 60)
  setTimeout(doResize, 200)
  setTimeout(doResize, 400)
  if (typeof requestAnimationFrame === 'function') requestAnimationFrame(doResize)
}

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
})

function onClosed() {
  // 彻底清理，确保二次打开是全新实例
  if (tempMarker) { tempMarker.remove(); tempMarker = null }
  if (routeLayer) { routeLayer.remove(); routeLayer = null }
  if (markersLayer) { try { markersLayer.clearLayers() } catch(_){} markersLayer = null }
  if (map) { try { map.remove() } catch(_){} map = null }
}
</script>

<style scoped>
.map-wrapper { position: relative; }
.map-container { 
  height: 520px; 
  width: 100%; 
  position: relative;
  z-index: 1;
}

/* 确保 Leaflet 缩放控件可见且可点击 */
:deep(.leaflet-control-zoom) {
  border: none !important;
  box-shadow: 0 1px 5px rgba(0,0,0,.4) !important;
}
:deep(.leaflet-control-zoom a) {
  background-color: #fff !important;
  color: #333 !important;
  border: 1px solid #ccc !important;
  width: 30px !important;
  height: 30px !important;
  line-height: 30px !important;
  font-size: 18px !important;
  font-weight: bold !important;
}
:deep(.leaflet-control-zoom a:hover) {
  background-color: #f4f4f4 !important;
}
:deep(.leaflet-control-zoom-in),
:deep(.leaflet-control-zoom-out) {
  text-decoration: none !important;
}
</style>

<!-- 全局样式：Leaflet 动态创建的标记元素需要全局样式 -->
<style>
/* 图三样式：高德地图POI标记 - 橙色/绿色圆形标记，带数字和名称标签 */
.mm-poi-div-icon { 
  z-index: 700 !important; 
  pointer-events: auto !important;
  background: transparent !important;
  border: none !important;
}
.mm-poi-marker {
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  cursor: pointer !important;
  transition: transform 0.2s !important;
  user-select: none !important;
}
.mm-poi-marker:hover {
  transform: scale(1.1) !important;
  z-index: 800 !important;
}
.mm-rank-icon {
  width: 48px !important;
  height: 48px !important;
  min-width: 48px !important;
  min-height: 48px !important;
  border-radius: 50% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-shadow: 0 3px 10px rgba(0,0,0,.4), 0 0 0 3px rgba(255,255,255,.8) !important;
  border: 3px solid #fff !important;
  cursor: pointer !important;
  position: relative !important;
  background: linear-gradient(135deg, rgba(255,255,255,.2) 0%, transparent 100%) !important;
  flex-shrink: 0 !important;
  overflow: hidden !important;
  box-sizing: border-box !important;
}
/* 告警点融合视频：视频作为背景层 */
.mm-video-bg {
  position: absolute !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  width: 100% !important;
  height: 100% !important;
  min-width: 100% !important;
  min-height: 100% !important;
  object-fit: cover !important;
  border-radius: 50% !important;
  z-index: 0 !important;
  opacity: 0.9 !important;
}
/* 视频叠加层：半透明橙色，保持告警视觉效果 */
.mm-video-overlay {
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  width: 100% !important;
  height: 100% !important;
  background: linear-gradient(135deg, rgba(255,107,53,.5) 0%, rgba(255,107,53,.3) 100%) !important;
  border-radius: 50% !important;
  z-index: 1 !important;
  pointer-events: none !important;
}
.mm-rank-icon .rank-num {
  color: #fff !important;
  font-weight: 800 !important;
  font-size: 18px !important;
  line-height: 1 !important;
  text-shadow: 0 2px 4px rgba(0,0,0,.5), 0 0 8px rgba(0,0,0,.3) !important;
  letter-spacing: -0.5px !important;
  display: block !important;
  text-align: center !important;
  position: relative !important;
  z-index: 2 !important;
}
.mm-poi-label {
  margin-top: 4px !important;
  padding: 2px 8px !important;
  background: rgba(255,255,255,.95) !important;
  color: #333 !important;
  font-size: 11px !important;
  font-weight: 500 !important;
  border-radius: 4px !important;
  white-space: nowrap !important;
  box-shadow: 0 2px 6px rgba(0,0,0,.25) !important;
  border: 1px solid rgba(0,0,0,.1) !important;
  max-width: 80px !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  text-align: center !important;
  line-height: 1.4 !important;
}

/* Leaflet 弹窗样式 */
.mm-popup .title { font-weight: 600; margin-bottom: 4px; }
.mm-popup .sub { font-size: 12px; color: #909399; margin-bottom: 6px; }
.mm-popup .btns { display:flex; gap:8px; }
.mm-popup .btn { padding:4px 8px; border:1px solid #dcdfe6; background:#fff; border-radius:4px; cursor:pointer; font-size:12px; }
.mm-popup .btn.view { border-color:#409eff; color:#409eff; }
.mm-popup .btn.del { border-color:#f56c6c; color:#f56c6c; }
.mm-popup .btn.primary { border-color:#409eff; color:#fff; background:#409eff; }
</style>

<style scoped>
/* 统计面板等组件样式保持 scoped */
.stats-panel {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 1000;
}
.stats-card {
  min-width: 120px;
  padding: 8px 12px;
}
.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;
}
.stat-row:last-child {
  margin-bottom: 0;
}
.stat-label {
  color: #606266;
}
.stat-value {
  font-weight: 700;
  font-size: 18px;
}
.stat-value.alarm {
  color: #ff6b35;
}
.stat-value.normal {
  color: #67c23a;
}

.map-legend { display:inline-flex; gap:16px; align-items:center; }
.legend-item { display:flex; align-items:center; gap:6px; color:#606266; }
.legend-item .dot { width:10px; height:10px; border-radius:50%; display:inline-block; }
.legend-item .dot.green { background:#67c23a; }
.legend-item .dot.red { background:#f56c6c; }
.spacer { flex:1; }
</style>


<template>
  <div class="page">
    <el-card>
      <div class="toolbar flex" style="gap:8px; align-items:center; margin-bottom:8px;">
        <span>置信度：</span>
        <el-slider v-model="threshold" :min="0" :max="1" :step="0.01" style="width:180px"/>
        <el-input-number v-model="threshold" :min="0" :max="1" :step="0.01" :precision="2" size="small" />
        <el-tag size="small" type="info">默认 0.55</el-tag>
      </div>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="图片识别" name="image">
          <el-upload
            drag
            :http-request="onUploadImage"
            :show-file-list="false"
            accept="image/*"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将图片拖拽到此处或 <em>点击上传</em></div>
          </el-upload>
          <div v-if="imageTask" class="mt-2">
            <el-alert title="图片任务已创建" type="success" show-icon :closable="false" />
            <div class="mt-1">任务ID：{{ imageTask.id }} 状态：{{ statusText(imageTask.status) }}</div>
            <div class="mt-1">
              <el-image v-if="imageTask.imagePath" :src="fileUrl(imageTask.imagePath)" style="max-width: 400px; margin-right: 16px"/>
              <el-image v-if="imageTask.resultPath" :src="fileUrl(imageTask.resultPath)" style="max-width: 400px"/>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="视频识别" name="video">
          <div class="flex">
            <el-upload
              :http-request="onUploadVideo"
              :show-file-list="false"
              accept="video/*"
            >
              <el-button type="primary">上传视频文件</el-button>
            </el-upload>
            <el-divider direction="vertical" />
            <el-input v-model="videoUrl" placeholder="RTSP/HTTP 视频流地址" style="max-width:420px" />
            <el-button class="ml-1" type="primary" @click="startStream">开始识别</el-button>
          </div>
          <div v-if="videoTask" class="mt-2">
            <el-alert title="视频任务已创建" type="success" show-icon :closable="false" />
            <div class="mt-1">任务ID：{{ videoTask.id }} 状态：{{ statusText(videoTask.status) }}</div>
            <div class="mt-1">
              <video v-if="isLocalFile(videoTask.videoPath)" :src="fileUrl(videoTask.videoPath)" controls style="max-width: 480px; margin-right: 16px" />
              <video v-if="videoTask.resultPath && isLocalFile(videoTask.resultPath)" :src="fileUrl(videoTask.resultPath)" controls style="max-width: 480px" />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card class="mt-2">
      <div class="flex-between">
        <div>
          <el-radio-group v-model="filters.taskType">
            <el-radio-button :label="undefined">全部</el-radio-button>
            <el-radio-button :label="0">图片</el-radio-button>
            <el-radio-button :label="1">视频</el-radio-button>
          </el-radio-group>
          <el-select v-model="filters.status" placeholder="状态" clearable class="ml-1" style="width:140px">
            <el-option :value="0" label="未开始"/>
            <el-option :value="1" label="进行中"/>
            <el-option :value="2" label="已完成"/>
            <el-option :value="3" label="失败"/>
          </el-select>
        </div>
        <div>
          <el-button @click="fetchTasks">刷新</el-button>
          <el-switch v-model="autoRefresh" active-text="自动刷新" class="ml-1"/>
        </div>
      </div>
      <el-table :data="tasks" stripe style="width:100%" class="mt-1">
        <el-table-column prop="id" label="任务ID" width="100"/>
        <el-table-column prop="type" label="类型" width="80"/>
        <el-table-column prop="statusText" label="状态" width="100"/>
        <el-table-column label="进度" width="200">
          <template #default="scope">
            <el-progress :percentage="taskProgress(scope.row)" :status="progressStatus(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间"/>
        <el-table-column label="操作" width="240">
          <template #default="scope">
            <el-button size="small" @click="viewDetail(scope.row)">详情</el-button>
            <el-popconfirm title="确认删除该任务？此操作不可恢复" @confirm="deleteTask(scope.row)">
              <template #reference>
                <el-button size="small" type="danger" class="ml-1">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex-end mt-1">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          v-model:page-size="pageSize"
          v-model:current-page="page"
          @size-change="fetchTasks"
          @current-change="fetchTasks"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="任务详情" width="820px">
      <template v-if="detail && detail.task">
        <div class="detail-wrap">
          <div class="meta">
            <div>任务ID：{{ detail.task.id }}</div>
            <div>类型：{{ detail.task.videoPath ? '视频' : '图片' }}</div>
            <div>状态：{{ statusText(detail.task.status) }}</div>
            <div>
              结果访问URL：
              <el-link v-if="detail.task.resultPath" :href="fileUrl(detail.task.resultPath)" target="_blank">
                {{ detail.task.resultPath }}
              </el-link>
              <span v-else>暂无</span>
            </div>
            <div class="tip">服务器文件保存目录：backend/uploads/results（YOLO 推理保存为子目录，例如 run_时间戳/文件）</div>
          </div>
          <div class="mt-1">
            <h4>进度</h4>
            <el-progress :percentage="detailTaskProgress" :status="detailTaskStatus" />
          </div>
          <div class="preview mt-1">
            <el-image v-if="detail.task.resultPath && !detail.task.videoPath" :src="fileUrl(detail.task.resultPath)" fit="contain" style="max-width:100%; max-height:520px"/>
            <video v-else-if="detail.task.resultPath && detail.task.videoPath" :src="fileUrl(detail.task.resultPath)" controls style="max-width:100%; max-height:520px"></video>
            <el-empty v-else description="暂无结果文件" />
          </div>
        </div>
        <div class="mt-1">
          <h4>识别结果汇总</h4>
          <div v-if="detail && detail.results && detail.results.length">
            <el-tag v-for="(count, name) in summaryMap" :key="name" class="mr-1" type="success">{{ name }} × {{ count }}</el-tag>
          </div>
          <el-text v-else type="info">暂无识别结果</el-text>
        </div>
      </template>
      <template #footer>
        <span class="dialog-footer">
          <el-popconfirm title="确认删除该任务？此操作不可恢复" @confirm="onDeleteInDetail">
            <template #reference>
              <el-button type="danger">删除</el-button>
            </template>
          </el-popconfirm>
          <el-button @click="detailVisible=false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import { UploadFilled } from '@element-plus/icons-vue'

const activeTab = ref('image')
const imageTask = ref(null)
const videoTask = ref(null)
const videoUrl = ref('')
const threshold = ref(0.55)

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tasks = ref([])
const filters = ref({ taskType: undefined, status: undefined })
const autoRefresh = ref(true)
let timer = null
let progressTimer = null
const progressMap = ref({})

async function deleteTask(row){
  try{
    await api.delete(`/tasks/${row.id}`, { params: { taskType: row.taskType }})
    ElMessage.success('删除成功')
    if (detailVisible.value && detail.value?.task?.id === row.id) {
      detailVisible.value = false
      detail.value = null
    }
    fetchTasks()
  }catch(e){
    ElMessage.error('删除失败')
  }
}

const detailVisible = ref(false)
const detail = ref(null)

const summaryMap = computed(()=>{
  const map = {}
  const arr = detail.value?.results || []
  for (const r of arr) {
    const name = r.objectClass || '未知'
    map[name] = (map[name] || 0) + 1
  }
  return map
})

function keyOf(row){
  return `${row.taskType}:${row.id}`
}
function taskProgress(row){
  const key = keyOf(row)
  const p = progressMap.value[key]?.progress
  if (row.status === 2) return 100
  if (row.status === 3) return 0
  return typeof p === 'number' ? Math.max(0, Math.min(100, p)) : 0
}
function progressStatus(row){
  const pct = taskProgress(row)
  if (row.status === 3) return 'exception'
  if (pct === 100) return 'success'
  return undefined
}

const detailTaskProgress = computed(()=>{
  if (!detail.value || !detail.value.task) return 0
  const t = detail.value.task
  const taskType = t.videoPath ? 1 : 0
  const key = `${taskType}:${t.id}`
  if (t.status === 2) return 100
  if (t.status === 3) return 0
  const p = progressMap.value[key]?.progress
  return typeof p === 'number' ? Math.max(0, Math.min(100, p)) : 0
})
const detailTaskStatus = computed(()=>{
  if (!detail.value || !detail.value.task) return undefined
  const t = detail.value.task
  const pct = detailTaskProgress.value
  if (t.status === 3) return 'exception'
  if (pct === 100) return 'success'
  return undefined
})

async function onDeleteInDetail(){
  if (!detail.value || !detail.value.task) return
  const task = detail.value.task
  const taskType = task.videoPath ? 1 : 0
  try{
    await api.delete(`/tasks/${task.id}`, { params: { taskType }})
    ElMessage.success('删除成功')
    detailVisible.value = false
    detail.value = null
    fetchTasks()
  }catch(e){
    ElMessage.error('删除失败')
  }
}

function fileUrl(p) {
  if (!p) return ''
  // 后端 WebConfig 暴露为 /uploads/**
  if (p.startsWith('./uploads') || p.startsWith('uploads') || p.startsWith('/uploads')) {
    const normalized = p.replace(/^\.\//, '/').replace(/^uploads/, '/uploads')
    return normalized
  }
  return p
}
function isLocalFile(p){
  return p && (p.includes('/uploads/') || p.startsWith('uploads') || p.startsWith('./uploads'))
}

function statusText(s){
  return s===0?'未开始':s===1?'进行中':s===2?'已完成':s===3?'失败':'未知'
}

async function onUploadImage({ file }) {
  const form = new FormData()
  form.append('file', file)
  form.append('conf', String(threshold.value))
  try {
    const { data } = await api.post('/recognize/image', form, { headers: { 'Content-Type': 'multipart/form-data' }})
    if (data.code === 200) {
      imageTask.value = data.data
      ElMessage.success('图片任务创建成功')
      fetchTasks()
    } else {
      ElMessage.error(data.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

async function onUploadVideo({ file }) {
  const form = new FormData()
  form.append('file', file)
  form.append('conf', String(threshold.value))
  try {
    const { data } = await api.post('/recognize/video', form, { headers: { 'Content-Type': 'multipart/form-data' }})
    if (data.code === 200) {
      videoTask.value = data.data
      ElMessage.success('视频任务创建成功')
      fetchTasks()
    } else {
      ElMessage.error(data.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

async function startStream(){
  if(!videoUrl.value){
    ElMessage.warning('请填写视频流地址')
    return
  }
  try{
    const form = new FormData()
    form.append('videoUrl', videoUrl.value)
    form.append('conf', String(threshold.value))
    const { data } = await api.post('/recognize/video', form)
    if (data.code === 200) {
      videoTask.value = data.data
      ElMessage.success('视频流任务创建成功')
      fetchTasks()
    } else {
      ElMessage.error(data.message || '创建失败')
    }
  }catch(e){
    ElMessage.error('创建失败')
  }
}

async function fetchTasks(){
  try{
    const params = { page: page.value, size: pageSize.value }
    if (filters.value.taskType !== undefined) params.taskType = filters.value.taskType
    if (filters.value.status !== undefined) params.status = filters.value.status
    const { data } = await api.get('/tasks', { params })
    if (data.code === 200) {
      tasks.value = data.data.records || []
      total.value = data.data.total || 0
    }
  }catch(e){
    // ignore
  }
}

async function viewDetail(row){
  try{
    const { data } = await api.get(`/tasks/${row.id}`, { params: { taskType: row.taskType }})
    if (data.code === 200) {
      detail.value = data.data
      detailVisible.value = true
    }
  }catch(e){
    // ignore
  }
}

async function pollProgress(){
  const list = tasks.value || []
  for (const row of list) {
    if (row.status !== 1) continue
    try {
      const { data } = await api.get('/recognize/progress', { params: { taskId: row.id, taskType: row.taskType } })
      if (data.code === 200) {
        progressMap.value[keyOf(row)] = data.data || { progress: 0, processed: 0, total: 0 }
      }
    } catch (e) { /* ignore */ }
  }
  // 清理已完成/失败的任务进度
  for (const row of list) {
    if (row.status === 2 || row.status === 3) {
      delete progressMap.value[keyOf(row)]
    }
  }
}

onMounted(()=>{
  fetchTasks()
  timer = setInterval(()=>{ if (autoRefresh.value) fetchTasks() }, 3000)
  progressTimer = setInterval(pollProgress, 1000)
})

onBeforeUnmount(()=>{ if (timer) clearInterval(timer); if (progressTimer) clearInterval(progressTimer) })

watch([page, pageSize, ()=>filters.value.taskType, ()=>filters.value.status], fetchTasks)
</script>

<style scoped>
.page { padding: 12px; }
.mt-1 { margin-top: 12px; }
.mt-2 { margin-top: 16px; }
.flex { display: flex; align-items: center; }
.flex-between { display:flex; align-items:center; justify-content: space-between; }
.flex-end { display:flex; justify-content: flex-end; }
.pre { background: #111; color:#0f0; padding: 12px; border-radius: 6px; max-height: 420px; overflow: auto; }
</style>

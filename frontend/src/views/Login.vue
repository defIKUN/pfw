<template>
  <div class="login-wrapper">
    <div class="login-container">
      <!-- 左侧品牌与卖点 -->
      <div class="left-panel">
        <div class="brand">
          <div class="brand-icon">
            <el-icon><VideoCameraFilled /></el-icon>
          </div>
          <div class="brand-title">河道漂浮物检测系统</div>
          <div class="brand-sub">基于AI目标检测的智能水域监测平台</div>
        </div>
        <div class="features">
          <div class="feature-item">
            <div class="fi-icon"><el-icon><Monitor /></el-icon></div>
            <div class="fi-text">
              <div class="fi-title">实时监控</div>
              <div class="fi-desc">24小时多源视频接入，支持摄像头/RTSP/文件</div>
            </div>
          </div>
          <div class="feature-item">
            <div class="fi-icon"><el-icon><DataAnalysis /></el-icon></div>
            <div class="fi-text">
              <div class="fi-title">数据分析</div>
              <div class="fi-desc">趋势统计与类别占比，任务与告警可追溯</div>
            </div>
          </div>
          <div class="feature-item">
            <div class="fi-icon"><el-icon><BellFilled /></el-icon></div>
            <div class="fi-text">
              <div class="fi-title">智能告警</div>
              <div class="fi-desc">支持阈值设置，指定类别实时提醒</div>
            </div>
          </div>
        </div>
        <div class="left-footer">
          <div class="lf-left">24/7 全天候监控</div>
          <div class="lf-right">1000+ 设备接入</div>
        </div>
      </div>

      <!-- 右侧登录卡片 -->
      <div class="right-panel">
        <el-card class="login-card" shadow="always">
          <div class="card-title">欢迎回来</div>
          <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <div class="row-between">
              <el-checkbox v-model="remember">记住我</el-checkbox>
              <el-link type="primary" :underline="false">忘记密码？</el-link>
            </div>
            <el-form-item>
              <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">立即登录</el-button>
            </el-form-item>
            <div class="assist">
              <span>还没有账号？</span>
              <el-link type="primary" :underline="false">立即注册</el-link>
            </div>
            <div class="or">或者</div>
            <el-button class="ghost-btn" @click="guestLogin">体验账号登录</el-button>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { VideoCameraFilled, Monitor, DataAnalysis, BellFilled } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const loginFormRef = ref(null)
const loading = ref(false)
const remember = ref(true)

const loginForm = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginFormRef.value || loading.value) return
  try {
    const valid = await loginFormRef.value.validate()
    if (!valid) return
    loading.value = true
    const result = await authStore.login(loginForm.username.trim(), loginForm.password)
    if (result.success) {
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(result.message || '登录失败')
    }
  } catch (e) {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}

const guestLogin = async () => {
  loginForm.username = 'admin'
  loginForm.password = 'admin123'
  await handleLogin()
}
</script>

<style scoped>
/* 背景与布局 */
.login-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #5ca6ff 0%, #409eff 45%, #6a88ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-container {
  width: 980px;
  max-width: 92vw;
  background: rgba(255,255,255,0.12);
  border-radius: 14px;
  backdrop-filter: blur(10px);
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  box-shadow: 0 18px 40px rgba(15,80,180,.25);
}

/* 左侧面板 */
.left-panel {
  padding: 36px 40px;
  color: #fff;
  position: relative;
}
.brand { text-align: center; margin-top: 12px; }
.brand-icon { font-size: 46px; width: 72px; height: 72px; border-radius: 16px; background: rgba(255,255,255,.18); display:flex; align-items:center; justify-content:center; margin: 0 auto 12px; }
.brand-title { font-size: 26px; font-weight: 700; letter-spacing: 1px; }
.brand-sub { margin-top: 6px; opacity: .9; font-size: 13px; }
.features { margin-top: 28px; display: grid; gap: 16px; }
.feature-item { display:flex; align-items:center; background: rgba(255,255,255,.16); border-radius: 12px; padding: 12px 14px; }
.fi-icon { width: 36px; height: 36px; border-radius: 10px; background: rgba(255,255,255,.22); display:flex; align-items:center; justify-content:center; margin-right: 12px; font-size: 18px; }
.fi-text { color:#fff; }
.fi-title { font-weight: 600; }
.fi-desc { font-size: 12px; opacity: .9; }
.left-footer { display:flex; align-items:center; justify-content: space-between; margin-top: 28px; opacity: .95; font-weight: 600; }

/* 右侧登录卡片 */
.right-panel { display:flex; align-items:center; justify-content:center; padding: 28px; }
.login-card { width: 420px; border-radius: 14px; box-shadow: 0 12px 28px rgba(0,0,0,.12); }
.card-title { text-align:center; font-size: 22px; font-weight: 700; margin-bottom: 16px; color:#303133; }
.row-between { display:flex; justify-content: space-between; align-items:center; margin-bottom: 10px; }
.login-btn { width: 100%; height: 40px; font-weight: 600; }
.assist { text-align:center; color:#909399; font-size: 13px; }
.assist span { margin-right: 6px; }
.or { text-align:center; color:#c0c4cc; margin: 10px 0; font-size: 12px; }
.ghost-btn { width:100%; border-style: dashed; }

/* 响应式 */
@media (max-width: 900px) {
  .login-container { grid-template-columns: 1fr; }
  .left-panel { display:none; }
}
</style>

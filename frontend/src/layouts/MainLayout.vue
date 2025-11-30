<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-left">
        <h2>河道漂浮物检测系统</h2>
      </div>
      <div class="header-right">
        <UserMenu />
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu
          :default-active="activeMenu"
          router
          class="menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/recognize">
            <el-icon><Camera /></el-icon>
            <span>识别</span>
          </el-menu-item>
          <el-menu-item index="/monitor">
            <el-icon><Camera /></el-icon>
            <span>监控</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DataBoard, Camera, User } from '@element-plus/icons-vue'
import UserMenu from '@/components/UserMenu.vue'

const route = useRoute()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.header {
  background-color: #409eff;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.aside {
  background-color: #fff;
  border-right: 1px solid #e4e7ed;
}

.menu {
  border-right: none;
}

.main {
  background-color: #f5f5f5;
  padding: 20px;
}
</style>


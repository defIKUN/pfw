<template>
  <el-dropdown trigger="hover" @command="onCommand">
    <span class="el-dropdown-link user-entry">
      <el-avatar size="small" icon="UserFilled" class="mr-1" />
      欢迎，{{ authStore.username }}
      <el-icon class="el-icon--right"><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ArrowDown, UserFilled } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const router = useRouter()

function onCommand(cmd){
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.user-entry { 
  display: inline-flex; 
  align-items: center; 
  cursor: pointer; 
  color: #fff;
}
.mr-1 { margin-right: 8px; }
</style>





<template>
  <div class="page center">
    <el-card class="card">
      <div class="title">找回密码</div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item prop="username" label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item prop="email" label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">发送重置邮件</el-button>
          <el-button link type="primary" @click="$router.push('/login')">返回登录</el-button>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" class="mt-1" title="演示：当前后端未开放找回密码接口，如需接入请告知我增加邮件/短信重置流程。" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const form = reactive({ username: '', email: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const submit = () => {
  formRef.value.validate((valid)=>{
    if(!valid) return
    ElMessage.success('已发送（演示），请检查邮箱')
  })
}
</script>

<style scoped>
.page { min-height: 100vh; display:flex; align-items:center; justify-content: center; background: #f5f7fa; }
.card { width: 520px; }
.title { font-size: 20px; font-weight: 700; margin-bottom: 16px; text-align:center; }
.mt-1 { margin-top: 12px; }
</style>



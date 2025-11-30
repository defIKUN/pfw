<template>
  <div class="page center">
    <el-card class="card">
      <div class="title">注册账号</div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item prop="username" label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item prop="password" label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item prop="confirm" label="确认密码">
          <el-input v-model="form.confirm" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">注册</el-button>
          <el-button link type="primary" @click="$router.push('/login')">返回登录</el-button>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" class="mt-1" title="演示：当前后端未开放注册接口，如需真实注册请联系管理员或我来接入 /api/users 创建逻辑。" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const form = reactive({ username: '', password: '', confirm: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (_, v, cb) => v !== form.password ? cb(new Error('两次输入的密码不一致')) : cb(), trigger: 'blur' }
  ]
}

const submit = () => {
  formRef.value.validate((valid)=>{
    if(!valid) return
    // 这里只做演示提示
    ElMessage.success('注册成功（演示），请使用分配账号登录')
  })
}
</script>

<style scoped>
.page { min-height: 100vh; display:flex; align-items:center; justify-content: center; background: #f5f7fa; }
.card { width: 520px; }
.title { font-size: 20px; font-weight: 700; margin-bottom: 16px; text-align:center; }
.mt-1 { margin-top: 12px; }
</style>



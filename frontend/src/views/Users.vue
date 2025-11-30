<template>
  <div class="page">
    <el-card>
      <div class="flex-between">
        <el-input v-model="keyword" placeholder="搜索用户名" clearable style="max-width:260px" @keyup.enter.native="fetchData" />
        <div>
          <el-button type="primary" @click="openCreate">新增用户</el-button>
          <el-button @click="fetchData">刷新</el-button>
        </div>
      </div>
      <el-table :data="users" stripe class="mt-1" style="width:100%">
        <el-table-column prop="id" label="ID" width="80"/>
        <el-table-column prop="username" label="用户名"/>
        <el-table-column prop="role" label="角色" width="120"/>
        <el-table-column prop="createTime" label="创建时间"/>
        <el-table-column prop="updateTime" label="更新时间"/>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-popconfirm title="确认删除该用户？" @confirm="remove(scope.row)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
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
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="88px">
        <el-form-item prop="username" label="用户名">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item prop="password" label="密码">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '不修改请留空' : ''" />
        </el-form-item>
        <el-form-item prop="role" label="角色">
          <el-select v-model="form.role" placeholder="请选择">
            <el-option label="admin" value="admin" />
            <el-option label="user" value="user" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

const users = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const dialogVisible = ref(false)
const formRef = ref(null)
const form = ref({ id: null, username: '', password: '', role: 'user' })

const rules = {
  username: [ { required: true, message: '请输入用户名', trigger: 'blur' } ],
  role: [ { required: true, message: '请选择角色', trigger: 'change' } ]
}

async function fetchData(){
  try{
    const params = { page: page.value, size: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    const { data } = await api.get('/users', { params })
    if (data.code === 200) {
      users.value = data.data.records || []
      total.value = data.data.total || 0
    }
  }catch(e){
    // ignore
  }
}

function openCreate(){
  form.value = { id: null, username: '', password: '', role: 'user' }
  dialogVisible.value = true
}

function openEdit(row){
  form.value = { id: row.id, username: row.username, password: '', role: row.role }
  dialogVisible.value = true
}

function submit(){
  formRef.value.validate(async (valid)=>{
    if(!valid) return
    try{
      if (!form.value.id) {
        const { data } = await api.post('/users', form.value)
        if (data.code === 200) {
          ElMessage.success('创建成功')
          dialogVisible.value = false
          fetchData()
        } else {
          ElMessage.error(data.message || '创建失败')
        }
      } else {
        const { data } = await api.put(`/users/${form.value.id}`, form.value)
        if (data.code === 200) {
          ElMessage.success('更新成功')
          dialogVisible.value = false
          fetchData()
        } else {
          ElMessage.error(data.message || '更新失败')
        }
      }
    }catch(e){
      ElMessage.error('操作失败')
    }
  })
}

async function remove(row){
  try{
    const { data } = await api.delete(`/users/${row.id}`)
    if (data.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  }catch(e){
    ElMessage.error('删除失败')
  }
}

onMounted(()=>{
  fetchData()
})
</script>

<style scoped>
.page { padding: 12px; }
.mt-1 { margin-top: 12px; }
.flex-between { display:flex; justify-content: space-between; align-items: center; }
.flex-end { display:flex; justify-content: flex-end; }
</style>


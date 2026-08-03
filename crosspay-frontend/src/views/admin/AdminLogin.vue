<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #303133">
    <el-card style="width: 400px" shadow="hover">
      <template #header>
        <h2 style="text-align: center; margin: 0">⚙️ 运营后台登录</h2>
      </template>
      <el-form :model="form" ref="formRef" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="admin123" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="loading" @click="handleLogin" style="width: 100%">登录</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align: center; font-size: 12px; color: #999">
        默认账号：admin / admin123
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123',
})

async function handleLogin() {
  loading.value = true
  try {
    const res = await adminLogin(form)
    const data = res.data.data
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', 'ADMIN')
    ElMessage.success('登录成功')
    router.push('/admin/merchants')
  } finally {
    loading.value = false
  }
}
</script>

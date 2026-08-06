<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f0f2f5">
    <el-card style="width: 400px" shadow="hover">
      <template #header>
        <h2 style="text-align: center; margin: 0">🏦 CrossPay 商户登录</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="demo@africashop.com" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="merchant123" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width: 100%">登录</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align: center">
        <el-link type="primary" @click="$router.push('/register')">还没有账号？立即注册</el-link>
      </div>
      <el-divider />
      <div style="text-align: center; font-size: 12px; color: #999">
        演示账号：demo@africashop.com / merchant123
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  email: 'demo@africashop.com',
  password: 'merchant123',
})

const rules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    // 清除旧会话，避免跨端角色冲突
    localStorage.clear()
    const res = await login(form)
    const data = res.data.data
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', 'MERCHANT')
    ElMessage.success(`欢迎回来，${data.name}`)
    router.push('/merchant/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

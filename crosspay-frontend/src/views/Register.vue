<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: 100vh; background: #f0f2f5">
    <el-card style="width: 450px" shadow="hover">
      <template #header>
        <h2 style="text-align: center; margin: 0">🏪 商户注册</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="商户名称" prop="name">
          <el-input v-model="form.name" placeholder="例如: AfricaShop" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="contact@yourbusiness.com" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="所在国家" prop="country">
          <el-select v-model="form.country" style="width: 100%">
            <el-option label="肯尼亚" value="Kenya" />
            <el-option label="尼日利亚" value="Nigeria" />
            <el-option label="南非" value="South Africa" />
            <el-option label="加纳" value="Ghana" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleRegister" style="width: 100%">注册</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align: center">
        <el-link type="primary" @click="$router.push('/login')">已有账号？返回登录</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  name: '',
  email: '',
  password: '',
  country: 'Kenya',
})

const rules = {
  name: [{ required: true, message: '请输入商户名称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await register(form)
    const data = res.data.data
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', 'MERCHANT')
    ElMessage.success('注册成功！')
    router.push('/merchant/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

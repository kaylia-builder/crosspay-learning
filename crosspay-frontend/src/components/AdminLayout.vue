<template>
  <el-container style="min-height: 100vh">
    <el-header style="background: #e6a23c; color: white; display: flex; align-items: center; justify-content: space-between">
      <span style="font-size: 20px; font-weight: bold">⚙️ CrossPay - 运营后台</span>
      <el-button type="danger" plain @click="logout">退出登录</el-button>
    </el-header>
    <el-container>
      <el-aside width="200px" style="background: #f5f7fa">
        <el-menu :default-active="currentRoute" router>
          <el-menu-item index="/admin/merchants">🏪 商户管理</el-menu-item>
          <el-menu-item index="/admin/orders">📦 订单管理</el-menu-item>
          <el-menu-item index="/admin/settlements">💰 结算管理</el-menu-item>
          <el-menu-item index="/admin/ai-assistant">🤖 AI 助手</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const currentRoute = computed(() => route.path)

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  router.push('/admin/login')
}
</script>

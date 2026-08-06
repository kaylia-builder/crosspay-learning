<template>
  <div>
    <h2>商户首页</h2>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>今日交易笔数</template>
          <div style="font-size: 36px; color: #409eff; font-weight: bold">{{ dashboard.todayTransactionCount }}</div>
          <div style="color: #999; font-size: 12px">成功率: {{ dashboard.successRate }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>今日交易金额</template>
          <div style="font-size: 36px; color: #67c23a; font-weight: bold">{{ dashboard.todayTransactionAmount }} {{ dashboard.currency }}</div>
          <div style="color: #999; font-size: 12px">{{ dashboard.name }} · {{ dashboard.country }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>手续费率</template>
          <div style="font-size: 36px; color: #e6a23c; font-weight: bold">{{ feePercent }}%</div>
          <div style="color: #999; font-size: 12px">商户号: {{ dashboard.merchantNo }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top: 20px" shadow="hover">
      <template #header>快速操作</template>
      <el-space>
        <el-button type="primary" @click="$router.push('/merchant/create-payment')">发起支付</el-button>
        <el-button type="success" @click="$router.push('/merchant/transactions')">查看交易</el-button>
        <el-button type="warning" @click="$router.push('/merchant/settlements')">查看结算</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getDashboard, type DashboardData } from '@/api/merchant'

const dashboard = ref<DashboardData>({
  merchantNo: '',
  name: '',
  country: '',
  currency: 'USD',
  feeRate: '0',
  status: '',
  todayTransactionCount: 0,
  todayTransactionAmount: '0.00',
  successRate: '0%',
})

const feePercent = computed(() => {
  const rate = parseFloat(dashboard.value.feeRate)
  return isNaN(rate) ? '--' : (rate * 100).toFixed(1)
})

async function fetchDashboard() {
  try {
    const res = await getDashboard()
    dashboard.value = res.data.data
  } catch {
    // error handled by request interceptor
  }
}

onMounted(() => fetchDashboard())
</script>

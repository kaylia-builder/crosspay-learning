<template>
  <div>
    <h2>订单管理</h2>
    <el-table :data="orders" v-loading="loading" stripe style="margin-top: 20px">
      <el-table-column prop="orderNo" label="订单号" width="220" />
      <el-table-column prop="merchantId" label="商户ID" width="100" />
      <el-table-column prop="amount" label="金额" width="120">
        <template #default="{ row }">{{ row.amount }} {{ row.currency }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="channel" label="支付渠道" width="130" />
      <el-table-column prop="channelOrderNo" label="渠道订单号" width="200">
        <template #default="{ row }">{{ row.channelOrderNo || '-' }}</template>
      </el-table-column>
      <el-table-column prop="failReason" label="失败原因" min-width="180">
        <template #default="{ row }">
          <span v-if="row.failReason" style="color: #f56c6c">{{ row.failReason }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrders } from '@/api/admin'
import type { PaymentOrder } from '@/types'

const loading = ref(false)
const orders = ref<PaymentOrder[]>([])

function statusTag(s: string) {
  const map: Record<string, string> = { CREATED: 'info', PROCESSING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[s] || 'info'
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getOrders({ page: 0, size: 50 })
    orders.value = res.data.data.content
  } finally {
    loading.value = false
  }
})
</script>

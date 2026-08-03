<template>
  <div>
    <h2>交易记录</h2>
    <el-table :data="transactions" style="margin-top: 20px" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单号" width="220" />
      <el-table-column prop="amount" label="金额" width="120">
        <template #default="{ row }">{{ row.amount }} {{ row.currency }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="channel" label="支付渠道" width="130" />
      <el-table-column prop="failReason" label="失败原因" min-width="200">
        <template #default="{ row }">
          <span v-if="row.failReason" style="color: #f56c6c">{{ row.failReason }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" />
    </el-table>
    <el-pagination
      v-model:current-page="page"
      :total="total"
      :page-size="size"
      layout="total, prev, pager, next"
      @current-change="fetchData"
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTransactions } from '@/api/payment'
import type { PaymentOrder } from '@/types'

const loading = ref(false)
const transactions = ref<PaymentOrder[]>([])
const page = ref(0)
const size = 20
const total = ref(0)

function statusTag(status: string) {
  const map: Record<string, string> = { CREATED: 'info', PROCESSING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

async function fetchData(p: number = 0) {
  loading.value = true
  try {
    const res = await getTransactions(p, size)
    transactions.value = res.data.data.content
    total.value = res.data.data.totalElements
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchData())
</script>

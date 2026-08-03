<template>
  <div>
    <h2>发起支付</h2>
    <el-card style="max-width: 500px; margin-top: 20px" shadow="hover">
      <el-form :model="form" label-width="120px" label-position="left">
        <el-form-item label="支付金额 (USD)">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="商户订单号">
          <el-input v-model="form.merchantOrderNo" placeholder="可选，你的内部订单号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="createPayment">发起支付</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card v-if="result" style="max-width: 500px; margin-top: 20px" shadow="hover">
      <template #header>支付结果</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ result.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ result.amount }} {{ result.currency }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(result.status)">{{ result.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="result.channel" label="支付渠道">{{ result.channel }}</el-descriptions-item>
        <el-descriptions-item v-if="result.failReason" label="失败原因">
          <span style="color: #f56c6c">{{ result.failReason }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ result.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { createPayment } from '@/api/payment'
import type { PaymentOrder } from '@/types'

const loading = ref(false)
const result = ref<PaymentOrder | null>(null)
const form = reactive({
  amount: 100.00,
  merchantOrderNo: '',
})

function statusTag(status: string) {
  const map: Record<string, string> = {
    CREATED: 'info',
    PROCESSING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger',
  }
  return map[status] || 'info'
}

async function createPayment() {
  loading.value = true
  try {
    const res = await createPayment({ amount: form.amount, merchantOrderNo: form.merchantOrderNo || undefined })
    result.value = res.data.data
  } finally {
    loading.value = false
  }
}
</script>

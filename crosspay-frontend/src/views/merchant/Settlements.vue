<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center">
      <h2>结算记录</h2>
      <el-button type="warning" :loading="triggerLoading" @click="triggerSettle">触发日终结算</el-button>
    </div>
    <el-table :data="settlements" style="margin-top: 20px" v-loading="loading" stripe>
      <el-table-column prop="settlementNo" label="结算编号" width="220" />
      <el-table-column prop="totalAmount" label="交易总额" width="130">
        <template #default="{ row }">{{ row.totalAmount }} {{ row.currency }}</template>
      </el-table-column>
      <el-table-column prop="feeAmount" label="手续费" width="120">
        <template #default="{ row }">-{{ row.feeAmount }} {{ row.currency }}</template>
      </el-table-column>
      <el-table-column prop="netAmount" label="到账金额" width="130">
        <template #default="{ row }">
          <span style="color: #67c23a; font-weight: bold">{{ row.netAmount }} {{ row.currency }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="settlementDate" label="结算日期" width="130" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && settlements.length === 0" description="暂无结算记录，先发起支付再触发结算" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSettlements, triggerSettlement } from '@/api/settlement'
import type { Settlement } from '@/types'

const loading = ref(false)
const triggerLoading = ref(false)
const settlements = ref<Settlement[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getSettlements(0, 20)
    settlements.value = res.data.data.content
  } finally {
    loading.value = false
  }
}

async function triggerSettle() {
  triggerLoading.value = true
  try {
    const res = await triggerSettlement()
    if (res.data.data) {
      ElMessage.success('结算完成！')
      await fetchData()
    } else {
      ElMessage.info('没有需要结算的订单')
    }
  } finally {
    triggerLoading.value = false
  }
}

onMounted(() => fetchData())
</script>

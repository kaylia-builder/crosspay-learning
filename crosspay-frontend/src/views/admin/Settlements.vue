<template>
  <div>
    <h2>结算管理</h2>
    <el-table :data="settlements" v-loading="loading" stripe style="margin-top: 20px">
      <el-table-column prop="settlementNo" label="结算编号" width="220" />
      <el-table-column prop="merchantId" label="商户ID" width="100" />
      <el-table-column prop="totalAmount" label="交易总额" width="120">
        <template #default="{ row }">{{ row.totalAmount }} {{ row.currency }}</template>
      </el-table-column>
      <el-table-column prop="feeAmount" label="手续费" width="120">
        <template #default="{ row }">-{{ row.feeAmount }}</template>
      </el-table-column>
      <el-table-column prop="netAmount" label="到账金额" width="130">
        <template #default="{ row }">
          <span style="color: #67c23a; font-weight: bold">{{ row.netAmount }} {{ row.currency }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="settlementDate" label="结算日" width="120" />
      <el-table-column prop="status" label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" type="success" size="small" @click="handleComplete(row.id)">
            确认完成
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminSettlements, completeSettlement } from '@/api/admin'
import type { Settlement } from '@/types'

const loading = ref(false)
const settlements = ref<Settlement[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminSettlements()
    settlements.value = res.data.data.content
  } finally {
    loading.value = false
  }
}

async function handleComplete(id: number) {
  try {
    await completeSettlement(id)
    ElMessage.success('结算已确认完成')
    await fetchData()
  } catch {
    // handled by interceptor
  }
}

onMounted(() => fetchData())
</script>

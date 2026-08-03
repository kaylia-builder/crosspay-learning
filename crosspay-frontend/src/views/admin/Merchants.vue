<template>
  <div>
    <h2>商户管理</h2>
    <el-table :data="merchants" v-loading="loading" stripe style="margin-top: 20px">
      <el-table-column prop="merchantNo" label="商户编号" width="180" />
      <el-table-column prop="name" label="商户名称" width="180" />
      <el-table-column prop="email" label="邮箱" width="220" />
      <el-table-column prop="country" label="国家" width="100" />
      <el-table-column prop="feeRate" label="费率" width="80">
        <template #default="{ row }">{{ (row.feeRate * 100).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMerchants } from '@/api/admin'
import type { Merchant } from '@/types'

const loading = ref(false)
const merchants = ref<Merchant[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMerchants()
    merchants.value = res.data.data.content
  } finally {
    loading.value = false
  }
})
</script>

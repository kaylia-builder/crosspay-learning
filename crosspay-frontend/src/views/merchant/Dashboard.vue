<template>
  <div>
    <h2>商户首页</h2>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>今日交易笔数</template>
          <div style="font-size: 36px; color: #409eff; font-weight: bold">--</div>
          <div style="color: #999; font-size: 12px">支付模块已就绪，请发起支付查看</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>今日交易金额</template>
          <div style="font-size: 36px; color: #67c23a; font-weight: bold">--</div>
          <div style="color: #999; font-size: 12px">数据将在发起支付后更新</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>手续费率</template>
          <div style="font-size: 36px; color: #e6a23c; font-weight: bold">3%</div>
          <div style="color: #999; font-size: 12px">默认商户费率</div>
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

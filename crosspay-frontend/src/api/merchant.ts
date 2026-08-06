import request from './request'
import type { ApiResponse } from '@/types'

export interface DashboardData {
  merchantNo: string
  name: string
  country: string
  currency: string
  feeRate: string
  status: string
  todayTransactionCount: number
  todayTransactionAmount: string
  successRate: string
}

export function getDashboard() {
  return request.get<ApiResponse<DashboardData>>('/merchant/dashboard')
}

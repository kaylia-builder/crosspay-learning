import request from './request'
import type { ApiResponse, Merchant, PaymentOrder, Settlement, Page } from '@/types'

export function getMerchants(page: number = 0, size: number = 20) {
  return request.get<ApiResponse<Page<Merchant>>>('/admin/merchants', { params: { page, size } })
}

export function getOrders(params: { status?: string; page?: number; size?: number }) {
  return request.get<ApiResponse<Page<PaymentOrder>>>('/admin/orders', { params })
}

export function getOrderDetail(orderNo: string) {
  return request.get<ApiResponse<PaymentOrder>>(`/admin/orders/${orderNo}`)
}

export function getAdminSettlements(page: number = 0, size: number = 20) {
  return request.get<ApiResponse<Page<Settlement>>>('/admin/settlements', { params: { page, size } })
}

export function completeSettlement(id: number) {
  return request.post<ApiResponse<string>>(`/admin/settlement/${id}/complete`)
}

export function aiQuery(question: string) {
  return request.post<ApiResponse<{ question: string; answer: string }>>('/ai/query', { question })
}

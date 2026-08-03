import request from './request'
import type { ApiResponse, PaymentOrder, Page } from '@/types'

export function createPayment(data: { amount: number; currency?: string; merchantOrderNo?: string }) {
  return request.post<ApiResponse<PaymentOrder>>('/payment/create', data)
}

export function queryPayment(orderNo: string) {
  return request.get<ApiResponse<PaymentOrder>>(`/payment/${orderNo}`)
}

export function getTransactions(page: number = 0, size: number = 20) {
  return request.get<ApiResponse<Page<PaymentOrder>>>('/payment/transactions', {
    params: { page, size },
  })
}

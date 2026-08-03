import request from './request'
import type { ApiResponse, Settlement, Page } from '@/types'

export function getSettlements(page: number = 0, size: number = 20) {
  return request.get<ApiResponse<Page<Settlement>>>('/settlement/list', {
    params: { page, size },
  })
}

export function triggerSettlement() {
  return request.post<ApiResponse<Settlement>>('/settlement/trigger')
}

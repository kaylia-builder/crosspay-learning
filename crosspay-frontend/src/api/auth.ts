import request from './request'
import type { ApiResponse, LoginResponse, Merchant } from '@/types'

export function register(data: { name: string; email: string; password: string; country?: string }) {
  return request.post<ApiResponse<LoginResponse>>('/auth/register', data)
}

export function login(data: { email: string; password: string }) {
  return request.post<ApiResponse<LoginResponse>>('/auth/login', data)
}

export function adminLogin(data: { username: string; password: string }) {
  return request.post<ApiResponse<LoginResponse>>('/auth/admin/login', data)
}

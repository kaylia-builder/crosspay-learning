// API 统一响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 商户
export interface Merchant {
  id: number
  merchantNo: string
  name: string
  email: string
  country: string
  currency: string
  feeRate: number
  status: string
  createdAt: string
}

// 支付订单
export interface PaymentOrder {
  orderNo: string
  amount: number
  currency: string
  status: string
  channel: string | null
  channelOrderNo: string | null
  failReason: string | null
  createdAt: string
}

// 结算记录
export interface Settlement {
  settlementNo: string
  totalAmount: number
  feeAmount: number
  netAmount: number
  currency: string
  settlementDate: string
  status: string
}

// 登录响应
export interface LoginResponse {
  token: string
  merchantNo?: string
  name?: string
  username?: string
  role?: string
}

// 分页
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

# Phase 2: 项目架构设计

> CrossPay Learning Project - 数据库设计、后端/前端目录结构、API 设计

---

## 一、数据库设计

### 表关系总览

```
┌─────────────────────────────────────────────────────────────┐
│                        MySQL Database                        │
│                                                              │
│  ┌──────────┐      ┌──────────────────┐      ┌───────────┐  │
│  │ merchants│      │  payment_orders   │      │settlements│  │
│  ├──────────┤      ├──────────────────┤      ├───────────┤  │
│  │id        │◄─────│merchant_id (FK)  │      │id         │  │
│  │merchant_no│     │order_no (UQ)     │◄──┐  │settlement_no│ │
│  │name      │      │merchant_order_no │   │  │merchant_id│  │
│  │email (UQ)│      │amount            │   │  │total_amount│ │
│  │password  │      │currency          │   │  │fee_amount │  │
│  │country   │      │status            │   │  │net_amount │  │
│  │currency  │      │channel           │   │  │status     │  │
│  │fee_rate  │      │channel_order_no  │   │  │settlement_date│
│  │status    │      │fail_reason       │   │  └───────────┘  │
│  └──────────┘      │callback_time     │   │        │        │
│                    └──────────────────┘   │  ┌─────▼──────┐ │
│                                           │  │settlement  │ │
│  ┌──────────┐                             │  │_details    │ │
│  │admin_users│                            │  ├────────────┤ │
│  ├──────────┤                             └──│settlement_id│ │
│  │id        │                                │order_id    │ │
│  │username  │                                └────────────┘ │
│  │password  │                                               │
│  │role      │                                               │
│  └──────────┘                                               │
└─────────────────────────────────────────────────────────────┘
```

### 建表 SQL

```sql
-- 商户表
CREATE TABLE merchants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_no VARCHAR(20) NOT NULL UNIQUE COMMENT '商户编号 M20240803001',
    name VARCHAR(100) NOT NULL COMMENT '商户名称',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '登录邮箱',
    password_hash VARCHAR(255) NOT NULL,
    country VARCHAR(50) COMMENT '所在国家',
    currency VARCHAR(10) DEFAULT 'USD',
    fee_rate DECIMAL(5,4) DEFAULT 0.0300 COMMENT '平台手续费率 3%',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE|SUSPENDED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 支付订单表
CREATE TABLE payment_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL UNIQUE COMMENT '平台订单号 PAY20240803001',
    merchant_id BIGINT NOT NULL,
    merchant_order_no VARCHAR(100) COMMENT '商户侧订单号',
    amount DECIMAL(12,2) NOT NULL COMMENT '支付金额',
    currency VARCHAR(10) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED|PROCESSING|SUCCESS|FAILED',
    channel VARCHAR(50) COMMENT '使用的支付渠道',
    channel_order_no VARCHAR(100) COMMENT '渠道侧订单号',
    fail_reason VARCHAR(500) COMMENT '失败原因',
    callback_received_at TIMESTAMP NULL COMMENT '收到回调时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (merchant_id) REFERENCES merchants(id)
);

-- 结算表
CREATE TABLE settlements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_no VARCHAR(30) NOT NULL UNIQUE COMMENT '结算编号 STL20240803',
    merchant_id BIGINT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL COMMENT '交易总额',
    fee_amount DECIMAL(12,2) NOT NULL COMMENT '手续费',
    net_amount DECIMAL(12,2) NOT NULL COMMENT '商户到账金额',
    currency VARCHAR(10) DEFAULT 'USD',
    settlement_date DATE NOT NULL COMMENT '结算日期',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING|COMPLETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (merchant_id) REFERENCES merchants(id)
);

-- 结算明细（结算与订单的关联）
CREATE TABLE settlement_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    payment_order_id BIGINT NOT NULL,
    FOREIGN KEY (settlement_id) REFERENCES settlements(id),
    FOREIGN KEY (payment_order_id) REFERENCES payment_orders(id)
);

-- 管理员表
CREATE TABLE admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'OPERATOR' COMMENT 'ADMIN|OPERATOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 二、后端目录结构

```
crosspay-backend/
├── pom.xml
├── src/main/java/com/crosspay/
│   ├── CrossPayApplication.java
│   │
│   ├── common/                          # 公共模块
│   │   ├── config/
│   │   │   ├── SecurityConfig.java      # Spring Security + JWT 配置
│   │   │   └── RedisConfig.java         # Redis 连接池配置
│   │   ├── dto/
│   │   │   └── ApiResponse.java         # 统一响应 {code, message, data}
│   │   ├── exception/
│   │   │   ├── BusinessException.java   # 业务异常
│   │   │   └── GlobalExceptionHandler.java
│   │   └── util/
│   │       └── OrderNoGenerator.java    # 订单号、商户号生成器
│   │
│   ├── module/
│   │   ├── auth/                        # 认证模块
│   │   │   ├── AuthController.java
│   │   │   ├── AuthService.java
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── dto/
│   │   │       ├── LoginRequest.java
│   │   │       ├── RegisterRequest.java
│   │   │       └── LoginResponse.java
│   │   │
│   │   ├── merchant/                    # 商户模块
│   │   │   ├── MerchantController.java
│   │   │   ├── MerchantService.java
│   │   │   ├── MerchantRepository.java
│   │   │   └── entity/
│   │   │       └── Merchant.java
│   │   │
│   │   ├── payment/                     # 支付订单模块
│   │   │   ├── PaymentController.java
│   │   │   ├── PaymentService.java
│   │   │   ├── PaymentRepository.java
│   │   │   ├── entity/
│   │   │   │   └── PaymentOrder.java
│   │   │   └── dto/
│   │   │       ├── CreatePaymentRequest.java
│   │   │       └── PaymentResponse.java
│   │   │
│   │   ├── gateway/                     # 支付渠道抽象层
│   │   │   ├── PaymentGateway.java      # 核心接口
│   │   │   ├── GatewayRouter.java       # 按商户路由渠道
│   │   │   ├── MockAfricaGateway.java   # 模拟非洲支付渠道
│   │   │   └── dto/
│   │   │       ├── GatewayPayRequest.java
│   │   │       └── GatewayPayResponse.java
│   │   │
│   │   ├── settlement/                  # 结算模块
│   │   │   ├── SettlementService.java
│   │   │   ├── SettlementRepository.java
│   │   │   ├── SettlementDetailRepository.java
│   │   │   ├── entity/
│   │   │   │   ├── Settlement.java
│   │   │   │   └── SettlementDetail.java
│   │   │   └── dto/
│   │   │       └── SettlementResponse.java
│   │   │
│   │   └── admin/                       # 运营管理
│   │       ├── AdminController.java
│   │       └── AdminService.java
│   │
│   └── ai/                              # AI 助手模块
│       ├── AiAssistantController.java
│       └── AiAssistantService.java
│
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__init_schema.sql
│
└── docker-compose.yml                   # MySQL + Redis + App
```

### 关键设计决策

| 决策 | 做法 | 原因 |
|------|------|------|
| 包结构 | 按模块分包，不按层分包 | 模块内聚，每个模块自成体系，方便后续拆分微服务 |
| Entity 与 DTO | 严格分离 | 不暴露数据库结构给 API |
| Gateway 抽象 | Interface + 路由表 | 适配器模式，加渠道不改支付代码 |
| Repository | Spring Data JPA | 简单场景用 JPA，复杂查询用 @Query |

---

## 三、前端目录结构

```
crosspay-frontend/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── index.html
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── router/
│   │   └── index.ts                   # 路由配置
│   ├── api/                           # API 调用层
│   │   ├── request.ts                 # Axios 封装 (拦截器、JWT)
│   │   ├── auth.ts
│   │   ├── payment.ts
│   │   ├── settlement.ts
│   │   └── admin.ts
│   ├── views/                         # 页面
│   │   ├── Login.vue                  # 商户登录
│   │   ├── Register.vue               # 商户注册
│   │   ├── merchant/
│   │   │   ├── Dashboard.vue          # 商户首页/概览
│   │   │   └── Transactions.vue       # 交易记录
│   │   ├── payment/
│   │   │   └── CreatePayment.vue      # 创建支付订单
│   │   └── admin/
│   │       ├── Merchants.vue          # 商户管理
│   │       ├── Orders.vue             # 订单管理
│   │       ├── Settlements.vue        # 结算管理
│   │       └── AiAssistant.vue        # AI 助手
│   ├── components/                    # 公共组件
│   │   ├── MerchantLayout.vue         # 商户端布局
│   │   └── AdminLayout.vue            # 运营端布局
│   └── types/                         # TypeScript 类型定义
│       └── index.ts
│
└── Dockerfile
```

---

## 四、API 设计

### RESTful API 总览

```
                          ┌──────────────────────────────┐
                          │        CrossPay API           │
                          └──────────────────────────────┘

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   商户端 API      │    │   运营端 API      │    │   公开 API        │
│ /api/auth/*      │    │ /api/admin/*     │    │ /api/callback/*   │
│ /api/merchant/*  │    │                  │    │                   │
│ /api/payment/*   │    │                  │    │                   │
│ /api/settlement/*│    │                  │    │                   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 详细 API 清单

#### 1. 认证模块

```
POST   /api/auth/register          # 商户注册
Body: { name, email, password, country }

POST   /api/auth/login             # 商户登录
Body: { email, password }
Response: { token, merchantNo, name }

POST   /api/auth/admin/login       # 管理员登录
Body: { username, password }
Response: { token, username, role }
```

#### 2. 商户模块 (需 JWT - Merchant Role)

```
GET    /api/merchant/profile       # 当前商户信息
GET    /api/merchant/transactions  # 当前商户交易列表 (分页)
       ?page=1&size=20&status=SUCCESS
```

#### 3. 支付模块 (需 JWT - Merchant Role)

```
POST   /api/payment/create         # 创建支付订单
Body: { amount, currency, merchantOrderNo }
Response: { orderNo, status: "CREATED" }

GET    /api/payment/{orderNo}      # 查询订单状态及详情
Response: { orderNo, amount, currency, status, channelOrderNo, failReason, created_at }
```

#### 4. 支付回调 (公开接口 - 不需要 JWT)

```
POST   /api/callback/{channel}     # 渠道异步通知
Header: X-Channel-Signature        # 模拟签名验证
Body: { channelOrderNo, platformOrderNo, status, amount }
```

#### 5. 结算模块 (需 JWT - Merchant Role)

```
GET    /api/settlement/list        # 当前商户结算列表
       ?page=1&size=20
```

#### 6. 运营管理 (需 JWT - Admin Role)

```
GET    /api/admin/merchants        # 商户列表 ?page=1&size=20
GET    /api/admin/orders           # 订单列表 ?status=&merchantId=&page=1&size=20
GET    /api/admin/orders/{orderNo} # 订单详情
GET    /api/admin/settlements      # 结算列表
POST   /api/admin/settlement/{id}/complete  # 确认结算完成
```

#### 7. AI 助手 (需 JWT - Admin Role)

```
POST   /api/ai/query
Body: { question: "订单 PAY20240803001 为什么失败？" }
Response: { answer: "订单 PAY20240803001 当前状态为 FAILED，失败原因是..." }
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

错误时：

```json
{
  "code": 40001,
  "message": "订单不存在",
  "data": null
}
```

### 错误码规划

| Code | 含义 |
|------|------|
| 200 | 成功 |
| 40001 | 参数校验失败 |
| 40002 | 商户不存在 |
| 40003 | 订单不存在 |
| 40004 | 订单状态不允许此操作 |
| 40100 | 未登录 |
| 40101 | Token 过期 |
| 40300 | 无权限 |
| 50000 | 系统内部错误 |
| 50001 | 支付渠道异常 |

---

## 五、Docker Compose 总览

```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8.0
    ports: ["3306:3306"]
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: crosspay

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  backend:
    build: ./crosspay-backend
    ports: ["8080:8080"]
    depends_on: [mysql, redis]

  frontend:
    build: ./crosspay-frontend
    ports: ["3000:80"]
    depends_on: [backend]
```

---

*Phase 2 完成 | 下一阶段: 逐步开发 — Module 1 商户管理*

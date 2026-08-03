# CrossPay Learning Project

> 简化版跨境支付 SaaS Demo — 用于学习和面试展示

## 项目概述

CrossPay 是一个学习项目，模拟了一个跨境支付平台的核心功能。目标不是构建商业系统，而是理解支付业务的核心概念和工程挑战。

## 模块清单

| 模块 | 说明 | 对应真实业务 |
|------|------|------------|
| 1. Merchant Management | 商户注册、登录、信息管理 | 商户入驻平台 |
| 2. Payment Order | 支付订单创建 + 状态机 | 支付核心 |
| 3. Mock Payment Gateway | 渠道抽象 + 模拟非洲支付 | 渠道适配层 |
| 4. Settlement Engine | 日终结算 + 手续费计算 | 结算流程 |
| 5. Admin Console | 运营管理后台 | 内部运营系统 |
| 6. AI Payment Assistant | AI 驱动的订单查询助手 | AI 赋能运营效率 |

## 技术栈

- **Backend:** Java 17, Spring Boot 3, Spring Security, JWT, MySQL, Redis
- **Frontend:** Vue 3, TypeScript, Element Plus
- **Infra:** Docker Compose

## 快速启动

```bash
# 1. 启动 MySQL + Redis
docker-compose up -d mysql redis

# 2. 初始化数据库（自动执行 V1__init_schema.sql）

# 3. 启动后端
cd crosspay-backend
./mvnw spring-boot:run

# 4. 启动前端
cd crosspay-frontend
npm install && npm run dev

# 5. 访问
# 商户端: http://localhost:3000/login (demo@africashop.com / merchant123)
# 运营后台: http://localhost:3000/admin/login (admin / admin123)
```

## 学习文档

- [Phase 1: 业务理解](phase1-business-understanding.md)
- [Phase 2: 项目架构设计](phase2-project-design.md)
- [Phase 3: 逐步实现（各模块源码在 crosspay-backend/ 和 crosspay-frontend/）]

## 面试话术

> "我之前没有直接参与支付系统开发，但我对这个领域很感兴趣。我通过 AI 辅助学习了跨境支付业务流程，并独立实现了一个简化版 Demo。通过这个项目，我理解了：
>
> - 商户系统是支付平台的基础——不同商户有不同费率、不同结算周期
> - 支付订单必须用状态机管理——因为支付涉及外部系统，结果是异步且不可靠的
> - 渠道抽象（Adapter 模式）是支付系统的核心架构——加渠道不改业务代码
> - 支付和结算是两个独立概念——支付是即时行为，结算是 T+N 的资金汇总
> - 运营后台是支付公司的指挥室——异常处理、对账、商户管理都需要人工介入
>
> 这些概念让我对支付系统的复杂性和工程挑战有了真实的感受。"

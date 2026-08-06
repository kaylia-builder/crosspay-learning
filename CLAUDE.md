# CLAUDE.md — CrossPay Learning Project

> AI-assisted cross-border payment system for learning and interview preparation.

## Project Identity

- **Repo:** `kaylia-builder/crosspay-learning`
- **Goal:** Demonstrate FinTech engineering thinking — not production code, but serious learning artifacts
- **Audience:** Hiring managers at cross-border payment companies

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.2, Spring Security, JPA, JWT (jjwt 0.12) |
| Frontend | Vue 3.4, TypeScript 5.3, Element Plus 2.5, Axios |
| Database | MySQL 8.0 (JPA `ddl-auto: validate`), Redis 7 |
| Infra | Docker Compose (MySQL + Redis + Backend + Frontend) |

## Architecture Rules

- **Package by module, not by layer** — `com.crosspay.module.payment`, `.merchant`, `.gateway`, etc.
- **Entity / DTO separation** — API contracts never expose JPA entities
- **State machine in domain entity** — `PaymentOrder.markProcessing()` etc., no raw `setStatus()`
- **Gateway Adapter pattern** — `PaymentGateway` interface, implementations per channel
- **Settlement reads payment, never writes** — separate modules, separate lifecycles

## Key Design Decisions (ADR in `docs/ARCHITECTURE.md`)

1. Payment state as in-entity state machine
2. Gateway Adapter over direct integration
3. Settlement as separate module (not a column on PaymentOrder)
4. JWT over server-side sessions
5. Package-by-module over package-by-layer
6. AI Assistant as top-level cross-cutting package

## Demo Credentials

| Role | Login |
|------|-------|
| Merchant | `demo@africashop.com` / `merchant123` |
| Admin | `admin` / `admin123` |

## Startup

```bash
# Ensure MySQL & Redis are running (Docker Hub unreachable in this env)
sudo systemctl start mysql redis-server
# Init database (first time only)
mysql -u root -proot123 crosspay < crosspay-backend/src/main/resources/db/migration/V1__init_schema.sql
# Start services
cd crosspay-backend && mvn spring-boot:run &
cd crosspay-frontend && npm install && npm run dev &
# Merchant: http://localhost:3000/login
# Admin: http://localhost:3000/admin/login
```

## RBAC Model

- `ROLE_MERCHANT` → `/api/merchant/**`, `/api/payment/**`, `/api/settlement/**`
- `ROLE_ADMIN` / `ROLE_OPERATOR` → `/api/admin/**`, `/api/ai/**`
- Public → `/api/auth/**`, `/api/callback/**`

## Session Notes

### 2026-08-06 — Environment Setup & Bug Fixes

- **Docker 不可用**：Docker Hub 网络不通（IPv4/IPv6 均超时），改用 apt 安装本地 MySQL 8.4 + Redis
- **MySQL 8.4 兼容**：`mysql_native_password` 插件已移除，需用 `caching_sha2_password`
- **密码哈希错误**：迁移脚本中的 bcrypt 哈希不匹配 `admin123`/`merchant123`，用 python bcrypt 重新生成并更新数据库
- **前端 Vite 别名**：`@` 别名需同时在 tsconfig.json 和 vite.config.ts（resolve.alias）中配置
- **全角字符编译错误**：`AiAssistantService.java:108-110` 使用了中文引号 `"\""` 和全角问号 `？`，替换为 ASCII 字符
- **Dashboard 硬编码**：`MerchantService.getDashboard()` 返回硬编码 0，改为从 PaymentRepository 实时查询今日交易笔数/金额/成功率
- **跨端角色冲突**：同一浏览器 localStorage 的 role 会被 admin/merchant 互相覆盖，登录页加 `localStorage.clear()`，路由守卫改用 `to.matched`

### 2026-08-03 — Initial Build

- Built entire backend (43 Java files) and frontend (14 Vue components) in a 3-phase approach
- Phase 1: Business understanding → `phase1-business-understanding.md`
- Phase 2: System design → `phase2-project-design.md`
- Phase 3: Step-by-step implementation of all 6 modules
- Polished for resume: professional README, MIT license, ADRs in `docs/ARCHITECTURE.md`
- Pushed to `kaylia-builder/crosspay-learning` with 3 clean commits

## When Modifying This Project

- Keep it simple and clear — the code should be understandable in an interview context
- Every design decision should have a documented "why"
- Comments explain business context, not just what the code does
- The README and ADR documents are as important as the code

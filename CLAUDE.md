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
docker compose up -d mysql redis
cd crosspay-backend && mvn spring-boot:run
cd crosspay-frontend && npm install && npm run dev
# Merchant: http://localhost:3000/login
# Admin: http://localhost:3000/admin/login
```

## RBAC Model

- `ROLE_MERCHANT` → `/api/merchant/**`, `/api/payment/**`, `/api/settlement/**`
- `ROLE_ADMIN` / `ROLE_OPERATOR` → `/api/admin/**`, `/api/ai/**`
- Public → `/api/auth/**`, `/api/callback/**`

## Session Notes

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

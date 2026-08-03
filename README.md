<p align="center">
  <h1 align="center">🌍 CrossPay</h1>
  <p align="center"><strong>A Simplified Cross-Border Payment System</strong><br>Built for learning FinTech — not a toy, not a production system,<br>but a serious engineering exercise that demonstrates real-world payment architecture.</p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3">
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs&logoColor=white" alt="Vue 3">
  <img src="https://img.shields.io/badge/TypeScript-5.3-3178C6?logo=typescript&logoColor=white" alt="TypeScript">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Docker-🐳-2496ED?logo=docker&logoColor=white" alt="Docker">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License">
</p>

---

## Why This Project Exists

I interviewed for a cross-border payment company. I didn't have prior FinTech experience — so instead of just saying "I'm a fast learner," I built something.

This project is the result of a structured self-study process:

1. **Learn the domain** — how cross-border payments actually work (SWIFT, acquiring, settlement)
2. **Design the system** — database schema, API contracts, module boundaries
3. **Implement it** — 6 modules, each tackling a real payment engineering problem
4. **Explain it** — every design decision has a documented "why"

**The goal:** demonstrate that I can ramp up on a complex domain and make sound engineering decisions — even without prior industry experience.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                          CrossPay Platform                            │
│                                                                       │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐              │
│  │   Merchant   │──▶│   Payment    │──▶│   Gateway    │──▶ Mock Bank │
│  │   Management │   │   Order      │   │   Adapter    │              │
│  └──────┬───────┘   └──────┬───────┘   └──────────────┘              │
│         │                  │                                          │
│  ┌──────▼───────┐   ┌──────▼───────┐   ┌──────────────┐              │
│  │   Auth       │   │   Redis      │   │  Settlement  │              │
│  │   (JWT)      │   │   (Lock)     │   │  Engine      │              │
│  └──────────────┘   └──────────────┘   └──────────────┘              │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │                     Admin Console                             │    │
│  │  Merchant List │ Order Search │ Settlement Review │ AI Bot    │    │
│  └──────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────┘
```

**Key Design Decisions:**

| Decision | Rationale |
|----------|-----------|
| Package-by-module, not by-layer | Each module self-contained; easier to split into microservices later |
| Entity / DTO separation | API contracts evolve independently from database schema |
| Gateway Adapter pattern | Adding a new payment channel = one new class, zero changes to payment core |
| State machine in PaymentOrder entity | Business logic lives in the domain, not scattered across services |
| Separate Settlement module | Payment and settlement have different lifecycles; coupling them is a common anti-pattern |

---

## Modules

### 1. Merchant Management
- Merchant registration with BCrypt password hashing
- JWT-based authentication (stateless, no sessions)
- Role-based access: `ROLE_MERCHANT` vs `ROLE_ADMIN`

### 2. Payment Order + State Machine
```
CREATED ──▶ PROCESSING ──▶ SUCCESS
                       ──▶ FAILED
```
Each transition is a method on the entity (`markProcessing()`, `markSuccess()`, `markFailed()`) that validates the current state before allowing the change. Illegal transitions (e.g., FAILED → SUCCESS) throw `BusinessException`.

### 3. Payment Gateway (Adapter Pattern)
```java
public interface PaymentGateway {
    GatewayPayResponse pay(GatewayPayRequest req);
    String getChannelName();
}
```
`MockAfricaGateway` simulates a third-party payment API with realistic latency and probabilistic results (80% success, 15% failure, 5% processing). The `GatewayRouter` selects the channel — to add Stripe, you'd just implement the interface and register it.

### 4. Settlement Engine
```
NetAmount = TotalAmount − (TotalAmount × FeeRate)

Example: $1,000 × 3% fee = $970 merchant payout
```
Finds all SUCCESS orders for a merchant, aggregates them, applies the merchant's fee rate, and generates a settlement record. In production, this runs as a daily batch job.

### 5. Admin Console
Separate login flow for internal operations staff. Views for merchant onboarding, order investigation, settlement approval. Each API endpoint is gated by `ROLE_ADMIN`.

### 6. AI Payment Assistant
Natural-language order lookup. Operator types: *"Why did order PAY20240803000001 fail?"* — the assistant extracts the order number, queries the database, and returns a structured analysis with status, failure reason, and suggested action. Designed to be swappable with a real LLM API.

---

## Tech Stack & Why

| Technology | Why This Choice |
|------------|-----------------|
| **Java 17** | Industry standard for payment backends (Adyen, Stripe, PayPal all run on JVM) |
| **Spring Boot 3** | Mature ecosystem; Spring Security + JPA cover auth and persistence out of the box |
| **MySQL** | Relational guarantees for financial data — no eventual consistency on payment records |
| **Redis** | Idempotency keys and distributed locks for settlement jobs |
| **Vue 3 + Element Plus** | Clean separation of merchant-facing and admin-facing UIs |
| **JWT** | Stateless auth — payment APIs are high-throughput, can't afford server-side sessions |

---

## Quick Start

```bash
# 1. Start infrastructure
docker compose up -d mysql redis

# 2. Backend (requires Java 17+ and Maven)
cd crosspay-backend
mvn spring-boot:run               # First run downloads dependencies, starts on :8080

# If you don't have Maven, generate the wrapper first:
# mvn wrapper:wrapper              # then use ./mvnw spring-boot:run

# 3. Frontend (requires Node.js 18+)
cd crosspay-frontend
npm install && npm run dev         # Starts on :3000, proxies /api to :8080
```

### Demo Credentials

| Role | URL | Login |
|------|-----|-------|
| Merchant | `http://localhost:3000/login` | `demo@africashop.com` / `merchant123` |
| Admin | `http://localhost:3000/admin/login` | `admin` / `admin123` |

### Demo Flow

```
1. Login as merchant
2. Create a payment (e.g., $100 USD)
3. See the order transition through the state machine (CREATED → SUCCESS)
4. Trigger settlement → see $100 total, $3 fee, $97 net
5. Switch to admin: view all merchants, orders, settlements
6. Ask AI: "Why did order PAY20240803xxxxxx fail?"
```

---

## Project Structure

```
crosspay-backend/src/main/java/com/crosspay/
├── common/              # ApiResponse, BusinessException, OrderNoGenerator, SecurityConfig
├── module/
│   ├── auth/            # JWT provider, filter, login/register
│   ├── merchant/        # Merchant entity, CRUD
│   ├── payment/         # PaymentOrder entity + state machine, create/query
│   ├── gateway/         # PaymentGateway interface, MockAfricaGateway, GatewayRouter
│   ├── settlement/      # Settlement + SettlementDetail, fee calculation
│   └── admin/           # Admin login, merchant/order/settlement management APIs
└── ai/                  # AiAssistantService (natural language → order analysis)

crosspay-frontend/src/
├── api/                 # Axios wrapper + typed API modules
├── views/
│   ├── merchant/        # Dashboard, Transactions, Settlements
│   ├── payment/         # CreatePayment
│   └── admin/           # Merchants, Orders, Settlements, AiAssistant
├── components/          # MerchantLayout, AdminLayout
├── router/              # Vue Router with auth guards
└── types/               # TypeScript interfaces matching API responses
```

---

## What I Learned

This project forced me to grapple with questions that go beyond CRUD:

- **Idempotency:** If a payment callback arrives twice, the state machine rejects the second transition — no double-processing.
- **Failure modes:** Not just "success or fail" — what about timeout? Callback lost? Gateway returns PROCESSING then goes silent?
- **Data integrity:** Settlement reads payment data but never writes to it. Two modules, two lifecycles, no circular dependencies.
- **Abstraction discipline:** The `PaymentGateway` interface has one method. It would be tempting to add `refund()`, `query()`, `void()` — but until you have a second implementation, you don't know the right abstraction.
- **Operational reality:** A payment system without an admin console is a black box. Someone needs to investigate failures, approve settlements, and answer merchant questions.

---

## Learning Resources

- [Phase 1: Business Domain Analysis](phase1-business-understanding.md)
- [Phase 2: System Architecture & Database Design](phase2-project-design.md)
- [Architecture Decision Records](docs/ARCHITECTURE.md)

---

## License

MIT — use this code for your own learning, interviews, or as a starting point for real projects.

---

<p align="center">
  <sub>Built as a self-directed learning project. No prior FinTech experience — just curiosity and engineering fundamentals.</sub>
</p>

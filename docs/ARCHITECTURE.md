# Architecture Decision Records

This document captures the key design decisions made during the CrossPay project — and more importantly, **why** each decision was made. Understanding tradeoffs is what separates a senior engineer from someone who just writes code.

---

## ADR-1: Payment State as an In-Entity State Machine

**Context:** Payment orders need to track their lifecycle: created, sent to a channel, waiting for callback, succeeded, or failed.

**Decision:** The `PaymentOrder` entity has three transition methods — `markProcessing()`, `markSuccess()`, `markFailed()` — each validates the current state before allowing the transition. No `setStatus()` setter is exposed.

**Why:**
1. **Safety:** Illegal transitions (e.g., FAILED → SUCCESS) are impossible at the object level. No developer can accidentally write `order.setStatus("SUCCESS")` without going through the guard.
2. **Auditability:** Every transition has a single place to log, emit events, or trigger side effects.
3. **Idempotency:** If a callback arrives twice, the second `markSuccess()` call on an already-SUCCESS order throws `BusinessException` — we catch it and return 200 to the caller (so the channel doesn't retry forever).

**Tradeoff:** More boilerplate than a raw `status` field with an `@Enumerated` enum. Worth it because payment state correctness is the #1 correctness requirement.

**Real-world reference:** Stripe's PaymentIntent object uses an identical pattern — `status` transitions are server-side only and gated by allowed transitions.

---

## ADR-2: Gateway Adapter Pattern over Direct Integration

**Context:** The system needs to call external payment APIs. We started with one mock channel, but a real system connects to dozens.

**Decision:** Define a `PaymentGateway` interface with a single `pay()` method. Each channel (Stripe, Flutterwave, M-Pesa) implements it. A `GatewayRouter` selects the right implementation at runtime.

**Why:**
1. **Open-closed principle:** Adding a channel = one new class. Zero changes to `PaymentService`.
2. **Testability:** Unit tests can inject a mock gateway without hitting a real API.
3. **Independent evolution:** Each gateway's internal logic (auth headers, retry policy, error mapping) is isolated.

**Tradeoff:** The interface needs to be generic enough to represent wildly different APIs. This works for `pay()`, but `refund()` and `query()` might need separate abstractions later. We deliberately keep the interface minimal — avoid premature generalization.

**Real-world reference:** Adyen, Checkout.com, and Stripe all provide their own SDKs. An adapter layer translates each SDK's model into the platform's internal model.

---

## ADR-3: Settlement as a Separate Module (Not a Column on PaymentOrder)

**Context:** After a payment succeeds, the platform calculates fees and generates a settlement record showing what the merchant will receive.

**Decision:** Settlement has its own `settlements` table and `SettlementService`, completely separate from the payment module.

**Why:**
1. **Different lifecycles:** Payment is real-time / near-real-time. Settlement is daily batch (T+1). Mixing them in one module would couple a real-time system to a batch process.
2. **Read vs. Write:** Settlement reads payment data but never modifies it. If they shared a module, a developer might be tempted to `UPDATE payment_orders SET settled = true` — breaking the separation.
3. **Independent scaling:** In a real system, settlement queries are heavy aggregates (sum all orders for merchant X on date Y). That query pattern is different from payment's single-row lookups.

**Tradeoff:** `settlement_details` is a join table that duplicates the relationship between settlement and orders. This is acceptable for a learning project; in production you'd likely use event sourcing (PaymentSucceeded event → settlement projection).

---

## ADR-4: JWT over Server-Side Sessions

**Context:** Authentication for both merchant-facing and admin APIs.

**Decision:** Stateless JWT with Spring Security. Token contains `userId` and `role`. No server-side session store.

**Why:**
1. **Payment APIs are stateless by nature:** Each request is independent. A merchant's API call to create a payment doesn't need session affinity.
2. **Horizontal scaling:** No session replication needed. Any backend instance can validate any token.
3. **Role embedded in token:** The `role` claim avoids a database lookup on every request.

**Tradeoff:** Token revocation is hard — if a merchant is suspended, their existing tokens remain valid until they expire (24h in this demo). In production, you'd add a token blacklist in Redis or use short-lived tokens (5–15 min) with refresh tokens.

---

## ADR-5: Package-by-Module over Package-by-Layer

**Context:** How to organize Java packages.

**Decision:** `com.crosspay.module.payment`, `com.crosspay.module.merchant`, etc. — each module contains its own controller, service, repository, entity, and DTOs.

**Why:**
1. **Cohesion:** Everything related to payments lives together. No jumping between `com.crosspay.controller.PaymentController` and `com.crosspay.service.PaymentService` in different package trees.
2. **Microservice readiness:** If payment needs to become its own service, you move one package. With package-by-layer, you'd extract files from 4 different packages.
3. **Module boundaries are explicit:** A class in `module.settlement` importing from `module.payment` is a visible dependency.

**Tradeoff:** Shared code (like `ApiResponse`) needs a `common` package. The rule: if two modules need it, it goes to `common`. If only one module needs it, it stays in that module.

---

## ADR-6: AI Assistant as a Separate Top-Level Package

**Context:** The AI assistant queries payment data and returns natural language responses.

**Decision:** `com.crosspay.ai` is a top-level package that depends on `module.payment` but is not inside it. It has its own controller and service.

**Why:**
1. **AI is a cross-cutting capability:** It touches payment, settlement, and potentially merchant data. It doesn't belong to any single module.
2. **Swap-ready:** The current implementation is rule-based (regex → DB lookup → template). Replacing it with an actual LLM API call changes only `AiAssistantService` — the controller and the rest of the system are unaffected.
3. **Signals intent:** Making AI its own top-level package tells an interviewer: "I thought about where this belongs architecturally, not just where to put the file."

---

## What I'd Do Differently in Production

| Current (Learning) | Production |
|---|---|
| Single MySQL instance | Read replicas for queries, primary for writes |
| Synchronous gateway calls | Async with message queue (RabbitMQ / Kafka) |
| Settlement triggered manually | Cron job with distributed lock (Redis / ShedLock) |
| Hardcoded fee rate | Fee rules engine (per-channel, per-region, tiered) |
| No idempotency keys | Redis-based idempotency with TTL |
| Passwords in application.yml | Vault / AWS Secrets Manager |
| Monolithic backend | Payment core as a separate service from settlement |

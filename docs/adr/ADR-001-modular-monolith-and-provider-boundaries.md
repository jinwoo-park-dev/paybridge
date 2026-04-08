# ADR-001 — Choose a modular monolith with provider-specific module boundaries


## Context

PayBridge models approval, refund, and partial-reversal flows across two providers with very different integration styles.

The application must satisfy several constraints at the same time:

- remain straightforward to build, run, and debug as a single deployable unit
- keep a shared domain model for payment and reversal state
- integrate two very different providers:
  - NicePay key-in (provider-specific transport, EUC-KR form POST, crypto/signature requirements)
  - Stripe PaymentIntent / Refund / Webhook (SDK + browser element + webhook model)
- support browser-based checkout and operator flows without adding a separate frontend deployment stack
- avoid reintroducing provider-specific coupling into shared payment code

---
## Options considered

### Option 1 — Layered monolith with shared provider service
Pros:
- fastest to scaffold

Cons:
- provider behavior would easily collapse into `if/else` orchestration
- domain rules around payment / reversal / partial reversal would drift into controllers and services
- provider-specific transport and signing concerns would leak into shared application code

### Option 2 — Modular monolith with provider adapter boundaries
Pros:
- keeps a single deployable unit
- preserves a coherent domain model
- isolates provider-specific transport and signatures inside adapters
- offers a good balance of scope, testability, and domain clarity

Cons:
- requires stricter package discipline up front

### Option 3 — Microservices from day 1
Pros:
- clear deployment separation on paper

Cons:
- distributed boundaries do not add enough value at the current scale
- payment lifecycle, auditability, and provider handling become harder to complete and test
- local development and debugging become more operationally expensive

---
## Decision

Choose **Option 2 — Modular Monolith with provider-specific module boundaries**.

---
## Consequences

### Positive
- The application stays easy to run and reason about locally.
- Domain modeling remains first-class.
- NicePay and Stripe can differ at the transport and protocol layers while still sharing provider-independent commands and results.
- The payment consistency boundary stays explicit.

### Negative / trade-offs
- Package boundaries need to stay deliberate from the start.
- Some capabilities that could live in separate services in a larger platform remain in-process.
- Additional providers, a separate SPA, or broker infrastructure should only be introduced when they add clear value.

---
## Implementation notes

Packages are organized around the domain, provider, operator, and support boundaries:

- `payment.domain`
- `payment.application`
- `payment.web`
- `providers.nicepay`
- `providers.stripe`
- `operator.api`
- `operator.web`
- `web.*`
- `audit`
- `ops.outbox`
- `support.*`

Provider protocol differences are allowed.
Provider transport is **not** forced into artificial symmetry.
The consistency boundary is the **domain**, not the raw HTTP payload shape.

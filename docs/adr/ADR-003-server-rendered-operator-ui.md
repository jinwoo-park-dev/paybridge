# ADR-003 — Use Server-rendered Pages with Minimal JavaScript for the Operator UI


## Context
PayBridge includes browser-based checkout, NicePay key-in, transaction search, and payment detail actions. These workflows need interactive pages, but the implementation should remain closely aligned with the backend and payment domain.

---
## Decision
Use Spring MVC + Thymeleaf + minimal JavaScript as the default UI approach.

---
## Consequences

### Positive
- aligns with the backend-oriented architecture
- keeps the repository compact and easy to run locally
- supports interactive checkout and operator flows without a separate frontend deployment stack
- keeps payment orchestration, provider boundaries, lifecycle handling, and reliability concerns close to the server code

### Negative
- less frontend breadth than a React-based admin shell
- some richer UI behavior is intentionally deferred

---
## Follow-up
If operator workflows later require a richer frontend shell, add it as a separate UI layer without disturbing the core payment domain and provider modules.

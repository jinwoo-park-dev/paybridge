# ADR-002 — Choose Outbox-First Eventing Instead of Day-1 Kafka/RabbitMQ


## Context
PayBridge needs a reliable publication boundary for payment lifecycle events, retry handling, and auditability. The initial implementation should favor transactional consistency, local operability, and straightforward failure analysis.

---
## Decision
Use a transactional outbox table as the first event publication boundary. Persist domain-adjacent events in the primary database and defer broker adoption until it is justified by downstream consumers or operational scale.

---
## Consequences

### Positive
- event publication intent is explicit
- auditability is easier to preserve and inspect
- delivery scope stays proportional to the current architecture
- failure paths are easier to test in a single service

### Negative
- no live broker topology in the initial implementation
- an additional dispatcher or publisher layer is still needed if the system grows beyond database-backed publication

---
## Why not Kafka on day 1?
A transactional outbox captures the important consistency and eventing concerns without introducing separate broker infrastructure before it is operationally necessary.

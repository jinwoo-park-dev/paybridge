# PayBridge

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE.txt)

PayBridge is a payment orchestration backend in Java 21 and Spring Boot that keeps one shared payment and reversal lifecycle across two very different providers while isolating provider specific behavior behind clear adapter boundaries.

The project contrasts two very different payment paths:

- **Stripe**: PaymentIntents, browser confirmation, return page verification, refunds, and webhooks
- **NicePay**: merchant hosted keyed entry approval, EUC KR form posting, provider signing, approval recording, and cancellation APIs

The core question behind the project is not "how do I call two payment SDKs?" It is "how do I keep approval, reversal, and transaction visibility consistent when provider behavior is not symmetric?"

---
## Table of Contents
- [Demo walkthrough](#demo-walkthrough)
- [Why I built this](#why-i-built-this)
- [What this project demonstrates](#what-this-project-demonstrates)
- [Tech stack](#tech-stack)
- [System overview](#system-overview)
- [Why Stripe and NicePay together matter](#why-stripe-and-nicepay-together-matter)
- [Main flows](#main-flows)
- [Repository map](#repository-map)
- [Local run](#local-run)
- [Deployment](#deployment)
- [Delivery workflow](#delivery-workflow)
- [Routes](#routes)
- [Interface snapshots](#interface-snapshots)
- [Manual smoke test](#manual-smoke-test)
- [Reliability and security highlights](#reliability-and-security-highlights)
- [Testing](#testing)
- [Additional docs](#additional-docs)

---
## Demo walkthrough

<p align="center">
  <img src="docs/assets/paybridge-demo.gif" alt="Animated PayBridge demo walkthrough" width="900" />
</p>

<p align="center">
  <sub><strong>Demo scope:</strong> checkout selection → Stripe test payment → public result page → operator payment detail → refund or cancellation flow → operator transaction search.</sub>
</p>

---
## Why I built this

I built PayBridge to show how I think about real payment system problems as a backend engineer. The project is informed by earlier work around keyed entry card payments and by wanting a portfolio project that contrasts that model with Stripe’s PaymentIntent plus webhook approach.

That combination makes the project useful for two kinds of roles:

- **Payment and fintech backend roles** because it deals with provider heterogeneity, reversal semantics, idempotency, webhook retries, auditability, and operational traceability
- **General backend and SWE roles** because it shows modular design, transactional consistency, local developer experience, testing, and API plus UI integration in one repository

---
## What this project demonstrates

### For payment and fintech roles

- A **shared payment and reversal model** across providers with very different approval and reversal contracts
- **Idempotent write paths** for approvals and reversals
- **Duplicate safe Stripe webhook handling** through signature verification and event deduplication
- Clear **provider boundaries** so Stripe and NicePay differences stay inside provider modules and transport code instead of leaking into the shared payment model
- Practical handling of **partial reversals**, **provider identifiers**, and **operator transaction inspection**

### For general backend roles

- A backend focused codebase organized around domain, provider, operator, and support boundaries
- Consistent transactional side effects through **audit logs** and a **transactional outbox**
- Server rendered pages for manual workflows plus a **read only operator API** that can be enabled for review tooling
- Public provider test pages that end in **limited result pages**, while transaction detail and money moving actions stay behind operator authentication
- Clear **configuration, feature flags, security defaults, and local run paths**
- Shared support for **validation, error handling, logging, metrics, masking, and correlation IDs**
- Coverage across **unit, MVC, controller, and PostgreSQL Testcontainers integration tests**

---
## Tech stack

| Category | Choice |
| --- | --- |
| Language and runtime | Java 21 |
| Framework | Spring Boot 3.5.12 |
| Architecture style | Payment orchestration backend with clear module boundaries |
| UI | Spring MVC, Thymeleaf, minimal JavaScript |
| Database | PostgreSQL 16 |
| Schema management | Flyway |
| Payment providers | Stripe, NicePay |
| Reliability controls | Idempotency records, webhook dedupe, audit logs, outbox rows |
| Shared support | Spring Security, validation, error handling, masked logging, correlation IDs, Micrometer, Prometheus, OpenAPI |
| Testing | JUnit 5, Spring test slices, Testcontainers |
| CI | GitHub Actions |

---
## System overview

<p align="center">
  <img src="docs/assets/paybridge-architecture.png" alt="PayBridge architecture overview diagram" width="900" />
</p>

<p align="center">
  <sub><strong>Architecture view:</strong> one Spring Boot service handles the public checkout pages, public provider result pages, operator pages, and Stripe webhook ingress. Provider specific logic stays inside the Stripe and NicePay modules. The shared payment core records approvals and reversals. PostgreSQL persists lifecycle state. Shared support covers security, validation, masking, correlation IDs, metrics, and OpenAPI.</sub>
</p>

---
## Why Stripe and NicePay together matter

| Concern | Stripe | NicePay | Shared outcome in PayBridge |
| --- | --- | --- | --- |
| Approval path | PaymentIntent plus browser confirmation | Merchant hosted keyed entry approval request | One approved `Payment` record |
| Reversal path | Refund API | Cancellation API | One `PaymentReversal` model |
| Async behavior | Webhook follow up and retries | Mostly synchronous approval and cancellation response flow | Shared payment lifecycle visibility |
| Provider specific concerns | Hosted fields, webhook signature verification, browser return flow | EUC KR posting, request signing, keyed entry card data, provider response signature verification | Differences stay in provider modules |
| Operator needs | Search, inspect, refund, replay safety | Search, inspect, full cancellation, partial cancellation | Same transaction search and detail surface |

---
## Main flows

### 1) Stripe checkout

1. The browser reaches the public checkout surface and opens `/payments/stripe/checkout`.
2. PayBridge creates a Stripe PaymentIntent on the server through the Stripe provider module.
3. Stripe.js confirms the payment in the browser.
4. The return page calls back into PayBridge at `/payments/stripe/return`.
5. PayBridge retrieves the PaymentIntent, records or reuses the payment in the shared payment core, and renders a **public Stripe result page** with a limited summary.
6. If Stripe later sends a webhook for the same payment event, PayBridge converges on the same transaction record instead of creating a duplicate.

### 2) Stripe webhook handling

1. Stripe sends an event to `/api/providers/stripe/webhooks`.
2. PayBridge verifies the Stripe signature at a dedicated webhook ingress.
3. The webhook event is stored with a uniqueness guard on `(provider, provider_event_id)`.
4. A duplicate delivery returns a safe acknowledgement without replaying the business mutation.
5. Audit and outbox rows preserve what happened, and `payment_intent.succeeded` can converge on the same recorded payment.

### 3) NicePay keyed entry payment test

1. The browser opens the public NicePay test page at `/payments/nicepay/keyin`.
2. PayBridge validates the form, clears sensitive fields on validation failure, encrypts the keyed entry payload, and signs the provider request.
3. NicePay approval data is mapped into the shared payment model.
4. PayBridge redirects to `/payments/nicepay/result` and shows a **public NicePay result page** with a limited summary that omits raw card fields.
5. Full and partial cancellations remain operator only actions from the protected payment detail page.

### 4) Operator workflow

1. Search by `orderId` or provider identifiers at `/ops/transactions/search`.
2. Open `/payments/{paymentId}` to inspect lifecycle state and reversal history.
3. Use refund or cancellation actions from the detail page.
4. Review related JSON endpoints for transactions, machine readable export, audit logs, outbox events, and recent Stripe webhooks.

---
## Repository map

- [`src/main/java/com/paybridge/web`](src/main/java/com/paybridge/web) — home page, checkout selection, navigation model attributes, and system info endpoint
- [`src/main/java/com/paybridge/operator`](src/main/java/com/paybridge/operator) — operator login and read only JSON API controllers
- [`src/main/java/com/paybridge/payment`](src/main/java/com/paybridge/payment) — shared payment domain, commands, queries, persistence mapping, and operator payment search/detail views
- [`src/main/java/com/paybridge/providers/stripe`](src/main/java/com/paybridge/providers/stripe) — Stripe checkout, confirmation, public result flow, refunds, webhook flow, and MVC pages
- [`src/main/java/com/paybridge/providers/nicepay`](src/main/java/com/paybridge/providers/nicepay) — NicePay approval, public result flow, cancellation, crypto helpers, and EUC KR form handling
- [`src/main/java/com/paybridge/audit`](src/main/java/com/paybridge/audit) — audit persistence, write/query services, and API view models used by operator inspection endpoints
- [`src/main/java/com/paybridge/ops/outbox`](src/main/java/com/paybridge/ops/outbox) — transactional outbox persistence, query services, and API view models used by operator inspection endpoints
- [`src/main/java/com/paybridge/support`](src/main/java/com/paybridge/support) — security, configuration, validation, error handling, logging, metrics, and OpenAPI wiring
- [`src/main/resources/templates`](src/main/resources/templates) — server rendered pages for the public payment tests, public result pages, and operator workflows
- [`docs/adr`](docs/adr) — architecture decisions
- [`scripts/dev/bootstrap.sh`](scripts/dev/bootstrap.sh) — local PostgreSQL or full Docker Compose bootstrap helper
- [`docs/openapi/paybridge-public.yaml`](docs/openapi/paybridge-public.yaml) — checked in OpenAPI reference for JSON endpoints

---
## Local run

### Fastest path: local JVM plus Dockerized Postgres

```bash
cp .env.example .env
./scripts/dev/bootstrap.sh db

# Important:
# .env is used by Docker Compose, but bootRun does not automatically read it.
# Export the variables in your shell or configure them in your IDE before starting the app.

export PAYBRIDGE_DB_URL=jdbc:postgresql://localhost:5432/paybridge
export PAYBRIDGE_DB_USERNAME=paybridge
export PAYBRIDGE_DB_PASSWORD=paybridge
export PAYBRIDGE_OPERATOR_USERNAME=operator
export PAYBRIDGE_OPERATOR_PASSWORD=operator-change-me

./gradlew bootRun --args="--spring.profiles.active=local"
```

### Alternative: full Docker Compose stack

```bash
cp .env.example .env
# add your provider test keys if you want Stripe or NicePay to be runnable
./scripts/dev/bootstrap.sh full
```

### Required configuration by scenario

| Scenario | Required variables |
| --- | --- |
| Minimal local app | `PAYBRIDGE_DB_URL`, `PAYBRIDGE_DB_USERNAME`, `PAYBRIDGE_DB_PASSWORD` |
| Operator login | `PAYBRIDGE_OPERATOR_USERNAME`, `PAYBRIDGE_OPERATOR_PASSWORD` |
| Stripe test payment | `PAYBRIDGE_STRIPE_ENABLED=true`, `PAYBRIDGE_STRIPE_PROVIDER_ENABLED=true`, `PAYBRIDGE_STRIPE_PUBLISHABLE_KEY`, `PAYBRIDGE_STRIPE_SECRET_KEY` |
| Stripe webhook verification | `PAYBRIDGE_STRIPE_WEBHOOK_SECRET` |
| NicePay payment test | `PAYBRIDGE_NICEPAY_ENABLED=true`, `PAYBRIDGE_NICEPAY_PROVIDER_ENABLED=true`, `PAYBRIDGE_NICEPAY_MID`, `PAYBRIDGE_NICEPAY_MERCHANT_KEY` |
| Operator JSON API in prod style runs | `PAYBRIDGE_OPERATOR_API_ENABLED=true` |

See [`.env.example`](.env.example) and [`src/main/resources/application.yml`](src/main/resources/application.yml) for the full configuration surface.

---
## Deployment

### Runtime

The checked in deployment path under [`deploy/single-ec2`](deploy/single-ec2) matches the current public demo shape.

- **Cloudflare** sits in front of the origin for DNS, edge TLS, cache control, and rate limiting.
- **One AWS Graviton EC2 host** runs the public demo runtime.
- **Docker Compose** supervises Caddy, the PayBridge Spring Boot container, and PostgreSQL on the same host.
- **Caddy** terminates origin HTTPS and reverse proxies traffic to the app container.
- **PostgreSQL** runs on the same host because this deployment is intentionally scoped as a small public portfolio runtime.
- **Amazon ECR** stores the container images used by the EC2 host.
- **SSM Parameter Store** provides runtime configuration and secrets that are rendered into `.env.prod` on the host.
- **Session Manager** is the preferred server access path, so the deployment does not depend on inbound SSH.


### Deployment files

| File | Role |
| --- | --- |
| `deploy/single-ec2/compose.prod.yml` | Production Compose stack for Caddy, PayBridge, and PostgreSQL |
| `deploy/single-ec2/Caddyfile` | Origin HTTPS reverse proxy configuration |
| `deploy/single-ec2/env.prod.example` | Reference file for required production environment variables |
| `deploy/single-ec2/ssm-parameters.example.json` | Reference naming contract for SSM parameters |
| `deploy/single-ec2/scripts/bootstrap-ec2.sh` | EC2 host bootstrap for Docker, AWS CLI, Python, and Docker Compose |
| `deploy/single-ec2/scripts/render-env-from-ssm.sh` | Generates `.env.prod` from SSM Parameter Store |
| `deploy/single-ec2/scripts/deploy.sh` | Pulls the selected image and starts the Compose stack |
| `deploy/single-ec2/scripts/stop-stack.sh` | Stops the running Compose stack |
| `deploy/single-ec2/scripts/backup-postgres.sh` | Creates a PostgreSQL backup from the running container |
| `deploy/single-ec2/scripts/restore-postgres.sh` | Restores a PostgreSQL backup into the running container |

---
## Delivery workflow

### Continuous integration

The repository keeps a standard GitHub Actions test workflow at [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

That workflow runs on push and pull request and executes the Gradle test suite so core payment logic, MVC flows, provider utilities, and PostgreSQL Testcontainers integration paths are validated before release changes are promoted.

### Release workflow

The release workflow lives at [`.github/workflows/deploy-single-ec2.yml`](.github/workflows/deploy-single-ec2.yml) and is triggered through `workflow_dispatch`.

Its current behavior is:

1. Check out the repository.
2. Set up Java 21.
3. Run `./gradlew test` with provider network integrations disabled for the workflow run.
4. Assume an AWS role through GitHub OIDC.
5. Log in to Amazon ECR.
6. Build a Linux ARM64 image for the Graviton EC2 target.
7. Push the image to ECR.
8. Use SSM Run Command to invoke the EC2 host side deploy script.
9. Run a basic HTTPS smoke test against `/actuator/health` on the deployed domain.

This release model keeps CI automatic and keeps deployment explicit. On a single host public runtime, that makes release timing easy to control while still proving a real test, build, push, remote deploy, and smoke test path.

### Repository configuration required by the release workflow

**Secret**

- `AWS_ROLE_ARN`

**Repository variables**

- `AWS_REGION`
- `ECR_REPOSITORY`
- `EC2_INSTANCE_ID`
- `SSM_PATH`
- `DEPLOY_DIR`

These values line up with the EC2 bootstrap and host side deployment scripts already checked into `deploy/single-ec2`.

---
## Routes

| Route | Purpose | Auth |
| --- | --- | --- |
| `/` | Home / overview page | Public |
| `/checkout` | Choose provider specific payment test flow | Public |
| `/operator/login` | Operator sign in page | Public |
| `/operator/logout` | Operator sign out endpoint | Authenticated operator session |
| `/payments/stripe/checkout` | Stripe checkout page | Public |
| `/payments/stripe/payment-intent` | Create a Stripe PaymentIntent for the checkout page | Public |
| `/payments/stripe/return` | Stripe browser return handler and public result renderer | Public |
| `/payments/stripe/result` | Public Stripe result page for an existing PaymentIntent | Public |
| `/payments/nicepay/keyin` | NicePay keyed entry payment test page | Public |
| `/payments/nicepay/keyin/approve` | Submit the NicePay keyed entry payment test form | Public |
| `/payments/nicepay/result` | Public NicePay result page for a recorded approval | Public |
| `/ops/transactions/search` | Transaction search | Operator |
| `/payments/{paymentId}` | Payment detail and reversal actions | Operator |
| `/api/ops/transactions` | Read only transaction search JSON API | Operator when `operator-api-enabled=true`; off by default in the production profile |
| `/api/ops/transactions/export` | Machine readable transaction export JSON API with paging and approved at filters | Operator when `operator-api-enabled=true`; off by default in the production profile |
| `/api/ops/transactions/{paymentId}` | Read only transaction detail JSON API | Operator when `operator-api-enabled=true`; off by default in the production profile |
| `/api/ops/transactions/{paymentId}/audit-logs` | Audit log JSON API | Operator when `operator-api-enabled=true`; off by default in the production profile |
| `/api/ops/transactions/{paymentId}/outbox-events` | Outbox JSON API | Operator when `operator-api-enabled=true`; off by default in the production profile |
| `/api/ops/stripe-webhooks` | Recent Stripe webhook events | Operator when `operator-api-enabled=true`; off by default in the production profile |
| `/api/system/info` | Runtime metadata JSON endpoint | Public |
| `/swagger-ui/index.html` | Runtime OpenAPI UI | Public in local and dev; disabled in the production profile |
| `/api-docs` | Runtime OpenAPI JSON | Public in local and dev; disabled in the production profile |
| `/actuator/prometheus` | Prometheus scrape endpoint | Authenticated |

---
## Interface snapshots

The screenshots below are representative views of the checkout page and provider specific operator detail pages. The latest page copy and public result pages have been refreshed since some of these images were captured, but the underlying flows and page roles remain the same.

### Checkout selection

<p align="center">
  <img src="docs/assets/paybridge-checkout.png" alt="PayBridge checkout selection page" width="800" />
</p>

<p align="center">
  <sub><strong>UI snapshot:</strong> the public checkout page separates Stripe and NicePay payment test flows while making the provider contrast easy to understand at a glance.</sub>
</p>

### Payment detail and reversal history

<p align="center">
  <img
    src="docs/assets/paybridge-stripe-payment-detail.png"
    alt="PayBridge Stripe payment detail page showing provider linkage, refund controls, and recorded reversal history"
    width="800"
  />
</p>

<p align="center">
  <sub><strong>Operator view:</strong> one Stripe backed payment record brings together provider linkage, current money state, related JSON inspection endpoints, refund controls, and reversal history. <br>In this example, a full refund has already succeeded, so the remaining reversible balance is USD 0.00 and the completed reversal is visible on the same page.</sub>
</p>

<br>

<details>
  <summary><strong>🖼️ Click to view NicePay payment detail page</strong></summary>
  <p align="center">
    <img src="docs/assets/paybridge-nicepay-payment-detail.png" alt="PayBridge NicePay payment detail page" width="800"/>
  </p>
</details>

---
## Manual smoke test

1. Open the home page and verify the feature flags reflected in the UI.
2. Run a Stripe test payment from `/payments/stripe/checkout`.
3. Confirm that the return flow lands on the public Stripe result page and then sign in to inspect the protected payment detail page.
4. Execute a Stripe refund from the operator detail page and verify that reversal history updates.
5. Search by `orderId` at `/ops/transactions/search`.
6. If NicePay test credentials are configured, run a NicePay keyed entry payment test from `/payments/nicepay/keyin` and confirm that the public result page appears without exposing card data.
7. Sign in and verify that NicePay full and partial cancellations remain available only from the protected payment detail page.
8. Review `/api/ops/transactions/export` for raw reconciliation friendly fields and pagination.
9. Review `/api/ops/transactions/{paymentId}/audit-logs` and `/api/ops/transactions/{paymentId}/outbox-events`.

---
## Reliability and security highlights

- **Idempotency records** on approval and reversal write paths
- **Webhook duplicate suppression** for Stripe events
- **Transactional outbox rows** persisted with payment lifecycle changes
- **Audit logs** for approvals, reversals, webhook receipt, duplicate detection, and failures
- **Correlation IDs** propagated through request handling and error responses
- **Masked logging** for sensitive values and provider identifiers
- **Operator only routes** for transaction search, payment detail, refunds, cancellations, and JSON exports
- **Public provider result pages** that keep checkout confirmation separate from internal operator detail
- **Feature flags** to disable providers cleanly and to keep the operator JSON API off by default in the production profile

---
## Testing

The project includes:

- domain tests for payment and reversal rules
- controller and form validation tests
- provider utility tests for Stripe and NicePay helpers
- PostgreSQL Testcontainers integration tests for persistence, operator pages, webhook flow, and security behavior

GitHub Actions runs the Gradle test suite on push and pull request.

---
## Additional docs

- [ADR-001 — modular monolith and provider boundaries](docs/adr/ADR-001-modular-monolith-and-provider-boundaries.md)
- [ADR-002 — outbox-first eventing](docs/adr/ADR-002-outbox-first-over-broker.md)
- [ADR-003 — server-rendered operator UI](docs/adr/ADR-003-server-rendered-operator-ui.md)
- [Checked in OpenAPI spec](docs/openapi/paybridge-public.yaml)

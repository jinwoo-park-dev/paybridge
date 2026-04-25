# ADR-004: Choose single EC2 Docker Compose deployment for the public demo

## Context

PayBridge needs a public deployment that proves the backend can be built, released, and operated beyond local development. The runtime should stay proportional to the project scope: one Spring Boot service, provider test flows, operator pages, PostgreSQL persistence, and a small amount of expected traffic.

The deployment must satisfy these constraints:

- keep monthly cost predictable for a personal project
- run the service, reverse proxy, and PostgreSQL with a simple operational model
- keep Stripe, NicePay, database, and operator credentials out of source control and out of the image
- support public Stripe and NicePay test flows
- keep transaction detail, refunds, cancellations, audit views, outbox views, and JSON exports behind operator authentication
- preserve a real release path from automated tests to image build, image registry, host deployment, and health check

A larger AWS plan was considered: ECS on EC2, ECR, ALB, RDS PostgreSQL, and SSM Parameter Store. It would show more AWS managed service coverage, but it adds fixed cost and more infrastructure than this demo currently needs.

The selected runtime uses one AWS Graviton EC2 host. Docker Compose runs Caddy, PayBridge, and PostgreSQL. Caddy handles HTTPS and reverse proxying. Amazon ECR stores application images. SSM Parameter Store provides runtime configuration and secrets. Session Manager is used for host access. GitHub Actions runs tests, builds an ARM64 image, pushes it to ECR, updates the EC2 host through SSM Run Command, and checks the public health endpoint.

---
## Options considered

### Option 1: ECS on EC2 + ECR + ALB + RDS PostgreSQL + SSM Parameter Store

Pros:
- shows ECS, ECR, ALB, RDS, IAM, and SSM in the running environment
- separates application runtime from database persistence
- gives a more managed AWS topology than a single host
- leaves a clearer path to scale the app and database separately

Cons:
- adds ALB and RDS cost for a low traffic demo
- requires ECS cluster, task definition, service, target group, capacity, and database networking decisions
- creates more failure points than this one service demo needs
- still requires EC2 host or capacity management when using ECS on EC2
- shifts attention toward infrastructure before the project needs that operating model

### Option 2: Single Graviton EC2 host + Docker Compose + Caddy + same host PostgreSQL

Pros:
- keeps the public demo online with lower and more predictable cost
- keeps the runtime easy to inspect, debug, stop, restore, and redeploy
- uses a real container release path instead of local execution only
- supports GitHub Actions, ECR, SSM Run Command, Docker Compose, and health checks
- avoids ALB, RDS, NAT Gateway, and ECS service configuration for the current scope
- keeps PostgreSQL persistence simple while still supporting explicit backup and restore scripts

Cons:
- does not show ECS service operations, ALB routing, or RDS administration in the live runtime
- PostgreSQL is not managed by RDS
- one EC2 host means no high availability
- host patching, disk capacity, Docker cleanup, backup, and restore remain owner responsibilities
- the deployment should not be described as a production payment platform

---
## Decision

Choose **Option 2: Single Graviton EC2 host + Docker Compose + Caddy + same host PostgreSQL**.

This deployment best fits the current project scope. PayBridge is a single backend service with public provider test flows and protected operator workflows. The chosen runtime gives enough operational proof without adding fixed cost and infrastructure surface that would mainly serve the deployment itself rather than the payment backend.

The selected approach is still a real deployment path. GitHub Actions validates the project with tests, builds an ARM64 Docker image, pushes the image to ECR, updates the EC2 host through SSM Run Command, runs the Docker Compose release, and checks `/actuator/health` over HTTPS.

---
## Consequences

### Positive

- The public demo can stay online at a predictable cost.
- The runtime remains small enough to debug and recover quickly.
- The repository contains a path from tests to image build to EC2 release.
- Caddy provides HTTPS without requiring ALB and ACM for this demo.
- SSM Parameter Store keeps provider, database, and operator credentials outside Git and outside the Docker image.
- Session Manager avoids opening SSH to the public internet.
- PostgreSQL data is kept in Docker volumes, with backup and restore scripts available.

### Negative

- The live runtime does not demonstrate RDS, ALB, or ECS service operations.
- Same host PostgreSQL requires disciplined backups.
- There is no high availability.
- The EC2 host must still be patched, monitored, and cleaned up.
- If the project grows beyond a single service demo, this decision should be revisited.

---
## Implementation notes

Deployment files live under `deploy/single-ec2`.

Runtime components:
- Caddy terminates HTTPS and proxies traffic to PayBridge.
- PayBridge runs as a Spring Boot container built from the repository Dockerfile.
- PostgreSQL runs on the same EC2 host with a named Docker volume.
- ECR stores release images.
- SSM Parameter Store provides runtime configuration and secrets.
- Session Manager is used for host access.

Release flow:
1. GitHub Actions runs the Gradle test suite.
2. GitHub Actions builds a Linux ARM64 Docker image.
3. The image is pushed to ECR.
4. Deployment files are synced to the EC2 host.
5. If PostgreSQL is already running, a backup is created before deployment.
6. SSM Run Command invokes the host deployment script.
7. The workflow checks `/actuator/health` over HTTPS.

Public and protected boundaries:
- Public: home page, checkout page, Stripe checkout, Stripe result, NicePay key in test page, and NicePay result.
- Protected: transaction detail, refunds, cancellations, audit views, outbox views, and JSON exports.
- PostgreSQL port `5432` and Spring Boot port `8080` are not exposed publicly.
- NicePay public testing uses provider test credentials only.

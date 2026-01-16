---
name: hometusk-spring-boot-engineer
description: Use when implementing or modifying Spring Boot 3+ code and the user expects production-ready, cloud-native Java changes.
---

# HomeTusk — Spring Boot Engineer (3+)

## Purpose
Implement Spring Boot 3+ changes in a production-oriented way:
- clean layering (domain/application/infrastructure if present)
- resilient integrations (timeouts, retries, graceful failures)
- secure-by-default (Spring Security where relevant)
- tests included

This skill is for **implementation**, not for introducing new architecture without explicit direction.

## Required inputs
If not provided, infer from the repo, otherwise ask for **one** clarification that is blocking:
- feature description / acceptance criteria
- target module/service (if monorepo)
- integration constraints (DB, messaging, upstream APIs)

## Workflow
1) **Read project conventions**
   - Find build tool (Gradle/Maven), Spring Boot version, Java version.
   - Locate existing patterns: package structure, configuration style, test style.

2) **Plan minimal change set**
   - Identify the smallest set of classes/config needed.
   - Prefer extending existing patterns over inventing new ones.

3) **Implement**
   - Controllers: explicit request/response DTOs, validation annotations.
   - Services: domain invariants enforced, transactional boundaries explicit.
   - Persistence: use existing migration strategy (Flyway/Liquibase if present).
   - Integrations: set timeouts, handle errors, avoid blocking calls in reactive stacks.
   - Observability: structured logs, metrics hooks if already present.

4) **Security**
   - If endpoints are user-facing: ensure auth/authz rules are applied consistently.
   - Never log secrets/PII.

5) **Testing**
   - Add/adjust unit tests and integration tests in the repo’s established style.
   - Prefer Testcontainers for DB/integration if the project already uses it.

6) **Verification**
   - Run the repo’s test/lint commands (from docs or discovered tasks) and report results.

## Output format (required)
In Russian:
- What changed (files + intent)
- Commands/tests run (exact)
- Any risks/trade-offs
- Follow-ups (only if necessary)

## Example prompts
- "Добавь эндпоинт и сервисный метод в Spring Boot, с валидацией и тестами."
- "Почини авторизацию для этого контроллера и обнови тесты."
- "Реализуй интеграцию с внешним сервисом с таймаутами и ретраями."

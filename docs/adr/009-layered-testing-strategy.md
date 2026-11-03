# ADR-009: Layered tests, with Oracle Database Free in Testcontainers

- **Status:** Proposed
- **Date:** 2026-10-22
- **Related:** [#78](https://github.com/angelgonzalezg/event-iq/issues/78), [Testing](../testing.md), [ADR-002](002-oracle-database-free.md), [ADR-003](003-gemini-through-spring-ai.md), [ADR-008](008-pinned-versions.md)

## Context

Until Sprint 4, testing was implicit. JUnit 5 and Mockito came with the Spring Boot test starters and `HomeControllerTests` showed how to test a view, but nothing asked for tests: the Definition of Done required only the Postman collection, and the `python` check compiled the analytics service without testing it. The stories planned for Sprints 5–8 hold the rules most likely to break silently: registration capacity, the payment status lifecycle, the security chains, the report queries and the analytics models.

The tests have to run in CI on every pull request, and CI has no Gemini key, no analytics service and no Oracle database of its own. They also have to be fast enough that two engineers run them before every pull request.

Most of the data rules live in Oracle itself: identity columns, `CHECK` constraints with regular expressions, unique and function-based indexes, and `db/schema.sql` is a SQL*Plus script.

## Decision

We test each layer with its own tool, and new logic arrives with its tests in the same pull request, as the Definition of Done requires:

| Layer | Tool |
|---|---|
| Business rules in services | JUnit 5 + Mockito, without Spring |
| View and API controllers | `@WebMvcTest` with mocked services (`@MockitoBean`) |
| Security rules | `spring-security-test`, with the real security configuration |
| Repositories: custom queries and schema constraints | `@DataJpaTest` against Oracle Database Free started by Testcontainers |
| Clients of Gemini and the analytics service | A mocked `ChatModel`; `MockRestServiceServer` |
| Analytics service | pytest with FastAPI's `TestClient` and `monkeypatch` |

- Tests that start Oracle are named `*IT` and run with Failsafe in `./mvnw verify`; the rest are named `*Tests` and run with Surefire in `./mvnw test`. The `java` check runs `./mvnw -B verify` and the `python` check runs `uv run pytest`.
- `OracleTestDatabase` (in `src/test/java/com/eventiq/`) is the only place that defines the test container. It uses `gvenzl/oracle-free:23.26.3-slim-faststart`, the same Oracle version as the application's container and the image the Testcontainers Oracle Free module is built for, and it creates the tables by running `db/schema.sql` with SQL*Plus inside the container.
- No test calls Gemini or the analytics service.
- Postman keeps documenting the API and serves as a manual smoke test; it is not part of the automated suite.

The conventions and examples are in [docs/testing.md](../testing.md).

## Alternatives considered

- **Postman as the only test tool:** it needs the whole application and Oracle running, does not run in CI and cannot isolate a business rule, so it finds regressions late.
- **H2 in Oracle compatibility mode for repository tests:** fast and needs no Docker, but it does not reproduce the full Oracle dialect or the schema's constraints, and it cannot run `db/schema.sql`. Tests could pass without testing what runs in production.
- **The official Oracle image in Testcontainers:** it is the image the application uses, but the Testcontainers Oracle Free module expects the `gvenzl/oracle-free` image and its configuration, and the official image starts more slowly.
- **Calling Gemini from the tests:** slow, different answers on every call, quota spent on every run, and CI has no key.
- **A coverage threshold as a merge gate:** a number invites tests written to reach it; the review checks the acceptance criteria instead.

## Consequences

- Business rules, controllers and clients are tested in seconds, in CI and before every pull request.
- The SQL and the constraints are tested against the engine that runs them, and every change to `db/schema.sql` is exercised by the repository tests.
- `./mvnw verify` now needs Docker running. The first run downloads the Oracle test image, which takes a few minutes, and each run spends some extra time starting the container; CI pays that cost on every pull request.
- The test image is one more pinned version, listed in [Dependency updates](../ways-of-working.md#dependency-updates); it moves together with the application's Oracle image.
- Reviews now also check the tests, which adds some time to each pull request.

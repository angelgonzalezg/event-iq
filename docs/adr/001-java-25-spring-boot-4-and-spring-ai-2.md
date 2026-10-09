# ADR-001: Java 25, Spring Boot 4 and Spring AI 2.0

- **Status:** Accepted
- **Date:** 2026-10-05
- **Related:** [#10](https://github.com/angelgonzalezg/event-iq/issues/10), [PR #62](https://github.com/angelgonzalezg/event-iq/pull/62), [ADR-003](003-gemini-through-spring-ai.md), [ADR-008](008-pinned-versions.md)

## Context

EventIQ is a Spring MVC and REST application, and its Events module needs a language model for the assistant (ADR-003). Spring AI is the Spring project for that, and its two lines have different baselines:

- Spring AI 1.x runs on Spring Boot 3. Spring Boot 3.5, the last 3.x line, reached the end of its open source support on 30 June 2026, before the project started.
- Spring AI 2.0 requires Spring Boot 4 and Java 21 or later.

The project runs for eight weeks from late September 2026 and stays public as a portfolio, so it should start on versions that still receive fixes. Java 25, released in September 2025, is the most recent long-term support (LTS) release.

## Decision

The application uses Java 25 (Eclipse Temurin), Spring Boot 4.1 and Spring AI 2.0, generated with Spring Initializr. The versions are pinned in `pom.xml`: `java.version`, the version of `spring-boot-starter-parent`, and the `spring-ai.version` property, which imports the Spring AI BOM. Spring Boot manages the versions of its starters. Maven runs through the wrapper (`./mvnw`), which pins Maven itself.

## Alternatives considered

- **Spring Boot 3.5, Spring AI 1.1 and Java 17:** the setup most tutorials show, but Spring Boot 3.5 no longer receives open source fixes, and moving to Spring AI 2.0 later would mean a migration in the middle of the project. Java 17 is also below Spring AI 2.0's minimum.
- **Java 21:** also an LTS release, and supported by Spring AI 2.0. It stays as the fallback if a tool fails on Java 25; switching means changing `java.version`, the CI setup and, once it exists, the application's Dockerfile.

## Consequences

- The stack is supported for the whole project: Spring Boot 4.1 receives open source fixes until mid-2027.
- Much of the material online targets Spring Boot 3 and Spring AI 1.x, and it fails in ways that are easy to miss:
  - Spring Boot 4 split its auto-configuration into smaller modules. Some starters changed names (this project uses `spring-boot-starter-webmvc`), and test annotations moved packages: `@WebMvcTest` is in `org.springframework.boot.webmvc.test.autoconfigure`.
  - Spring AI 2.0 dropped the `.options` segment from its properties: `spring.ai.google.genai.chat.model`, not `...chat.options.model`. The old keys still work as deprecated aliases, so a copied example shows no error.
- Every machine and CI need JDK 25, and Maven must run on it: `./mvnw -v` has to report Java 25. `scripts/check-setup.sh` verifies it, and [setup troubleshooting](../setup-troubleshooting.md#release-version-25-not-supported) covers `release version 25 not supported`.
- Spring Initializr does not stop anyone from combining Java 17 with Spring AI 2.0; with that combination, the project does not compile.

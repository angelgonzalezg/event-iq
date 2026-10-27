# ADR-006: Functional modules owned end to end by one engineer

- **Status:** Accepted
- **Date:** 2026-10-05
- **Related:** [`CODEOWNERS`](../../.github/CODEOWNERS), [Ways of working: Team](../ways-of-working.md#team), [ADR-004](004-form-login-for-views-jwt-for-api.md)

## Context

Two engineers build EventIQ in eight weeks, one interested in applied AI and the other in data science. By the end, each has to be able to explain a complete feature in an interview: data model, Java CRUD, views and a differentiating component. They also need to work in parallel without editing the same files every day.

## Decision

The work is split by functional module, not by layer:

| Module | Focus | Differentiating component |
|---|---|---|
| Events & Attendees | Applied AI | Conversational assistant with RAG (ADR-005) |
| Payments & Reports | Data science | Python analytics service: exploratory analysis, revenue prediction and comment sentiment |

- Each engineer owns a module from the database to the views: its Java packages, templates and message file, plus `analytics-service/` for Payments & Reports. Ownership is recorded in `CODEOWNERS`, which requests the owner's review automatically, and summarized in [Ways of working: Team](../ways-of-working.md#team); it is not repeated in the code.
- Each shared piece has a single owner: the core entities, security, sample data and Docker Compose. The Tech Lead owns the platform (build, CI, shared configuration and layout) and everything not assigned to a module, and both engineers review the shared schema in `db/`.
- Anyone may change a file they do not own, through a pull request that its owner reviews. Every pull request needs an approval from another team member.
- Each `package-info.java` documents its package's purpose and boundaries: `payments` may depend on `events`, because registrations link the modules, but never the reverse; only `ai` calls the language model, and `analytics` is the client of the analytics service.

## Alternatives considered

- **Split by layer (backend and frontend):** one engineer would not be able to explain the whole system.
- **Shared ownership of the setup and security:** duplicated work and constant conflicts in the same files.

## Consequences

- Both engineers work in parallel with few merge conflicts, and each ends with a complete story to tell.
- Both write CRUD in Java, so neither loses the Oracle and Java fundamentals while specializing.
- The modules depend on each other at a few points that need planning: Payments needs the Events entities early, the security story protects both modules, and schema changes need both reviews.
- Each engineer knows one module much better than the other. Cross-reviews, cross-testing before the release and explaining each other's module in the demo keep that gap small.

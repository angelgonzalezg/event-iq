# ADR-002: Oracle Database Free instead of Autonomous Database

- **Status:** Accepted
- **Date:** 2026-10-05
- **Related:** [README: Getting Started](../../README.md#getting-started), [Shared database model](../er-diagram.md), [ADR-005](005-sql-retrieval-as-rag-baseline.md), [ADR-008](008-pinned-versions.md)

## Context

Both modules share one Oracle database: the Spring Boot application writes events, registrations and payments, and the analytics service reads them. Oracle offers two free options:

- **Autonomous AI Database:** the managed cloud service, also available as a free container (`adb-free`). What sets it apart is autonomous operation (patching, tuning, backups) and Select AI, which turns natural-language questions into SQL inside the database.
- **Oracle Database Free:** the free edition of the Oracle engine, successor to Express Edition (XE), distributed as a container image.

Each engineer runs the database on a laptop during development, the whole stack must be free, and the setup week is the one with the most friction.

## Decision

Development uses Oracle Database Free in Docker, from the pinned image `container-registry.oracle.com/database/free:23.26.3.0` (Oracle AI Database 26ai Free).

- The application data lives in the `FREEPDB1` pluggable database, under a single user, `app_user`, which both Spring Boot and the analytics service connect as.
- Connections use the thin drivers (`ojdbc17` and `python-oracledb`) with a plain `localhost:1521/FREEPDB1` address: no wallet and no Oracle Client.
- Data persists in the `oradata` Docker volume.

## Alternatives considered

- **Autonomous AI Database:** autonomous operation adds nothing to a database that each engineer runs in a local container, while Oracle Database Free offers the same SQL engine over standard JDBC, without a wallet. Select AI is the feature worth seeing, so it stays as an optional, isolated exploration (see the [Roadmap](../../README.md#roadmap)), not as the project's database.

## Consequences

- The same SQL dialect and JDBC driver as any Oracle database, with connection settings that work the same on every machine, offline and at no cost.
- AI Vector Search is included in Oracle Database Free, so the assistant can move to semantic retrieval later without changing databases (ADR-005).
- Each machine needs Docker with at least 4 GB of memory, and the first start takes several minutes. The image runs natively on Apple Silicon (`arm64`).
- The project shows little of Oracle's cloud services. The Select AI exploration makes up for part of that, but only if someone completes it.
- A deployment to Oracle Cloud Infrastructure, also on the Roadmap, would mean choosing a managed database at that point and revisiting this decision.

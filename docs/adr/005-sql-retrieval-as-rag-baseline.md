# ADR-005: SQL-based retrieval as the RAG baseline

- **Status:** Accepted
- **Date:** 2026-10-05
- **Related:** [#28](https://github.com/angelgonzalezg/event-iq/issues/28), [#30](https://github.com/angelgonzalezg/event-iq/issues/30), [#34](https://github.com/angelgonzalezg/event-iq/issues/34), [ADR-002](002-oracle-database-free.md), [ADR-003](003-gemini-through-spring-ai.md)

## Context

The assistant answers questions about events with retrieval-augmented generation (RAG): it retrieves data from Oracle, adds it to the prompt and lets the model write the answer, so the answer comes from real data instead of the model's guesses.

Retrieval can work in two ways:

- **A SQL query** over the structured data, such as the upcoming events.
- **Semantic search:** store an embedding of each event's description and retrieve the events closest in meaning to the question, using Oracle AI Vector Search, which Oracle Database Free includes (ADR-002).

The assistant is the Events module's differentiating component and has to work within the eight-week plan. Semantic search adds an embedding model and its Spring AI starter, a `VECTOR` column, embeddings generated on every create and update plus a backfill, and driver-specific code to bind vectors.

## Decision

The baseline assistant retrieves its context with SQL queries over the structured data, written with Spring Data JPA. A starting point is the upcoming events ordered by date, one line of context per event, and a system prompt that tells the model to answer only from that context and to say so when the answer is not there.

This ADR settles the approach, not its details. The RAG design spike (#28) decides which data is retrieved, the context format, the row limit and the prompt, and records them in `docs/rag-design.md` and an ADR of its own.

Semantic retrieval with AI Vector Search is an optional improvement (#34), time-boxed to 3–4 hours and attempted only once the baseline works. If it is built, the README compares both versions with the spike's test questions.

## Alternatives considered

- **Vector search from the start:** more moving parts in the module's central component, and a higher risk of reaching the demo without a working assistant.

## Consequences

- The assistant can be built in one sprint with tools the engineer already uses (Spring Data JPA), and tested with a mocked repository and model.
- The context is exact data from the database, so answers about dates, places and capacity are reliable.
- The assistant only knows what the queries retrieve. Questions about a topic, or about events similar to another one, depend on the model reading through the context, and the prompt grows with the rows retrieved; that is why the spike sets a row limit. Beyond the sample data (about 50 events), better filters or semantic retrieval become worth their cost.
- Select AI, which turns questions into SQL inside the database, is only available on Autonomous Database (ADR-002). It stays as an isolated exploration that can be compared with this implementation.

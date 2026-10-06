# Architecture Decision Records

An architecture decision record (ADR) captures one significant decision: the context that made it necessary, what was decided, the alternatives that were rejected and the consequences the team accepts. ADRs explain why the code looks the way it does, so nobody has to rediscover the reasoning, or undo a decision without knowing what it protects.

The list of ADRs is in the README, under [Architecture Decisions](../../README.md#architecture-decisions). When to write one is described in [Ways of working: Architecture decisions](../ways-of-working.md#architecture-decisions). This page explains how.

ADR-001 to ADR-008 were recorded after the fact: they describe the decisions made while planning the project and building the base platform (v0.1.0).

## Writing an ADR

1. Copy [`template.md`](template.md) to `NNN-short-title.md`, using the next free number. If two open pull requests take the same number, the one merged second renumbers its ADR.
2. Fill it in with the status **Proposed**, and add it to [Architecture Decisions](../../README.md#architecture-decisions) in the README.
3. Open a pull request, by itself or together with the change it describes, and discuss the decision in the review.
4. Once the reviewers agree, set the status to **Accepted** before merging.

Keep it short: one decision per ADR, and about a page. Write the context so that someone who joins later understands it, list only the alternatives that were actually considered, and include the negative consequences as well as the positive ones.

## Changing a decision

Accepted ADRs are not rewritten, because they record what was decided, and why, at the time. To change a decision, write a new ADR that explains what changed, and set the old one's status to **Superseded by ADR-NNN**, with a link to the new one, both in the old ADR and in the README. Fixing a typo or a broken link is fine.

# ADR-008: Pinned versions everywhere, changed only through pull requests

- **Status:** Accepted
- **Date:** 2026-10-05
- **Related:** [#13](https://github.com/angelgonzalezg/event-iq/issues/13), [PR #65](https://github.com/angelgonzalezg/event-iq/pull/65), [Dependency updates](../ways-of-working.md#dependency-updates), [ADR-002](002-oracle-database-free.md), [ADR-007](007-uv-for-python-dependencies.md)

## Context

Two engineers and CI must run the same tools: a bug that shows up on one machine and not on another wastes hours, especially in a team that is still learning the stack. Floating versions change without notice:

- An image tagged `latest` can be a different Oracle version for each person who pulls it, and the `oradata` volume stays tied to the version that created it.
- A version range or an unpinned package resolves to whatever is newest on the day of the install.
- A tag on a third-party GitHub Action can be moved to different code, which then runs in CI.

## Decision

Every library, container image and build tool the project depends on is pinned to an explicit version. Changing one, or adding or removing a dependency, is ordinary work: an issue, a branch and a reviewed pull request.

| What | How it is pinned |
|---|---|
| Java, Spring Boot, Spring AI | `java.version`, the parent version and `spring-ai.version` in `pom.xml`; Spring Boot manages the versions of its starters |
| Maven | The Maven wrapper |
| Oracle Database Free | Image tag `23.26.3.0`, never `latest` |
| Python and its packages | `.python-version`, `==` in `pyproject.toml`, and `uv.lock` (ADR-007) |
| uv | Exact version in CI |
| GitHub Actions | Official `actions/*` by major tag, such as `@v7`, so they receive GitHub's patches; third-party actions by full commit SHA, with the version as a comment |
| Bootstrap, Bootstrap Icons, Chart.js | Exact version in the CDN URL, plus an `integrity` (SRI) hash that the browser checks |
| Gemini model | A specific model name as the default in `application.yml`, overridable with `AI_MODEL` |

A few versions float on purpose, because pinning them would add upkeep without making builds more reproducible: the JDK and Python are pinned to their release (25 and 3.14), not to a patch; local uv installs need 0.12 or later; and CI runs on GitHub's `ubuntu-latest` runner.

[Dependency updates](../ways-of-working.md#dependency-updates) lists the commands and every file where each version lives. When a tool appears in several files, all of them change in the same pull request.

## Alternatives considered

- **`latest` tags and version ranges:** updates arrive on their own, but builds stop being reproducible, and a breaking change can land on one machine in the middle of a sprint.

## Consequences

- Builds are reproducible, and every version change is visible and reviewed.
- Updates do not happen on their own: someone has to check for new versions (`./mvnw versions:display-dependency-updates`, `uv lock --upgrade`) and open the pull request, and security fixes arrive only when someone does. A bot such as Dependabot could open those pull requests later without changing this decision.
- One tool can live in several files (Java in `pom.xml` and in the CI workflow, for example), so updates follow the table in Dependency updates.
- The `oradata` volume is tied to its image version: moving Oracle to a new version may mean recreating the volume and loading the data again.

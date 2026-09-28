# EventIQ - Ways of Working

How the EventIQ team plans, builds and ships. If something here stops being useful, raise it in the retrospective and change it.

## Team

| Person | Role | Owns |
|---|---|---|
| Angel | Tech Lead | Platform, CI, architecture decisions, releases, code review support |
| Cesar | Software Engineer · Events & AI | `events/`, `ai/`, `security/`, `templates/events/` |
| Gianina | Software Engineer · Payments & Analytics | `payments/`, `analytics/`, `analytics-service/`, `templates/payments/` |

Ownership is recorded in `.github/CODEOWNERS`. Changing a file you do not own is fine, but it goes through a pull request that the owner reviews.

## Sprint cadence

Sprints last one week, Monday to Friday. Sprint 0 is the Tech Lead's platform setup; Sprints 1–8 are delivery.

| Ceremony | When | Length | Purpose |
|---|---|---|---|
| Sprint Planning | Monday | 30 min | Agree on the sprint goal and move the items planned for this sprint from Backlog to Todo |
| Daily standup | Every workday, async | 5 min | Reply in that sprint's Standups discussion (see below) |
| Sprint Review | Friday | 30 min | Demo what is Done; only merged work counts |
| Retrospective | Friday, after the review | 15 min | One thing to keep, one to change, one action item |

A blocker older than half a day gets the `blocked` label and a mention of the person who can unblock it. Do not wait for the next ceremony.

**Standup discussions:** one GitHub Discussion per sprint, in the **Standups** category, opened by whoever runs Sprint Planning on Monday.

Opening post:

```markdown
**Sprint goal:** <one line>

Reply here each workday with your update. Keep it short.

**Yesterday:**
**Today:**
**Blockers:**
```

Daily reply, from each person, each workday:

```markdown
**Yesterday:** 
**Today:** 
**Blockers:** none
```

## Workflow

| Status | Meaning |
|---|---|
| Backlog | Captured; not yet pulled into the current sprint |
| Todo | Pulled into the current sprint at Sprint Planning; ready to start |
| In Progress | Someone is actively working on it; a branch exists |
| In Review | A pull request is open and linked with `Closes #N` |
| Done | Merged to `main` and meets the Definition of Done |

Backlog holds everything not yet started, whether or not it is fully detailed. The **Sprint** field is the forward-looking plan: it says which week an item is planned for, independent of Status. Status Todo says something different: the Tech Lead pulled it into the sprint currently running, at Sprint Planning. An item can sit in Backlog with `Sprint: Sprint 4` weeks in advance; it only becomes Todo once Sprint 4 is the active sprint and it is actually pulled in. Keep at most two items In Progress per person. Finishing beats starting.

## Definition of Ready

An item can enter a sprint when:

- The goal is clear: a user story, a task description or a spike question.
- Acceptance criteria are written and testable.
- It is estimated, and 8 points or less (split anything bigger).
- Its blockers are Done or will be early in the same sprint.

## Definition of Done

An item is Done when:

- All acceptance criteria are met.
- The pull request is approved by a teammate and CI is green.
- The application starts and the feature works locally.
- The Postman collection is updated if an endpoint was added or changed.
- Views degrade gracefully if an external service (Gemini, analytics) fails.
- No secrets are committed.

## Issue types

| Type | Use it for |
|---|---|
| Epic | A module or large capability; groups other issues as sub-issues |
| Story | User-facing behavior, written as "As a … I want … so that …" with Given/When/Then criteria |
| Task | Technical work without direct user-facing behavior (setup, entities, CI, docs) |
| Spike | Time-boxed research; ends in a decision, design doc or ADR, and a negative result is valid |
| Bug | A defect, with steps to reproduce and a severity |

## Estimation

Story points use the Fibonacci scale: 1, 2, 3, 5, 8. Points measure size and uncertainty, not hours.

| Points | Rough anchor |
|---|---|
| 1 | Trivial, well understood |
| 2–3 | A small, familiar change |
| 5 | A full story with API, view and validation |
| 8 | Large or with unknowns; consider splitting |

Aim for about 10–13 points per person per sprint and adjust after the first two sprints. Velocity is a planning aid, not a performance metric.

## Priority and severity

| Priority | Meaning |
|---|---|
| P0 - Critical | On the critical path; someone else is waiting for it |
| P1 - High | Required for the MVP release |
| P2 - Medium | Should be in the MVP; can slip one sprint |
| P3 - Low | Stretch goal; only when the MVP is on track |

Bug severity (S1 Blocker to S4 Trivial) is set in the bug form. S1 and S2 bugs are fixed before new stories are started.

## Reporting bugs and adding new work

Anyone on the team can add work to the backlog at any time. New issues land in **Backlog** automatically and are prioritized during triage or planning.

**Found a bug?**

1. Go to **Issues → New issue → Bug** and fill in the form: steps to reproduce, expected and actual result, severity and environment. Paste logs or screenshots in the form.
2. Set the parent epic of the affected module (e.g. *Payments*), so the bug appears under it as a sub-issue and stays in **Backlog**.
3. If the bug was introduced by a known story, write `Found in #<story>` in the summary.
4. The owner of the affected component triages it within one business day: sets Priority, Component, Story Points and Sprint. An S1 moves straight to **Todo** or even **In Progress**, sprint or no sprint; it does not wait for planning.

| Severity | Priority | When it is fixed |
|---|---|---|
| S1 Blocker | P0 | Immediately, in the current sprint, before anything else |
| S2 Major | P1 | In the current sprint, before new stories are started |
| S3 Minor | P2 | Planned in the next Sprint Planning |
| S4 Trivial | P3 | When there is spare capacity |

**Problems found while reviewing a pull request** are reported as review comments, not as issues, unless they are outside the pull request's scope.

**New stories or tasks** use the Story or Task form. They start in Backlog and move to Todo only after meeting the Definition of Ready in Sprint Planning. Anything that would change the current sprint's goal is discussed with the Tech Lead first.

## Language

Everything in the repository is written in English: code, routes, database tables, columns and status values, classes, methods, variables, comments, commit messages, issues, pull requests and UI texts. It keeps the codebase consistent and readable for anyone who reviews the portfolio. UI texts live in `src/main/resources/i18n/`, so another language can be added later without touching templates; see [View Conventions](view-conventions.md#texts).

## Branches, commits and pull requests

**Branches:** one per issue, `feature/<issue>-<short-slug>` for stories and tasks, `fix/<issue>-<short-slug>` for bugs. Example: `feature/42-registration-capacity`.

**Commits** start with the issue key, followed by a [Conventional Commits](https://www.conventionalcommits.org) type and optional scope:

```
EIQ-<issue> <type>(<scope>): <summary>

EIQ-42 feat(events): validate capacity on registration
EIQ-57 fix(payments): reject refunds on pending payments
EIQ-12 docs(readme): add Docker Compose instructions
```

| Part | Rules |
|---|---|
| `EIQ-<issue>` | `EIQ` is the project key and the number is the GitHub issue number: `EIQ-42` refers to issue #42. |
| `type` | `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore` |
| `scope` | Optional: `events`, `ai`, `security`, `payments`, `analytics`, `db`, `platform`, `readme` |
| `summary` | Imperative mood, lowercase, no final period, up to 100 characters |

The key uses `EIQ-` rather than `#` because Git treats lines starting with `#` as comments and would delete the message when it is written in an editor. The number is always the GitHub issue number, so new issues, including bugs, get their key automatically.

**Local hooks** (run once after cloning): `git config core.hooksPath .githooks`. The `prepare-commit-msg` hook adds `EIQ-<issue>` from the branch name, so `git commit -m "feat(events): validate capacity"` on `feature/42-registration-capacity` becomes `EIQ-42 feat(events): validate capacity`. The `commit-msg` hook rejects messages that do not follow the format.

**CI** runs the `commit-format` check on every pull request, validating the pull request title and every commit (merge commits are ignored).

**Pull requests:**

- The title follows the same format as commits; with squash merging, it becomes the commit on `main`.
- The description uses the template: Overview (with screenshots or logs), Developer/Reviewer checklist, Documentation impact and How to test.
- Link the issue with `Closes #<issue>` in the description. This connects the pull request to the issue, closes it on merge and, with squash merging, adds a clickable `#<issue>` link to the commit on `main`. `EIQ-42` alone is plain text.
- Keep pull requests small; reviews happen within one business day and include at least one useful comment.
- Before opening a pull request, merge the latest `main` and check that the application starts.
- `main` is protected by a ruleset: a pull request, one approval and the `java`, `python` and `commit-format` checks are required, and force pushes are blocked. The Tech Lead can bypass it for pull requests only, never for direct pushes; every bypass is logged in the repository's Rule insights.

## Dependency updates

Versions are pinned everywhere, so the whole team and CI run exactly the same tools. Changing a version, or adding or removing a dependency, is ordinary work: an issue, a branch and a pull request, reviewed like any other change.

**Analytics service (uv).** Run the commands from `analytics-service/` and commit `pyproject.toml` and `uv.lock` together:

| To… | Run |
|---|---|
| Add a package | `uv add <package>==<version>` |
| Add a development-only package (tests, tools) | `uv add --dev <package>==<version>` |
| Remove a package | `uv remove <package>` |
| Upgrade a direct dependency | `uv add <package>==<new-version>` |
| Refresh indirect dependencies | `uv lock --upgrade` |

Never use `pip install`: the `python` check runs `uv sync --locked` and fails if `uv.lock` is out of date. After pulling changes, run `uv sync` to update your local environment.

**Spring Boot application (Maven).**

- Spring Boot: the `<parent>` version in `pom.xml`. Boot manages the versions of its starters and of most libraries, so new dependencies are added without a `<version>`; only libraries that Boot does not manage get an explicit one.
- Spring AI: the `spring-ai.version` property in `pom.xml`.
- Maven itself: `./mvnw wrapper:wrapper -Dmaven=<version>`.
- To see what is available: `./mvnw versions:display-parent-updates` and `./mvnw versions:display-dependency-updates`.

**GitHub Actions.** Official actions (`actions/*`) use their major tag, for example `@v7`, and receive patches automatically; update them when a new major version is released. Third-party actions are pinned by full commit SHA, with the version as a comment, because a tag can be moved to different code and a SHA cannot:

```yaml
- uses: astral-sh/setup-uv@c18668ad3cf93ea998bef934396af7bb5c839dc7 # v10.2.0
```

Get the SHA of a tag with `git ls-remote https://github.com/<owner>/<repo> 'refs/tags/<tag>*'` (for annotated tags, use the line ending in `^{}`) and update the SHA and the comment together.

The job ids in `.github/workflows/` (`java`, `python`, `commit-format`) are the check names the ruleset requires: renaming a job means updating the ruleset in the same change, or every pull request stays blocked waiting for a check that no longer runs. For the same reason, workflows with required checks have no `paths:` filters.

**Front-end libraries (CDN).** Bootstrap, Bootstrap Icons and Chart.js load from jsDelivr, with the version in the URL and an `integrity` hash that makes the browser reject any file that does not match. Update the version and the hash together; to get the hash of a file:

```bash
curl -sL https://cdn.jsdelivr.net/npm/bootstrap@<version>/dist/css/bootstrap.min.css | openssl dgst -sha384 -binary | openssl base64 -A
```

Prefix the result with `sha384-`. Bootstrap also publishes the hashes on its download page.

**Where each version lives.** Some tools appear in more than one file; change all of them in the same pull request:

| Tool | Files |
|---|---|
| Java | `pom.xml` (`java.version`), `.github/workflows/build.yml` (`java-version`), root `Dockerfile` |
| Spring Boot, Spring AI | `pom.xml` |
| Maven | `.mvn/wrapper/maven-wrapper.properties` |
| Python | `analytics-service/.python-version`, `analytics-service/pyproject.toml` (`requires-python`), `analytics-service/Dockerfile` |
| Python packages | `analytics-service/pyproject.toml` and `analytics-service/uv.lock` |
| uv | `.github/workflows/build.yml` (`setup-uv` step), `analytics-service/Dockerfile`, local install (`brew upgrade uv`) |
| Oracle Database Free | `README.md` (Getting Started), `docker-compose.yml` |
| GitHub Actions | `.github/workflows/*.yml` |
| Bootstrap, Bootstrap Icons | CDN URLs and `integrity` hashes in `src/main/resources/templates/fragments/layout.html` |
| Chart.js | CDN URL and `integrity` hash in the analytics template (`templates/payments/`) and in `docs/view-conventions.md` |
| Gemini model | `src/main/resources/application.yml` (default of `AI_MODEL`) and `.env.example` |

The Dockerfiles and `docker-compose.yml` arrive with #51. When the pull request is merged, also update the versions table in the project guide (§5.1).

## Releases

Releases follow [Semantic Versioning](https://semver.org) and match the milestones: `v0.1.0` is the base platform and `v1.0.0` the MVP. The Tech Lead runs them:

1. When every issue in the milestone is closed, open a release pull request that sets the version in `pom.xml` (`<version>`) and in `analytics-service/pyproject.toml` (`version`, then run `uv lock`, because the lock file records the project version), and updates anything in the README that depends on the release.
2. After it is merged, tag `main` as `vX.Y.Z` and publish a GitHub release: what is included, how to run it, known limitations and the merged pull requests.
3. Close the milestone.

## Architecture decisions

Significant decisions are recorded as ADRs in `docs/adr/`: context, decision, consequences. If you are choosing between two approaches and the choice would be hard to reverse, write an ADR and link it from the pull request.

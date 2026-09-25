# EventIq - Ways of Working

How the EventIQ team plans, builds and ships. If something here stops being useful, raise it in the retrospective and change it.

## Team

| Person | Role | Owns |
|---|---|---|
| Angel | Tech Lead | Platform, CI, architecture decisions, releases, code review support |
| Cesar | Software Engineer · Events & AI | `events/`, `ai/`, `security/`, `templates/eventos/` |
| Gianina | Software Engineer · Payments & Analytics | `payments/`, `analytics/`, `python-service/`, `templates/pagos/` |

Ownership is recorded in `.github/CODEOWNERS`. Changing a file you do not own is fine, but it goes through a pull request that the owner reviews.

## Sprint cadence

Sprints last one week, Monday to Friday. Sprint 0 is the Tech Lead's platform setup; Sprints 1–8 are delivery.

| Ceremony | When | Length | Purpose |
|---|---|---|---|
| Sprint Planning | Monday | 30 min | Agree on the sprint goal and move the items planned for this sprint from Backlog to Todo |
| Daily standup | Every workday, async | 5 min | Post in the Standups discussion: yesterday, today, blockers |
| Sprint Review | Friday | 30 min | Demo what is Done; only merged work counts |
| Retrospective | Friday, after the review | 15 min | One thing to keep, one to change, one action item |

A blocker older than half a day gets the `blocked` label and a mention of the person who can unblock it. Do not wait for the next ceremony.

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

## Architecture decisions

Significant decisions are recorded as ADRs in `docs/adr/`: context, decision, consequences. If you are choosing between two approaches and the choice would be hard to reverse, write an ADR and link it from the pull request.

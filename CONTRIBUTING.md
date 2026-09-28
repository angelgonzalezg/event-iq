# Contributing

Quick reference for day-to-day work. Full detail — ceremonies, Definition of Ready/Done, estimation, bug triage — lives in [`docs/ways-of-working.md`](docs/ways-of-working.md).

## Language

Everything in the repository is in English: code, routes, database objects, identifiers, comments, commits, issues, pull requests and UI texts. See [Language](docs/ways-of-working.md#language).

## Branches

One branch per issue: `feature/<issue>-<short-slug>` for stories and tasks, `fix/<issue>-<short-slug>` for bugs.

```
feature/42-registration-capacity
fix/57-refund-on-pending-payment
```

## Commits

```
EIQ-<issue> <type>(<scope>): <summary>
```

| Part | Example |
|---|---|
| `EIQ-<issue>` | `EIQ-42` — the GitHub issue number |
| `type` | `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore` |
| `scope` | `events`, `ai`, `security`, `payments`, `analytics`, `db`, `platform` (optional) |
| `summary` | imperative, lowercase, no final period |

```
EIQ-42 feat(events): validate capacity on registration
```

Run `git config core.hooksPath .githooks` once after cloning: it adds the `EIQ-<issue>` prefix from your branch name automatically and rejects badly formatted commits before they're made.

## Pull requests

- Title follows the same format as commits; with squash merging it becomes the commit on `main`.
- Link the issue with `Closes #<issue>` in the description.
- Use the PR template: Overview, Developer/Reviewer checklist, Documentation impact, How to test.
- Keep PRs small. Reviews happen within one business day.
- Merge the latest `main` and confirm the app starts before opening a PR.

## Views

New pages reuse the shared layout and keep their texts in the module's message file. Routes, templates, alerts and styles are described in [`docs/view-conventions.md`](docs/view-conventions.md).

## Dependencies

Versions are pinned. To add, upgrade or remove a dependency, follow [Dependency updates](docs/ways-of-working.md#dependency-updates): it lists the commands and every file that pins each version.

## Documentation

Each setup document has one job: the README describes the project and how to run it, [`docs/onboarding.md`](docs/onboarding.md) takes a new machine to a working setup, and [`docs/setup-troubleshooting.md`](docs/setup-troubleshooting.md) gives the symptom, cause and fix of each failure. Keep each instruction in one of them and link to it from the others, so they do not drift apart. When a setup step changes, update [`scripts/check-setup.sh`](scripts/check-setup.sh) too if it checks that step.

## Questions

Open an issue with the Task form, or ask in the sprint's Standups discussion.

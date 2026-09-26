# Contributing

Quick reference for day-to-day work. Full detail — ceremonies, Definition of Ready/Done, estimation, bug triage — lives in [`docs/ways-of-working.md`](docs/ways-of-working.md).

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

## Questions

Open an issue with the Task form, or ask in the sprint's Standups discussion.

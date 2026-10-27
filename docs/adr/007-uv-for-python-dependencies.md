# ADR-007: uv for Python and the analytics service dependencies

- **Status:** Accepted
- **Date:** 2026-10-05
- **Related:** [#12](https://github.com/angelgonzalezg/event-iq/issues/12), [PR #63](https://github.com/angelgonzalezg/event-iq/pull/63), [Dependency updates](../ways-of-working.md#dependency-updates), [ADR-008](008-pinned-versions.md)

## Context

The analytics service is a Python application (FastAPI, pandas, scikit-learn, python-oracledb and TextBlob). Both engineers, CI and, later, a Docker image must run it with the same Python version and the same packages.

The usual setup, `pip`, `venv` and `requirements.txt`, leaves several manual steps: install the right Python version on each operating system, create and activate the environment, then install the packages. A `requirements.txt` that lists only the direct dependencies lets the indirect ones drift between machines, and keeping it complete by hand with `pip freeze` is easy to forget.

## Decision

[uv](https://docs.astral.sh/uv/) manages Python and the dependencies of `analytics-service/`:

- `.python-version` pins Python 3.14, and uv installs it: nobody installs Python by hand.
- `pyproject.toml` declares the direct dependencies with exact versions (`==`), and `uv.lock` locks every dependency, including the indirect ones. Both are committed together; `.venv/` is not.
- `uv sync` creates the environment from the lock file, `uv run` runs commands in it, and `uv add <package>==<version>` adds or upgrades a dependency. Nobody runs `pip install`.
- CI pins the uv version and runs `uv sync --locked`, which fails if `uv.lock` does not match `pyproject.toml`.

## Alternatives considered

- **pip, venv and requirements.txt:** more manual steps, Python installed separately on each machine, and only the direct dependencies pinned unless the file is maintained by hand.

## Consequences

- One command installs Python 3.14 and the exact locked environment, on every machine and in CI.
- uv is one more tool to install, and most tutorials still show `pip`. A `pip install` or a `requirements.txt` in a pull request gets a review comment: use `uv add`.
- `uv.lock` is generated: reviewers read the changes in `pyproject.toml`, not the lock file line by line.
- The uv version is pinned in CI and, once it exists, in the analytics Dockerfile, and changes there through a pull request (ADR-008). Local installs only need uv 0.12 or later, because `uv sync` installs what the lock file says.
- Changing the service's own `version` in `pyproject.toml` requires `uv lock`, because the lock file records it.

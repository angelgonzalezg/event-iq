# Analytics service

FastAPI service that reads the shared Oracle database and exposes analytics (exploratory analysis, revenue prediction and comment sentiment) to the Spring Boot application.

## Requirements

- [uv](https://docs.astral.sh/uv/): installs Python 3.14 (pinned in `.python-version`) and the dependencies locked in `uv.lock`.
- Oracle Database Free running, with `app_user` created (see the root [README](../README.md)).
- The `.env` file at the repository root. The service reads `DB_USER`, `DB_PASSWORD` and, optionally, `DB_DSN` (default `localhost:1521/FREEPDB1`).

## Run

```bash
cd analytics-service
uv sync                                        # creates .venv from uv.lock
uv run uvicorn main:app --reload --port 8000
```

- Health check: `curl -i localhost:8000/health` returns `200` with `{"status":"UP","database":"UP"}`, or `503` with `"DOWN"` if Oracle is unreachable.
- Interactive API docs: <http://localhost:8000/docs>

## Tests

```bash
cd analytics-service
uv run pytest            # all tests in tests/
uv run pytest -k health  # only the tests whose name matches
```

The tests do not need Oracle: they replace `get_connection` or `query_to_dataframe` with `monkeypatch` and build their data inside the test. `tests/test_health.py` is the reference, and [`docs/testing.md`](../docs/testing.md#analytics-service) explains the conventions. The `python` check runs the same command on every pull request.

## Dependencies

Add or upgrade a dependency with `uv add <package>==<version>` and remove one with `uv remove <package>`: both update `pyproject.toml` and `uv.lock` together, and both files are committed. Do not use `pip install`: `uv.lock` goes out of date and the `python` check fails. The full process is in [Dependency updates](../docs/ways-of-working.md#dependency-updates).

Before working on sentiment analysis, download the TextBlob corpora once: `uv run python -m textblob.download_corpora`.

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

## Dependencies

Add or update a dependency with `uv add <package>==<version>`: it updates `pyproject.toml` and `uv.lock` together, and both files are committed. Do not use `pip install`, or `uv.lock` goes out of date.

Before working on sentiment analysis, download the TextBlob corpora once: `uv run python -m textblob.download_corpora`.

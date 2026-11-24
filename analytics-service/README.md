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
- Test dataframe querying: `python -c "from database import query_to_dataframe; print(query_to_dataframe('SELECT COUNT(*) as total FROM events'))"`
- Interactive API docs: <http://localhost:8000/docs>

## Exploratory analysis

Two endpoints answer what drives income. Both appear in <http://localhost:8000/docs>:

- `GET /analytics/revenue-by-event`
   One row per event, highest revenue first: `event_id`, `name`, `event_date`, `capacity`, `tickets_sold`, `revenue` 

- `GET /analytics/ticket-type-distribution`
   One row per ticket type (General, VIP, Student): `tickets`, `ticket_share`, `revenue`, `revenue_share` 

Only `paid` payments count as revenue and as tickets sold: a `pending` payment has not been charged and a `refunded` one was returned. Events and ticket types with no sales appear with zeros. If Oracle is unreachable, both answer `503`, like `/health`. The queries and the response models are in `eda.py`.

[`docs/analysis.ipynb`](../docs/analysis.ipynb) explores the same data: distributions, correlations and written findings. It is saved with its results, so GitHub shows the charts without running anything. The figures in it come from the fixed-seed data; to reproduce them:

```bash
cd analytics-service
uv run python seed_data.py --truncate --seed
```

Then open the notebook in your IDE and choose the `analytics-service/.venv` interpreter (`ipykernel` and `matplotlib` are development dependencies), or run it in JupyterLab without adding anything to the project: `uv run --with jupyterlab==4.6.6 jupyter lab ../docs/analysis.ipynb`.

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

## Synthetic Data

The database seeding script generates realistic synthetic data (~50 events, ~200 attendees, ~800 registrations with payments, and ~200 comments) for development and model training purposes. You can populate the database by running:

```bash
# To run a fully randomized seed (with table truncation)
uv run python seed_data.py --truncate

# To run with a fixed random seed for reproducibility
uv run python seed_data.py --truncate --seed
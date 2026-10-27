# Analytics Service Validation Report (Issue #42)

## Overview
This document outlines the validation steps and results for connecting the Python-based `analytics-service` to the shared Oracle database, verifying the integration with the synthetic data from Issue #22.

## Validated Items
- **Environment Setup:** Virtual environment synchronized successfully using `uv sync`.
- **Dependencies & Corpora:** TextBlob corpora downloaded and operational.
- **Service Health:** `/health` endpoint verified returning `{"status":"UP","database":"UP"}`.
- **Data Querying:** `query_to_dataframe` function tested against the Oracle database `events` table.

## How to Test / Reproduction Steps
1. Navigate to the `analytics-service` directory:
   cd analytics-service

2. Synchronize the environment and dependencies using uv:
   uv sync

3. Activate the virtual environment:
   source .venv/bin/activate

4. Download the TextBlob corpora:
   python -m textblob.download.corpora

5. Start the FastAPI application:
   uvicorn main:app --reload --port 8000

6. Verify the health endpoint in another terminal or browser:
   curl http://localhost:8000/health
# Expected response: {"status":"UP","database":"UP"}

7. Test dataframe querying and row counts:
   python -c "from database import query_to_dataframe; print(query_to_dataframe('SELECT COUNT(*) as total FROM events'))"
# Expected output: total matching the synthetic dataset rows
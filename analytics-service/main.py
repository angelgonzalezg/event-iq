"""EventIQ analytics service: exposes analytics over the shared Oracle database."""

import logging

import oracledb
from fastapi import FastAPI
from fastapi.responses import JSONResponse

from database import get_connection

logger = logging.getLogger(__name__)

app = FastAPI(title="EventIQ Analytics Service")


@app.get("/health")
def health():
    """Checks the service and runs a round trip to Oracle; returns 503 if the database is unreachable."""
    try:
        with get_connection() as conn, conn.cursor() as cursor:
            cursor.execute("SELECT 1 FROM dual")
            cursor.fetchone()
    except (oracledb.Error, KeyError):  # KeyError: DB_USER or DB_PASSWORD is not set
        logger.exception("Health check failed: database unavailable")
        return JSONResponse(status_code=503, content={"status": "DOWN", "database": "DOWN"})
    return {"status": "UP", "database": "UP"}

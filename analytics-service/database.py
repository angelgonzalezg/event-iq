"""Oracle connection helpers for the analytics service."""

import os

import oracledb
import pandas as pd
from dotenv import load_dotenv

# Searches for .env from this folder up to the repository root. Variables that are
# already set (e.g. by Docker Compose) take precedence over the file.
load_dotenv()


def get_connection() -> oracledb.Connection:
    """Opens a connection to the shared Oracle database as the application user."""
    return oracledb.connect(
        user=os.environ["DB_USER"],
        password=os.environ["DB_PASSWORD"],
        dsn=os.getenv("DB_DSN", "localhost:1521/FREEPDB1"),
    )


def query_to_dataframe(sql: str) -> pd.DataFrame:
    """Runs a query and returns the result as a DataFrame with lowercase column names."""
    with get_connection() as conn:
        df = pd.read_sql(sql, conn)  # pandas warns that it prefers SQLAlchemy; harmless here
    df.columns = df.columns.str.lower()  # Oracle returns column names in uppercase
    return df

"""GET /health without a database: get_connection is replaced, so the tests run anywhere, CI included."""

import oracledb
import pytest
from fastapi.testclient import TestClient

import main

client = TestClient(main.app)


class FakeCursor:
    def __enter__(self):
        return self

    def __exit__(self, *exc_info):
        return False

    def execute(self, sql):
        self.sql = sql

    def fetchone(self):
        return (1,)


class FakeConnection:
    def __enter__(self):
        return self

    def __exit__(self, *exc_info):
        return False

    def cursor(self):
        return FakeCursor()


def test_health_answers_200_when_the_database_answers(monkeypatch):
    monkeypatch.setattr(main, "get_connection", FakeConnection)

    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "UP", "database": "UP"}


@pytest.mark.parametrize(
    "error",
    [oracledb.OperationalError("DPY-6005: cannot connect to database"), KeyError("DB_USER")],
    ids=["oracle-unreachable", "credentials-not-set"],
)
def test_health_answers_503_when_the_database_is_down(monkeypatch, error):
    def failing_connection():
        raise error

    monkeypatch.setattr(main, "get_connection", failing_connection)

    response = client.get("/health")

    assert response.status_code == 503
    assert response.json() == {"status": "DOWN", "database": "DOWN"}

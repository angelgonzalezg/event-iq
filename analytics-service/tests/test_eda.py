"""GET /analytics/revenue-by-event and /analytics/ticket-type-distribution without Oracle.

Two kinds of test, both building their own data:
- API shape: query_to_dataframe is replaced by a small DataFrame.
- Business rules: the real SQL runs on an in-memory SQLite database. The queries use standard SQL,
  so this checks the rules (only paid payments are income) but not Oracle's own dialect.
"""

import sqlite3

import oracledb
import pandas as pd
import pytest
from fastapi.testclient import TestClient

import eda
import main

client = TestClient(main.app)

def revenue_frame(rows):
    columns = ["event_id", "name", "event_date", "capacity", "tickets_sold", "revenue"]
    return pd.DataFrame(rows, columns=columns)


def test_revenue_by_event_returns_every_event_with_its_name_and_revenue(monkeypatch):
    df = revenue_frame(
        [
            (1, "Java Day", pd.Timestamp("2027-03-01"), 100, 3, 700.0),
            (2, "Free Fest", pd.Timestamp("2027-04-10"), 50, 1, 0.0),
        ]
    )
    monkeypatch.setattr(eda, "query_to_dataframe", lambda sql: df)

    response = client.get("/analytics/revenue-by-event")

    assert response.status_code == 200
    assert response.json() == [
        {"event_id": 1, "name": "Java Day", "event_date": "2027-03-01", "capacity": 100, "tickets_sold": 3, "revenue": 700.0},
        {"event_id": 2, "name": "Free Fest", "event_date": "2027-04-10", "capacity": 50, "tickets_sold": 1, "revenue": 0.0},
    ]


def test_revenue_by_event_returns_an_empty_list_when_there_are_no_events(monkeypatch):
    monkeypatch.setattr(eda, "query_to_dataframe", lambda sql: revenue_frame([]))

    response = client.get("/analytics/revenue-by-event")

    assert response.status_code == 200
    assert response.json() == []


def test_ticket_type_distribution_adds_each_types_share_of_tickets_and_revenue(monkeypatch):
    df = pd.DataFrame({"ticket_type": ["General", "VIP"], "tickets": [3, 1], "revenue": [200.0, 500.0]})
    monkeypatch.setattr(eda, "query_to_dataframe", lambda sql: df)

    response = client.get("/analytics/ticket-type-distribution")

    assert response.status_code == 200
    assert response.json() == [
        {"ticket_type": "General", "tickets": 3, "ticket_share": 0.75, "revenue": 200.0, "revenue_share": 0.2857},
        {"ticket_type": "VIP", "tickets": 1, "ticket_share": 0.25, "revenue": 500.0, "revenue_share": 0.7143},
    ]


def test_ticket_type_distribution_answers_zero_shares_when_nothing_was_sold(monkeypatch):
    df = pd.DataFrame({"ticket_type": ["General", "VIP"], "tickets": [0, 0], "revenue": [0.0, 0.0]})
    monkeypatch.setattr(eda, "query_to_dataframe", lambda sql: df)

    response = client.get("/analytics/ticket-type-distribution")

    assert response.status_code == 200
    assert [row["ticket_share"] for row in response.json()] == [0.0, 0.0]
    assert [row["revenue_share"] for row in response.json()] == [0.0, 0.0]


def test_ticket_type_distribution_returns_an_empty_list_when_there_are_no_ticket_types(monkeypatch):
    df = pd.DataFrame({"ticket_type": [], "tickets": [], "revenue": []})
    monkeypatch.setattr(eda, "query_to_dataframe", lambda sql: df)

    response = client.get("/analytics/ticket-type-distribution")

    assert response.status_code == 200
    assert response.json() == []


@pytest.mark.parametrize("path", ["/analytics/revenue-by-event", "/analytics/ticket-type-distribution"])
@pytest.mark.parametrize(
    "error",
    [
        oracledb.OperationalError("DPY-6005: cannot connect to database"),
        KeyError("DB_USER"),
        pd.errors.DatabaseError("Execution failed on sql"),
    ],
    ids=["oracle-unreachable", "credentials-not-set", "query-failed"],
)
def test_endpoints_answer_503_when_the_database_is_down(monkeypatch, path, error):
    def failing_query(sql):
        raise error

    monkeypatch.setattr(eda, "query_to_dataframe", failing_query)

    response = client.get(path)

    assert response.status_code == 503
    assert response.json() == {"detail": "The database is unavailable."}


def test_both_endpoints_appear_in_the_api_documentation():
    paths = client.get("/openapi.json").json()["paths"]

    assert "/analytics/revenue-by-event" in paths
    assert "/analytics/ticket-type-distribution" in paths


@pytest.fixture
def sqlite_database(monkeypatch):
    """Event 1 sells General (100) and VIP (500) in every payment status, event 2 is free and event 3 sold nothing."""
    conn = sqlite3.connect(":memory:")
    conn.executescript(
        """
        CREATE TABLE events (event_id INTEGER PRIMARY KEY, name TEXT, event_date TEXT, capacity INTEGER);
        CREATE TABLE ticket_types (ticket_type_id INTEGER PRIMARY KEY, event_id INTEGER, name TEXT, price REAL);
        CREATE TABLE payments (payment_id INTEGER PRIMARY KEY, ticket_type_id INTEGER, event_id INTEGER, amount REAL, status TEXT);

        INSERT INTO events VALUES (1, 'Java Day', '2027-03-01', 100), (2, 'Free Fest', '2027-04-10', 50), (3, 'Empty Event', '2027-05-20', 10);
        INSERT INTO ticket_types VALUES (1, 1, 'General', 100), (2, 1, 'VIP', 500), (3, 2, 'General', 0), (4, 3, 'General', 100);
        INSERT INTO payments (ticket_type_id, event_id, amount, status) VALUES
            (1, 1, 100, 'paid'), (1, 1, 100, 'paid'), (2, 1, 500, 'paid'),
            (2, 1, 500, 'refunded'), (1, 1, 100, 'pending'),
            (3, 2, 0, 'paid');
        """
    )
    monkeypatch.setattr(eda, "query_to_dataframe", lambda sql: pd.read_sql(sql, conn))
    yield conn
    conn.close()


def test_revenue_only_counts_paid_payments(sqlite_database):
    df = eda.revenue_by_event_df()

    java_day = df[df["name"] == "Java Day"].iloc[0]
    assert java_day["revenue"] == 700  # 2 x 100 + 1 x 500; the refunded 500 and the pending 100 are not income
    assert java_day["tickets_sold"] == 3


def test_free_events_appear_with_zero_revenue_and_their_free_tickets_count_as_sold(sqlite_database):
    df = eda.revenue_by_event_df()

    free_fest = df[df["name"] == "Free Fest"].iloc[0]
    assert free_fest["revenue"] == 0
    assert free_fest["tickets_sold"] == 1


def test_events_without_payments_appear_with_zeros(sqlite_database):
    df = eda.revenue_by_event_df()

    empty_event = df[df["name"] == "Empty Event"].iloc[0]
    assert empty_event["revenue"] == 0
    assert empty_event["tickets_sold"] == 0


def test_events_are_sorted_by_revenue_and_then_by_name(sqlite_database):
    df = eda.revenue_by_event_df()

    assert list(df["name"]) == ["Java Day", "Empty Event", "Free Fest"]


def test_ticket_types_are_grouped_by_name_across_events_counting_only_paid_tickets(sqlite_database):
    df = eda.ticket_type_distribution_df().set_index("ticket_type")

    assert df.loc["General", "tickets"] == 3  # 2 paid in event 1 plus the free one in event 2; the pending one is left out
    assert df.loc["General", "revenue"] == 200
    assert df.loc["VIP", "tickets"] == 1  # the refunded VIP ticket is left out
    assert df.loc["VIP", "revenue"] == 500
    assert df.loc["General", "ticket_share"] == 0.75
    assert df.loc["VIP", "revenue_share"] == 0.7143


def test_a_ticket_type_nobody_bought_still_appears_with_zero_tickets(sqlite_database):
    sqlite_database.execute("INSERT INTO ticket_types VALUES (5, 3, 'Student', 50)")

    df = eda.ticket_type_distribution_df().set_index("ticket_type")

    assert df.loc["Student", "tickets"] == 0
    assert df.loc["Student", "revenue"] == 0

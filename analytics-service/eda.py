import logging
from datetime import date

import oracledb
import pandas as pd
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from database import query_to_dataframe

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/analytics", tags=["exploratory analysis"])

REVENUE_BY_EVENT_SQL = """
    SELECT e.event_id,
           e.name,
           e.event_date,
           e.capacity,
           COUNT(CASE WHEN p.status = 'paid' THEN 1 END) AS tickets_sold,
           COALESCE(SUM(CASE WHEN p.status = 'paid' THEN p.amount END), 0) AS revenue
    FROM events e
    LEFT JOIN payments p ON p.event_id = e.event_id
    GROUP BY e.event_id, e.name, e.event_date, e.capacity
    ORDER BY revenue DESC, e.name, e.event_id
"""

TICKET_TYPE_DISTRIBUTION_SQL = """
    SELECT t.name AS ticket_type,
           COUNT(p.payment_id) AS tickets,
           COALESCE(SUM(p.amount), 0) AS revenue
    FROM ticket_types t
    LEFT JOIN payments p ON p.ticket_type_id = t.ticket_type_id AND p.status = 'paid'
    GROUP BY t.name
    ORDER BY tickets DESC, t.name
"""

class RevenueByEvent(BaseModel):
    event_id: int
    name: str
    event_date: date
    capacity: int
    tickets_sold: int
    revenue: float


class TicketTypeShare(BaseModel):
    ticket_type: str
    tickets: int
    ticket_share: float
    revenue: float
    revenue_share: float


def revenue_by_event_df() -> pd.DataFrame:
    """One row per event (including events with no income), highest revenue first."""
    df = query_to_dataframe(REVENUE_BY_EVENT_SQL)
    df["event_date"] = pd.to_datetime(df["event_date"]).dt.date
    return df


def ticket_type_distribution_df() -> pd.DataFrame:
    """One row per ticket type name with its paid tickets, revenue and the share of each total."""
    df = query_to_dataframe(TICKET_TYPE_DISTRIBUTION_SQL)
    df["ticket_share"] = _share(df["tickets"])
    df["revenue_share"] = _share(df["revenue"])
    return df


def _share(values: pd.Series) -> pd.Series:
    """Each value divided by the total; all zeros when the total is zero (nothing sold yet)."""
    total = values.sum()
    if total == 0:
        return values.astype(float) * 0.0
    return (values / total).round(4)


@router.get("/revenue-by-event", response_model=list[RevenueByEvent])
def revenue_by_event():
    """Income of every event: paid tickets and revenue, highest revenue first. Refunded and pending payments are not income."""
    return _records(revenue_by_event_df)


@router.get("/ticket-type-distribution", response_model=list[TicketTypeShare])
def ticket_type_distribution():
    """How many paid tickets and how much revenue each ticket type (General, VIP, Student) brings, with each type's share of the total."""
    return _records(ticket_type_distribution_df)


def _records(read) -> list[dict]:
    """Runs a read function and answers 503, like /health, when the database is unavailable."""
    try:
        return read().to_dict("records")
    except (oracledb.Error, KeyError, pd.errors.DatabaseError):
        logger.exception("Analytics query failed: database unavailable")
        raise HTTPException(status_code=503, detail="The database is unavailable.")
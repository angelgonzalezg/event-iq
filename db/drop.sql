-- Run with SQL*Plus as the application schema owner.
-- Development reset: permanently removes application tables and data.
-- Missing tables are skipped to support partially created schemas.
-- DDL statements commit implicitly in Oracle.

WHENEVER OSERROR EXIT FAILURE ROLLBACK
WHENEVER SQLERROR EXIT FAILURE ROLLBACK

DROP TABLE IF EXISTS comments PURGE;
DROP TABLE IF EXISTS payments PURGE;
DROP TABLE IF EXISTS ticket_types PURGE;
DROP TABLE IF EXISTS registrations PURGE;
DROP TABLE IF EXISTS attendees PURGE;
DROP TABLE IF EXISTS events PURGE;

EXIT SUCCESS

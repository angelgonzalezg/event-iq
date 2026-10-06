-- Run with SQL*Plus as the application schema owner.
-- Development reset: permanently removes application tables and data.
-- Missing tables are skipped to support partially created schemas.
-- DDL statements commit implicitly in Oracle.

WHENEVER OSERROR EXIT FAILURE ROLLBACK
WHENEVER SQLERROR EXIT FAILURE ROLLBACK

PROMPT WARNING: Development reset. The six EventIQ tables and ALL their data will be permanently deleted.
PROMPT PURGE bypasses the recycle bin. ROLLBACK cannot undo this reset.
PROMPT Current connection:

SELECT USER AS schema_owner,
       SYS_CONTEXT('USERENV', 'DB_NAME') AS database_name,
       SYS_CONTEXT('USERENV', 'CON_NAME') AS container_name,
       SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS service_name
FROM dual;

PROMPT Dropping EventIQ tables in the connection shown above. No interactive confirmation is requested.

DROP TABLE IF EXISTS comments PURGE;
DROP TABLE IF EXISTS payments PURGE;
DROP TABLE IF EXISTS ticket_types PURGE;
DROP TABLE IF EXISTS registrations PURGE;
DROP TABLE IF EXISTS attendees PURGE;
DROP TABLE IF EXISTS events PURGE;

EXIT SUCCESS

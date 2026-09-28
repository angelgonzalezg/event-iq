# EventIQ - Setup Troubleshooting

Symptom, cause and fix for the problems most likely to show up while following [Getting Started](../README.md#getting-started). Start with the quick checks: they usually point to the right section.

Validated on macOS with Apple Silicon, Oracle Database Free `23.26.3.0`, Temurin 25 and uv 0.12. Windows with WSL2 has not been validated yet; see [Windows with WSL2](#windows-with-wsl2-not-validated-yet).

Replace `<app_password>` and `<admin_password>` with your own values. Never paste passwords, API keys or the contents of `.env` into issues or chats.

---

## Quick checks

`scripts/check-setup.sh` runs these checks, and a few more, in one go; see [Onboarding](onboarding.md#3-check-your-setup). To check by hand:

| Check | Command | Expected |
|---|---|---|
| Oracle container | `docker ps --filter name=oracle-free` | `STATUS` starts with `Up` |
| Service registered | `docker exec oracle-free lsnrctl status \| grep -i freepdb1` | A `Service "freepdb1"` line |
| `app_user` can log in | `docker exec -it oracle-free sqlplus app_user@//localhost:1521/FREEPDB1` | `Connected to:` after typing the password |
| JDK used by Maven | `./mvnw -v` | `Java version: 25` |
| uv installed | `uv --version` | `uv 0.12` or later |
| Analytics service | `curl -i localhost:8000/health` | `200 OK` and `{"status":"UP","database":"UP"}` |

If `sqlplus` connects as `app_user` but an application does not, the database is fine and the problem is the configuration: go to [Environment variables](#environment-variables). If `sqlplus` fails too, go to [Connecting to Oracle](#connecting-to-oracle).

`sqlplus` asks for the password instead of taking it on the command line, so it does not end up in your shell history.

---

## Docker and the Oracle container

### Port 1521 is already in use

`docker run` fails with `port is already allocated` or `address already in use`.

**Cause:** another container (often an Oracle container from an earlier tutorial) or a local Oracle installation is using port 1521.

**Fix:** find what holds the port:

```bash
docker ps --filter publish=1521   # another container?
lsof -i :1521                     # a local process? (macOS and Linux)
```

Stop it (`docker stop <name>`), or publish Oracle on another host port and point both services to it in `.env`:

```bash
docker run -d --name oracle-free -p 1522:1521 ...   # rest of the README command unchanged
```

```text
DB_URL=jdbc:oracle:thin:@localhost:1522/FREEPDB1
DB_DSN=localhost:1522/FREEPDB1
```

A failed `docker run` still creates the container; remove it before trying again (next entry).

### The container name is already in use

`docker run` fails with `Conflict. The container name "/oracle-free" is already in use`.

**Cause:** a container with that name already exists: stopped, or left behind by a `docker run` that failed.

**Fix:** if it was created with the right options, start it with `docker start oracle-free`. Otherwise remove it and run the README command again. The data lives in the `oradata` volume, so removing the container does not delete it:

```bash
docker rm -f oracle-free
```

### The first start takes several minutes

Right after `docker run`, connections fail with `ORA-12541`, `ORA-12514` or `ORA-01033`.

**Cause:** on the first start with an empty `oradata` volume, the container prepares the database, which takes a few minutes. Until it finishes, the listener or the `FREEPDB1` service is not available. Later starts are much faster.

**Fix:** follow the log and wait for the ready line before connecting:

```bash
docker logs -f oracle-free   # wait for: DATABASE IS READY TO USE!
```

If the line never appears, see the next entry.

### The container stops during the first start

`docker ps` no longer lists `oracle-free`, `docker ps -a` shows `Exited (137)`, or the log never reaches `DATABASE IS READY TO USE!`.

**Cause:** Docker does not have enough memory for Oracle. Exit code 137 means the process was killed.

**Fix:** check the reason:

```bash
docker inspect oracle-free --format '{{.State.ExitCode}} OOMKilled={{.State.OOMKilled}}'
docker logs --tail 50 oracle-free
```

Give Docker at least 4 GB of memory (Docker Desktop → Settings → Resources; on WSL2 see [below](#windows-with-wsl2-not-validated-yet)) and start the container again. If the first start was interrupted halfway, the volume may be incomplete; in that case, [start over](#starting-over).

### Apple Silicon: the image runs under emulation

On a Mac with an M-series chip, Oracle is extremely slow or crashes.

**Cause:** an `amd64` image was pulled, for example because `DOCKER_DEFAULT_PLATFORM=linux/amd64` is set or `--platform linux/amd64` was used, and it runs under emulation. The Oracle image publishes a native `arm64` variant.

**Fix:** the architecture must be `arm64`:

```bash
docker image inspect container-registry.oracle.com/database/free:23.26.3.0 --format '{{.Architecture}}'
echo $DOCKER_DEFAULT_PLATFORM   # should print nothing
```

If it says `amd64`, unset the variable (and remove it from your shell profile), remove the container and the image, and run README step 2 again:

```bash
unset DOCKER_DEFAULT_PLATFORM
docker rm -f oracle-free
docker rmi container-registry.oracle.com/database/free:23.26.3.0
```

### The SYSTEM password does not work

`sqlplus system@//localhost:1521/FREEPDB1` fails with `ORA-01017` even though you typed the password from `docker run`.

**Cause:** `ORACLE_PWD` is only applied when the database is created. If the `oradata` volume already existed from an earlier container, the database keeps its original password.

**Fix:** set a new password while the container is running, then update `ORACLE_PWD` in `.env` to match:

```bash
docker exec oracle-free /opt/oracle/setPassword.sh <admin_password>
```

---

## Connecting to Oracle

Spring Boot (JDBC driver) and the analytics service (python-oracledb) report some connection errors with different codes; both are listed. Run the SQL statements in this section as `system` connected to `FREEPDB1`:

```bash
docker exec -it oracle-free sqlplus system@//localhost:1521/FREEPDB1
```

### ORA-12541 or DPY-6005: no listener

- Spring Boot: `ORA-12541: Cannot connect. No listener at host localhost port 1521.`
- Analytics service: `DPY-6005: cannot connect to database` … `Connection refused`

**Cause:** nothing is listening on `localhost:1521`: the container is stopped (it does not start by itself after a reboot), is still starting, or is published on another port.

**Fix:**

```bash
docker ps -a --filter name=oracle-free   # Exited? Start it:
docker start oracle-free
docker logs -f oracle-free               # wait for: DATABASE IS READY TO USE!
```

If you published Oracle on another port, check that `DB_URL` and `DB_DSN` in `.env` use it.

### ORA-12514 or DPY-6001: service not registered

- Spring Boot: `ORA-12514: Cannot connect to database. Service … is not registered with the listener …`
- Analytics service: `DPY-6001: … Service "…" is not registered with the listener …`

**Cause:** the service name is wrong (the app uses `FREEPDB1`; names such as `XEPDB1` or `ORCLPDB1` come from other tutorials), or the database has not finished starting and has not registered the service yet.

**Fix:** list the registered services and compare them with `DB_URL` and `DB_DSN`:

```bash
docker exec oracle-free lsnrctl status | grep -i service
```

If `freepdb1` is not listed yet, wait for `DATABASE IS READY TO USE!`.

A related error, `ORA-12505`, means the URL uses the SID form `localhost:1521:FREEPDB1`. Use the service form, with a slash: `localhost:1521/FREEPDB1`.

### ORA-65096 when creating app_user

`CREATE USER app_user ...` fails with `ORA-65096`.

**Cause:** you are connected to the container database (service `FREE`), where user names must start with `C##`. `app_user` belongs in the pluggable database `FREEPDB1`.

**Fix:** connect to `FREEPDB1` as shown at the start of this section and run the README statements again. `SHOW CON_NAME` must print `FREEPDB1`.

### ORA-01017: invalid credential or not authorized

`ORA-01017: invalid credential or not authorized; logon denied`

**Cause:** since Oracle 23ai, this single error covers both a wrong user or password and a user without permission to connect. The usual causes, most likely first:

1. `app_user` does not have the `CONNECT` role: the `GRANT` in README step 2 was skipped.
2. The password in `.env` does not match. Passwords are case-sensitive.
3. The connection string points to `FREE` instead of `FREEPDB1`; `app_user` only exists in `FREEPDB1`.
4. The application reads a different value than you expect: see [Environment variables](#environment-variables).

**Fix:** test the credentials without the applications:

```bash
docker exec -it oracle-free sqlplus app_user@//localhost:1521/FREEPDB1
```

If that fails, check the user as `system`:

```sql
SELECT username, account_status FROM dba_users WHERE username = 'APP_USER';
SELECT granted_role FROM dba_role_privs WHERE grantee = 'APP_USER';
```

- No rows in the first query: create the user (README step 2).
- `CONNECT` or `RESOURCE` missing: `GRANT CONNECT, RESOURCE TO app_user;`
- Password in doubt: `ALTER USER app_user IDENTIFIED BY "<app_password>";`, and copy the same value to `DB_PASSWORD` in `.env`.
- `account_status` starts with `LOCKED`: see the next entry.

If `sqlplus` connects but an application still fails, go to [Environment variables](#environment-variables).

### ORA-28000: the account is locked

**Cause:** too many failed logins (10 with the default profile). An application that keeps retrying with a wrong password locks the account quickly.

**Fix:** correct the password in `.env` first, then unlock the account as `system`:

```sql
ALTER USER app_user ACCOUNT UNLOCK;
```

### ORA-01950: no privileges on tablespace 'USERS'

Creating tables works, but the first `INSERT` (for example while loading sample data) fails with `ORA-01950`.

**Cause:** `app_user` has no quota on the `USERS` tablespace. The `RESOURCE` role allows creating tables but not storing rows in them.

**Fix:** as `system`:

```sql
ALTER USER app_user QUOTA UNLIMITED ON USERS;
SELECT tablespace_name, max_bytes FROM dba_ts_quotas WHERE username = 'APP_USER';   -- -1 means unlimited
```

### ORA-00942: table or view does not exist

The application connects, but queries fail with `ORA-00942`.

**Cause:** the schema has not been created, or `db/schema.sql` ran as another user (for example `system`), so the tables were created in that user's schema instead of `app_user`'s.

**Fix:** connected as `app_user`, list its tables:

```sql
SELECT table_name FROM user_tables;
```

If the list is empty, run `db/schema.sql` as `app_user` (README step 2). Tables created by mistake under another user are not used by the applications; drop them or [start over](#starting-over).

---

## Environment variables

Spring Boot reads `.env` through `spring.config.import` and the analytics service through `python-dotenv`. They parse the file differently, and in both, variables already set in your shell or IDE take precedence over `.env`. Neither service reloads `.env` while running (uvicorn's `--reload` only watches Python files): restart after every change.

### The analytics service connects but Spring Boot gets ORA-01017

`/health` returns `UP`, but Spring Boot fails with `ORA-01017`, or another value (such as the Gemini key) works in Python and not in Java.

**Cause:** Spring Boot reads `.env` as a `.properties` file, so quotes, comments after a value and trailing spaces become part of the value. `python-dotenv` strips them, so the same line works in Python and fails in Java:

| Line in `.env` | Spring Boot reads | Python reads |
|---|---|---|
| `DB_PASSWORD="secret"` | `"secret"` | `secret` |
| `DB_PASSWORD=secret # dev` | `secret # dev` | `secret` |
| `DB_PASSWORD=secret` followed by a space | `secret` followed by a space | `secret` |

**Fix:** write every line as `KEY=value`: no quotes, no comments after the value, no trailing spaces. This lists the offending keys without printing their values:

```bash
grep -nE "^[^#].*([\"']| #|[[:space:]]\$)" .env | cut -d= -f1
```

### Changes to .env have no effect

You edit `.env`, restart, and the application still uses the old value.

**Cause:** the variable is also set outside `.env`: an `export` in your shell or its profile (`~/.zshrc`, `~/.bashrc`), or the environment of the IDE run configuration. Those values win over `.env`.

**Fix:** list which EventIQ variables are set in the current shell (names only):

```bash
env | grep -E '^(ORACLE_PWD|DB_USER|DB_PASSWORD|DB_URL|DB_DSN|ADMIN_PASSWORD|JWT_SECRET|AI_API_KEY|AI_MODEL|ANALYTICS_URL)=' | cut -d= -f1
```

`unset` them, remove the `export` lines from your shell profile, clear them from the IDE run configuration, and restart the application.

### Works from the terminal but not from the IDE

Spring Boot starts with `./mvnw spring-boot:run` but fails from the IDE (or the reverse) with `ORA-01017` or `Could not resolve placeholder 'DB_USER'`.

**Cause:** Spring Boot looks for `.env` in the working directory, and the import is `optional:`, so a missing file is skipped without any warning. Unresolved values can then reach Oracle literally, for example the user name `${DB_USER}`.

**Fix:** set the working directory of the IDE run configuration to the repository root (IntelliJ IDEA: Run → Edit Configurations → Working directory; VS Code: `"cwd": "${workspaceFolder}"` in `launch.json`). The analytics service does not have this problem: `python-dotenv` searches for `.env` from `analytics-service/` up to the repository root.

---

## Java and Maven

### release version 25 not supported

```text
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:...:compile (default-compile) on project event-iq: Fatal error compiling: error: release version 25 not supported
```

**Cause:** Maven is running on a JDK older than 25. The Maven wrapper uses `JAVA_HOME`, or the `java` on your `PATH` when `JAVA_HOME` is not set.

**Fix:** check which JDK Maven uses; it must report `Java version: 25`:

```bash
./mvnw -v
```

If it reports another version, point `JAVA_HOME` to JDK 25 in your shell profile, as in the onboarding for [macOS](onboarding.md#macos) or [Linux and WSL2](onboarding.md#linux-ubuntu-or-debian), and open a new terminal. If JDK 25 is not installed (`/usr/libexec/java_home -V` lists the installed JDKs on macOS, `ls /usr/lib/jvm` on Linux), install it with the same steps.

The IDE has its own settings: select JDK 25 as the project SDK and as the JDK that runs Maven (IntelliJ IDEA: File → Project Structure → Project → SDK, and Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → JRE).

### UnsupportedClassVersionError: class file version 69.0

```text
java.lang.UnsupportedClassVersionError: com/eventiq/EventIqApplication has been compiled by a more recent version of the Java Runtime (class file version 69.0), this version of the Java Runtime only recognizes class file versions up to 65.0
```

**Cause:** the classes were compiled with JDK 25 (class file version 69) but run on an older JVM (65 is Java 21, 61 is Java 17). It usually happens when the IDE or another terminal compiled the project and a terminal whose `JAVA_HOME` still points to an older JDK runs it: Maven does not recompile classes that are up to date, so the error only appears at run time.

**Fix:** as in the previous entry, make `./mvnw -v` and the IDE report Java 25, then run the application again.

---

## Analytics service

Run every command from `analytics-service/` and through `uv run`, which uses the project's locked environment in `.venv`.

### uv: command not found

**Fix:** install uv and open a new terminal:

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh   # or: brew install uv
```

You do not need to install Python separately: the first `uv sync` downloads Python 3.14, as set in `.python-version`.

### uv sync --locked says the lockfile needs to be updated

``The lockfile at `uv.lock` needs to be updated, but `--locked` was provided.``

**Cause:** `pyproject.toml` changed without regenerating `uv.lock`. CI runs the same command, so this should not happen on an up-to-date `main`.

**Fix:** pull the latest `main`. If you changed `pyproject.toml` yourself, run `uv lock` and commit `uv.lock` with it; see [Dependency updates](ways-of-working.md#dependency-updates).

### ModuleNotFoundError or Failed to spawn: uvicorn

`ModuleNotFoundError: No module named 'fastapi'` (or `oracledb`, `pandas`), or `` Failed to spawn: `uvicorn` ``.

**Cause:** Python ran without the project environment: `python` or `uvicorn` was called directly instead of through `uv run`, or the command ran outside `analytics-service/`.

**Fix:**

```bash
cd analytics-service
uv sync
uv run uvicorn main:app --reload --port 8000
```

### /health returns 503

`curl -i localhost:8000/health` returns `503` with `{"status":"DOWN","database":"DOWN"}`.

**Cause:** the service is running but could not reach Oracle. The uvicorn terminal shows `Health check failed: database unavailable` followed by a traceback; its last line names the cause:

| Last line of the traceback | Meaning |
|---|---|
| `KeyError: 'DB_USER'` or `KeyError: 'DB_PASSWORD'` | The variable is missing from `.env` in the repository root |
| `DPY-6005` … `Connection refused` | [ORA-12541 or DPY-6005](#ora-12541-or-dpy-6005-no-listener) |
| `DPY-6001` | [ORA-12514 or DPY-6001](#ora-12514-or-dpy-6001-service-not-registered) |
| `ORA-01017` | [ORA-01017](#ora-01017-invalid-credential-or-not-authorized) |

### MissingCorpusError from TextBlob

`textblob.exceptions.MissingCorpusError` when analyzing comments.

**Fix:** download the corpora once:

```bash
uv run python -m textblob.download_corpora
```

---

## Ports 8080 and 8000

`Web server failed to start. Port 8080 was already in use.` (Spring Boot), or `[Errno 48] Address already in use` (uvicorn; `Errno 98` on Linux).

**Cause:** an earlier run is still alive, often in another terminal or in the IDE.

**Fix:** find the process and stop it:

```bash
lsof -i :8080   # or :8000; then: kill <PID>
```

Or use another port: `SERVER_PORT=8081 ./mvnw spring-boot:run` for Spring Boot, or `uv run uvicorn main:app --port 8001` for the analytics service, together with `ANALYTICS_URL=http://localhost:8001` in `.env`.

---

## Git hooks

### Commit rejected by the commit-msg hook

`Commit message must look like: EIQ-42 feat(events): validate capacity on registration`

**Cause:** the subject does not follow `EIQ-<issue> <type>(<scope>): <summary>`. The hook adds the `EIQ-<issue>` prefix automatically only when the branch name contains the issue number (`feature/42-...`).

**Fix:** rename the branch with `git branch -m feature/<issue>-<slug>`, or type the prefix yourself. The format is described in [CONTRIBUTING.md](../CONTRIBUTING.md#commits).

### Commits are not prefixed or validated

**Cause:** the hooks are not enabled in this clone.

**Fix:** run this once per clone; `git config core.hooksPath` then prints `.githooks`:

```bash
git config core.hooksPath .githooks
```

---

## Windows with WSL2 (not validated yet)

The installation steps are in [Onboarding → Windows](onboarding.md#windows). These are the usual WSL2 pitfalls; they have not been tested on this project yet. If you set up EventIQ on Windows, confirm or correct this section in a pull request.

- **`java`, `uv` or `git` is missing or has the wrong version inside Ubuntu:** WSL does not use the Windows installations. Install the tools inside Ubuntu with the [Linux steps](onboarding.md#linux-ubuntu-or-debian).
- **`docker` is not found inside Ubuntu:** in Docker Desktop, enable Settings → Resources → WSL integration for your distribution; `docker ps` must work inside WSL.
- **Builds are very slow, or scripts fail with `bad interpreter: /bin/bash^M`:** the repository is under `/mnt/c/` or was cloned with Git for Windows, which can convert line endings to CRLF. Clone it again inside the Linux file system (for example `~/projects/event-iq`) with the Git inside Ubuntu.
- **Oracle stops during the first start:** Docker's memory comes from WSL2. Raise the limit in `%UserProfile%\.wslconfig`, run `wsl --shutdown` and start Docker Desktop again:

  ```ini
  [wsl2]
  memory=6GB
  ```

---

## Starting over

From least to most destructive:

| Goal | Command |
|---|---|
| Restart Oracle | `docker restart oracle-free` |
| Recreate the Oracle container, keeping the data | `docker rm -f oracle-free`, then the `docker run` from README step 2 |
| Rebuild the analytics service environment | `cd analytics-service && rm -rf .venv && uv sync` |
| Rebuild the Java project | `./mvnw clean verify` |
| Delete the database and start from scratch | `docker rm -f oracle-free && docker volume rm oradata`, then README step 2 from the beginning |

The last option permanently deletes everything in the database, including `app_user` and its tables.

---

## Still stuck?

Open an issue with the Task form, or ask in the sprint's Standups discussion. Include:

- operating system and chip (for example, macOS on Apple Silicon)
- the output of `./mvnw -v`, `uv --version` and `docker ps -a`
- the command you ran and the full error message

Leave out passwords, API keys and the contents of `.env`. Once the problem is solved, add it to this guide so the next person can fix it without waiting for help.

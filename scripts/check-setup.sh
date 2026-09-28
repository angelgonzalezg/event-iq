#!/usr/bin/env bash
# Checks a local EventIQ setup: tools, configuration, database, build and tests, and a short run
# of both services. Works on macOS, Linux and Windows with WSL2. Run it from anywhere in the repo:
#
#   scripts/check-setup.sh           # everything; the first run takes a few minutes (downloads)
#   scripts/check-setup.sh --quick   # tools, configuration and database only
#
# It never prints the values in .env. Logs go to target/check-setup/.
# Help for each failure: docs/setup-troubleshooting.md
set -uo pipefail

QUICK=false
for arg in "$@"; do
  case "$arg" in
    --quick) QUICK=true ;;
    -h | --help) sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown option: $arg (use --quick or --help)" >&2; exit 2 ;;
  esac
done

cd "$(dirname "$0")/.." || exit 1

ORACLE_IMAGE="container-registry.oracle.com/database/free:23.26.3.0"
ORACLE_CONTAINER="oracle-free"
HELP_DOC="docs/setup-troubleshooting.md"
LOG_DIR="target/check-setup"
APP_PORT=8080
ANALYTICS_PORT=8000
PASSED=0
FAILED=0
WARNED=0
JAVA_BIN=""
BACKGROUND_PIDS=""

if [ -t 1 ]; then
  GREEN=$'\033[32m' RED=$'\033[31m' YELLOW=$'\033[33m' BOLD=$'\033[1m' RESET=$'\033[0m'
else
  GREEN="" RED="" YELLOW="" BOLD="" RESET=""
fi

section() { printf '\n%s%s%s\n' "$BOLD" "$1" "$RESET"; }
pass() { PASSED=$((PASSED + 1)); printf '  %s✔%s %s\n' "$GREEN" "$RESET" "$1"; }
fail() {
  FAILED=$((FAILED + 1)); printf '  %s✘%s %s\n' "$RED" "$RESET" "$1"
  if [ -n "${2:-}" ]; then printf '      → %s\n' "$2"; fi
}
warn() {
  WARNED=$((WARNED + 1)); printf '  %s!%s %s\n' "$YELLOW" "$RESET" "$1"
  if [ -n "${2:-}" ]; then printf '      → %s\n' "$2"; fi
}
has() { command -v "$1" >/dev/null 2>&1; }

cleanup() {
  for pid in $BACKGROUND_PIDS; do kill "$pid" 2>/dev/null; done
}
trap cleanup EXIT
trap 'exit 130' INT TERM

# Value of a variable as the applications see it: exported in the shell first, then .env.
config_value() {
  local exported="${!1:-}"
  if [ -n "$exported" ]; then printf '%s' "$exported"; return; fi
  [ -f .env ] && grep -E "^$1=" .env | tail -n 1 | cut -d= -f2-
}

port_in_use() { curl -s -o /dev/null --max-time 2 "http://localhost:$1/"; [ $? -ne 7 ]; }

http_code() { curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$1"; }

# Waits up to $2 seconds for URL $1 to answer at all (any HTTP status) while process $3 is alive.
wait_for_http() {
  local url="$1" seconds="$2" pid="$3"
  while [ "$seconds" -gt 0 ]; do
    [ "$(http_code "$url")" != "000" ] && return 0
    kill -0 "$pid" 2>/dev/null || return 1
    sleep 1
    seconds=$((seconds - 1))
  done
  return 1
}

mkdir -p "$LOG_DIR"
printf '%sEventIQ setup check%s (%s)\n' "$BOLD" "$RESET" "$(uname -s) $(uname -m)"

# ------------------------------------------------------------------------------------------------
section "Tools"

for tool in git curl docker uv; do
  if has "$tool"; then
    pass "$tool: $("$tool" --version 2>/dev/null | head -n 1 | sed 's/ (.*//; s/,.*//')"
  else
    fail "$tool is not installed" "install it: docs/onboarding.md, section 1"
  fi
done

MVNW_OUT="$(./mvnw -v 2>&1)"
JAVA_LINE="$(printf '%s\n' "$MVNW_OUT" | grep -m 1 '^Java version:')"
if printf '%s' "$JAVA_LINE" | grep -q '^Java version: 25[.,]'; then
  pass "Maven runs on Java $(printf '%s' "$JAVA_LINE" | sed 's/^Java version: \([^,]*\),.*/\1/')"
  JAVA_BIN="$(printf '%s' "$JAVA_LINE" | sed -n 's/.*runtime: //p')/bin/java"
elif [ -n "$JAVA_LINE" ]; then
  fail "Maven runs on Java $(printf '%s' "$JAVA_LINE" | sed 's/^Java version: \([^,]*\),.*/\1/'); it must be Java 25" \
    "point JAVA_HOME to JDK 25 ($HELP_DOC: release version 25 not supported)"
else
  fail "./mvnw -v did not run: $(printf '%s\n' "$MVNW_OUT" | tail -n 1)" "install JDK 25 and set JAVA_HOME"
fi

DOCKER_OK=false
if has docker; then
  if docker info >/dev/null 2>&1; then
    DOCKER_OK=true
    pass "Docker is running"
    mem_bytes="$(docker info --format '{{.MemTotal}}' 2>/dev/null)"
    if [ -n "$mem_bytes" ] && [ "$mem_bytes" -lt 3500000000 ] 2>/dev/null; then
      warn "Docker has $((mem_bytes / 1000000)) MB of memory; Oracle needs about 4 GB" \
        "raise it in Docker Desktop, Settings → Resources"
    fi
  elif docker info 2>&1 | grep -qi "permission denied"; then
    fail "your user cannot use Docker" "sudo usermod -aG docker \"$USER\", then log out and back in"
  else
    fail "Docker is installed but not running" "open Docker Desktop, or on Linux: sudo systemctl start docker"
  fi
fi

# ------------------------------------------------------------------------------------------------
section "Configuration"

if [ "$(git config core.hooksPath)" = ".githooks" ]; then
  pass "Git hooks are enabled"
else
  fail "Git hooks are not enabled" "git config core.hooksPath .githooks"
fi

if [ -f .env ]; then
  pass ".env exists"
  for key in ORACLE_PWD DB_USER DB_PASSWORD AI_API_KEY; do
    value="$(config_value "$key")"
    case "$value" in
      "" | "<"*) fail "$key is not set" "fill it in .env (see .env.example)" ;;
      *) pass "$key is set" ;;
    esac
  done
  for key in ADMIN_PASSWORD JWT_SECRET; do
    case "$(config_value "$key")" in
      "" | "<"*) warn "$key is still a placeholder" "needed once sign-in arrives; see .env.example" ;;
    esac
  done
  bad_lines="$(grep -nE "^[^#].*([\"']| #|[[:space:]]\$)" .env | cut -d= -f1 | tr '\n' ' ' | sed 's/ $//')"
  if [ -n "$bad_lines" ]; then
    fail "quotes, comments or trailing spaces in .env: $bad_lines" "write each line as KEY=value ($HELP_DOC)"
  fi
  exported="$(env | grep -E '^(ORACLE_PWD|DB_USER|DB_PASSWORD|DB_URL|DB_DSN|AI_API_KEY|AI_MODEL|ANALYTICS_URL)=' | cut -d= -f1 | tr '\n' ' ' | sed 's/ $//')"
  if [ -n "$exported" ]; then
    warn "exported in this shell, overriding .env: $exported" "unset them if .env should win"
  fi
else
  fail ".env does not exist" "cp .env.example .env, then fill in the values"
fi

# ------------------------------------------------------------------------------------------------
section "Database"

DB_READY=false
if [ "$DOCKER_OK" = true ]; then
  if [ -n "$(docker ps -q --filter "name=^${ORACLE_CONTAINER}\$" --filter status=running)" ]; then
    pass "container $ORACLE_CONTAINER is running"
    image="$(docker inspect --format '{{.Config.Image}}' "$ORACLE_CONTAINER")"
    if [ "$image" = "$ORACLE_IMAGE" ]; then
      pass "image $image"
    else
      warn "image $image; the project pins $ORACLE_IMAGE" "recreate the container with the pinned image (README, step 2)"
    fi
    case "$(uname -m)" in arm64 | aarch64) expected="arm64" ;; *) expected="amd64" ;; esac
    arch="$(docker image inspect --format '{{.Architecture}}' "$image" 2>/dev/null)"
    if [ -n "$arch" ] && [ "$arch" != "$expected" ]; then
      fail "the image is $arch but this machine is $expected: it runs under emulation" "$HELP_DOC: Apple Silicon"
    fi
    # Ask the database itself: log messages can be rotated away, so they are only a hint.
    db_user="$(config_value DB_USER)"
    login="$(printf 'connect %s/"%s"@//localhost:1521/FREEPDB1\nselect %s from dual;\nexit\n' \
      "$db_user" "$(config_value DB_PASSWORD)" "'EIQ_' || 'OK'" |
      docker exec -i "$ORACLE_CONTAINER" sqlplus -s -L /nolog 2>&1)"
    error="$(printf '%s' "$login" | grep -o 'ORA-[0-9]*' | head -n 1)"
    if printf '%s' "$login" | grep -q "EIQ_OK"; then
      DB_READY=true
      pass "the database is up and $db_user can log in to FREEPDB1"
    else
      case "$error" in
        ORA-01017 | ORA-01045 | ORA-28000 | ORA-28001)
          fail "the database is up, but $db_user cannot log in to FREEPDB1 ($error)" "$HELP_DOC: $error" ;;
        *)
          health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$ORACLE_CONTAINER" 2>/dev/null)"
          fail "the database does not accept connections yet (${error:-no answer}${health:+, health: $health})" \
            "if it just started, wait a few minutes (docker logs -f $ORACLE_CONTAINER); otherwise see $HELP_DOC: ${error:-ORA-12541}" ;;
      esac
    fi
  elif [ -n "$(docker ps -aq --filter "name=^${ORACLE_CONTAINER}\$")" ]; then
    fail "container $ORACLE_CONTAINER exists but is stopped" "docker start $ORACLE_CONTAINER"
  else
    fail "container $ORACLE_CONTAINER does not exist" "create it: README, Getting Started, step 2"
  fi
else
  fail "skipped: Docker is not available"
fi

# ------------------------------------------------------------------------------------------------
if [ "$QUICK" = false ]; then
  section "Build and tests"

  if [ -z "$JAVA_BIN" ]; then
    fail "skipped the Java build: Maven is not running on Java 25"
  elif ./mvnw -B verify >"$LOG_DIR/maven.log" 2>&1; then
    pass "Java build and tests pass (./mvnw verify)"
  else
    fail "./mvnw verify failed" "see $LOG_DIR/maven.log"
    JAVA_BIN=""
  fi

  ANALYTICS_OK=false
  if ! has uv; then
    fail "skipped the analytics service: uv is not installed"
  elif (cd analytics-service && uv sync --locked) >"$LOG_DIR/uv.log" 2>&1; then
    ANALYTICS_OK=true
    pass "analytics service dependencies installed (uv sync --locked)"
  else
    fail "uv sync --locked failed" "see $LOG_DIR/uv.log"
  fi

  # ----------------------------------------------------------------------------------------------
  section "Smoke test"

  if [ "$ANALYTICS_OK" = true ] && [ "$DB_READY" = true ]; then
    if port_in_use "$ANALYTICS_PORT"; then
      fail "port $ANALYTICS_PORT is in use" "stop the running analytics service, then run this again"
    else
      (cd analytics-service && exec .venv/bin/python -m uvicorn main:app --port "$ANALYTICS_PORT") \
        >"$LOG_DIR/analytics.log" 2>&1 &
      pid=$!
      BACKGROUND_PIDS="$BACKGROUND_PIDS $pid"
      if ! wait_for_http "http://localhost:$ANALYTICS_PORT/health" 60 "$pid"; then
        fail "analytics service: did not start" "see $LOG_DIR/analytics.log"
      elif [ "$(http_code "http://localhost:$ANALYTICS_PORT/health")" = "200" ] &&
        curl -s "http://localhost:$ANALYTICS_PORT/health" | grep -q '"database":"UP"'; then
        pass "analytics service: GET /health answers 200 with the database UP"
      else
        fail "analytics service: /health reports the database DOWN" "see $LOG_DIR/analytics.log and $HELP_DOC"
      fi
      kill "$pid" 2>/dev/null
    fi
  else
    fail "skipped the analytics service: it needs the dependencies and the database"
  fi

  # Newest jar: older versions may still be in target/. The names are plain, and ls -t sorts portably.
  # shellcheck disable=SC2012
  jar="$(ls -t target/event-iq-*.jar 2>/dev/null | head -n 1)"
  if [ -n "$JAVA_BIN" ] && [ -n "$jar" ] && [ "$DB_READY" = true ]; then
    if port_in_use "$APP_PORT"; then
      fail "port $APP_PORT is in use" "stop the running application, then run this again"
    else
      "$JAVA_BIN" -jar "$jar" >"$LOG_DIR/app.log" 2>&1 &
      pid=$!
      BACKGROUND_PIDS="$BACKGROUND_PIDS $pid"
      if wait_for_http "http://localhost:$APP_PORT/" 120 "$pid"; then
        case "$(http_code "http://localhost:$APP_PORT/")" in
          200)
            if curl -s "http://localhost:$APP_PORT/" | grep -q "Welcome to EventIQ"; then
              pass "application: the home page renders on http://localhost:$APP_PORT"
            else
              fail "application: the home page answered without its content" "see $LOG_DIR/app.log"
            fi ;;
          302 | 401) pass "application: up on http://localhost:$APP_PORT (the home page asks you to sign in)" ;;
          *) fail "application: the home page answered HTTP $(http_code "http://localhost:$APP_PORT/")" "see $LOG_DIR/app.log" ;;
        esac
        if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$APP_PORT/css/app.css")" = "200" ]; then
          pass "application: static files are served"
        else
          fail "application: /css/app.css is not served" "see $LOG_DIR/app.log"
        fi
      else
        error="$(grep -o 'ORA-[0-9]*' "$LOG_DIR/app.log" | head -n 1)"
        fail "application: did not start${error:+ ($error)}" "see $LOG_DIR/app.log and $HELP_DOC"
      fi
      kill "$pid" 2>/dev/null
    fi
  else
    fail "skipped the application: it needs a successful build and the database"
  fi
fi

# ------------------------------------------------------------------------------------------------
printf '\n%s%d passed, %d failed, %d warnings%s\n' "$BOLD" "$PASSED" "$FAILED" "$WARNED" "$RESET"
if [ "$FAILED" -gt 0 ]; then
  printf 'Fix the items marked ✘ and run the check again. Help: %s\n' "$HELP_DOC"
  exit 1
fi
if [ "$QUICK" = true ]; then
  echo "Configuration and database look good. Run without --quick to build and start both services."
else
  echo "Your setup is ready. Post this summary in your onboarding issue."
fi

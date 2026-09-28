# Onboarding

From a clean machine to a running EventIQ base platform, on your own. Plan about an hour; most of it is downloads. If a step fails, [`setup-troubleshooting.md`](setup-troubleshooting.md) has the fix for the most common errors.

1. [Install the tools](#1-install-the-tools) for your operating system.
2. [Get the platform running](#2-get-the-platform-running) with the README.
3. [Check your setup](#3-check-your-setup) with `scripts/check-setup.sh`.
4. [Close your onboarding issue](#4-close-your-onboarding-issue).

---

## 1. Install the tools

| Tool | Version | Used for |
|---|---|---|
| Git | Any recent version | The repository and the commit hooks |
| JDK (Eclipse Temurin) | 25 | The Spring Boot application |
| Docker | Docker Desktop on macOS and Windows, Docker Engine on Linux; 4 GB of memory or more | Oracle Database Free |
| [uv](https://docs.astral.sh/uv/) | 0.12 or later | Installs Python 3.14 and the analytics service's dependencies |
| An editor | IntelliJ IDEA, or VS Code with the Extension Pack for Java, Spring Boot Extension Pack and Python extensions | Everyday work |

You do not install Python or Maven yourself: uv brings Python 3.14, and the Maven wrapper (`./mvnw`) brings Maven.

### macOS

With [Homebrew](https://brew.sh):

```bash
brew install git uv
brew install --cask temurin@25 docker-desktop
```

Open Docker Desktop once, accept its terms and set **Settings → Resources → Memory** to at least 4 GB. Then make Maven use JDK 25 in every terminal:

```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 25)' >> ~/.zshrc
source ~/.zshrc
```

On Apple Silicon, the Oracle image runs natively (`arm64`).

### Linux (Ubuntu or Debian)

```bash
sudo apt update && sudo apt install -y git curl wget gpg

# JDK 25 (Eclipse Temurin)
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update && sudo apt install -y temurin-25-jdk

# Docker Engine, plus permission to use it without sudo (log out and back in afterwards)
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"

# uv
curl -LsSf https://astral.sh/uv/install.sh | sh
```

If another JDK is the default, add `export JAVA_HOME=/usr/lib/jvm/<jdk-25-folder>` to `~/.bashrc` (`ls /usr/lib/jvm` lists the folders). Docker Engine uses the machine's memory directly, so keep at least 4 GB free. For other distributions, follow the [Temurin](https://adoptium.net/installation/linux/) and [Docker Engine](https://docs.docker.com/engine/install/) instructions.

### Windows

Work inside WSL2 with Ubuntu: the Maven wrapper, the Git hooks and the setup check are shell scripts. This path has not been validated on this project yet; note anything that differs in your onboarding issue.

1. In PowerShell as administrator, run `wsl --install -d Ubuntu`, restart and create your Linux user.
2. Install [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/). In **Settings → Resources → WSL integration**, enable Ubuntu. Do not install Docker Engine inside Ubuntu.
3. Open Ubuntu and follow the Linux steps above, skipping Docker Engine.
4. Clone the repository inside the Linux file system (for example `~/projects`), not under `/mnt/c/`.
5. Open it with VS Code and its WSL extension, or with IntelliJ IDEA's WSL support.

Docker's memory limit comes from WSL2; see [Windows with WSL2](setup-troubleshooting.md#windows-with-wsl2-not-validated-yet) if Oracle stops during its first start.

### On every system

- Download the Oracle image in advance; it takes several GB: `docker pull container-registry.oracle.com/database/free:23.26.3.0`
- Create a free Gemini API key in [Google AI Studio](https://aistudio.google.com) (an *auth key*).
- Check the tools: `git --version`, `java -version`, `docker run hello-world` and `uv --version`.

---

## 2. Get the platform running

1. Clone the repository and enable the commit hooks:

   ```bash
   git clone https://github.com/angelgonzalezg/event-iq.git
   cd event-iq
   git config core.hooksPath .githooks
   ```

2. Follow [Getting Started](../README.md#getting-started) in the README, steps 1 to 3: fill in `.env`, start Oracle, create `app_user`, and run the application and the analytics service. Its status note lists the steps that do not apply yet.
3. Open `http://localhost:8080` and check `curl -i localhost:8000/health`.

---

## 3. Check your setup

Stop the application and the analytics service if they are running, and then run from the repository root:

```bash
scripts/check-setup.sh
```

The script checks everything below and prints ✔ or ✘ for each item, with a hint for every failure. It never prints the values in `.env`, and it writes its logs to `target/check-setup/`.

| Group | What it checks |
|---|---|
| Tools | Git, curl, Docker and uv are installed; `./mvnw` runs on Java 25; Docker is running and has enough memory |
| Configuration | Git hooks are enabled; `.env` exists, has the required values and no quotes or trailing comments; no exported variable overrides it |
| Database | The `oracle-free` container runs the pinned image for your architecture, and `app_user` can log in to `FREEPDB1` |
| Build and tests | `./mvnw verify` passes and `uv sync --locked` installs the analytics service |
| Smoke test | It starts both services for a few seconds: `GET /health` answers `200` with the database up, the home page renders and static files are served |

`scripts/check-setup.sh --quick` checks only tools, configuration and the database, which takes a few seconds. Run the full check again after changing your setup or pulling changes to the tooling.

---

## 4. Close your onboarding issue

- Post the script's last lines (the summary) and a screenshot of `http://localhost:8080` in your onboarding issue.
- Reply to the current sprint's discussion in the **Standups** category.
- Before your first pull request, read [`CONTRIBUTING.md`](../CONTRIBUTING.md) and, if you will build pages, [`view-conventions.md`](view-conventions.md).

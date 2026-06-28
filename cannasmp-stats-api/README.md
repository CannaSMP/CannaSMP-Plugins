# CannaSMP ServerAPI

Production-oriented Paper plugin for live CannaSMP server data, private integrations, and GitHub Pages status publishing.

## Architecture Decision

Three approaches were considered:

1. Direct browser calls from GitHub Pages to the Minecraft server API.
   This is simple, but fragile because HTTPS pages cannot reliably call plain HTTP APIs, hosting firewalls may block custom ports, and exposing detailed player endpoints publicly is risky.

2. Plugin commits a public JSON snapshot into the GitHub Pages repository.
   This works well for static hosting, keeps the website fast, and avoids browser firewall/CORS issues. It is not a replacement for private authenticated API consumers.

3. External cloud API/proxy.
   This is the most scalable long-term option, but adds another hosted service, secrets, cost, and operational surface.

Chosen design: combine options 1 and 2. The plugin runs an authenticated live REST API for Discord bots, dashboards, mobile apps, and future plugins. It also exports a small public JSON snapshot for GitHub Pages at `data/server-status.json`. That gives CannaSMP a reliable website today and a clean expansion path later.

## Build

Requires Java 21 and Gradle.

```powershell
gradle clean build
```

Output:

```text
build/libs/CannaSMPStatsAPI-2.0.0.jar
```

This workspace did not have global Gradle installed, so the jar was verified and packaged with the bundled JDK at:

```text
C:\Users\luke\Documents\CannaSMP\CannaSMP-Plugins\cannasmp-stats-api\build\libs\CannaSMPStatsAPI-2.0.0.jar
```

## Commands

```text
/serverapi status
/serverapi reload
/serverapi api
/serverapi version
/serverapi key
```

Legacy alias:

```text
/statsapi status
```

Permission:

```text
cannasmp.serverapi.admin
```

## Files

```text
src/main/java/net/cannasmp/statsapi/api
src/main/java/net/cannasmp/statsapi/authentication
src/main/java/net/cannasmp/statsapi/commands
src/main/java/net/cannasmp/statsapi/config
src/main/java/net/cannasmp/statsapi/endpoints
src/main/java/net/cannasmp/statsapi/events
src/main/java/net/cannasmp/statsapi/listeners
src/main/java/net/cannasmp/statsapi/managers
src/main/java/net/cannasmp/statsapi/models
src/main/java/net/cannasmp/statsapi/services
src/main/java/net/cannasmp/statsapi/storage
src/main/java/net/cannasmp/statsapi/tasks
src/main/java/net/cannasmp/statsapi/utils
src/main/java/net/cannasmp/statsapi/web
```

## Security

Protected endpoints accept either:

```text
Authorization: Bearer <api-key>
X-API-Key: <api-key>
```

Enable `security.ip-whitelist.enabled` for private bot/dashboard deployments. Keep public website data limited to the generated static snapshot.

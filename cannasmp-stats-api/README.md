# CannaSMP Stats API

Production-ready read-only HTTP JSON API for exposing live CannaSMP Paper server stats to a Discord bot and a static website.

## Architecture Decision

Chosen architecture: embedded REST API over HTTP using Java's built-in `HttpServer`.

Why this was chosen:

- Easiest to debug with a browser, `curl`, Postman, Discord bots, and GitHub Pages.
- Works with LiteByte's opened extra port `25886`.
- No external web server or database required.
- Read-only JSON endpoints are simple, predictable, and cache-friendly.
- The plugin controls auth, rate limiting, CORS, startup, and shutdown in one jar.

Tradeoffs:

- REST is request/response, so it is not instant push like WebSockets.
- Static websites cannot safely store private API keys, so only public endpoints should be fetched directly by GitHub Pages.
- Raw TCP would be harder to test and document.
- File export/JSON would be safer but less live and harder for Discord bots to query remotely.
- WebSockets are useful for live dashboards later, but are more complex than needed for v1.

## Project Tree

```text
cannasmp-stats-api/
  build.gradle
  settings.gradle
  README.md
  src/
    main/
      java/
        net/
          cannasmp/
            statsapi/
              CannaSMPStatsApiPlugin.java
      resources/
        config.yml
        plugin.yml
```

## Requirements

- Paper 26.1.2 or newer compatible stable Paper build
- Java 25 or newer
- No required plugin dependencies

Optional integrations:

- Vault for economy balances
- LuckPerms for primary groups
- PlaceholderAPI can coexist, but this plugin does not require it
- CannaSMP shard data YAML if available

## Build

From this folder:

```powershell
gradle clean build
```

The jar will be:

```text
build/libs/CannaSMPStatsAPI-1.0.0.jar
```

If Gradle is not installed:

```powershell
winget install Gradle.Gradle
```

or open the folder in IntelliJ IDEA and run the Gradle `build` task.

## Deployment

1. Build the jar.
2. Stop the Minecraft server.
3. Upload `build/libs/CannaSMPStatsAPI-1.0.0.jar` to `/plugins`.
4. Start the server once so `/plugins/CannaSMPStatsAPI/config.yml` is generated.
5. Stop the server again.
6. Edit the config.
7. Start the server.
8. Test `/health` and `/api/v1/status`.

## Config

Recommended LiteByte config:

```yaml
server:
  port: 25886
  bind-host: "0.0.0.0"
  public-host: "cannasmp.smpserver.net"
  worker-threads: 4

security:
  api-key: "CHANGE_THIS_TO_A_LONG_RANDOM_SECRET"
  public-endpoints:
    - "/health"
    - "/api/v1/status"
    - "/api/v1/meta"
  require-auth-by-default: true

cors:
  enabled: true
  allowed-origins:
    - "https://cannasmp.github.io"
  allowed-methods:
    - "GET"
    - "OPTIONS"

rate-limit:
  enabled: true
  requests-per-minute: 120

stats:
  refresh-seconds: 5
  public-player-list: false
  expose-player-details: true
  leaderboard-size: 10
```

Generate a strong API key with PowerShell:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

Do not commit the live config or API key to GitHub.

## Commands

```text
/statsapi status
/statsapi reload
/statsapi key
```

Permission:

```text
cannasmp.statsapi.admin
```

## Security Model

Public by default:

- `GET /health`
- `GET /api/v1/status`
- `GET /api/v1/meta`

Protected by default:

- `GET /api/v1/players`
- `GET /api/v1/worlds`
- `GET /api/v1/performance`
- `GET /api/v1/leaderboards`
- `GET /api/v1/player/{uuid-or-name}`

Auth headers:

```http
Authorization: Bearer YOUR_API_KEY
```

or:

```http
X-API-Key: YOUR_API_KEY
```

GitHub Pages note: never put a private API key in public frontend JavaScript. Either keep website usage to public endpoints, or proxy protected requests through a Discord bot/backend.

## API Reference

### GET `/health`

Auth: public

Example:

```bash
curl http://130.12.156.44:25886/health
```

Response:

```json
{
  "ok": true,
  "plugin": "1.0.0"
}
```

### GET `/api/v1/status`

Auth: public by default

Example:

```bash
curl http://130.12.156.44:25886/api/v1/status
```

Response:

```json
{
  "online": true,
  "publicHost": "cannasmp.smpserver.net",
  "onlinePlayers": 2,
  "maxPlayers": 20,
  "minecraftVersion": "1.21.6",
  "uptimeSeconds": 1234,
  "snapshotTime": "2026-06-28T01:00:00Z",
  "tps": {
    "oneMinute": 20.0,
    "fiveMinute": 20.0,
    "fifteenMinute": 20.0
  }
}
```

### GET `/api/v1/players`

Auth: protected by default

Example:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY" http://130.12.156.44:25886/api/v1/players
```

Response:

```json
{
  "onlinePlayers": 1,
  "maxPlayers": 20,
  "players": [
    {
      "name": "ItzOnlyFisher",
      "uuid": "00000000-0000-0000-0000-000000000000",
      "world": "world",
      "ping": 89,
      "kills": 0,
      "deaths": 12,
      "playtimeTicks": 1560000,
      "playtimeSeconds": 78000,
      "balance": 284.0,
      "rank": "developer",
      "shards": 120
    }
  ]
}
```

### GET `/api/v1/worlds`

Auth: protected by default

Response:

```json
{
  "worlds": [
    {
      "name": "world",
      "environment": "normal",
      "players": 1
    },
    {
      "name": "afk",
      "environment": "normal",
      "players": 0
    }
  ]
}
```

### GET `/api/v1/performance`

Auth: protected by default

Response:

```json
{
  "tps": {
    "oneMinute": 20.0,
    "fiveMinute": 20.0,
    "fifteenMinute": 20.0
  },
  "mspt": {
    "average": 8.4,
    "min": 0.0,
    "max": 0.0
  },
  "uptimeSeconds": 1234,
  "snapshotTime": "2026-06-28T01:00:00Z"
}
```

### GET `/api/v1/leaderboards`

Auth: protected by default

Leaderboards are built from cached online player data in v1. If a permanent stats database is added later, this endpoint can be extended.

Response:

```json
{
  "leaderboards": {
    "kills": [
      {
        "name": "Player",
        "uuid": "00000000-0000-0000-0000-000000000000",
        "value": 10
      }
    ],
    "deaths": [],
    "playtime": [],
    "balance": [],
    "shards": []
  }
}
```

### GET `/api/v1/player/{uuid-or-name}`

Auth: protected by default

Example:

```bash
curl -H "X-API-Key: YOUR_API_KEY" http://130.12.156.44:25886/api/v1/player/ItzOnlyFisher
```

Response:

```json
{
  "found": true,
  "player": {
    "name": "ItzOnlyFisher",
    "uuid": "00000000-0000-0000-0000-000000000000",
    "world": "world",
    "ping": 89
  }
}
```

### GET `/api/v1/meta`

Auth: public by default

Response:

```json
{
  "name": "CannaSMPStatsAPI",
  "pluginVersion": "1.0.0",
  "minecraftVersion": "26.1.2",
  "bukkitVersion": "git-Paper-...",
  "apiVersion": "v1",
  "readOnly": true
}
```

## Error Responses

```json
{
  "ok": false,
  "error": {
    "code": "unauthorized",
    "message": "Missing or invalid API key."
  }
}
```

Status codes:

- `200` OK
- `204` CORS preflight OK
- `401` missing/bad API key
- `404` endpoint not found
- `405` method not allowed
- `429` rate limited
- `500` internal error

## Discord Bot Example

```js
const API_BASE = "http://130.12.156.44:25886";
const API_KEY = process.env.CANNASMP_STATS_KEY;

async function getStatus() {
  const res = await fetch(`${API_BASE}/api/v1/status`);
  if (!res.ok) throw new Error(`Stats API failed: ${res.status}`);
  return await res.json();
}

async function getPlayers() {
  const res = await fetch(`${API_BASE}/api/v1/players`, {
    headers: { Authorization: `Bearer ${API_KEY}` }
  });
  if (!res.ok) throw new Error(`Stats API failed: ${res.status}`);
  return await res.json();
}
```

## GitHub Pages Website Example

Use only public endpoints directly from GitHub Pages:

```html
<script>
async function loadServerStatus() {
  const res = await fetch("http://130.12.156.44:25886/api/v1/status");
  const data = await res.json();
  document.querySelector("#players").textContent =
    `${data.onlinePlayers}/${data.maxPlayers}`;
}
loadServerStatus();
</script>
```

If the browser blocks mixed content, use HTTPS through a proxy/backend later. GitHub Pages is HTTPS, while this plugin serves HTTP.

## LiteByte Setup Guide

1. Build `CannaSMPStatsAPI-1.0.0.jar`.
2. Open LiteByte panel.
3. Stop the Paper server.
4. Upload the jar to `/plugins`.
5. Start the server once.
6. Confirm the console says:

```text
[CannaSMPStatsAPI] HTTP stats API listening on 0.0.0.0:25886
```

7. Stop the server.
8. Edit `/plugins/CannaSMPStatsAPI/config.yml`.
9. Set:

```yaml
server:
  port: 25886
  bind-host: "0.0.0.0"
  public-host: "cannasmp.smpserver.net"
```

10. Set a strong `security.api-key`.
11. In LiteByte allocations/networking, make sure external allocation `130.12.156.44:25886` is assigned/open for the server.
12. Start the server.

No startup arguments are required.

Test public endpoints:

```text
http://130.12.156.44:25886/health
http://130.12.156.44:25886/api/v1/status
```

Test authenticated endpoint:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY" http://130.12.156.44:25886/api/v1/players
```

From PowerShell:

```powershell
Invoke-RestMethod -Uri "http://130.12.156.44:25886/api/v1/status"
Invoke-RestMethod -Uri "http://130.12.156.44:25886/api/v1/players" -Headers @{ Authorization = "Bearer YOUR_API_KEY" }
```

## Troubleshooting

Port already in use:

- Another plugin/process is using `25886`.
- Change `server.port` or free the LiteByte allocation.

Connection refused:

- Plugin did not start.
- Wrong port.
- Server is offline.
- Check console logs for startup errors.

Timeout:

- LiteByte allocation/firewall is not open.
- Wrong external IP/port.
- `bind-host` is not `0.0.0.0`.

Bad API key:

- Use `Authorization: Bearer YOUR_API_KEY`.
- Make sure the config value does not include extra spaces.
- Run `/statsapi reload` after editing config.

CORS blocked:

- Add the website origin to `cors.allowed-origins`.
- For GitHub Pages use `https://cannasmp.github.io`.
- Do not include a trailing slash.

Plugin failed to load:

- Make sure the server is Paper 26.1.2 or a newer compatible stable Paper build.
- Make sure Java 25 or newer is used.
- Check that the jar is in `/plugins`, not inside the plugin config folder.

Java version mismatch:

- Update the server Java runtime to Java 25 or newer.

Website cannot access protected endpoints:

- Do not put API keys in public JavaScript.
- Use public endpoints only or proxy through a backend/Discord bot.

Discord bot cannot access API:

- Test the URL from your PC/browser first.
- Test the protected endpoint with curl and the same token.
- Confirm LiteByte allocation `130.12.156.44:25886` is open.

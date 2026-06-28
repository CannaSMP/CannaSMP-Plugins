# Setup Guide

## 1. Build

Install Java 21 and Gradle, then run:

```powershell
cd "C:\Users\luke\Documents\CannaSMP\CannaSMP-Plugins\cannasmp-stats-api"
gradle clean build
```

Use:

```text
build/libs/CannaSMPStatsAPI-2.0.0.jar
```

## 2. Upload to LiteByte

Upload the jar to:

```text
/plugins/CannaSMPStatsAPI-2.0.0.jar
```

Restart the server once so Paper creates:

```text
/plugins/CannaSMPStatsAPI/config.yml
```

## 3. Configure API

Edit `config.yml`:

```yaml
server:
  port: 8080
  bind-host: "0.0.0.0"
  public-host: "130.12.156.44"

security:
  api-key: "generate-a-long-random-secret"
```

In LiteByte, allocate/open the configured API port. If LiteByte only allows certain extra allocations, set `server.port` to the allocated port.

## 4. Firewall

Allow inbound TCP traffic to the API port only if you need public API access. For private bots, enable the IP whitelist:

```yaml
security:
  ip-whitelist:
    enabled: true
    allowed:
      - "YOUR_BOT_SERVER_IP"
```

## 5. GitHub Pages Snapshot

Recommended website path:

```text
CannaSMP Website/CannaSMP/data/server-status.json
```

For automatic publishing, create a fine-grained GitHub token with contents read/write access to `CannaSMP/cannasmp.github.io`, then configure:

```yaml
website-export:
  github:
    enabled: true
    owner: "CannaSMP"
    repo: "cannasmp.github.io"
    branch: "main"
    path: "data/server-status.json"
    token: "github_pat_..."
```

Never commit the token.

## 6. Test

```bash
curl http://130.12.156.44:8080/api/v1/status
curl -H "Authorization: Bearer YOUR_KEY" http://130.12.156.44:8080/api/v1
```

In game:

```text
/serverapi status
/serverapi key
```

## 7. Website

The GitHub Pages site reads:

```text
data/server-status.json
```

Commit and push the website changes after confirming the JSON file updates.

## Troubleshooting

- API does not respond: check LiteByte port allocation, firewall, and `server.bind-host`.
- `401 unauthorized`: check `Authorization: Bearer <key>` or `X-API-Key`.
- Website still offline: confirm `data/server-status.json` exists in the GitHub Pages repo and GitHub Pages has deployed the latest commit.
- Missing balance/rank/AFK: confirm Vault, LuckPerms, and EssentialsX are installed and enabled.

## Verification Checklist

- Plugin loads with no startup errors.
- `/serverapi status` reports a current snapshot.
- `/api/v1/status` returns JSON.
- Authenticated `/api/v1/players` returns player data.
- `plugins/CannaSMPStatsAPI/website-export/server-status.json` is created.
- GitHub Pages displays the live status section.

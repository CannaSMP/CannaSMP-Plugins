# API Reference

Base URL:

```text
http://<server-ip>:<api-port>/api/v1
```

Default port:

```text
8080
```

## Public Endpoints

```text
GET /health
GET /api/v1/health
GET /api/v1/status
GET /api/v1/version
```

## Authenticated Endpoints

```text
GET /api/v1
GET /api/v1/server
GET /api/v1/players
GET /api/v1/player/{name}
GET /api/v1/player/{uuid}
GET /api/v1/plugins
GET /api/v1/system
GET /api/v1/integrations
```

## Example Response

```json
{
  "ok": true,
  "online": true,
  "snapshotTime": "2026-06-28T07:53:00Z",
  "currentPlayers": 12,
  "maxPlayers": 100,
  "tps": {
    "oneMinute": 20.0,
    "fiveMinute": 19.98,
    "fifteenMinute": 19.96
  },
  "mspt": {
    "average": 12.4,
    "min": 4.1,
    "max": 33.2
  }
}
```

## curl

```bash
curl http://130.12.156.44:8080/api/v1/status
curl -H "Authorization: Bearer YOUR_KEY" http://130.12.156.44:8080/api/v1/players
```

## JavaScript

```js
const response = await fetch("http://130.12.156.44:8080/api/v1/server", {
  headers: { Authorization: "Bearer YOUR_KEY" }
});
const data = await response.json();
console.log(data.server.currentPlayers);
```

## Python

```python
import requests

response = requests.get(
    "http://130.12.156.44:8080/api/v1/server",
    headers={"Authorization": "Bearer YOUR_KEY"},
    timeout=10,
)
print(response.json()["server"]["currentPlayers"])
```

## Discord Bot Example

```js
import { Client, GatewayIntentBits } from "discord.js";

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

async function serverStatus() {
  const res = await fetch(process.env.CANNASMP_API_URL + "/api/v1/status");
  const data = await res.json();
  return `CannaSMP: ${data.online ? "online" : "offline"} - ${data.currentPlayers}/${data.maxPlayers}`;
}

client.once("ready", async () => {
  console.log(await serverStatus());
});

client.login(process.env.DISCORD_TOKEN);
```

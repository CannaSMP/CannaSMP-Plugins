# CannaSMP Plugins

Custom Minecraft plugins made for CannaSMP.

This repository contains the source for server-specific plugins and patched custom plugins used on the CannaSMP Paper server. Live production configs, tokens, webhooks, SFTP details, and generated build jars are intentionally not included.

## Plugins

| Folder | What it does |
| --- | --- |
| `cannasmp-afk-world` | Sends players to the AFK world and handles AFK-world behavior. |
| `cannasmp-consent` | Shows the CannaSMP cannabis/mature-theme warning GUI and stores player acceptance. |
| `cannasmp-discord-bridge` | Custom Discord/Minecraft bridge with chat, linking, rank sync, staff bridge, and Discord-side server commands. |
| `cannasmp-koth` | Custom KOTH plugin patched/rebranded for CannaSMP. |
| `cannasmp-leaderboard-gui` | `/leaderboard` GUI for balances, kills, deaths, playtime, and shards. |
| `cannasmp-menu` | `/menu` GUI listing normal player commands and quick actions. |
| `cannasmp-rank-command` | Staff rank helper commands using LuckPerms-style permission assignment. |
| `cannasmp-shard-leaderboard` | AFK shard leaderboard support. |
| `cannasmp-shards-placeholder` | PlaceholderAPI expansion for AFK shards. |
| `cannasmp-tools` | Emoji replacement, joke system, chat/command filter, clear chat, and server utility commands. |

## Notes

- These are server-custom plugins, so some code expects CannaSMP's installed plugin stack.
- Config files in this repo use placeholders for secrets.
- Build jars are not committed. Build locally, then upload the jar to the server's `plugins/` folder.

## Server Stack

Main dependencies used across these plugins include Paper, PlaceholderAPI, LuckPerms, Vault, EssentialsX, TAB, DecentHolograms, ajLeaderboards, VirtualSpawners, and JDA for Discord integration.

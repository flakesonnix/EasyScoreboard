# EasyScoreboard — Easy Config + DB

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.26.2-0288D1?logo=minecraft)](https://papermc.io)
[![Spigot](https://img.shields.io/badge/Spigot-compatible-ED8B00)](https://www.spigotmc.org)

Your own scoreboard system — **easy config** (`config.yml` boards with title/lines/permission/animation) + **database** (`scoreboard_prefs` via HikariCP, from template) + **i18n** (en/de). Paper & Spigot, PurrCore optional.

> From `paper-kotlin-template` + `PurrCore` core. You write — this is your scoreboard.

## Easy Config

`src/main/resources/config.yml` → `plugins/EasyScoreboard/config.yml`:

```yaml
scoreboard:
  enabled: true
  update-interval-ticks: 20 # 1s
  use-purrcore-db: true # share PurrCore pool if present
  placeholderapi: true
  boards:
    default:
      title: "&b&lEasyScoreboard"
      lines:
        - "&7&m-------------------"
        - "&fPlayer: &e{player}"
        - "&fOnline: &a{online}&7/&a{max_online}"
        - "&fPing: &e{ping}ms"
        - "&fWorld: &e{world}"
        - "&7&m-------------------"
      title-animation:
        enabled: false
        frames: ["&b&lEasyScoreboard", "&a&lEasyScoreboard"]
        interval-ticks: 10
    vip:
      permission: "easyscoreboard.vip"
      title: "&6&lVIP Board"
      lines: [...]
```

Edit `boards.<id>.title`/`lines` — supports `{player}`, `{online}`, `{max_online}`, `{ping}`, `{world}`, `{health}`, `{level}`, `{tps}`, `{x}`, `{y}`, `{z}` + **PlaceholderAPI** (`%player%`, `%vault%`, etc.) if installed.

Add new board: copy `default` → change `permission` → players with perm see that board automatically (via `resolveBoardId`).

## Database (from template)

- **HikariCP** pool, `sqlite` default (`database.db` → `plugins/EasyScoreboard/database.db`), or `mysql`/`postgresql` via `docker-compose.yml`.
- **Table** `scoreboard_prefs` (`uuid` PK, `enabled` BOOLEAN, `board_id` VARCHAR, `updated_at`).
- **PurrCore optional:** If `scoreboard.use-purrcore-db: true` and `PurrCore` plugin present, EasyScoreboard shares its pool (via reflection `PurrCore.get().database`), else uses own.
- **Persistence:** `/scoreboard toggle` → `prefsRepo.setEnabled` → DB, survives restart.

Config DB: `database.type: sqlite|mysql|postgresql` + `pool.*` + `sqlite.file` etc. See `config.yml`.

## Features

- **Per-player toggle** — `/scoreboard toggle|on|off` → DB `scoreboard_prefs.enabled`
- **Board select** — `/scoreboard board <id>` → `board_id` DB, permission check (`easyscoreboard.vip`)
- **Auto board** — `resolveBoardId` picks highest perm board player can see
- **Update** — `Bukkit.getScheduler().runTaskTimer` every `update-interval-ticks`, rebuilds `Scoreboard` with title animation, duplicate line handling (`§r`), placeholders
- **PlaceholderAPI** — if `placeholderapi: true` and PlaceholderAPI present, `PlaceholderAPI.setPlaceholders(player, line)` called
- **Paper & Spigot** — only Bukkit API (`Scoreboard`, `Objective`, `DisplaySlot.SIDEBAR`), no NMS
- **i18n** — `lang/en.yml`, `de.yml` (`prefix`, `enabled`, `board-set` etc.), `language: en` in `config.yml`, copied to `plugins/EasyScoreboard/lang/` on first run
- **Lightweight** — async DB (Hikari), `ConcurrentHashMap` boards/tasks, `stopAll` on disable

## Commands

- `/scoreboard`/`/sb`/`/esb` `toggle` — toggle own board (perm `easyscoreboard.use`)
- `/scoreboard on|off` — explicit
- `/scoreboard reload` — reload config + restart boards (perm `easyscoreboard.admin`)
- `/scoreboard board <id>` — select board (checks `scoreboard.boards.<id>.permission`)

## Build & Run

```bash
nix develop
gradle spotlessApply # Kotlin fmt ktlint 1.5.0
gradle shadowJar # → build/libs/easyscoreboard-1.0.0.jar
```

Copy to `plugins/` (plus `PurrCore.jar` if you want shared DB, plus `PlaceholderAPI.jar` optional) → restart.

`nix build` → `result/*.jar` (impure, `sandbox relaxed`), `nix fmt` (nixfmt), `gradle idea` (IDEA).

## From Template

Built from `paper-kotlin-template` with DB (HikariCP) from template + `PurrCore` core. See `docs/` for template's DATABASE, DEVELOPMENT etc. Scoreboard config is yours — edit `config.yml` boards easily.

## TODO you can add

- Per-world boards, per-gamemode lines
- Vault `{money}` placeholder (already via PlaceholderAPI if Vault installed)
- Animated lines (scroll, rainbow)
- Conditions (health < 10 → different board)

*You write — template handles boilerplate.*

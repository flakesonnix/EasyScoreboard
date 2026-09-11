# EasyScoreboard

Paper & Spigot — Kotlin — easy, configurable scoreboard.

Boards from `config.yml` (title, lines, permission, animation). Per-player toggle, DB `scoreboard_prefs`, PlaceholderAPI support. Uses PurrCore if present, otherwise its own HikariCP pool (sqlite/mysql/pg).

```bash
gradle shadowJar
# → build/libs/easyscoreboard-1.0.0.jar
```

## Commands

- `/scoreboard` `/sb` — toggle your board (`easyscoreboard.use`)
- `/scoreboard on|off|reload|board <name>` — admin: `easyscoreboard.admin`

## Config

`plugins/EasyScoreboard/config.yml`:

```yaml
scoreboard:
  update-interval-ticks: 20
  boards:
    default:
      title: "&b&lEasyScoreboard"
      lines: ["&fPlayer: {player}", "&fPing: {ping}ms"]
    vip:
      permission: "easyscoreboard.vip"
      title: "&6&lVIP Board"
```

Placeholders: `{player}`, `{online}`, `{ping}`, `{world}`, plus PlaceholderAPI if installed.

## Dev

```bash
nix develop
gradle spotlessApply && nix fmt
gradle shadowJar
```

Depends on `PurrCore` (soft). See `docs/` for details.

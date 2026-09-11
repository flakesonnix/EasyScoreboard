# Architecture — EasyScoreboard

```
Paper/Spigot
  └─ EasyScoreboardPlugin
       ├─ Database — prefs via HikariCP (or PurrCore pool if present)
       ├─ ScoreboardPrefsRepository — per-player toggle + board pref
       ├─ ScoreboardService — builds/edits scoreboard, PlaceholderAPI parse
       └─ ScoreboardCommand — /scoreboard toggle/on/off/reload
```

On join: load pref, show board if enabled. Every 20 ticks: update lines.

Config boards have `permission`, `title`, `lines`, optional `title-animation`.

Uses PurrCore DB when `scoreboard.use-purrcore-db: true` and PurrCore is loaded.

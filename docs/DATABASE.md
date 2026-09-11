# Database — EasyScoreboard

Single table `scoreboard_prefs` (uuid, enabled, board).

- If PurrCore present and `use-purrcore-db: true` → share its pool
- Otherwise own HikariCP pool (sqlite file `plugins/EasyScoreboard/database.db`)

Repo: `ScoreboardPrefsRepository` — `get(uuid)`, `setEnabled(uuid, bool)`, `setBoard(uuid, name)`.

SQLite pool forced 1. Switch to mysql in config for networks.

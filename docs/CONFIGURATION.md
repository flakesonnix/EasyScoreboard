# Configuration — EasyScoreboard

`plugins/EasyScoreboard/config.yml`:

```yaml
language: en
scoreboard:
  enabled: true
  update-interval-ticks: 20
  use-purrcore-db: true
  placeholderapi: true
  boards:
    default:
      permission: ""
      title: "&b&lEasyScoreboard"
      lines:
        - "&fPlayer: {player}"
        - "&fPing: {ping}ms"
```

Add boards with permission — first matching board is used. Placeholders: `{player}`, `{online}`, `{world}` + PlaceholderAPI.

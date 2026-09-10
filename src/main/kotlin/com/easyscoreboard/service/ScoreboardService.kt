package com.easyscoreboard.service

import com.easyscoreboard.db.ScoreboardPrefsRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard

/**
 * EasyScoreboard — per-player Scoreboard, DB prefs, config boards, PlaceholderAPI support, Paper & Spigot.
 * Config: scoreboard.boards.<id>.title/lines/permission
 * DB: scoreboard_prefs (enabled, board_id)
 */
class ScoreboardService(
    private val plugin: JavaPlugin,
    private val prefsRepo: ScoreboardPrefsRepository,
) {
    private val boards = ConcurrentHashMap<UUID, Scoreboard>()
    private val tasks = ConcurrentHashMap<UUID, BukkitTask>()
    private val animationIndex = ConcurrentHashMap<UUID, Int>()

    fun start(player: Player) {
        if (!plugin.config.getBoolean("scoreboard.enabled", true)) return
        val prefs = prefsRepo.get(player.uniqueId)
        if (!prefs.enabled) return
        val boardId = resolveBoardId(player, prefs.boardId)
        createOrUpdate(player, boardId)
        schedule(player)
    }

    fun stop(player: Player) {
        tasks.remove(player.uniqueId)?.cancel()
        boards.remove(player.uniqueId)
        // reset to empty scoreboard to hide
        try {
            player.scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
        } catch (_: Exception) {
        }
    }

    fun toggle(player: Player): Boolean {
        val cur = prefsRepo.get(player.uniqueId)
        val next = !cur.enabled
        prefsRepo.setEnabled(player.uniqueId, next)
        if (next) start(player) else stop(player)
        return next
    }

    fun setBoard(player: Player, boardId: String): Boolean {
        val sec = plugin.config.getConfigurationSection("scoreboard.boards.$boardId") ?: return false
        val perm = sec.getString("permission", "")!!
        if (perm.isNotEmpty() && !player.hasPermission(perm)) return false
        prefsRepo.setBoard(player.uniqueId, boardId)
        createOrUpdate(player, boardId)
        return true
    }

    fun reload() {
        // restart for online players
        Bukkit.getOnlinePlayers().forEach { p ->
            stop(p)
            if (prefsRepo.get(p.uniqueId).enabled) start(p)
        }
    }

    private fun resolveBoardId(player: Player, preferred: String): String {
        // if preferred board requires perm and player lacks it, fallback to default
        val prefSec = plugin.config.getConfigurationSection("scoreboard.boards.$preferred")
        if (prefSec != null) {
            val perm = prefSec.getString("permission", "")!!
            if (perm.isEmpty() || player.hasPermission(perm)) return preferred
        }
        // find first board player has perm for, else default
        val boardsSec = plugin.config.getConfigurationSection("scoreboard.boards") ?: return "default"
        for (key in boardsSec.getKeys(false)) {
            val perm = boardsSec.getString("$key.permission", "")!!
            if (perm.isEmpty() || player.hasPermission(perm)) {
                if (key == preferred) return key
            }
        }
        // check vip etc. — return highest priority board player can see
        for (key in boardsSec.getKeys(false)) {
            val perm = plugin.config.getString("scoreboard.boards.$key.permission", "")!!
            if (perm.isNotEmpty() && player.hasPermission(perm)) return key
        }
        return "default"
    }

    private fun schedule(player: Player) {
        tasks[player.uniqueId]?.cancel()
        val interval = plugin.config.getLong("scoreboard.update-interval-ticks", 20)
        val task = Bukkit.getScheduler().runTaskTimer(
            plugin,
            Runnable {
                if (!player.isOnline) {
                    stop(player)
                    return@Runnable
                }
                val prefs = prefsRepo.get(player.uniqueId)
                if (!prefs.enabled) {
                    stop(player)
                    return@Runnable
                }
                val boardId = resolveBoardId(player, prefs.boardId)
                createOrUpdate(player, boardId)
            },
            interval,
            interval,
        )
        tasks[player.uniqueId] = task
    }

    private fun createOrUpdate(player: Player, boardId: String) {
        val sec = plugin.config.getConfigurationSection("scoreboard.boards.$boardId") ?: plugin.config.getConfigurationSection("scoreboard.boards.default") ?: return
        var title = sec.getString("title", "&bBoard")!!
        // title animation
        val animSec = sec.getConfigurationSection("title-animation")
        if (animSec != null && animSec.getBoolean("enabled", false)) {
            val frames = animSec.getStringList("frames")
            if (frames.isNotEmpty()) {
                val idx = animationIndex.getOrDefault(player.uniqueId, 0)
                title = frames[idx % frames.size]
                animationIndex[player.uniqueId] = (idx + 1) % frames.size
            }
        }
        val lines = sec.getStringList("lines")

        val scoreboard = boards.getOrPut(player.uniqueId) {
            val sm = Bukkit.getScoreboardManager()!!
            sm.newScoreboard
        }
        var obj = scoreboard.getObjective("easyscoreboard")
        if (obj == null) {
            obj = scoreboard.registerNewObjective("easyscoreboard", "dummy", color(title))
            obj.displaySlot = DisplaySlot.SIDEBAR
        } else {
            obj.displayName = color(title)
        }

        // clear old scores — easiest: unregister and re-register
        // but we can update scores in place; for simplicity, reset all entries
        scoreboard.entries.forEach { scoreboard.resetScores(it) }

        // Bukkit scoreboard displays highest score at top, so reverse lines
        // Use unique entries to avoid duplicate lines (add color codes)
        val coloredLines = lines.mapIndexed { idx, raw -> applyPlaceholders(player, raw) }.map { color(it) }
        // handle duplicates by adding §r variations
        val used = mutableSetOf<String>()
        for ((i, line) in coloredLines.withIndex()) {
            var entry = line
            var suffix = 0
            while (!used.add(entry)) {
                entry = line + "§r".repeat(++suffix)
            }
            val score = coloredLines.size - i
            obj.getScore(entry).score = score
        }

        // Paper & Spigot: set player's scoreboard
        if (player.scoreboard != scoreboard) {
            player.scoreboard = scoreboard
        }
    }

    private fun applyPlaceholders(player: Player, raw: String): String {
        var s = raw
            .replace("{player}", player.name)
            .replace("{displayname}", player.displayName)
            .replace("{world}", player.world.name)
            .replace("{online}", Bukkit.getOnlinePlayers().size.toString())
            .replace("{max_online}", Bukkit.getMaxPlayers().toString())
            .replace(
                "{ping}",
                try {
                    player.ping.toString()
                } catch (_: Exception) {
                    "0"
                },
            )
            .replace("{health}", String.format("%.1f", player.health))
            .replace("{level}", player.level.toString())
            .replace("{tps}", getTps())
            .replace("{x}", player.location.blockX.toString())
            .replace("{y}", player.location.blockY.toString())
            .replace("{z}", player.location.blockZ.toString())
        // PlaceholderAPI
        if (plugin.config.getBoolean("scoreboard.placeholderapi", true)) {
            try {
                val cls = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                val m = cls.getMethod("setPlaceholders", Player::class.java, String::class.java)
                s = m.invoke(null, player, s) as String
            } catch (_: Exception) {
            }
        }
        return s
    }

    private fun getTps(): String = try {
        val server = Bukkit.getServer()
        val m = server.javaClass.getMethod("getTPS")
        val tps = m.invoke(server) as DoubleArray
        String.format("%.1f", tps[0])
    } catch (_: Exception) {
        try {
            val field = Bukkit.getServer().javaClass.getField("TPS")
            val tps = (field.get(null) as DoubleArray)[0]
            String.format("%.1f", tps)
        } catch (_: Exception) {
            "20.0"
        }
    }

    private fun color(s: String): String = ChatColor.translateAlternateColorCodes('&', s)

    fun stopAll() {
        Bukkit.getOnlinePlayers().forEach { stop(it) }
        tasks.values.forEach { it.cancel() }
        tasks.clear()
        boards.clear()
    }
}

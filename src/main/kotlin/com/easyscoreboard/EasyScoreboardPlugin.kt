package com.easyscoreboard

import com.easyscoreboard.command.ScoreboardCommand
import com.easyscoreboard.db.Database
import com.easyscoreboard.db.ScoreboardPrefsRepository
import com.easyscoreboard.i18n.I18n
import com.easyscoreboard.service.ScoreboardService
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class EasyScoreboardPlugin :
    JavaPlugin(),
    Listener {
    lateinit var database: Database
        private set
    lateinit var prefsRepo: ScoreboardPrefsRepository
        private set
    lateinit var scoreboardService: ScoreboardService
        private set
    lateinit var i18n: I18n
        private set

    // Optional PurrCore integration — returns true if PurrCore DB is available
    private fun hasPurrCore(): Boolean {
        if (!config.getBoolean("scoreboard.use-purrcore-db", true)) return false
        return try {
            val cls = Class.forName("com.purrcore.PurrCorePlugin")
            val get = cls.getMethod("get")
            val inst = get.invoke(null)
            inst != null
        } catch (_: Exception) {
            false
        }
    }

    override fun onEnable() {
        saveDefaultConfig()
        // i18n
        i18n = I18n(this)
        i18n.load()

        // DB — create own Database, PurrCore sharing happens at config level
        // (both plugins can use same database.type/sqlite.file/mysql.* config)
        database = Database(this)
        if (hasPurrCore()) {
            logger.info("PurrCore detected — ensure both plugins use same DB config for pool sharing")
        }
        try {
            database.connect()
            database.migrate()
        } catch (e: Exception) {
            logger.severe("DB failed: ${e.message}")
            e.printStackTrace()
        }

        prefsRepo = ScoreboardPrefsRepository(database, logger)
        scoreboardService = ScoreboardService(this, prefsRepo)

        // Command — Paper & Spigot
        getCommand("scoreboard")?.let {
            val cmd = ScoreboardCommand(scoreboardService, prefsRepo, i18n)
            it.setExecutor(cmd)
            it.tabCompleter = cmd
        }

        server.pluginManager.registerEvents(this, this)

        // start for online players (reload)
        server.onlinePlayers.forEach { scoreboardService.start(it) }

        logger.info("EasyScoreboard enabled — easy config + DB (PurrCore=${hasPurrCore()}) — Paper & Spigot")
    }

    override fun onDisable() {
        scoreboardService.stopAll()
        if (::database.isInitialized) database.close()
        logger.info("EasyScoreboard disabled")
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val p: Player = e.player
        // delay 1 tick to ensure prefs loaded
        server.scheduler.runTask(this, Runnable { scoreboardService.start(p) })
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        scoreboardService.stop(e.player)
    }
}

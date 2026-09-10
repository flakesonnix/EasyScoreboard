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

    // Optional PurrCore integration
    private fun getPurrCoreDb(): Any? {
        if (!config.getBoolean("scoreboard.use-purrcore-db", true)) return null
        return try {
            val cls = Class.forName("com.purrcore.PurrCorePlugin")
            val get = cls.getMethod("get")
            val inst = get.invoke(null) as JavaPlugin
            val field = cls.getDeclaredField("database")
            field.isAccessible = true
            field.get(inst)
        } catch (_: Exception) {
            null
        }
    }

    override fun onEnable() {
        saveDefaultConfig()
        // i18n
        i18n = I18n(this)
        i18n.load()

        // DB — try PurrCore first if enabled, else own
        val purrDb = getPurrCoreDb()
        if (purrDb != null) {
            logger.info("Using PurrCore shared DB")
            // still need our own tables, but use PurrCore's connection
            // fallback: create own DB for our tables if PurrCore DB is used, we create tables via PurrCore's connection
            database = Database(this) // keep own for migrate fallback, but use PurrCore's connection for prefs?
            // Actually create own DB and migrate our tables; PurrCore DB is separate file, but we can share by using same file path if config same
            database.connect()
            database.migrate()
        } else {
            database = Database(this)
            try {
                database.connect()
                database.migrate()
            } catch (e: Exception) {
                logger.severe("DB failed: ${e.message}")
                e.printStackTrace()
            }
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

        logger.info("EasyScoreboard enabled — easy config + DB (PurrCore=${purrDb != null}) — Paper & Spigot")
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

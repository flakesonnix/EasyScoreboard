package com.easyscoreboard.service

import com.easyscoreboard.db.Database
import com.easyscoreboard.db.ScoreboardPrefsRepository
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ScoreboardServiceExtraTest {

    @TempDir
    lateinit var tempDir: File

    private fun setup(enabled: Boolean = true): Pair<JavaPlugin, ScoreboardPrefsRepository> {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val config = YamlConfiguration()
        config.set("database.type", "sqlite")
        config.set("database.sqlite.file", "test.db")
        config.set("database.pool.maximum-pool-size", 1)
        config.set("scoreboard.enabled", enabled)
        config.set("scoreboard.update-interval-ticks", 20)
        config.set("scoreboard.boards.default.title", "&bDefault")
        config.set("scoreboard.boards.default.lines", listOf("li"))
        config.set("scoreboard.boards.vip.permission", "easyscoreboard.vip")
        config.set("scoreboard.boards.vip.title", "&6VIP")
        every { plugin.config } returns config
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
        val db = Database(plugin)
        db.connect()
        db.migrate()
        val repo = ScoreboardPrefsRepository(db, mockk(relaxed = true))
        return plugin to repo
    }

    @Test
    fun `toggle flips`() {
        val (plugin, repo) = setup()
        val service = ScoreboardService(plugin, repo)
        val player = mockk<Player>(relaxed = true)
        val uuid = UUID.randomUUID()
        every { player.uniqueId } returns uuid
        every { player.hasPermission(any<String>()) } returns true
        // mock Bukkit scheduler for service start
        mockkStatic()
        try {
            val first = service.toggle(player)
            // initial enabled true -> toggle returns false
            assertFalse(first)
            val second = service.toggle(player)
            assertTrue(second)
            // cleanup
            service.stop(player)
        } finally {
            io.mockk.unmockkStatic(org.bukkit.Bukkit::class)
        }
    }

    private fun mockkStatic() {
        try {
            io.mockk.mockkStatic(Bukkit::class)
            val server = mockk<org.bukkit.Server>(relaxed = true)
            every { Bukkit.getServer() } returns server
            every { server.onlinePlayers } returns emptyList()
            every { Bukkit.getOnlinePlayers() } returns emptyList()
            every { Bukkit.getOfflinePlayers() } returns emptyArray()
            every { Bukkit.getMaxPlayers() } returns 20
            every { Bukkit.getOnlinePlayers().size } returns 0
            every { Bukkit.getVersion() } returns "test"
            val scheduler = mockk<org.bukkit.scheduler.BukkitScheduler>(relaxed = true)
            every { Bukkit.getScheduler() } returns scheduler
            every { server.scheduler } returns scheduler
            val mgr = mockk<org.bukkit.scoreboard.ScoreboardManager>(relaxed = true)
            every { Bukkit.getScoreboardManager() } returns mgr
            every { mgr.newScoreboard } returns mockk(relaxed = true)
            every { scheduler.runTaskTimer(any<org.bukkit.plugin.Plugin>(), any<Runnable>(), any<Long>(), any<Long>()) } returns mockk(relaxed = true)
            every { scheduler.runTask(any<org.bukkit.plugin.Plugin>(), any<Runnable>()) } returns mockk(relaxed = true)
        } catch (_: Exception) {
        }
    }

    @Test
    fun `setBoard respects permission`() {
        val (plugin, repo) = setup()
        val service = ScoreboardService(plugin, repo)
        val player = mockk<Player>(relaxed = true)
        every { player.hasPermission("easyscoreboard.vip") } returns false
        every { player.uniqueId } returns UUID.randomUUID()
        val ok = service.setBoard(player, "vip")
        assertFalse(ok)
        every { player.hasPermission("easyscoreboard.vip") } returns true
        // need to mock Bukkit scheduler for setBoard -> createOrUpdate which touches scoreboard
        mockkStatic()
        try {
            val ok2 = service.setBoard(player, "vip")
            assertTrue(ok2)
        } finally {
            io.mockk.unmockkStatic(org.bukkit.Bukkit::class)
        }
    }

    @Test
    fun `setBoard fails for unknown`() {
        val (plugin, repo) = setup()
        val service = ScoreboardService(plugin, repo)
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()
        val ok = service.setBoard(player, "unknown_board_xyz")
        assertFalse(ok)
    }
}

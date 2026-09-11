package com.easyscoreboard.service

import com.easyscoreboard.db.Database
import com.easyscoreboard.db.ScoreboardPrefsRepository
import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ScoreboardServiceTest {
    @TempDir lateinit var tempDir: File

    @Test
    fun `service creates without error`() {
        val plugin = mockk<JavaPlugin>(relaxed = true)
        val config = YamlConfiguration()
        config.set("database.type", "sqlite")
        config.set("database.sqlite.file", "test.db")
        config.set("database.pool.maximum-pool-size", 1)
        config.set("scoreboard.enabled", false)
        config.set("scoreboard.update-interval-ticks", 20)
        config.set("scoreboard.boards.default.title", "&bTest")
        config.set("scoreboard.boards.default.lines", listOf("line1"))
        every { plugin.config } returns config
        every { plugin.dataFolder } returns tempDir
        every { plugin.logger } returns mockk(relaxed = true)
        every { plugin.getResource(any()) } returns null
        val db = Database(plugin)
        db.connect()
        db.migrate()
        val repo = ScoreboardPrefsRepository(db, mockk(relaxed = true))
        val service = ScoreboardService(plugin, repo)
        assertDoesNotThrow { service.stopAll() }
        db.close()
    }
}

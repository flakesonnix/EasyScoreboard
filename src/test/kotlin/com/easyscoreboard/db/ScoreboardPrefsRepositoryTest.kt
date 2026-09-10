package com.easyscoreboard.db

import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.util.UUID
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ScoreboardPrefsRepositoryTest {
    @TempDir lateinit var tempDir: File

    private fun mockPlugin(): JavaPlugin {
        val p = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("database.type", "sqlite")
        c.set("database.sqlite.file", "test.db")
        c.set("database.pool.maximum-pool-size", 1)
        c.set("database.pool.minimum-idle", 1)
        every { p.config } returns c
        every { p.dataFolder } returns tempDir
        every { p.logger } returns mockk(relaxed = true)
        return p
    }

    private fun db(): Database {
        val plugin = mockPlugin()
        val db = Database(plugin)
        db.connect()
        db.migrate()
        return db
    }

    @Test
    fun `get returns default when not exists`() {
        val db = db()
        val repo = ScoreboardPrefsRepository(db, mockk(relaxed = true))
        val uuid = UUID.randomUUID()
        val prefs = repo.get(uuid)
        assertEquals(uuid, prefs.uuid)
        assertTrue(prefs.enabled)
        assertEquals("default", prefs.boardId)
        db.close()
    }

    @Test
    fun `setEnabled persists`() {
        val db = db()
        val repo = ScoreboardPrefsRepository(db, mockk(relaxed = true))
        val uuid = UUID.randomUUID()
        repo.setEnabled(uuid, false)
        assertFalse(repo.get(uuid).enabled)
        repo.setEnabled(uuid, true)
        assertTrue(repo.get(uuid).enabled)
        db.close()
    }

    @Test
    fun `setBoard persists`() {
        val db = db()
        val repo = ScoreboardPrefsRepository(db, mockk(relaxed = true))
        val uuid = UUID.randomUUID()
        repo.setBoard(uuid, "vip")
        assertEquals("vip", repo.get(uuid).boardId)
        db.close()
    }

    @Test
    fun `upsert updates`() {
        val db = db()
        val repo = ScoreboardPrefsRepository(db, mockk(relaxed = true))
        val uuid = UUID.randomUUID()
        repo.upsert(ScoreboardPrefsRepository.Prefs(uuid, true, "default"))
        repo.upsert(ScoreboardPrefsRepository.Prefs(uuid, false, "vip"))
        val loaded = repo.get(uuid)
        assertFalse(loaded.enabled)
        assertEquals("vip", loaded.boardId)
        db.close()
    }
}

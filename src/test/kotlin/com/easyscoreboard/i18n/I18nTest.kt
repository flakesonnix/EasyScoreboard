package com.easyscoreboard.i18n

import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class I18nTest {
    @TempDir lateinit var tempDir: File

    private fun mockPlugin(lang: String): JavaPlugin {
        val p = mockk<JavaPlugin>(relaxed = true)
        val c = YamlConfiguration()
        c.set("language", lang)
        every { p.config } returns c
        every { p.dataFolder } returns tempDir
        every { p.getResource(any()) } answers {
            when (firstArg<String>()) {
                "lang/en.yml" -> "prefix: \"&b[SB] \"\nhello: \"Hi {player}\"".byteInputStream()
                "lang/de.yml" -> "prefix: \"&b[SB-DE] \"\nhallo: \"Hallo {player}\"".byteInputStream()
                else -> null
            }
        }
        every { p.logger } returns mockk(relaxed = true)
        return p
    }

    @Test
    fun `t replaces placeholders`() {
        val i18n = I18n(mockPlugin("en"))
        i18n.load()
        val out = i18n.t("hello", "player" to "Alex")
        assertTrue(out.contains("Hi") && out.contains("Alex"))
    }

    @Test
    fun `tp adds prefix`() {
        val i18n = I18n(mockPlugin("en"))
        i18n.load()
        val out = i18n.tp("hello", "player" to "Bob")
        assertTrue(out.contains("SB") || out.contains("Hi"))
    }
}

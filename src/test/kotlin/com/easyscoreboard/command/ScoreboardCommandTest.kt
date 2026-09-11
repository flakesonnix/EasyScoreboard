package com.easyscoreboard.command

import com.easyscoreboard.db.ScoreboardPrefsRepository
import com.easyscoreboard.i18n.I18n
import com.easyscoreboard.service.ScoreboardService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScoreboardCommandTest {

    private fun mocks(): Triple<ScoreboardService, ScoreboardPrefsRepository, I18n> {
        val service = mockk<ScoreboardService>(relaxed = true)
        val prefs = mockk<ScoreboardPrefsRepository>(relaxed = true)
        val i18n = mockk<I18n>(relaxed = true)
        every { i18n.t(any(), *anyVararg()) } returns "msg"
        every { i18n.tp(any(), *anyVararg()) } returns "msg"
        return Triple(service, prefs, i18n)
    }

    @Test
    fun `toggle by player`() {
        val (service, _, i18n) = mocks()
        every { service.toggle(any()) } returns true
        val cmd = ScoreboardCommand(service, mockk(relaxed = true), i18n)
        val player = mockk<Player>(relaxed = true)
        val result = cmd.onCommand(player, mockk(relaxed = true), "scoreboard", arrayOf())
        assertTrue(result)
        verify { service.toggle(player) }
        verify { i18n.send(player, "toggle-on") }
    }

    @Test
    fun `toggle off`() {
        val (service, _, i18n) = mocks()
        every { service.toggle(any()) } returns false
        val cmd = ScoreboardCommand(service, mockk(relaxed = true), i18n)
        val player = mockk<Player>(relaxed = true)
        cmd.onCommand(player, mockk(relaxed = true), "scoreboard", arrayOf("toggle"))
        verify { i18n.send(player, "toggle-off") }
    }

    @Test
    fun `non-player toggle sends only-players`() {
        val (service, _, i18n) = mocks()
        val cmd = ScoreboardCommand(service, mockk(relaxed = true), i18n)
        val sender = mockk<org.bukkit.command.CommandSender>(relaxed = true)
        cmd.onCommand(sender, mockk(relaxed = true), "scoreboard", arrayOf())
        verify { i18n.send(sender, "only-players") }
    }

    @Test
    fun `on enables`() {
        val (service, prefs, i18n) = mocks()
        val cmd = ScoreboardCommand(service, prefs, i18n)
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns java.util.UUID.randomUUID()
        cmd.onCommand(player, mockk(relaxed = true), "scoreboard", arrayOf("on"))
        verify { prefs.setEnabled(any(), true) }
        verify { service.start(player) }
    }

    @Test
    fun `off disables`() {
        val (service, prefs, i18n) = mocks()
        val cmd = ScoreboardCommand(service, prefs, i18n)
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns java.util.UUID.randomUUID()
        cmd.onCommand(player, mockk(relaxed = true), "scoreboard", arrayOf("off"))
        verify { prefs.setEnabled(any(), false) }
        verify { service.stop(player) }
    }

    @Test
    fun `reload requires perm`() {
        val (service, _, i18n) = mocks()
        val cmd = ScoreboardCommand(service, mockk(relaxed = true), i18n)
        val sender = mockk<Player>(relaxed = true)
        every { sender.hasPermission("easyscoreboard.admin") } returns false
        cmd.onCommand(sender, mockk(relaxed = true), "scoreboard", arrayOf("reload"))
        verify { i18n.send(sender, "no-permission") }
        // service.reload not called
        verify(exactly = 0) { service.reload() }
    }

    @Test
    fun `reload success`() {
        val (service, _, i18n) = mocks()
        val cmd = ScoreboardCommand(service, mockk(relaxed = true), i18n)
        val sender = mockk<Player>(relaxed = true)
        every { sender.hasPermission("easyscoreboard.admin") } returns true
        cmd.onCommand(sender, mockk(relaxed = true), "scoreboard", arrayOf("reload"))
        verify { i18n.reload() }
        verify { service.reload() }
    }

    @Test
    fun `board set success and failure`() {
        val (service, _, i18n) = mocks()
        every { service.setBoard(any(), "vip") } returns true
        every { service.setBoard(any(), "nope") } returns false
        val cmd = ScoreboardCommand(service, mockk(relaxed = true), i18n)
        val player = mockk<Player>(relaxed = true)
        cmd.onCommand(player, mockk(relaxed = true), "scoreboard", arrayOf("board", "vip"))
        verify { i18n.send(player, "board-set", any()) }
        cmd.onCommand(player, mockk(relaxed = true), "scoreboard", arrayOf("board", "nope"))
        verify { i18n.send(player, "board-not-found", any()) }
    }

    @Test
    fun `tabComplete first arg`() {
        val (service, prefs, i18n) = mocks()
        val cmd = ScoreboardCommand(service, prefs, i18n)
        val sender = mockk<Player>(relaxed = true)
        val res = cmd.onTabComplete(sender, mockk(relaxed = true), "scoreboard", arrayOf("to"))
        assertTrue(res.contains("toggle"))
    }

    @Test
    fun `tabComplete unknown returns empty`() {
        val (service, prefs, i18n) = mocks()
        val cmd = ScoreboardCommand(service, prefs, i18n)
        val sender = mockk<Player>(relaxed = true)
        val res = cmd.onTabComplete(sender, mockk(relaxed = true), "scoreboard", arrayOf("unknown"))
        assertTrue(res.isEmpty())
    }
}

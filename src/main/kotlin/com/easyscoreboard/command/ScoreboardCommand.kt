package com.easyscoreboard.command

import com.easyscoreboard.db.ScoreboardPrefsRepository
import com.easyscoreboard.i18n.I18n
import com.easyscoreboard.service.ScoreboardService
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ScoreboardCommand(
    private val service: ScoreboardService,
    private val prefsRepo: ScoreboardPrefsRepository,
    private val i18n: I18n,
) : CommandExecutor,
    TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty() || args[0].lowercase() == "toggle") {
            if (sender !is Player) {
                i18n.send(sender, "only-players")
                return true
            }
            val on = service.toggle(sender)
            i18n.send(sender, if (on) "toggle-on" else "toggle-off")
            return true
        }
        when (args[0].lowercase()) {
            "on", "enable" -> {
                if (sender !is Player) {
                    i18n.send(sender, "only-players")
                    return true
                }
                prefsRepo.setEnabled(sender.uniqueId, true)
                service.start(sender)
                i18n.send(sender, "enabled")
                return true
            }
            "off", "disable" -> {
                if (sender !is Player) {
                    i18n.send(sender, "only-players")
                    return true
                }
                prefsRepo.setEnabled(sender.uniqueId, false)
                service.stop(sender)
                i18n.send(sender, "disabled")
                return true
            }
            "reload", "rl" -> {
                if (!sender.hasPermission("easyscoreboard.admin")) {
                    i18n.send(sender, "no-permission")
                    return true
                }
                i18n.reload()
                service.reload()
                i18n.send(sender, "reloaded")
                return true
            }
            "board", "set" -> {
                if (sender !is Player) {
                    i18n.send(sender, "only-players")
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage(i18n.t("usage"))
                    // list boards
                    val boards = sender.server.pluginManager.getPlugin("EasyScoreboard")?.config?.getConfigurationSection("scoreboard.boards")?.getKeys(false) ?: emptySet()
                    sender.sendMessage("§7Boards: ${boards.joinToString(", ")}")
                    return true
                }
                val board = args[1]
                val ok = service.setBoard(sender, board)
                if (ok) i18n.send(sender, "board-set", "board" to board) else i18n.send(sender, "board-not-found", "board" to board)
                return true
            }
            else -> {
                i18n.send(sender, "usage")
                return true
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) return listOf("toggle", "on", "off", "reload", "board").filter { it.startsWith(args[0], true) }
        if (args.size == 2 && args[0].lowercase() in setOf("board", "set")) {
            val plugin = sender.server.pluginManager.getPlugin("EasyScoreboard") ?: return emptyList()
            val boards = plugin.config.getConfigurationSection("scoreboard.boards")?.getKeys(false) ?: return emptyList()
            return boards.filter { it.startsWith(args[1], true) }
        }
        return emptyList()
    }
}

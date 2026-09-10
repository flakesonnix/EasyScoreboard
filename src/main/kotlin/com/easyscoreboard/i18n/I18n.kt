package com.easyscoreboard.i18n

import java.io.File
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

class I18n(private val plugin: JavaPlugin) {
    private var lang: String = "en"
    private var messages: YamlConfiguration = YamlConfiguration()
    private var fallback: YamlConfiguration = YamlConfiguration()

    fun load() {
        lang = plugin.config.getString("language", "en")!!.lowercase()
        val langDir = File(plugin.dataFolder, "lang")
        langDir.mkdirs()
        for (code in listOf("en", "de")) {
            val out = File(langDir, "$code.yml")
            if (!out.exists()) {
                plugin.getResource("lang/$code.yml")?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
        plugin.getResource("lang/en.yml")?.use { input ->
            fallback = YamlConfiguration.loadConfiguration(input.reader())
        }
        val selectedFile = File(langDir, "$lang.yml")
        messages = if (selectedFile.exists()) {
            YamlConfiguration.loadConfiguration(selectedFile)
        } else {
            plugin.getResource("lang/$lang.yml")?.use { input ->
                YamlConfiguration.loadConfiguration(input.reader())
            } ?: fallback
        }
        plugin.logger.info("I18n loaded lang=$lang (${messages.getKeys(true).size} keys)")
    }

    fun t(key: String, vararg placeholders: Pair<String, String>): String {
        var raw = messages.getString(key) ?: fallback.getString(key) ?: "[$key]"
        for ((k, v) in placeholders) raw = raw.replace("{$k}", v)
        return color(raw)
    }

    fun tp(key: String, vararg placeholders: Pair<String, String>): String {
        val prefix = messages.getString("prefix") ?: fallback.getString("prefix") ?: ""
        return color(prefix) + t(key, *placeholders)
    }

    fun send(sender: CommandSender, key: String, vararg placeholders: Pair<String, String>) {
        sender.sendMessage(tp(key, *placeholders))
    }

    private fun color(s: String): String = ChatColor.translateAlternateColorCodes('&', s)
    fun reload() = load()
}

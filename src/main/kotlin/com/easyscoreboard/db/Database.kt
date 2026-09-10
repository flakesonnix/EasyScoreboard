package com.easyscoreboard.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException
import java.util.logging.Level
import org.bukkit.plugin.java.JavaPlugin

/** EasyScoreboard DB — fallback if PurrCore not present. Uses same HikariCP config as template. */
class Database(private val plugin: JavaPlugin) {
    private var ds: HikariDataSource? = null

    fun connect() {
        if (ds?.isClosed == false) return
        val type = plugin.config.getString("database.type", "sqlite")!!.lowercase()
        val cfg = HikariConfig().apply {
            poolName = "EasyScoreboard-Hikari"
            maximumPoolSize = plugin.config.getInt("database.pool.maximum-pool-size", 10)
            minimumIdle = plugin.config.getInt("database.pool.minimum-idle", 2)
            connectionTimeout = plugin.config.getLong("database.pool.connection-timeout", 30000)
            idleTimeout = plugin.config.getLong("database.pool.idle-timeout", 600000)
            maxLifetime = plugin.config.getLong("database.pool.max-lifetime", 1800000)
            leakDetectionThreshold = 60000
        }
        when (type) {
            "mysql" -> configureMySql(cfg)
            "postgresql", "postgres", "pgsql" -> configurePostgres(cfg)
            "sqlite" -> configureSqlite(cfg)
            else -> {
                plugin.logger.warning("Unknown database.type '$type' → sqlite")
                configureSqlite(cfg)
            }
        }
        cfg.addDataSourceProperty("cachePrepStmts", "true")
        cfg.addDataSourceProperty("prepStmtCacheSize", "250")
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        ds = HikariDataSource(cfg)
        plugin.logger.info("DB connected [$type] pool=${cfg.maximumPoolSize}")
    }

    private fun configureSqlite(cfg: HikariConfig) {
        val fileName = plugin.config.getString("database.sqlite.file", "database.db")!!
        val file = File(plugin.dataFolder, fileName)
        file.parentFile?.mkdirs()
        cfg.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
        cfg.driverClassName = "org.sqlite.JDBC"
        cfg.maximumPoolSize = 1
        cfg.minimumIdle = 1
        cfg.connectionTestQuery = "SELECT 1"
    }

    private fun configureMySql(cfg: HikariConfig) {
        val host = plugin.config.getString("database.mysql.host", "localhost")!!
        val port = plugin.config.getInt("database.mysql.port", 3306)
        val db = plugin.config.getString("database.mysql.database", "minecraft")!!
        val user = plugin.config.getString("database.mysql.user", "root")!!
        val pass = plugin.config.getString("database.mysql.password", "")!!
        val params = plugin.config.getString("database.mysql.params", "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4")!!
        cfg.jdbcUrl = "jdbc:mysql://$host:$port/$db?$params"
        cfg.username = user
        cfg.password = pass
        cfg.driverClassName = "com.mysql.cj.jdbc.Driver"
    }

    private fun configurePostgres(cfg: HikariConfig) {
        val host = plugin.config.getString("database.postgresql.host", "localhost")!!
        val port = plugin.config.getInt("database.postgresql.port", 5432)
        val db = plugin.config.getString("database.postgresql.database", "minecraft")!!
        val user = plugin.config.getString("database.postgresql.user", "postgres")!!
        val pass = plugin.config.getString("database.postgresql.password", "")!!
        val params = plugin.config.getString("database.postgresql.params", "sslmode=disable")!!
        cfg.jdbcUrl = "jdbc:postgresql://$host:$port/$db?$params"
        cfg.username = user
        cfg.password = pass
        cfg.driverClassName = "org.postgresql.Driver"
    }

    fun migrate() {
        val ddls = arrayOf(
            """
            CREATE TABLE IF NOT EXISTS scoreboard_prefs (
              uuid VARCHAR(36) PRIMARY KEY,
              enabled BOOLEAN DEFAULT TRUE,
              board_id VARCHAR(50) DEFAULT 'default',
              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
        )
        try {
            getConnection().use { c ->
                c.createStatement().use { s -> ddls.forEach { s.execute(it) } }
            }
            plugin.logger.info("DB migrate done (EasyScoreboard)")
        } catch (e: SQLException) {
            plugin.logger.log(Level.SEVERE, "DB migrate failed", e)
        }
    }

    @Throws(SQLException::class)
    fun getConnection(): Connection {
        val d = ds ?: throw SQLException("DataSource not initialized — call connect()")
        return d.connection
    }

    fun isConnected(): Boolean = ds?.isClosed == false
    fun isSqlite(): Boolean = ds?.jdbcUrl?.startsWith("jdbc:sqlite") == true
    fun close() {
        val d = ds
        if (d != null && !d.isClosed) {
            d.close()
            plugin.logger.info("DB pool closed")
        }
    }

    val dataSource: HikariDataSource? get() = ds
}

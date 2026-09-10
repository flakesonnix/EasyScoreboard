package com.easyscoreboard.db

import java.sql.SQLException
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

class ScoreboardPrefsRepository(private val db: Database, private val log: Logger) {
    data class Prefs(val uuid: UUID, val enabled: Boolean = true, val boardId: String = "default")

    fun get(uuid: UUID): Prefs {
        val sql = "SELECT enabled, board_id FROM scoreboard_prefs WHERE uuid=?"
        try {
            db.getConnection().use { c ->
                c.prepareStatement(sql).use { ps ->
                    ps.setString(1, uuid.toString())
                    ps.executeQuery().use { rs ->
                        if (rs.next()) return Prefs(uuid, rs.getBoolean(1), rs.getString(2))
                    }
                }
            }
        } catch (e: SQLException) {
            log.log(Level.WARNING, "get prefs failed", e)
        }
        return Prefs(uuid)
    }

    fun setEnabled(uuid: UUID, enabled: Boolean) {
        val cur = get(uuid)
        upsert(cur.copy(enabled = enabled))
    }

    fun setBoard(uuid: UUID, boardId: String) {
        val cur = get(uuid)
        upsert(cur.copy(boardId = boardId))
    }

    fun upsert(p: Prefs) {
        val sql = if (db.isSqlite()) {
            "INSERT INTO scoreboard_prefs(uuid, enabled, board_id) VALUES (?,?,?) ON CONFLICT(uuid) DO UPDATE SET enabled=excluded.enabled, board_id=excluded.board_id, updated_at=CURRENT_TIMESTAMP"
        } else {
            "INSERT INTO scoreboard_prefs(uuid, enabled, board_id) VALUES (?,?,?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled), board_id=VALUES(board_id)"
        }
        try {
            db.getConnection().use { c ->
                c.prepareStatement(sql).use { ps ->
                    ps.setString(1, p.uuid.toString())
                    ps.setBoolean(2, p.enabled)
                    ps.setString(3, p.boardId)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            log.log(Level.WARNING, "upsert prefs failed", e)
        }
    }
}

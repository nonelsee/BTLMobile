package com.example.soulmole.controller

import com.example.soulmole.db.DatabaseHelper
import com.example.soulmole.model.GameSession
import com.example.soulmole.model.Player

class GameSessionDAO(private val dbHelper: DatabaseHelper) {
    fun insertGameSession(gameSession: GameSession): Long {
        return dbHelper.insertGameSession(gameSession)
    }

    fun getGameSessionsByPlayerId(playerId: Int): List<GameSession> {
        val db = dbHelper.readableDatabase
        val sessions = mutableListOf<GameSession>()

        val query = """
            SELECT gs.${DatabaseHelper.COLUMN_SESSION_ID}, 
                   p.${DatabaseHelper.COLUMN_PLAYER_ID},
                   p.${DatabaseHelper.COLUMN_USERNAME},
                   p.${DatabaseHelper.COLUMN_SCORE},
                   p.${DatabaseHelper.COLUMN_HEALTH},
                   p.${DatabaseHelper.COLUMN_DEPTH}
            FROM ${DatabaseHelper.TABLE_GAME_SESSIONS} gs
            INNER JOIN ${DatabaseHelper.TABLE_PLAYER} p 
            ON gs.${DatabaseHelper.COLUMN_PLAYER_ID} = p.${DatabaseHelper.COLUMN_PLAYER_ID}
            WHERE p.${DatabaseHelper.COLUMN_PLAYER_ID} = ?
        """

        val cursor = db.rawQuery(query, arrayOf(playerId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val sessionId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SESSION_ID))
                val player = Player(
                    playerId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PLAYER_ID)),
                    username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME)),
                    score = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCORE)),
                    health = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HEALTH)),
                    depth = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEPTH))
                )
                sessions.add(GameSession(sessionId, player))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return sessions
    }
}

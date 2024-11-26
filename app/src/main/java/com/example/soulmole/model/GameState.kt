package com.example.soulmole.model

import com.example.soulmole.controller.PlayerManager

data class GameState(
    val isInitialized: Boolean,
    val isLevelTransitioning: Boolean,
    val playerManager: PlayerManager,
    val blocks: MutableList<MutableList<Block>>,
    val items : MutableList<Item>,
    val blockSize: Float,
    val pixelSize: Float,
    val playerX: Float,
    val playerY: Float,
    val maxY: Float,
    val activeTrap: Trap? = null
)

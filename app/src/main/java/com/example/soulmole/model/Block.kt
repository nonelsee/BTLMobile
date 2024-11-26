package com.example.soulmole.model

enum class BlockType {
    DIRT,    // Block đất cơ bản
    STONE,   // Block đá cứng
    WOOD,    // Block gỗ có thể rơi
    EMPTY    // Khoảng trống
}

data class Block(
    var type: BlockType,
    var hitsRemaining: Int,
    var isFalling: Boolean = false
)
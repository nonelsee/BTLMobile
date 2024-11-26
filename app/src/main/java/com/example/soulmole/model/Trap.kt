package com.example.soulmole.model

enum class TrapType {
    LASER
}

data class Trap(
    val type: TrapType,
    val startX: Float,
    val startY: Float,
    val isHorizontal: Boolean,
    val damagePhase: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

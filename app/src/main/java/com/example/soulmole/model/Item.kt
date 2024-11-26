package com.example.soulmole.model

enum class ItemType {
    HEART, // Item tim để hồi máu
    RAINBOW_BANANA
}

data class Item(
    val type: ItemType,
    val x: Float,  // Vị trí x
    val y: Float   // Vị trí y
)
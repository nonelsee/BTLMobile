package com.example.soulmole.model

data class Player(
    val playerId: Int = 0,
    var username: String ,
    var score: Int = 0,
    var health: Int = 100,
    var depth: Int = 0
)
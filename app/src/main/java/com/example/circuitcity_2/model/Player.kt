package com.example.circuitcity_2.model

class Player(
    xTiles: Float,
    yTiles: Float
) : Entity(
    x = xTiles,
    y = yTiles,
    w = 0.8f,
    h = 0.9f
) {
    // Animation / facing state
    var isMoving: Boolean = false
    var facingRight: Boolean = true

    // Grace windows (seconds remaining)
    var coyoteTimer: Float = 0f
    var jumpBufferTimer: Float = 0f
}

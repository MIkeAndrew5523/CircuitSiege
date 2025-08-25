// app/src/main/java/com/example/circuitcity_2/model/Player.kt
package com.example.circuitcity_2.model

class Player(xTiles: Float, yTiles: Float) : Entity(
    x = xTiles,
    y = yTiles,
    w = 0.8f,
    h = 0.9f
) {
    // Grace windows (seconds remaining)
    var coyoteTimer: Float = 0f
    var jumpBufferTimer: Float = 0f
}

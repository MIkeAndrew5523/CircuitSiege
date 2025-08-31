package com.example.circuitcity_2.model

/** Minimal state container used by Physics + renderer. */
class Player(
    var x: Float,
    var y: Float
) {
    // Size in tiles (tweak to match your sprites)
    var w: Float = 0.9f
    var h: Float = 0.9f

    // Kinematics
    var vx: Float = 0f
    var vy: Float = 0f
    var onGround: Boolean = false

    // Animation hints used by renderer
    var isMoving: Boolean = false
    var facingRight: Boolean = true
}

package com.example.circuitcity_2.model

open class Entity(
    var x: Float,  // tile units
    var y: Float,  // tile units
    var w: Float,  // tile width in tiles
    var h: Float   // tile height in tiles
) {
    var vx: Float = 0f  // tiles/sec
    var vy: Float = 0f  // tiles/sec
    var grounded: Boolean = false
}

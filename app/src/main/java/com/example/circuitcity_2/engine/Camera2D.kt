package com.example.circuitcity_2.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.exp

class Camera2D(
    private val levelWidthTiles: Int,
    private val levelHeightTiles: Int
) {
    var cx = 0f
    var cy = 0f

    /** Width/height of the dead-zone (in tiles). Player can move inside it without camera motion. */
    var deadZoneX = 6f
    var deadZoneY = 3f

    /** Easing factor (1/s). Larger = snappier follow. */
    var smooth = 8f

    /**
     * Follow the player using a centered dead-zone and exponential easing.
     * @param px,py      player center in tile coordinates
     * @param viewTilesX,Y number of tiles visible on screen
     * @param dtSec      delta time
     */
    fun follow(px: Float, py: Float, viewTilesX: Float, viewTilesY: Float, dtSec: Float) {
        val halfX = viewTilesX / 2f
        val halfY = viewTilesY / 2f

        // Dead-zone rect centered on current camera
        var dzLeft   = cx - deadZoneX / 2f
        var dzRight  = cx + deadZoneX / 2f
        var dzTop    = cy - deadZoneY / 2f
        var dzBottom = cy + deadZoneY / 2f

        // Shift target to keep player inside dead-zone
        var targetX = cx
        var targetY = cy
        if (px < dzLeft)   targetX -= (dzLeft - px)
        if (px > dzRight)  targetX += (px - dzRight)
        if (py < dzTop)    targetY -= (dzTop - py)
        if (py > dzBottom) targetY += (py - dzBottom)

        // Clamp target so the view never shows outside the level
        targetX = min(max(targetX, halfX), levelWidthTiles  - halfX)
        targetY = min(max(targetY, halfY), levelHeightTiles - halfY)

        // Exponential smoothing toward the target (frame-rate independent)
        val t = (1f - exp(-smooth * dtSec)).coerceIn(0f, 1f)
        cx += (targetX - cx) * t
        cy += (targetY - cy) * t
    }

    /** Snap camera immediately (used on respawn) */
    fun snap(px: Float, py: Float, viewTilesX: Float, viewTilesY: Float) {
        val halfX = viewTilesX / 2f
        val halfY = viewTilesY / 2f
        cx = min(max(px, halfX), levelWidthTiles  - halfX)
        cy = min(max(py, halfY), levelHeightTiles - halfY)
    }
}

package com.example.circuitcity_2.model

import kotlin.math.floor
import kotlin.math.sign

class SecurityBot(
    xTiles: Float,
    yTiles: Float,
    private val speed: Float = 1.2f,      // tiles/sec
    patrolHalfRange: Float = 3f
) : Enemy(xTiles, yTiles) {

    private val leftLimit  = xTiles - patrolHalfRange
    private val rightLimit = xTiles + patrolHalfRange
    private var dir = 1f                   // +1 right, -1 left

    var facingRight = true
        private set

    private var fireCooldown = 0f

    override fun update(level: Level, player: Player, dt: Float) {
        // Patrol
        vx = dir * speed
        x += vx * dt

        // Bounce at patrol bounds
        if (x < leftLimit)  { x = leftLimit;  dir = +1f }
        if (x > rightLimit) { x = rightLimit; dir = -1f }

        // Turn at walls or ledges ahead
        val aheadX  = x + dir * 0.51f
        val feetY   = y + h - 0.05f
        val txAhead = floor(aheadX).toInt()
        val tyFeet  = floor(feetY).toInt()

        val wallAhead  = level.isSolid(txAhead, floor(y).toInt())
        val ledgeAhead = !level.isSolid(txAhead, tyFeet + 1)
        if (wallAhead || ledgeAhead) dir = -dir

        facingRight = dir > 0f

        // Very simple LOS "hit" on same row (optional)
        fireCooldown -= dt
        if (sameRow(player) && clearLine(level, player) && fireCooldown <= 0f) {
            onPlayerHit(player)
            fireCooldown = 1.0f
        }
    }

    override fun onPlayerHit(player: Player) {
        val push = if (x < player.x) +1f else -1f
        player.vx += 0.6f * push
    }

    private fun sameRow(player: Player): Boolean =
        floor(player.y + player.h * 0.5f).toInt() == floor(y + h * 0.5f).toInt()

    private fun clearLine(level: Level, player: Player): Boolean {
        val yRow = floor(y + h * 0.5f).toInt()
        val start = floor(x + w * 0.5f).toInt()
        val end   = floor(player.x + player.w * 0.5f).toInt()
        if (start == end) return true
        val step = sign((end - start).toFloat()).toInt().coerceIn(-1, 1)
        var t = start
        while (t != end) {
            if (level.isSolid(t, yRow)) return false
            t += step
        }
        return true
    }
}

package com.example.circuitcity_2.engine

import com.example.circuitcity_2.model.Level
import com.example.circuitcity_2.model.Player
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.floor

object Physics {
    const val GRAVITY = 28f
    private const val EXTRA_GRAV_UP_RELEASED = 42f
    private const val MAX_FALL_SPEED = 22f
    private const val JUMP_SPEED = 12f
    private const val RUN_MAX = 7.5f
    private const val RUN_ACCEL = 55f
    private const val RUN_DECEL = 65f
    private const val GROUND_OFFSET = 0.04f

    data class Input(
        val left: Boolean,
        val right: Boolean,
        val jumpPressed: Boolean,
        val jumpHeld: Boolean
    )

    fun step(level: Level, p: Player, input: Input, dt: Float) {
        val want = when {
            input.left && !input.right -> -RUN_MAX
            input.right && !input.left -> RUN_MAX
            else -> 0f
        }
        val accel = if (want != 0f) RUN_ACCEL else RUN_DECEL
        p.vx = approach(p.vx, want, accel * dt)

        if (input.jumpPressed && p.onGround) {
            p.vy = -JUMP_SPEED
            p.onGround = false
        }
        if (!input.jumpHeld && p.vy < 0f) {
            p.vy += EXTRA_GRAV_UP_RELEASED * dt
        }

        // apply gravity
        p.vy += GRAVITY * dt
        moveAndCollide(p, level, dt)
    }

    private fun approach(current: Float, target: Float, delta: Float): Float {
        if (current == target) return current
        val dir = sign(target - current)
        val next = current + dir * delta
        return if ((dir > 0 && next > target) || (dir < 0 && next < target)) target else next
    }

    fun moveAndCollide(p: Player, level: Level, dt: Float) {
        // clamp fall
        p.vy = max(-999f, min(MAX_FALL_SPEED, p.vy))

        // ---- Horizontal (unchanged) ----
        var newX = p.x + p.vx * dt
        if (level.collidesHorizontally(newX, p.y)) {
            // back off a tiny epsilon along vx direction
            while (level.collidesHorizontally(newX, p.y)) {
                newX -= sign(p.vx) * 0.001f
            }
            newX = floor(newX * 1000f) / 1000f
            p.vx = 0f
        }
        p.x = newX

        // ---- Vertical: probe feet & snap bottom, not center ----
        val EPS = 0.001f
        val tileSize = 1f // set to 32f if you use pixels
        val halfW = 0.5f * p.w
        val halfH = 0.5f * p.h

        var newY = p.y + p.vy * dt
        val goingDown = p.vy > 0f

        // Feet positions at next frame (two probes for stability)
        val leftFootX  = p.x - halfW * 0.7f
        val rightFootX = p.x + halfW * 0.7f
        val feetYNext  = newY + halfH

        val hitFloor =
            level.collidesVertically(leftFootX,  feetYNext) ||
                    level.collidesVertically(rightFootX, feetYNext)

        if (goingDown && hitFloor) {
            // Snap player BOTTOM to top-of-tile
            val tileRowTop = floor(feetYNext / tileSize) * tileSize
            newY = tileRowTop - halfH - EPS
            p.vy = 0f
            p.onGround = true
        } else {
            // Optionally add head/ceiling check when going up:
            if (p.vy < 0f) {
                val headYNext = newY - halfH
                val hitCeil =
                    level.collidesVertically(p.x - halfW * 0.4f, headYNext) ||
                            level.collidesVertically(p.x + halfW * 0.4f, headYNext)
                if (hitCeil) {
                    val tileRowBottom = floor(headYNext / tileSize + 1f) * tileSize
                    newY = tileRowBottom + halfH + EPS
                    p.vy = 0f
                }
            }
            // airborne unless we just snapped to floor
            if (!goingDown || !hitFloor) p.onGround = false
        }

        p.y = newY
    }

}
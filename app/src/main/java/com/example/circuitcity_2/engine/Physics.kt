package com.example.circuitcity_2.engine

import com.example.circuitcity_2.model.Level
import com.example.circuitcity_2.model.Player
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

object Physics {

    // ---- Tunables (tiles / s, tiles / s^2) ----
    private const val GRAVITY        = 28f
    private const val EXTRA_GRAV_UP_RELEASED = 42f   // extra gravity while rising if jump is NOT held
    private const val MAX_FALL_SPEED = 22f
    private const val JUMP_SPEED     = 12f

    private const val RUN_MAX     = 7.5f
    private const val RUN_ACCEL_G = 55f
    private const val RUN_ACCEL_A = 28f
    private const val RUN_DECEL_G = 65f
    private const val RUN_DECEL_A = 18f

    private const val COYOTE_WINDOW = 0.12f
    private const val JUMP_BUFFER   = 0.12f

    /** Controller snapshot */
    data class Input(
        val left: Boolean,
        val right: Boolean,
        val jumpPressed: Boolean,   // edge (single frame)
        val jumpHeld: Boolean       // level (while finger kept after swipe)
    )

    fun step(level: Level, p: Player, input: Input, dt: Float) {
        // --- grace timers ---
        p.coyoteTimer     = if (p.grounded) COYOTE_WINDOW else max(0f, p.coyoteTimer - dt)
        p.jumpBufferTimer = if (input.jumpPressed) JUMP_BUFFER else max(0f, p.jumpBufferTimer - dt)

        // --- horizontal ---
        val intent = when {
            input.left && !input.right -> -1f
            input.right && !input.left -> +1f
            else -> 0f
        }
        val accel = if (p.grounded) RUN_ACCEL_G else RUN_ACCEL_A
        val decel = if (p.grounded) RUN_DECEL_G else RUN_DECEL_A
        p.vx = if (intent != 0f) approach(p.vx, intent * RUN_MAX, accel * dt)
        else               approach(p.vx, 0f,           decel * dt)

        // --- jump using coyote + buffer ---
        val canJump = p.coyoteTimer > 0f && p.jumpBufferTimer > 0f
        if (canJump) {
            p.vy = -JUMP_SPEED
            p.grounded = false
            p.coyoteTimer = 0f
            p.jumpBufferTimer = 0f
        }

        // --- gravity (variable when rising and jump not held) ---
        val risingAndReleased = (p.vy < 0f) && !input.jumpHeld
        val g = GRAVITY + if (risingAndReleased) EXTRA_GRAV_UP_RELEASED else 0f
        p.vy = min(p.vy + g * dt, MAX_FALL_SPEED)

        // --- integrate + collide X then Y ---
        val nx = collideAxis(level, p.x + p.vx * dt, p.y, p.w, p.h, axis = 0, dir = signInt(p.vx))
        val prevY = p.y
        val ny = collideAxis(level, nx, p.y + p.vy * dt, p.w, p.h, axis = 1, dir = signInt(p.vy))

        val landed = (p.vy > 0f) && (ny == prevY)
        p.grounded = landed
        if (landed) p.vy = 0f

        p.x = nx
        p.y = ny
    }

    private fun signInt(v: Float) = when { v > 0f -> 1; v < 0f -> -1; else -> 0 }

    private fun approach(current: Float, target: Float, delta: Float): Float {
        val diff = target - current
        return if (abs(diff) <= delta) target else current + delta * sign(diff)
    }

    private fun collideAxis(
        level: Level, x: Float, y: Float, w: Float, h: Float, axis: Int, dir: Int
    ): Float {
        var px = x; var py = y
        val left   = floor(px).toInt()
        val right  = floor(px + w - 1e-4f).toInt()
        val top    = floor(py).toInt()
        val bottom = floor(py + h - 1e-4f).toInt()

        if (axis == 0 && dir != 0) {
            val edgeX = if (dir > 0) floor(px + w).toInt() else floor(px).toInt()
            val t0 = max(top, 0); val t1 = min(bottom, level.height - 1)
            for (ty in t0..t1) {
                val tx = if (dir > 0) edgeX else floor(px).toInt()
                if (level.tileAt(tx, ty).solid) return if (dir > 0) edgeX - w else tx + 1f
            }
            return px
        }

        if (axis == 1 && dir != 0) {
            val edgeY = if (dir > 0) floor(py + h).toInt() else floor(py).toInt()
            val l0 = max(left, 0); val l1 = min(right, level.width - 1)
            for (tx in l0..l1) {
                val ty = if (dir > 0) edgeY else floor(py).toInt()
                if (level.tileAt(tx, ty).solid) return if (dir > 0) edgeY - h else ty + 1f
            }
            return py
        }

        return if (axis == 0) px else py
    }
}

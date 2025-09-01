package com.example.circuitcity_2.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.example.circuitcity_2.input.TouchController
import com.example.circuitcity_2.model.Level
import com.example.circuitcity_2.model.Player
import com.example.circuitcity_2.render.SpriteRenderer
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import java.util.Locale

/**
 * Main gameplay screen for CircuitCity 2. Handles level loading, player and enemy management,
 * input, physics, rendering, HUD, and game progression.
 * @param context Android context
 * @param sm ScreenManager for navigation
 * @param levelPath Path to the level asset
 * @param onComplete Callback for when the level is completed
 */
class GameScreen(
    private val context: Context,
    private val sm: ScreenManager,
    private val levelPath: String = "levels/level_01.txt",
    private val onComplete: (() -> Screen)? = null
) : Screen {

    private lateinit var level: Level
    private lateinit var player: Player
    private val renderer = SpriteRenderer(context)
    private lateinit var camera: Camera2D

    // Keep generic so you can mix enemy types later
    private val enemies = mutableListOf<com.example.circuitcity_2.model.Enemy>()

    // Controller: left area = jump, right area = horizontal axis (-1..+1)
    private val touch = TouchController(0, 0)

    private var lastW = 0
    private var lastH = 0

    // ---------- Respawn / hazards ----------
    private var respawnX = 0f
    private var respawnY = 0f
    private var isDead = false
    private var deathTimer = 0f
    private val DEATH_FREEZE_SEC = 0.8f
    private var invulnTimer = 0f

    // ---------- Pickups / checkpoints ----------
    private var keys = 0
    private var keyFlash = 0f
    private var checkpointFlash = 0f
    private var doorFlash = 0f

    // ---------- HUD state ----------
    private var lives = 3
    private var lifeFlash = 0f
    private var timeSec = 0f
    private val MAX_LIVES = 3
    private var gameOverPending = false

    // ---------- HUD paints / overlays ----------
    private val overlay = Paint(Paint.ANTI_ALIAS_FLAG)
    private val heartsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        textAlign = Paint.Align.RIGHT
    }
    private val hudLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.LEFT
    }
    private val hudRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.RIGHT
    }
    private val hudCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW; textAlign = Paint.Align.CENTER
    }
    private val hudCenterGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN; textAlign = Paint.Align.CENTER
    }
    private val hudCenterRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED; textAlign = Paint.Align.CENTER
    }

    init { load() }

    /** Loads the level, player, camera, and spawns enemies. */
    private fun load() {
        level = Level.fromAsset(context, levelPath)
        val sx = level.playerSpawnX.toFloat()
        val sy = max(0, level.playerSpawnY - 1).toFloat()
        player = Player(sx, sy)
        respawnX = sx; respawnY = sy
        camera = Camera2D(level.width, level.height).apply {
            deadZoneX = 5.5f; deadZoneY = 2.8f; smooth = 10f
        }
        timeSec = 0f

        // --- spawn enemies from 'E' tiles (example) ---
        enemies.clear()
        for (ty in 0 until level.height) {
            for (tx in 0 until level.width) {
                if (level.isEnemySpawn(tx, ty)) {
                    enemies += com.example.circuitcity_2.model.SecurityBot(
                        xTiles = tx + 0.10f,
                        yTiles = ty - 0.10f,
                        speed = 1.2f,
                        patrolHalfRange = 3f
                    )
                    level.consumeAt(tx, ty) // clear 'E' so it renders as floor
                }
            }
        }
    }

    /**
     * Updates game state, physics, timers, enemies, camera, and checks for hazards and exit.
     * @param dtSec Time since last update in seconds
     */
    override fun update(dtSec: Float) {
        if (isDead) {
            deathTimer -= dtSec
            if (deathTimer <= 0f) {
                if (gameOverPending) sm.set(GameOverScreen(context, sm, levelPath))
                else respawn()
            }
            return
        }

        // timers
        timeSec += dtSec
        if (invulnTimer > 0f) invulnTimer -= dtSec
        if (keyFlash > 0f) keyFlash -= dtSec
        if (checkpointFlash > 0f) checkpointFlash -= dtSec
        if (doorFlash > 0f) doorFlash -= dtSec
        if (lifeFlash > 0f) lifeFlash -= dtSec

        // -------- INPUT → PHYSICS --------
        // -------- INPUT → PHYSICS --------
        val a = touch.poll() // PadState(left,right,jumpPressed,jumpHeld)
        val left  = a.left
        val right = a.right


        // keep your existing physics interface (left/right booleans + jump)
        val input = Physics.Input(left, right, a.jumpPressed, a.jumpHeld)
        Physics.step(level, player, input, dtSec)

        // Animation/facing for renderer
        player.isMoving = left || right
        if (left)  player.facingRight = false
        if (right) player.facingRight = true

        // clamp to level bounds
        player.x = min(max(0f, player.x), (level.width - player.w))
        player.y = min(max(0f, player.y), (level.height - player.h))

        // ---------- ENEMIES ----------
        enemies.forEach { it.update(level, player, dtSec) }

        if (invulnTimer <= 0f) {
            val hit = enemies.any { e ->
                val overlapX = e.x < player.x + player.w && player.x < e.x + e.w
                val overlapY = e.y < player.y + player.h && player.y < e.y + e.h
                overlapX && overlapY
            }
            if (hit) kill()
        }

        // ---------- CAMERA ----------
        val (tilesX, tilesY) = estimateViewTiles()
        camera.follow(
            player.x + player.w * 0.5f,
            player.y + player.h * 0.5f,
            tilesX, tilesY, dtSec
        )

        // ---------- EXIT ----------
        if (level.hasExit()) {
            val cx = player.x + player.w * 0.5f
            val cy = player.y + player.h * 0.5f
            val tx = floor(cx).toInt(); val ty = floor(cy).toInt()
            if (tx == level.exitX && ty == level.exitY) {
                onComplete?.let { sm.set(it()) }
                return
            }
        }

        // ---- player center tile ----
        val tx = floor(player.x + player.w * 0.5f).toInt()
        val ty = floor(player.y + player.h * 0.5f).toInt()

        // pickups / checkpoints
        if (level.isKey(tx, ty) && level.consumeAt(tx, ty) == 'K') {
            keys += 1; keyFlash = 0.7f
        }
        if (level.isCheckpoint(tx, ty) && level.consumeAt(tx, ty) == 'C') {
            respawnX = tx.toFloat()
            respawnY = max(0, ty - 1).toFloat()
            checkpointFlash = 0.9f
        }

        // doors
        if (level.isLockedDoor(tx, ty) && keys > 0 && level.unlockDoorAt(tx, ty)) {
            keys -= 1; doorFlash = 0.8f
        }

        // tile hazards
        if (invulnTimer <= 0f && isOnHazard()) {
            kill()
        }
    }

    /**
     * Renders the world, player, enemies, overlays, and HUD to the canvas.
     * @param canvas Canvas to draw on
     */
    override fun render(canvas: Canvas) {
        lastW = canvas.width
        lastH = canvas.height

        touch.layout(lastW, lastH)

        // world: tiles + player
        renderer.draw(canvas, level, player, camera.cx, camera.cy)

        // world: enemies
        renderer.drawEnemies(canvas, enemies, camera.cx, camera.cy, level)

        // UI controls
        touch.drawOverlay(canvas)

        // death overlay
        if (isDead) {
            val t = (1f - (deathTimer / DEATH_FREEZE_SEC)).coerceIn(0f, 1f)
            val alpha = (t * 180f).toInt().coerceIn(0, 180)
            overlay.color = Color.argb(alpha, 255, 0, 0)
            canvas.drawRect(0f, 0f, lastW.toFloat(), lastH.toFloat(), overlay)

            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = lastH * 0.06f
            }
            canvas.drawText("Ouch! Respawning…", lastW/2f, lastH*0.45f, p)
        }

        // invulnerability overlay
        if (!isDead && invulnTimer > 0f) {
            overlay.color = Color.argb(70, 120, 160, 255)
            canvas.drawRect(0f, 0f, lastW.toFloat(), lastH.toFloat(), overlay)
        }

        // ---------- HUD ----------
        val padX = lastH * 0.03f
        val topY = lastH * 0.08f
        val rowGap = lastH * 0.055f

        hudLeft.textSize = lastH * 0.045f
        hudRight.textSize = lastH * 0.045f
        heartsPaint.textSize = lastH * 0.065f

        // Left: Keys
        canvas.drawText("Keys: $keys", padX, topY, hudLeft)

        // Right row 1: Time
        canvas.drawText("Time: ${formatTime(timeSec)}", lastW - padX, topY, hudRight)

        // Right row 2: Hearts (♥ / ♡)
        val solidHearts  = "\u2665".repeat(lives.coerceAtLeast(0))
        val hollowHearts = "\u2661".repeat((MAX_LIVES - lives).coerceAtLeast(0))
        val hearts = (solidHearts + hollowHearts).ifEmpty { "\u2661" }
        canvas.drawText(hearts, lastW - padX, topY + rowGap, heartsPaint)

        if (keyFlash > 0f) {
            hudCenter.textSize = lastH * 0.06f
            canvas.drawText("+1 Key", lastW / 2f, topY + rowGap, hudCenter)
        }
        if (checkpointFlash > 0f) {
            hudCenterGreen.textSize = lastH * 0.06f
            canvas.drawText("Checkpoint!", lastW / 2f, topY + rowGap * 2f, hudCenterGreen)
        }
        if (doorFlash > 0f) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 165, 0)
                textAlign = Paint.Align.CENTER
                textSize = lastH * 0.06f
            }
            canvas.drawText("Door unlocked!", lastW / 2f, topY + rowGap * 3f, p)
        }
        if (lifeFlash > 0f) {
            hudCenterRed.textSize = lastH * 0.065f
            canvas.drawText("-1 life", lastW / 2f, topY + rowGap * 4f, hudCenterRed)
        }
    }

    /**
     * Handles touch input and forwards to the controller.
     * @param ev MotionEvent from the user
     * @return Always true
     */
    override fun onTouch(ev: MotionEvent): Boolean { touch.onTouch(ev); return true }

    /** Checks if the player is currently on a hazard tile. */
    private fun isOnHazard(): Boolean {
        val pts = arrayOf(
            player.x + player.w * 0.5f to player.y + player.h * 0.5f,
            player.x + player.w * 0.5f to player.y + player.h - 1e-3f,
            player.x + 1e-3f              to player.y + player.h * 0.5f,
            player.x + player.w - 1e-3f   to player.y + player.h * 0.5f,
            player.x + player.w * 0.5f    to player.y + 1e-3f
        )
        for ((fx, fy) in pts) {
            val tx = floor(fx).toInt(); val ty = floor(fy).toInt()
            if (level.isHazard(tx, ty)) return true
        }
        return false
    }

    /** Handles player death, life decrement, and game over logic. */
    private fun kill() {
        isDead = true
        deathTimer = DEATH_FREEZE_SEC
        player.vx = 0f; player.vy = 0f

        if (invulnTimer <= 0f) {
            lives = max(0, lives - 1)
            lifeFlash = 0.6f
        }
        if (lives <= 0) gameOverPending = true
    }

    /** Respawns the player at the last checkpoint and resets camera. */
    private fun respawn() {
        isDead = false
        gameOverPending = false
        player.x = respawnX; player.y = respawnY
        player.vx = 0f; player.vy = 0f
        invulnTimer = 1.2f

        val (vx, vy) = estimateViewTiles()
        camera.snap(
            player.x + player.w * 0.5f,
            player.y + player.h * 0.5f,
            vx, vy
        )
    }

    /** Estimates the number of visible tiles based on screen size. */
    private fun estimateViewTiles(): Pair<Float, Float> {
        if (lastW == 0 || lastH == 0) return 16f to 9f
        val cellH = lastH.toFloat() / level.height
        val cellW = cellH
        val tilesX = (lastW / cellW).toFloat().coerceAtLeast(1f)
        val tilesY = (lastH / cellH).toFloat().coerceAtLeast(1f)
        return tilesX to tilesY
    }

    /** Formats the elapsed time for HUD display. */
    private fun formatTime(t: Float): String {
        val clamped = max(0f, t)
        val minutes = (clamped / 60f).toInt()
        val seconds = clamped - minutes * 60
        return String.format(Locale.US, "%d:%04.1f", minutes, seconds)
    }
}

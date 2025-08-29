package com.example.circuitcity_2.render

import android.content.Context
import android.graphics.*
import androidx.annotation.DrawableRes
import com.example.circuitcity_2.R
import com.example.circuitcity_2.model.Level
import com.example.circuitcity_2.model.Player
import kotlin.math.max
import kotlin.math.min

class SpriteRenderer(private val context: Context) {

    // ----- Tilesheet config -----
    private val tilePx   = 16      // atlas tile size
    private val padPx    = 1       // gutter between tiles
    private val borderPx = 1       // outer border around the atlas (set to 0 if none)

    // ----- Player sheet (nova_spritesheet.png = 4 cols x 2 rows) -----
    private val playerCols = 4
    private val playerRows = 2
    private val playerBmp by lazy { decodeNoScale(R.drawable.nova_spritesheet) }
    private val playerFrameW by lazy { playerBmp.width / playerCols }
    private val playerFrameH by lazy { playerBmp.height / playerRows }

    private val totalPlayerFrames get() = playerCols * playerRows
    private val walkFps = 10f
    private val playerScale = 1.6f   // make the character bigger than tiles

    // Turn off filtering for crisp pixels
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = false
        isDither = false
    }

    // ----- Tiles atlas -----
    private val tilesBmp  = decodeNoScale(R.drawable.circuit_city_tilesheet)

    // Reusable rects
    private val src = Rect()
    private val dst = RectF()

    // ----- Atlas helpers (for tiles) -----
    data class Cell(val c: Int, val r: Int)
    private fun srcRectFor(cell: Cell): Rect {
        val x = borderPx + padPx + cell.c * (tilePx + padPx)
        val y = borderPx + padPx + cell.r * (tilePx + padPx)
        return Rect(x, y, x + tilePx, y + tilePx)
    }

    // Map your level symbols to atlas cells
    private val atlasMap = mapOf(
        '#' to Cell(0,0),    // solid wall
        'X' to Cell(0,1),    // hazard
        'K' to Cell(0,2),    // key
        'C' to Cell(1,2),    // checkpoint
        'D' to Cell(2,2),    // exit
        'd' to Cell(3,2),    // locked door
        '.' to Cell(2,0)     // floor
    )

    // ----- Security Bot sheet (4x2) -----
    private val botCols = 4
    private val botRows = 2
    private val botBmp by lazy { decodeNoScale(R.drawable.security_bot) }
    private val botFrameW by lazy { botBmp.width / botCols }
    private val botFrameH by lazy { botBmp.height / botRows }
    private val botFps = 8f
    private val botScale = 1.4f

    // If your bot sheet ever has gutters or a border, set these (>0).
    private val botPadPx = 0           // gutter between bot frames in source
    private val botBorderPx = 0        // outer border around bot sheet

    // Visual nudge downwards if feet aren't exactly at the bottom of the frame (in *source* pixels).
    // Positive -> draws LOWER on screen. Try 2..4 if the bot looks a bit too high.
    private val botYOffsetSrcPx = 20

    // ----- Public draw -----
    fun draw(canvas: Canvas, level: Level, player: Player, camCx: Float, camCy: Float) {
        // Clear
        canvas.drawColor(Color.BLACK)

        // Fit level height to canvas; keep square pixels
        val cellH = canvas.height.toFloat() / level.height
        val cellW = cellH

        // Visible window
        val tilesX = (canvas.width / cellW).toInt().coerceAtLeast(1)
        val tilesY = (canvas.height / cellH).toInt().coerceAtLeast(1)
        val startTx = max(0, (camCx - tilesX / 2f).toInt())
        val startTy = max(0, (camCy - tilesY / 2f).toInt())
        val endTx = min(level.width - 1, startTx + tilesX + 1)
        val endTy = min(level.height - 1, startTy + tilesY + 1)

        val ox = (camCx - tilesX / 2f - startTx) * cellW
        val oy = (camCy - tilesY / 2f - startTy) * cellH

        // ---- draw tiles ----
        for (ty in startTy..endTy) {
            for (tx in startTx..endTx) {
                val ch = level.tileAt(tx, ty).symbol
                val cell = atlasMap[ch] ?: continue

                // src from atlas (accounts for gutter + border)
                src.set(srcRectFor(cell))

                // dst on screen
                val dx = (tx - startTx) * cellW - ox
                val dy = (ty - startTy) * cellH - oy
                dst.set(dx, dy, dx + cellW, dy + cellH)

                canvas.drawBitmap(tilesBmp, src, dst, paint)
            }
        }

        // ---- draw player (idle vs walking; flip by canvas scale) ----
        val frameIndex =
            if (player.isMoving) {
                val nowMs = android.os.SystemClock.uptimeMillis()
                ((nowMs / (1000f / walkFps)).toInt()) % totalPlayerFrames
            } else 0

        val pCol = frameIndex % playerCols
        val pRow = frameIndex / playerCols

        // Source rect from nova_spritesheet (no gutters)
        val psx = pCol * playerFrameW
        val psy = pRow * playerFrameH
        src.set(psx, psy, psx + playerFrameW, psy + playerFrameH)

        // Destination: scale bigger than a tile, feet bottom-aligned, centered on tile
        val baseX = (player.x - startTx) * cellW - ox
        val baseY = (player.y - startTy) * cellH - oy
        val scaledW = cellW * playerScale
        val scaledH = cellH * playerScale
        val dstLeft = baseX + (cellW - scaledW) / 2f
        val dstTop  = baseY + (cellH - scaledH)
        dst.set(dstLeft, dstTop, dstLeft + scaledW, dstTop + scaledH)

        // Flip by scaling canvas around sprite center
        canvas.save()
        if (!player.facingRight) {
            val cx = dst.centerX()
            val cy = dst.centerY()
            canvas.scale(-1f, 1f, cx, cy)
        }
        canvas.drawBitmap(playerBmp, src, dst, paint)
        canvas.restore()
    }

    fun drawEnemies(
        canvas: Canvas,
        enemies: List<com.example.circuitcity_2.model.Enemy>,
        camCx: Float,
        camCy: Float,
        level: com.example.circuitcity_2.model.Level
    ) {
        val cellH = canvas.height.toFloat() / level.height
        val cellW = cellH

        val tilesX = (canvas.width / cellW).toInt().coerceAtLeast(1)
        val tilesY = (canvas.height / cellH).toInt().coerceAtLeast(1)
        val startTx = kotlin.math.max(0, (camCx - tilesX / 2f).toInt())
        val startTy = kotlin.math.max(0, (camCy - tilesY / 2f).toInt())
        val ox = (camCx - tilesX / 2f - startTx) * cellW
        val oy = (camCy - tilesY / 2f - startTy) * cellH

        val nowMs = android.os.SystemClock.uptimeMillis()
        val totalFrames = botCols * botRows

        enemies.forEach { e ->
            val moving = kotlin.math.abs(e.vx) > 0.01f
            val frameIndex = if (moving)
                ((nowMs / (1000f / botFps)).toInt()) % totalFrames
            else 0

            val c = frameIndex % botCols
            val r = frameIndex / botCols

            // --- SOURCE RECT (supports optional gutter/border) ---
            val sx = botBorderPx + c * (botFrameW + botPadPx)
            val sy = botBorderPx + r * (botFrameH + botPadPx)
            src.set(sx, sy, sx + botFrameW, sy + botFrameH)

            // --- DEST RECT (bottom-aligned, scaled, with visual Y nudge) ---
            val baseX = (e.x - startTx) * cellW - ox
            val baseY = (e.y - startTy) * cellH - oy
            val dw = cellW * botScale
            val dh = cellH * botScale

            // Convert the source-pixel nudge into screen pixels at the current scale
            val scaleFromSource = (cellH / botFrameH) * botScale
            val yNudge = botYOffsetSrcPx * scaleFromSource   // positive -> lower on screen

            val left = baseX + (cellW - dw) / 2f
            val top  = baseY + (cellH - dh) + yNudge
            dst.set(left, top, left + dw, top + dh)

            // Facing: use SecurityBot.facingRight when available, else infer from vx
            val facingRight = when (e) {
                is com.example.circuitcity_2.model.SecurityBot -> e.facingRight
                else -> e.vx >= 0f
            }

            canvas.save()
            if (!facingRight) {
                val cx = dst.centerX()
                val cy = dst.centerY()
                canvas.scale(-1f, 1f, cx, cy)
            }
            canvas.drawBitmap(botBmp, src, dst, paint)
            canvas.restore()
        }
    }

    private fun decodeNoScale(@DrawableRes resId: Int): Bitmap {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(context.resources, resId, opts)
    }
}

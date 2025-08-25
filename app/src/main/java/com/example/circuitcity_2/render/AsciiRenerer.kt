package com.example.circuitcity_2.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.circuitcity_2.model.Level
import com.example.circuitcity_2.model.Player

class AsciiRenderer(private val context: Context) {

    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE;  textAlign = Paint.Align.LEFT; typeface = Typeface.MONOSPACE }
    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN;  textAlign = Paint.Align.LEFT; typeface = Typeface.MONOSPACE }
    private val hazardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED;   textAlign = Paint.Align.LEFT; typeface = Typeface.MONOSPACE }
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.YELLOW;   textAlign = Paint.Align.LEFT; typeface = Typeface.MONOSPACE }
    private val checkpointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GREEN; textAlign = Paint.Align.LEFT; typeface = Typeface.MONOSPACE }
    private val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255,165,0); textAlign = Paint.Align.LEFT; typeface = Typeface.MONOSPACE } // orange 'd'
    private val exitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.MAGENTA; textAlign = Paint.Align.LEFT; typeface = Typeface.MONOSPACE }         // magenta 'D'

    private var cellW = 0f
    private var cellH = 0f
    private var baselineAdjust = 0f

    fun draw(canvas: Canvas, level: Level, player: Player, camCx: Float, camCy: Float) {
        cellH = canvas.height.toFloat() / level.height
        listOf(white, playerPaint, hazardPaint, keyPaint, checkpointPaint, doorPaint, exitPaint).forEach { it.textSize = cellH * 0.95f }
        cellW = white.measureText("M")
        baselineAdjust = -white.fontMetrics.top
        canvas.drawColor(Color.BLACK)

        val tilesX = (canvas.width / cellW).toInt().coerceAtLeast(1)
        val tilesY = (canvas.height / cellH).toInt().coerceAtLeast(1)
        val startTx = (camCx - tilesX / 2f).toInt().coerceAtLeast(0)
        val startTy = (camCy - tilesY / 2f).toInt().coerceAtLeast(0)
        val endTx = (startTx + tilesX + 1).coerceAtMost(level.width - 1)
        val endTy = (startTy + tilesY + 1).coerceAtMost(level.height - 1)
        val ox = (camCx - tilesX / 2f - startTx) * cellW
        val oy = (camCy - tilesY / 2f - startTy) * cellH

        for (ty in startTy..endTy) {
            for (tx in startTx..endTx) {
                val ch = level.tileAt(tx, ty).symbol
                if (ch != '.') {
                    val px = (tx - startTx) * cellW - ox
                    val py = (ty - startTy) * cellH - oy + baselineAdjust
                    val p = when (ch) {
                        'X' -> hazardPaint
                        'K' -> keyPaint
                        'C' -> checkpointPaint
                        'd' -> doorPaint
                        'D' -> exitPaint
                        else -> white
                    }
                    canvas.drawText(ch.toString(), px, py, p)
                }
            }
        }

        val px = (player.x - startTx) * cellW - ox
        val py = (player.y - startTy) * cellH - oy + baselineAdjust
        canvas.drawText("@", px, py, playerPaint)
    }
}

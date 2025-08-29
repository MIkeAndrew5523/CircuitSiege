package com.example.circuitcity_2.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent

class GameOverScreen(
    private val context: Context,
    private val sm: ScreenManager,
    private val levelPath: String = "levels/level_01.txt"
) : Screen {

    private var w = 0
    private var h = 0
    private val pCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textAlign = Paint.Align.CENTER
    }
    private val overlay = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun update(dtSec: Float) { /* static */ }

    override fun render(canvas: Canvas) {
        w = canvas.width; h = canvas.height
        // dark background
        canvas.drawColor(Color.BLACK)
        overlay.color = Color.argb(140, 0, 0, 0)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), overlay)

        pCenter.textSize = h * 0.10f
        canvas.drawText("GAME OVER", w/2f, h*0.42f, pCenter)

        pSub.textSize = h * 0.04f
        canvas.drawText("Tap to restart", w/2f, h*0.52f, pSub)
        canvas.drawText("Back: system button", w/2f, h*0.58f, pSub)
    }

    override fun onTouch(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_UP) {
            // Restart the same level with fresh state
            sm.set(GameScreen(context, sm, levelPath))
            return true
        }
        return false
    }
}

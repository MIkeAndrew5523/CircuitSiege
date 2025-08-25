package com.example.circuitcity_2.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.example.circuitcity_2.engine.Screen
import kotlin.math.abs
import kotlin.math.sin

class DebugBlankScreen : Screen {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 48f
    }
    private var t = 0f
    private var frames = 0
    private var fps = 0

    override fun update(dtSec: Float) {
        t += dtSec
        frames++
        if (t >= 1f) { fps = frames; frames = 0; t -= 1f }
    }

    override fun render(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        val shade = (32 + (abs(sin(t.toDouble())) * 64).toInt())
        canvas.drawColor(Color.rgb(shade, shade, shade))
        canvas.drawText("Game loop running", w/2f, h*0.45f, paint)
        canvas.drawText("FPS: $fps", w/2f, h*0.55f, paint)
    }

    override fun onTouch(ev: MotionEvent): Boolean = false
}

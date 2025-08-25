package com.example.circuitcity_2.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.example.circuitcity_2.engine.Screen
import com.example.circuitcity_2.engine.ScreenManager

class BackstoryScreen(
    private val context: Context,
    private val sm: ScreenManager,
    private val nextFactory: () -> Screen
) : Screen {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val lines: List<String> = context.assets.open("backstory.txt")
        .bufferedReader().use { it.readText().split("\n") }

    private var timeSinceShown = 0f
    private var ready = false
    private var activePointerId = -1
    private var downSeen = false

    override fun update(dtSec: Float) {
        if (!ready) {
            timeSinceShown += dtSec
            if (timeSinceShown >= 0.30f) ready = true  // slightly longer guard
        }
    }

    override fun render(canvas: Canvas) {
        val w = canvas.width.toFloat(); val h = canvas.height.toFloat()
        canvas.drawColor(Color.rgb(10,10,10))

        val title = Paint(paint).apply { color = Color.CYAN; textAlign = Paint.Align.CENTER; textSize = h*0.06f }
        paint.textSize = h*0.035f; paint.textAlign = Paint.Align.LEFT

        canvas.drawText("Backstory", w/2f, h*0.12f, title)

        val margin = w*0.08f; val lh = paint.textSize*1.4f
        var y = h*0.20f
        for (line in lines) { canvas.drawText(line, margin, y, paint); y += lh }

        val hint = Paint(paint).apply { color = Color.LTGRAY; textAlign = Paint.Align.CENTER }
        canvas.drawText("Tap to Start Level 1", w/2f, h*0.92f, hint)
    }

    override fun onTouch(ev: MotionEvent): Boolean {
        if (!ready) return true
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (activePointerId == -1) {
                    activePointerId = ev.getPointerId(ev.actionIndex)
                    downSeen = true
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val id = ev.getPointerId(ev.actionIndex)
                if (id == activePointerId && downSeen) {
                    sm.set(nextFactory())
                }
                activePointerId = -1
                downSeen = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointerId = -1
                downSeen = false
                return true
            }
        }
        return true
    }
}

package com.example.circuitcity_2.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.example.circuitcity_2.engine.Screen
import com.example.circuitcity_2.engine.ScreenManager

class VictoryScreen(
    private val sm: ScreenManager,
    private val makeNext: (() -> Screen)? = null,
    private val makeTitle: (() -> Screen)? = null,
    private val minShowSec: Float = 2.0f,          // <-- require 2s before taps work
    private val autoAdvanceSec: Float? = null      // <-- set e.g. 8.0f to auto-reset after 8s
) : Screen {

    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GREEN; textAlign = Paint.Align.CENTER }
    private val hint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textAlign = Paint.Align.CENTER }

    private var elapsed = 0f
    private var activePointerId = -1
    private var downSeen = false

    override fun update(dtSec: Float) {
        elapsed += dtSec
        autoAdvanceSec?.let { if (elapsed >= it) advance() }
    }

    override fun render(canvas: Canvas) {
        val w = canvas.width.toFloat(); val h = canvas.height.toFloat()
        canvas.drawColor(Color.BLACK)
        title.textSize = h * 0.09f
        hint .textSize = h * 0.04f

        canvas.drawText("Level Complete!", w/2f, h*0.42f, title)

        val message = if (elapsed >= minShowSec)
            "Tap to continue"
        else
            "…"

        canvas.drawText(message, w/2f, h*0.58f, hint)
    }

    override fun onTouch(ev: MotionEvent): Boolean {
        if (elapsed < minShowSec) return true   // ignore carry-over touch from gameplay

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
                if (id == activePointerId && downSeen) advance()
                activePointerId = -1
                downSeen = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointerId = -1; downSeen = false; return true
            }
        }
        return true
    }

    private fun advance() {
        (makeNext ?: makeTitle)?.let { sm.set(it()) }
    }
}

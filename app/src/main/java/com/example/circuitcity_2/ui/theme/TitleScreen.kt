package com.example.circuitcity_2.ui

import android.graphics.*
import android.view.MotionEvent
import com.example.circuitcity_2.engine.Screen
import com.example.circuitcity_2.engine.ScreenManager

class TitleScreen(
    private val sm: ScreenManager,
    private val nextFactory: () -> Screen
) : Screen {

    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
    private val hint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textAlign = Paint.Align.CENTER }

    // Debounce so a carry-over event can't trigger immediately
    private var timeSinceShown = 0f
    private var ready = false
    private var activePointerId = -1
    private var downSeen = false

    override fun update(dtSec: Float) {
        if (!ready) {
            timeSinceShown += dtSec
            if (timeSinceShown >= 0.15f) ready = true  // ~150 ms debounce
        }
    }

    override fun render(canvas: Canvas) {
        val w = canvas.width.toFloat(); val h = canvas.height.toFloat()
        canvas.drawColor(Color.rgb(16,16,24))
        title.textSize = h * 0.09f
        hint.textSize  = h * 0.035f
        canvas.drawText("CIRCUIT CITY", w/2f, h*0.42f, title)
        canvas.drawText("Tap to Continue", w/2f, h*0.58f, hint)
    }

    override fun onTouch(ev: MotionEvent): Boolean {
        if (!ready) return true  // ignore early events
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

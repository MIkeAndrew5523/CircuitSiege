package com.example.circuitcity_2.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.example.circuitcity_2.engine.Screen
import com.example.circuitcity_2.engine.ScreenManager

/**
 * Screen displayed upon level completion, showing a victory message and handling user input to proceed.
 * Supports auto-advance and delayed tap activation.
 * @param sm ScreenManager for navigation
 * @param makeNext Factory for next screen
 * @param makeTitle Factory for title screen
 * @param minShowSec Minimum seconds before tap is accepted
 * @param autoAdvanceSec Seconds before auto-advance (optional)
 */
class VictoryScreen(
    private val sm: ScreenManager,
    private val makeNext: (() -> Screen)? = null,
    private val makeTitle: (() -> Screen)? = null,
    private val minShowSec: Float = 2.0f,
    private val autoAdvanceSec: Float? = null
) : Screen {
    /** Paint for title text. */
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GREEN; textAlign = Paint.Align.CENTER }
    /** Paint for hint text. */
    private val hint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textAlign = Paint.Align.CENTER }

    private var elapsed = 0f
    private var activePointerId = -1
    private var downSeen = false

    /**
     * Updates the timer and checks for auto-advance.
     * @param dtSec Time since last update in seconds
     */
    override fun update(dtSec: Float) {
        elapsed += dtSec
        autoAdvanceSec?.let { if (elapsed >= it) advance() }
    }

    /**
     * Renders the victory message and hint to the canvas.
     * @param canvas Canvas to draw on
     */
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

    /**
     * Handles touch input to proceed to the next screen after the guard period.
     * @param ev MotionEvent from the user
     * @return true if the event was handled
     */
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

    /**
     * Advances to the next screen or title screen.
     */
    private fun advance() {
        (makeNext ?: makeTitle)?.let { sm.set(it()) }
    }
}

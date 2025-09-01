package com.example.circuitcity_2.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.example.circuitcity_2.engine.Screen
import com.example.circuitcity_2.engine.ScreenManager

/**
 * Screen displaying the game's backstory before gameplay begins.
 * Handles rendering of story text and user input to proceed to the next screen.
 * @param context Android context for asset access
 * @param sm ScreenManager for navigation
 * @param nextFactory Factory function to create the next Screen
 */
class BackstoryScreen(
    private val context: Context,
    private val sm: ScreenManager,
    private val nextFactory: () -> Screen
) : Screen {

    /** Paint object for drawing text. */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    /** List of story lines loaded from assets. */
    private val lines: List<String> = context.assets.open("backstory.txt")
        .bufferedReader().use { it.readText().split("\n") }

    private var timeSinceShown = 0f
    private var ready = false
    private var activePointerId = -1
    private var downSeen = false

    /**
     * Updates the screen timer and enables input after a short delay.
     * @param dtSec Time since last update in seconds
     */
    override fun update(dtSec: Float) {
        if (!ready) {
            timeSinceShown += dtSec
            if (timeSinceShown >= 0.30f) ready = true  // slightly longer guard
        }
    }

    /**
     * Renders the backstory text and hint to the canvas.
     * @param canvas Canvas to draw on
     */
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

    /**
     * Handles touch input to proceed to the next screen after the guard period.
     * @param ev MotionEvent from the user
     * @return true if the event was handled
     */
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

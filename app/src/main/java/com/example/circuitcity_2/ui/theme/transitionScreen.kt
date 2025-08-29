package com.example.circuitcity_2.ui.theme

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.example.circuitcity_2.engine.ScreenManager
import com.example.circuitcity_2.engine.Screen

/**
 * Simple interstitial/backstory screen shown between levels.
 * Tap (or wait a short time) to continue to the next screen.
 */
class TransitionScreen(
    private val context: Context,
    private val sm: ScreenManager,
    private val title: String = "Mission Update",
    private val bodyLines: List<String> = emptyList(),
    private val nextScreen: () -> Screen,        // factory for the next screen
    private val minShowSeconds: Float = 0.8f,    // unskippable for this long
    private val fadeSeconds: Float = 0.5f        // fade-in and fade-out time
) : Screen {

    private var elapsed = 0f
    private var finishing = false
    private var finishT = 0f

    private val bgPaint = Paint()
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(240, 160, 40)
        textAlign = Paint.Align.CENTER
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textAlign = Paint.Align.CENTER
    }

    private var w = 0
    private var h = 0

    override fun update(dtSec: Float) {
        if (!finishing) {
            elapsed += dtSec
        } else {
            finishT += dtSec
            if (finishT >= fadeSeconds) {
                sm.set(nextScreen())
            }
        }
    }

    override fun render(canvas: Canvas) {
        w = canvas.width; h = canvas.height

        // --------- fade in/out over a black BG ----------
        val tIn  = (elapsed / fadeSeconds).coerceIn(0f, 1f)
        val tOut = if (finishing) (finishT / fadeSeconds).coerceIn(0f, 1f) else 0f
        val alphaIn  = (tIn * 255).toInt()
        val alphaOut = (tOut * 255).toInt()

        // black background
        canvas.drawColor(Color.BLACK)

        // vignette overlay (subtle)
        val centerX = w / 2f
        val centerY = h / 2f
        val radial = RadialGradient(
            centerX, centerY, h * 0.75f,
            intArrayOf(Color.TRANSPARENT, Color.argb(180, 0, 0, 0)),
            floatArrayOf(0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = radial
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        bgPaint.shader = null

        // scale text sizes
        titlePaint.textSize = h * 0.07f
        bodyPaint.textSize  = h * 0.035f
        hintPaint.textSize  = h * 0.03f

        // set composite alpha (fade in then out)
        val a = (alphaIn * (255 - alphaOut) / 255).coerceIn(0, 255)
        titlePaint.alpha = a; bodyPaint.alpha = a; hintPaint.alpha = a

        // draw title
        val pad = h * 0.08f
        canvas.drawText(title, w/2f, pad + titlePaint.textSize, titlePaint)

        // draw body lines
        var y = pad + titlePaint.textSize + h * 0.06f
        val lineGap = h * 0.05f
        for (line in bodyLines) {
            canvas.drawText(line, w/2f, y, bodyPaint)
            y += lineGap
        }

        // draw hint (blinks after minShowSeconds)
        if (elapsed >= minShowSeconds) {
            val blink = (System.currentTimeMillis() / 400L) % 2L == 0L
            hintPaint.alpha = if (blink) a else a / 3
            canvas.drawText("Tap to continue", w/2f, h - pad, hintPaint)
        }
    }

    override fun onTouch(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN && !finishing && elapsed >= minShowSeconds) {
            finishing = true
            finishT = 0f
        }
        return true
    }
}

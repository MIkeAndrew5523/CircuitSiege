package com.example.circuitcity_2.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameEngine @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), Runnable, SurfaceHolder.Callback {

    private val screenManager = ScreenManager()
    private var running = false
    private var loopThread: Thread? = null
    private var lastNs = 0L

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
    }

    fun setInitialScreen(screen: Screen) = screenManager.set(screen)
    fun screens(): ScreenManager = screenManager

    override fun run() {
        lastNs = System.nanoTime()
        while (running) {
            if (!holder.surface.isValid) continue

            val now = System.nanoTime()
            val dtSec = ((now - lastNs) / 1_000_000_000.0).toFloat()
            lastNs = now

            screenManager.get()?.update(dtSec)

            val canvas: Canvas = holder.lockCanvas()
            try {
                canvas.drawColor(Color.BLACK)
                screenManager.get()?.render(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Forward to current screen; always return true so we keep receiving the gesture
        screenManager.get()?.onTouch(event)
        return true
    }

    // Surface lifecycle
    override fun surfaceCreated(holder: SurfaceHolder) { start() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { stop() }

    fun start() {
        if (!running) {
            running = true
            loopThread = Thread(this, "GameLoop").also { it.start() }
        }
    }

    fun stop() {
        running = false
        loopThread?.join(100)
        loopThread = null
    }
}

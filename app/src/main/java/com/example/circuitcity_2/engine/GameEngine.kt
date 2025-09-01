package com.example.circuitcity_2.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Main game engine class responsible for managing the game loop, rendering, and input handling.
 * Extends SurfaceView and implements Runnable and SurfaceHolder.Callback for drawing and lifecycle management.
 * @constructor Creates a GameEngine instance with the given context and optional attribute set.
 * @param context Android context
 * @param attrs Optional attribute set
 */
class GameEngine @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SurfaceView(context, attrs), Runnable, SurfaceHolder.Callback {

    /** Manages the current screen and transitions. */
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

    /**
     * Sets the initial screen for the game.
     * @param screen The starting Screen
     */
    fun setInitialScreen(screen: Screen) = screenManager.set(screen)

    /**
     * Returns the ScreenManager instance.
     */
    fun screens(): ScreenManager = screenManager

    /**
     * Main game loop. Updates and renders the current screen.
     */
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

    /**
     * Forwards touch events to the current screen.
     * @param event MotionEvent from the user
     * @return Always true to keep receiving gestures
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Forward to current screen; always return true so we keep receiving the gesture
        screenManager.get()?.onTouch(event)
        return true
    }

    // Surface lifecycle
    /** Called when the surface is created. Starts the game loop. */
    override fun surfaceCreated(holder: SurfaceHolder) { start() }
    /** Called when the surface changes. (No-op) */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    /** Called when the surface is destroyed. Stops the game loop. */
    override fun surfaceDestroyed(holder: SurfaceHolder) { stop() }

    /**
     * Starts the game loop thread.
     */
    fun start() {
        if (!running) {
            running = true
            loopThread = Thread(this, "GameLoop").also { it.start() }
        }
    }

    /**
     * Stops the game loop thread.
     */
    fun stop() {
        running = false
        loopThread?.join(100)
        loopThread = null
    }
}

package com.example.circuitcity_2.engine

import android.graphics.Canvas
import android.view.MotionEvent

/**
 * Interface for game screens in CircuitCity 2. Defines lifecycle and input methods
 * for updating, rendering, and handling user interaction.
 */
interface Screen {
    /**
     * Updates the screen state.
     * @param dtSec Time since last update in seconds
     */
    fun update(dtSec: Float)

    /**
     * Renders the screen to the provided canvas.
     * @param canvas Canvas to draw on
     */
    fun render(canvas: Canvas)

    /**
     * Handles touch input events.
     * @param ev MotionEvent from the user
     * @return true if the event was handled
     */
    fun onTouch(ev: MotionEvent): Boolean = false

    /** Called when the screen resumes (optional). */
    fun onResume() {}
    /** Called when the screen is paused (optional). */
    fun onPause() {}
    /** Called when the screen is disposed (optional). */
    fun dispose() {}
}

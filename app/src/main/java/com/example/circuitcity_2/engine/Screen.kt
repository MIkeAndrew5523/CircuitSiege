package com.example.circuitcity_2.engine

import android.graphics.Canvas
import android.view.MotionEvent

interface Screen {
    fun update(dtSec: Float)
    fun render(canvas: Canvas)
    fun onTouch(ev: MotionEvent): Boolean = false
    fun onResume() {}
    fun onPause() {}
    fun dispose() {}
}

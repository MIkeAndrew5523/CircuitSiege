package com.example.circuitcity_2.input

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class PadState(
    val left: Boolean,
    val right: Boolean,
    val jumpPressed: Boolean,
    val jumpHeld: Boolean
)

class TouchController(
    private var screenW: Int = 0,
    private var screenH: Int = 0
) {
    private var leftPointerId: Int? = null
    private var rightPointerId: Int? = null
    private var axisX = 0f
    private var jumpHeldInternal = false
    private var jumpPressedOnce = false

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val arrows = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    // Layout vars
    private var padRadius = 0f
    private var leftCX = 0f
    private var rightCX = 0f
    private var cy = 0f
    // Put this inside TouchController class
    fun layout(w: Int, h: Int) = onResize(w, h)

    fun onResize(w: Int, h: Int) {
        screenW = w; screenH = h
        padRadius = min(screenW, screenH) * 0.12f // smaller equal pads
        cy = screenH - padRadius - (screenH * 0.04f) // bottom padding
        leftCX = padRadius + (screenW * 0.05f)
        rightCX = screenW - padRadius - (screenW * 0.05f)

        label.textSize = screenH * 0.03f
        arrows.textSize = screenH * 0.05f
    }

    fun onTouch(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = ev.actionIndex
                val id = ev.getPointerId(i)
                val x = ev.getX(i); val y = ev.getY(i)
                if (dist2(x, y, leftCX, cy) <= padRadius * padRadius && leftPointerId == null) {
                    leftPointerId = id; jumpHeldInternal = true; jumpPressedOnce = true
                } else if (dist2(x, y, rightCX, cy) <= padRadius * padRadius && rightPointerId == null) {
                    rightPointerId = id; updateRightAxis(x)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                rightPointerId?.let { rid ->
                    val i = ev.findPointerIndex(rid)
                    if (i >= 0) updateRightAxis(ev.getX(i))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val i = ev.actionIndex
                val id = ev.getPointerId(i)
                if (id == leftPointerId) { leftPointerId = null; jumpHeldInternal = false }
                if (id == rightPointerId) { rightPointerId = null; axisX = 0f }
            }
        }
        return true
    }

    private fun dist2(x: Float, y: Float, cx: Float, cy: Float): Float {
        val dx = x - cx; val dy = y - cy
        return dx * dx + dy * dy
    }

    private fun updateRightAxis(x: Float) {
        val raw = (x - rightCX) / padRadius
        axisX = min(1f, max(-1f, raw))
        if (abs(axisX) < 0.12f) axisX = 0f
    }

    fun poll(): PadState {
        val left = axisX < -0.15f
        val right = axisX > 0.15f
        val out = PadState(left, right, jumpPressedOnce, jumpHeldInternal)
        jumpPressedOnce = false
        return out
    }

    fun drawOverlay(canvas: Canvas) {
        if (screenW <= 0 || screenH <= 0) return

        // --- Left circle (Jump) ---
        fill.color = if (jumpHeldInternal) Color.argb(130, 255, 255, 255) else Color.argb(70, 255, 255, 255)
        ring.color = Color.argb(160, 255, 255, 255)
        canvas.drawCircle(leftCX, cy, padRadius, fill)
        canvas.drawCircle(leftCX, cy, padRadius, ring)
        label.color = Color.WHITE
        canvas.drawText("JUMP", leftCX, cy + label.textSize * 0.35f, label)

        // --- Right circle (Left/Right) ---
        fill.color = Color.argb(70, 255, 255, 255)
        ring.color = Color.argb(160, 255, 255, 255)
        canvas.drawCircle(rightCX, cy, padRadius, fill)
        canvas.drawCircle(rightCX, cy, padRadius, ring)

        arrows.color = if (axisX < -0.15f) Color.YELLOW else Color.WHITE
        canvas.drawText("←", rightCX - padRadius * 0.55f, cy + arrows.textSize * 0.35f, arrows)
        arrows.color = if (axisX > 0.15f) Color.YELLOW else Color.WHITE
        canvas.drawText("→", rightCX + padRadius * 0.55f, cy + arrows.textSize * 0.35f, arrows)
    }
}

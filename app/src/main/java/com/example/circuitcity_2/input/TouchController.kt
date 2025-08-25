package com.example.circuitcity_2.input

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.abs

data class Actions(
    val left: Boolean,
    val right: Boolean,
    val jumpPressed: Boolean,
    val jumpHeld: Boolean
)

/**
 * Left = bottom-left, Right = bottom-right.
 * Swipe up inside a side to jump in that direction.
 * Jump "held" stays true while the swipe-origin finger remains down
 * (with a small minimum hold window so short swipes still get some height).
 */
class TouchController {

    private val leftRect = RectF()
    private val rightRect = RectF()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255) }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f }

    private var leftHeld = false
    private var rightHeld = false
    private var jumpPress = false

    // Which pointer triggered the current jump (if any)
    private var jumpPtrId: Int? = null
    private var jumpHeldFrames = 0       // minimum hold window (~0.16 s @ 60 fps)

    private enum class Region { NONE, LEFT, RIGHT }
    private data class Ptr(var id: Int, var startX: Float, var startY: Float, var region: Region, var swipeDone: Boolean = false)
    private val ptrs = HashMap<Int, Ptr>()

    private var swipeMinPx = 60f
    private val SWIPE_MAX_OFF_AXIS_RATIO = 2.5f

    fun layout(screenW: Int, screenH: Int) {
        val pad = screenH * 0.04f
        val btn = screenH * 0.18f
        leftRect.set(pad, screenH - btn - pad, pad + btn, screenH - pad)
        rightRect.set(screenW - btn - pad, screenH - btn - pad, screenW - pad, screenH - pad)
        swipeMinPx = screenH * 0.10f
    }

    fun onTouch(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = ev.actionIndex
                val id = ev.getPointerId(i)
                val x = ev.getX(i); val y = ev.getY(i)
                val region = when {
                    leftRect.contains(x, y)  -> Region.LEFT
                    rightRect.contains(x, y) -> Region.RIGHT
                    else -> Region.NONE
                }
                ptrs[id] = Ptr(id, x, y, region)
                recomputeHeld(ev)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                recomputeHeld(ev)
                for (p in ptrs.values) {
                    val i = ev.findPointerIndex(p.id)
                    if (i < 0) continue
                    val curX = ev.getX(i); val curY = ev.getY(i)
                    val dy = curY - p.startY
                    val dx = curX - p.startX
                    val mostlyUp = abs(dy) > abs(dx) / SWIPE_MAX_OFF_AXIS_RATIO

                    if (!p.swipeDone && p.region != Region.NONE && dy <= -swipeMinPx && mostlyUp) {
                        jumpPress = true
                        jumpPtrId = p.id
                        jumpHeldFrames = maxOf(jumpHeldFrames, 10) // ~0.16s baseline hold
                        p.swipeDone = true
                        // optional: lateral impulses handled in your physics via left/right input
                    }
                }
                // extend hold while swipe-origin pointer remains down
                jumpPtrId?.let { id ->
                    if (ptrs.containsKey(id)) jumpHeldFrames = maxOf(jumpHeldFrames, 1)
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val id = ev.getPointerId(ev.actionIndex)
                val wasJumpPtr = (id == jumpPtrId)
                ptrs.remove(id)
                recomputeHeld(ev, ignoreActionIndex = true)
                if (wasJumpPtr) {
                    jumpPtrId = null
                    // let jumpHeld decay naturally (no immediate zero) for a softer cut
                }
                return true
            }
        }
        return true
    }

    private fun recomputeHeld(ev: MotionEvent, ignoreActionIndex: Boolean = false) {
        var l = false; var r = false
        for (i in 0 until ev.pointerCount) {
            if (ignoreActionIndex && i == ev.actionIndex && ev.actionMasked != MotionEvent.ACTION_MOVE) continue
            val x = ev.getX(i); val y = ev.getY(i)
            if (leftRect.contains(x, y))  l = true
            if (rightRect.contains(x, y)) r = true
        }
        leftHeld = l
        rightHeld = r
    }

    fun poll(): Actions {
        val jumpHeldNow = jumpHeldFrames > 0 || (jumpPtrId?.let { ptrs.containsKey(it) } ?: false)
        if (jumpHeldFrames > 0) jumpHeldFrames--
        val a = Actions(leftHeld, rightHeld, jumpPress, jumpHeldNow)
        jumpPress = false
        return a
    }

    fun drawOverlay(canvas: Canvas) {
        canvas.drawRoundRect(leftRect, 18f, 18f, fill)
        canvas.drawRoundRect(rightRect, 18f, 18f, fill)
        canvas.drawRoundRect(leftRect, 18f, 18f, line)
        canvas.drawRoundRect(rightRect, 18f, 18f, line)
    }
}

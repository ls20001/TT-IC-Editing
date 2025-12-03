package com.example.tticediting

import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MASK
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * 处理矩形编辑框的拖动。
 */
class DragBoxController : View.OnTouchListener {
    private var dragBoxBound = RectF()
    private var dragBox = RectF()
    private var aspectRatio = 0f

    /**
     * 设置拖动边框
     */
    fun setDragBoxBound(bound: RectF) {
        require(bound.width() >= 0 && bound.height() >= 0)

        dragBoxBound = RectF(bound)
        adjustDragBox(Anchor.NONE)
    }

    /**
     * 设置/获取拖动框
     */
    fun getDragBox(): RectF {
        return RectF(dragBox)
    }

    fun setDragBox(box: RectF) {
        require(box.width() >= 0 && box.height() >= 0)

        dragBox = RectF(box)
        adjustDragBox(Anchor.NONE)
    }

    /**
     * 设置固定/自由长宽比模型
     */
    fun setFixAspectRatio(w: Int, h: Int) {
        require(w > 0 && h > 0)

        aspectRatio = w.toFloat() / h.toFloat()
        adjustDragBox(Anchor.NONE)
    }

    fun setFreeAspectRatio() {
        aspectRatio = 0f
    }

    fun isFixAspectRatio(): Boolean {
        return aspectRatio > 0f
    }

    private enum class Anchor {
        NONE,
        LEFT,
        TOP,
        RIGHT,
        BOTTOM,
        LEFTTOP,
        RIGHTTOP,
        LEFTBOTTOM,
        RIGHTBOTTOM,
        CENTER,
    }

    private var anchorUnderTrack = Anchor.NONE
    private var pointerPositionBefore = PointF()
    private var dragBoxBefore = RectF()

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v.performClick())
            return true

        when (event.action and ACTION_MASK) {
            ACTION_DOWN -> {
                return onActionDown(v, PointF(event.x, event.y))
            }

            ACTION_POINTER_DOWN -> {
                anchorUnderTrack = Anchor.NONE
                pointerPositionBefore = PointF()
                dragBoxBefore = RectF()
                return false
            }

            ACTION_MOVE -> {
                return onActionMove(v, PointF(event.x, event.y))
            }

            ACTION_UP -> {
                anchorUnderTrack = Anchor.NONE
                pointerPositionBefore = PointF()
                dragBoxBefore = RectF()
                return true
            }
        }
        return false
    }

    private fun onActionDown(view: View, point: PointF): Boolean {
        val anchor = getAnchorUnderPoint(point)
        if (anchor == Anchor.NONE)
            return false

        anchorUnderTrack = anchor
        pointerPositionBefore = PointF(point.x, point.y)
        dragBoxBefore = RectF(dragBox)
        return true
    }

    private fun onActionMove(view: View, point: PointF): Boolean {
        val dx = point.x - pointerPositionBefore.x
        val dy = point.y - pointerPositionBefore.y

        when (anchorUnderTrack) {
            Anchor.LEFT -> {
                dragBox.left = min(dragBoxBefore.left + dx, dragBoxBefore.right)
            }

            Anchor.TOP -> {
                dragBox.top = min(dragBoxBefore.top + dy, dragBoxBefore.bottom)
            }

            Anchor.RIGHT -> {
                dragBox.right = max(dragBoxBefore.right + dx, dragBoxBefore.left)
            }

            Anchor.BOTTOM -> {
                dragBox.bottom = max(dragBoxBefore.bottom + dy, dragBoxBefore.top)
            }

            Anchor.LEFTTOP -> {
                dragBox.left = min(dragBoxBefore.left + dx, dragBoxBefore.right)
                dragBox.top = min(dragBoxBefore.top + dy, dragBoxBefore.bottom)
            }

            Anchor.RIGHTTOP -> {
                dragBox.right = max(dragBoxBefore.right + dx, dragBoxBefore.left)
                dragBox.top = min(dragBoxBefore.top + dy, dragBoxBefore.bottom)
            }

            Anchor.LEFTBOTTOM -> {
                dragBox.left = min(dragBoxBefore.left + dx, dragBoxBefore.right)
                dragBox.bottom = max(dragBoxBefore.bottom + dy, dragBoxBefore.top)
            }

            Anchor.RIGHTBOTTOM -> {
                dragBox.right = max(dragBoxBefore.right + dx, dragBoxBefore.left)
                dragBox.bottom = max(dragBoxBefore.bottom + dy, dragBoxBefore.top)
            }

            Anchor.CENTER -> {
                dragBox.left = dragBoxBefore.left + dx
                dragBox.right = dragBoxBefore.right + dx
                dragBox.top = dragBoxBefore.top + dy
                dragBox.bottom = dragBoxBefore.bottom + dy
            }

            Anchor.NONE -> {
                return false
            }
        }
        adjustDragBox(anchorUnderTrack)
        view.invalidate()
        return true
    }

    // 根据宽高比和拖动范围，调整拖动框的位置和大小
    private fun adjustDragBox(anchor: Anchor) {
        // 限制边界范围
        dragBox.left = max(dragBox.left, dragBoxBound.left)
        dragBox.top = max(dragBox.top, dragBoxBound.top)
        dragBox.right = min(dragBox.right, dragBoxBound.right)
        dragBox.bottom = min(dragBox.bottom, dragBoxBound.bottom)

        // 限制最小尺寸，假设 dragBoxBound 具有足够的尺寸。
        var width = max(dragBox.width(), MIN_BOX_SIZE)
        var height = max(dragBox.height(), MIN_BOX_SIZE)
        if (isFixAspectRatio()) {
            if (width / height > aspectRatio) {
                width = height * aspectRatio
            } else {
                height = width / aspectRatio
            }
        }

        // 平移锚点：限制在边界内
        if (anchor == Anchor.CENTER || anchor == Anchor.NONE) {
            // 用于设置方法
            if (anchor == Anchor.NONE) {
                dragBox.right = dragBox.left + width
                dragBox.bottom = dragBox.top + height
            }
            if (dragBox.left < dragBoxBound.left) {
                dragBox.left = dragBoxBound.left
                dragBox.right = dragBoxBound.left + width
            }
            if (dragBox.top < dragBoxBound.top) {
                dragBox.top = dragBoxBound.top
                dragBox.bottom = dragBoxBound.top + height
            }
            if (dragBox.right > dragBoxBound.right) {
                dragBox.right = dragBoxBound.right
                dragBox.left = dragBoxBound.right - width
            }
            if (dragBox.bottom > dragBoxBound.bottom) {
                dragBox.bottom = dragBoxBound.bottom
                dragBox.top = dragBoxBound.bottom - height
            }
        } else {
            // 边界锚点
            @Suppress("KotlinConstantConditions")
            when (anchor) {
                Anchor.LEFTTOP,
                Anchor.TOP,
                Anchor.RIGHTTOP -> {
                    dragBox.top = dragBox.bottom - height
                }

                Anchor.LEFT,
                Anchor.RIGHT -> {
                    val center = (dragBox.top + dragBox.bottom) / 2f
                    dragBox.top = center - height / 2f
                    dragBox.bottom = center + height / 2f
                }

                Anchor.LEFTBOTTOM,
                Anchor.BOTTOM,
                Anchor.RIGHTBOTTOM -> {
                    dragBox.bottom = dragBox.top + height
                }

                Anchor.NONE -> Unit
                Anchor.CENTER -> Unit
            }
            @Suppress("KotlinConstantConditions")
            when (anchor) {
                Anchor.LEFTTOP,
                Anchor.LEFT,
                Anchor.LEFTBOTTOM -> {
                    dragBox.left = dragBox.right - width
                }

                Anchor.TOP,
                Anchor.BOTTOM -> {
                    val center = (dragBox.left + dragBox.right) / 2f
                    dragBox.left = center - width / 2f
                    dragBox.right = center + width / 2f
                }

                Anchor.RIGHTTOP,
                Anchor.RIGHT,
                Anchor.RIGHTBOTTOM -> {
                    dragBox.right = dragBox.left + width
                }

                Anchor.NONE -> Unit
                Anchor.CENTER -> Unit
            }
        }
    }

    // 获取光标所在位置的锚点
    private fun getAnchorUnderPoint(point: PointF): Anchor {
        val x0 = point.x - dragBox.left
        val y0 = point.y - dragBox.top
        val w = dragBox.width()
        val h = dragBox.height()

        if (x0 < -ANCHOR_SIZE || x0 > w + ANCHOR_SIZE)
            return Anchor.NONE
        if (y0 < -ANCHOR_SIZE || y0 > h + ANCHOR_SIZE)
            return Anchor.NONE

        if (x0 <= ANCHOR_SIZE && x0 <= w / 2f) {
            return if (y0 <= ANCHOR_SIZE && y0 <= h / 2f) {
                Anchor.LEFTTOP
            } else if (y0 >= h - ANCHOR_SIZE && y0 >= h / 2f) {
                Anchor.LEFTBOTTOM
            } else {
                Anchor.LEFT
            }
        } else if (x0 >= w - ANCHOR_SIZE && x0 >= w / 2f) {
            return if (y0 <= ANCHOR_SIZE && y0 <= h / 2f) {
                Anchor.RIGHTTOP
            } else if (y0 >= h - ANCHOR_SIZE && y0 >= h / 2f) {
                Anchor.RIGHTBOTTOM
            } else {
                Anchor.RIGHT
            }
        } else {
            return if (y0 <= ANCHOR_SIZE && y0 <= h / 2f) {
                Anchor.TOP
            } else if (y0 >= h - ANCHOR_SIZE && y0 >= h / 2f) {
                Anchor.BOTTOM
            } else {
                Anchor.CENTER
            }
        }
    }

    companion object {
        const val ANCHOR_SIZE = 80.0f       // 锚点大小，像素
        const val MIN_BOX_SIZE = 160.0f     // 最小尺寸，像素
    }
}
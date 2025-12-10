package com.example.tticediting

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MASK
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import androidx.core.graphics.withSave
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 可拖动/缩放/旋转矩形框
 */
class DragableRotableController : View.OnTouchListener {
    private var dragableRect = RotRect()
    private var aspectRatio = 0f

    /**
     * 设置矩形范围和转角
     */
    fun getDragableRect(): RotRect {
        return dragableRect
    }

    fun setDragableRect(rect: RotRect) {
        dragableRect = rect
        adjustDragableRectAfterSet()
    }

    fun setAngle(angle: Float) {
        dragableRect.angle = angle
        adjustDragableRectAfterSet()
    }

    fun setSize(width: Float, height: Float) {
        dragableRect.width = width
        dragableRect.height = height
        adjustDragableRectAfterSet()
    }

    fun setCenter(center: PointF) {
        dragableRect.x = center.x
        dragableRect.y = center.y
        adjustDragableRectAfterSet()
    }

    /**
     * 设置固定/自由长宽比模型
     */
    fun setFixAspectRatio(w: Int, h: Int) {
        aspectRatio = w.toFloat() / h.toFloat()
        adjustDragableRectAfterSet()
    }

    fun setFixAspectRatio(ratio: Float) {
        aspectRatio = ratio
        adjustDragableRectAfterSet()
    }

    fun setFreeAspectRatio() {
        aspectRatio = 0f
        adjustDragableRectAfterSet()
    }

    fun isFixAspectRatio(): Boolean {
        return aspectRatio > 0f
    }

    /**
     * 处理事件回调，onUpdate()为矩形位置更新时触发，onDoubleTap()为双击事件时触发
     */
    interface EventHandler {
        fun onUpdate()
        fun onDoubleTap(point: PointF)
    }

    private var eventHandler: EventHandler? = null

    fun setEventHandler(handler: EventHandler) {
        eventHandler = handler
    }

    // 手势处理
    private var previousDownTime = 0L
    private var pointerId = -1
    private var pointerPositionInit = PointF()
    private var anchorUnderTrack = ANCHOR_NONE
    private var dragableRectInit = RotRect()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action and ACTION_MASK) {
            ACTION_DOWN -> {
                return onActionDown(event)
            }

            ACTION_POINTER_DOWN -> {
                return onActionPointerDown(event)
            }

            ACTION_MOVE -> {
                return onActionMove(event)
            }

            ACTION_UP -> {
                return onActionUp(event)
            }

            else -> {
                return false
            }
        }
    }

    private fun onActionDown(event: MotionEvent): Boolean {
        val point = PointF(event.x, event.y)
        val anchor = getAnchorUnderPoint(point)
        if (anchor == ANCHOR_NONE)
            return false

        if (event.downTime - previousDownTime <= DOUBLE_TAP_INTERVAL) {
            // 处理双击事件
            previousDownTime = 0
            pointerId = -1
            pointerPositionInit = PointF()
            anchorUnderTrack = ANCHOR_NONE
            dragableRectInit = RotRect()
            eventHandler?.onDoubleTap(point)

        } else {
            // 记录点击位置
            previousDownTime = event.downTime
            pointerId = event.getPointerId(0)
            pointerPositionInit = point
            anchorUnderTrack = anchor
            dragableRectInit = dragableRect.copy()
        }
        return true
    }

    private fun onActionPointerDown(event: MotionEvent): Boolean {
        // 多指操作由下层处理
        pointerId = -1
        pointerPositionInit = PointF()
        anchorUnderTrack = ANCHOR_NONE
        dragableRectInit = RotRect()
        return false
    }

    private fun onActionMove(event: MotionEvent): Boolean {
        if (anchorUnderTrack == ANCHOR_NONE || pointerId == -1)
            return false

        val pointerIndex = event.findPointerIndex(pointerId)
        if (pointerIndex == -1)
            return false

        val point = PointF(event.getX(pointerIndex), event.getY(pointerIndex))
        if (anchorUnderTrack == ANCHOR_ROTATION) {
            handleRotation(point)
        } else {
            handleDrag(point)
        }
        eventHandler?.onUpdate()
        return true
    }

    private fun onActionUp(event: MotionEvent): Boolean {
        pointerId = -1
        dragableRectInit = RotRect()
        pointerPositionInit = PointF()
        anchorUnderTrack = ANCHOR_NONE
        return true
    }

    private fun handleRotation(point: PointF) {
        val dxInit = pointerPositionInit.x - dragableRectInit.x
        val dyInit = pointerPositionInit.y - dragableRectInit.y
        val angleInit = atan2(dxInit, dyInit)
        val angleCurrent = atan2(point.x - dragableRect.x, point.y - dragableRect.y)
        dragableRect.angle = dragableRectInit.angle - radToDeg(angleCurrent - angleInit)
    }

    private fun handleDrag(point: PointF) {
        val dx = point.x - pointerPositionInit.x
        val dy = point.y - pointerPositionInit.y

        // 处理平移
        if (anchorUnderTrack == ANCHOR_CENTER) {
            dragableRect.x = dragableRectInit.x + dx
            dragableRect.y = dragableRectInit.y + dy
            return
        }

        // 处理拖动缩放
        val angle = dragableRect.angle
        val dragNorm = rotatePoint(PointF(dx, dy), -angle)

        // 根据拖动的锚点调整矩形长度
        var width = dragableRectInit.width
        when (anchorUnderTrack and ANCHOR_HMASK) {
            ANCHOR_LEFT -> {
                width -= dragNorm.x
            }

            ANCHOR_RIGHT -> {
                width += dragNorm.x
            }

            ANCHOR_HCENTER -> {
                dragNorm.x = 0f
            }
        }

        var height = dragableRectInit.height
        when (anchorUnderTrack and ANCHOR_VMASK) {
            ANCHOR_TOP -> {
                height -= dragNorm.y
            }

            ANCHOR_BOTTOM -> {
                height += dragNorm.y
            }

            ANCHOR_VCENTER -> {
                dragNorm.y = 0f
            }
        }

        // 处理尺寸过小和长宽比，根据拖动距离较长的方向，调整另一个方向
        if (isFixAspectRatio()) {
            if (abs(dragNorm.x) > abs(dragNorm.y)) {
                width = max(max(width, MIN_RECT_SIZE), MIN_RECT_SIZE * aspectRatio)
                height = width / aspectRatio
            } else {
                height = max(max(height, MIN_RECT_SIZE), MIN_RECT_SIZE / aspectRatio)
                width = height * aspectRatio
            }
        } else {
            width = max(width, MIN_RECT_SIZE)
            height = max(height, MIN_RECT_SIZE)
        }

        // 调整位置
        dragableRect.width = width
        dragableRect.height = height

        val deltaWidthX = (width - dragableRectInit.width) * cos(degToRad(angle))
        val deltaWidthY = (width - dragableRectInit.width) * sin(degToRad(angle))
        val deltaHeightX = (height - dragableRectInit.height) * -sin(degToRad(angle))
        val deltaHeightY = (height - dragableRectInit.height) * cos(degToRad(angle))
        when (anchorUnderTrack and ANCHOR_HMASK) {
            ANCHOR_LEFT -> {
                dragableRect.x = dragableRectInit.x - deltaWidthX / 2f
                dragableRect.y = dragableRectInit.y - deltaWidthY / 2f
            }

            ANCHOR_RIGHT -> {
                dragableRect.x = dragableRectInit.x + deltaWidthX / 2f
                dragableRect.y = dragableRectInit.y + deltaWidthY / 2f
            }
        }
        when (anchorUnderTrack and ANCHOR_VMASK) {
            ANCHOR_TOP -> {
                dragableRect.x = dragableRectInit.x - deltaHeightX / 2f
                dragableRect.y = dragableRectInit.y - deltaHeightY / 2f
            }

            ANCHOR_BOTTOM -> {
                dragableRect.x = dragableRectInit.x + deltaHeightX / 2f
                dragableRect.y = dragableRectInit.y + deltaHeightY / 2f
            }
        }
    }

    // 获取 point 所处的锚点，返回 ANCHOR_NONE 表示不在范围内
    private fun getAnchorUnderPoint(point: PointF): Int {
        val center = dragableRect.center()
        val width = dragableRect.width
        val height = dragableRect.height
        val pointNorm = rotatePoint(point, -dragableRect.angle, center)

        // 旋转锚点
        val rotationAnchorX = center.x
        val rotationAnchorY = center.y + height / 2f + ROTATION_DISTANCE
        if (abs(pointNorm.x - rotationAnchorX) <= ANCHOR_SIZE &&
            abs(pointNorm.y - rotationAnchorY) <= ANCHOR_SIZE
        ) {
            return ANCHOR_ROTATION
        }

        val dx = pointNorm.x - center.x
        val dy = pointNorm.y - center.y
        if (abs(dx) > width / 2f + ANCHOR_SIZE || abs(dy) > height / 2f + ANCHOR_SIZE) {
            return ANCHOR_NONE
        }

        // 拖动锚点
        val anchorTypeHorizontal = if (dx <= -(width / 2f - ANCHOR_SIZE)) {
            ANCHOR_LEFT
        } else if (dx >= (width / 2f - ANCHOR_SIZE)) {
            ANCHOR_RIGHT
        } else {
            ANCHOR_HCENTER
        }
        val anchorTypeVertical = if (dy <= -(height / 2f - ANCHOR_SIZE)) {
            ANCHOR_TOP
        } else if (dy >= (height / 2f - ANCHOR_SIZE)) {
            ANCHOR_BOTTOM
        } else {
            ANCHOR_VCENTER
        }
        return anchorTypeHorizontal or anchorTypeVertical
    }

    private fun adjustDragableRectAfterSet() {
        dragableRect.width = max(MIN_RECT_SIZE, dragableRect.width)
        dragableRect.height = max(MIN_RECT_SIZE, dragableRect.height)
        if (isFixAspectRatio()) {
            if (dragableRect.width / dragableRect.height > aspectRatio) {
                dragableRect.width = dragableRect.height * aspectRatio
            } else {
                dragableRect.height = dragableRect.width / aspectRatio
            }
        }
        eventHandler?.onUpdate()
    }

    private val paint = Paint()

    /**
     * 绘制控件
     */
    fun drawController(canvas: Canvas) {
        canvas.withSave {
            rotate(dragableRect.angle, dragableRect.x, dragableRect.y)
            drawControllerFrame(canvas)
            drawControllerRotationAnchor(canvas)
        }
    }

    private fun drawControllerFrame(canvas: Canvas) {
        val left = dragableRect.x - dragableRect.width / 2f
        val top = dragableRect.y - dragableRect.height / 2f
        val right = dragableRect.x + dragableRect.width / 2f
        val bottom = dragableRect.y + dragableRect.height / 2f

        paint.style = Paint.Style.STROKE
        paint.color = EDGE_COLOR
        paint.strokeWidth = 10f
        canvas.drawRect(left, top, right, bottom, paint)

        paint.style = Paint.Style.FILL
        paint.color = ANCHOR_COLOR
        canvas.drawCircle(left, top, ANCHOR_DRAW_SIZE, paint)
        canvas.drawCircle(left, bottom, ANCHOR_DRAW_SIZE, paint)
        canvas.drawCircle(right, top, ANCHOR_DRAW_SIZE, paint)
        canvas.drawCircle(right, bottom, ANCHOR_DRAW_SIZE, paint)
    }

    private fun drawControllerRotationAnchor(canvas: Canvas) {
        val boundX = dragableRect.x
        val boundY = dragableRect.y + dragableRect.height / 2f
        val rotationAnchorX = boundX
        val rotationAnchorY = boundY + ROTATION_DISTANCE

        paint.style = Paint.Style.STROKE
        paint.color = EDGE_COLOR
        paint.strokeWidth = EDGE_WIDTH
        canvas.drawLine(boundX, boundY, rotationAnchorX, rotationAnchorY, paint)

        paint.style = Paint.Style.FILL
        paint.color = ANCHOR_COLOR
        canvas.drawCircle(rotationAnchorX, rotationAnchorY, ANCHOR_DRAW_SIZE, paint)
    }

    companion object {
        const val ANCHOR_SIZE = 40.0f           // 锚点大小，像素
        const val MIN_RECT_SIZE = 40.0f        // 最小尺寸，像素
        const val ROTATION_DISTANCE = 120.0f    // 拖动锚点到矩形边界的距离

        // 锚点类别常量
        private const val ANCHOR_NONE = 0x0

        private const val ANCHOR_LEFT = 0x01
        private const val ANCHOR_HCENTER = 0x02
        private const val ANCHOR_RIGHT = 0x03

        private const val ANCHOR_TOP = 0x10
        private const val ANCHOR_VCENTER = 0x20
        private const val ANCHOR_BOTTOM = 0x30

        private const val ANCHOR_CENTER = ANCHOR_HCENTER or ANCHOR_VCENTER

        private const val ANCHOR_HMASK = 0x0F
        private const val ANCHOR_VMASK = 0xF0

        private const val ANCHOR_ROTATION = 0x100

        // 绘图参数
        private const val ANCHOR_DRAW_SIZE = ANCHOR_SIZE * 0.5f
        private const val EDGE_WIDTH = 10f
        private const val EDGE_COLOR = 0xFF0000FF.toInt()
        private const val ANCHOR_COLOR = 0xFF00FF00.toInt()
    }
}
package com.example.tticediting

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MASK
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot

/**
 * 编辑器主视图，用于显示图片，实时渲染图像结果。默认进入预览模式，支持手势缩放和平移。
 * 主要功能如下：
 *   1. 单指点击拖动视图
 *   2. 双击缩放
 *   3. 双指缩放查看图像细节。
 *   4. 拖动到边缘时会限制进一步拖动。
 */
class ImagePreview : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private lateinit var imageEdit: ImageEditCore

    fun setImageEdit(imageEdit: ImageEditCore) {
        this.imageEdit = imageEdit
    }

    override fun onDraw(canvas: Canvas) {
        imageEdit.drawOnCanvas(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
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

            ACTION_POINTER_UP -> {
                return onActionPointerUp(event)
            }

            ACTION_UP -> {
                return onActionUp(event)
            }
        }
        return true
    }

    private var previousDownTime = 0L

    private var pointer1Id = -1
    private var pointer1Position = PointF()

    private var pointer2Id = -1
    private var pointer2Position = PointF()

    private fun onActionDown(event: MotionEvent): Boolean {
        if (event.downTime - previousDownTime <= DOUBLE_TAP_INTERVAL) {
            // 处理双击事件
            previousDownTime = 0
            pointer1Id = -1
            pointer1Position = PointF()
            handleDoubleTap(PointF(event.x, event.y))

        } else {
            // 记录点击位置
            previousDownTime = event.downTime
            pointer1Id = event.getPointerId(0)
            pointer1Position = PointF(event.x, event.y)
        }
        return true
    }

    private fun onActionPointerDown(event: MotionEvent): Boolean {
        if (pointer2Id == -1) {
            // 第二个 pointer 尚未记录，跟踪 pointer 作为第二指位置
            pointer2Id = event.getPointerId(1)
            pointer2Position = PointF(event.getX(1), event.getY(1))
            return true
        }
        return false
    }

    private fun onActionMove(event: MotionEvent): Boolean {
        if (pointer1Id != -1 && pointer2Id != -1) {
            // 处理双指移动
            val pointer1Index = event.findPointerIndex(pointer1Id)
            val pointer2Index = event.findPointerIndex(pointer2Id)
            val point1New = PointF(event.getX(pointer1Index), event.getY(pointer1Index))
            val point2New = PointF(event.getX(pointer2Index), event.getY(pointer2Index))
            handleTwoPointGesture(pointer1Position, pointer2Position, point1New, point2New)
            pointer1Position = point1New
            pointer2Position = point2New
            return true

        } else if (pointer1Id != -1) {
            // 处理单指拖动
            val pointer1Index = event.findPointerIndex(pointer1Id)
            val point1New = PointF(event.getX(pointer1Index), event.getY(pointer1Index))
            handleOnePointGesture(pointer1Position, point1New)
            pointer1Position = point1New
            return true

        } else if (pointer2Id != -1) {
            // 处理单指拖动
            val pointer2Index = event.findPointerIndex(pointer2Id)
            val point2New = PointF(event.getX(pointer2Index), event.getY(pointer2Index))
            handleOnePointGesture(pointer2Position, point2New)
            pointer2Position = point2New
            return true

        } else {
            return false
        }
    }

    private fun onActionPointerUp(event: MotionEvent): Boolean {
        if (pointer1Id != -1) {
            val pointer1Index = event.findPointerIndex(pointer1Id)
            if (pointer1Index == event.actionIndex) {
                // 光标1抬起
                pointer1Id = -1
                pointer1Position = PointF()
                return true
            }
        }
        if (pointer2Id != -1) {
            val pointer2Index = event.findPointerIndex(pointer2Id)
            if (pointer2Index == event.actionIndex) {
                // 光标2抬起
                pointer2Id = -1
                pointer2Position = PointF()
                return true
            }
        }
        return false
    }

    private fun onActionUp(event: MotionEvent): Boolean {
        pointer1Id = -1
        pointer2Id = -1
        pointer1Position = PointF()
        pointer2Position = PointF()
        return true
    }

    // 处理触控事件，将手势转换为编辑器动作
    private fun handleDoubleTap(point: PointF) {
        if (abs(imageEdit.getZoomLevel() - 1.0f) <= 1E-5) {
            // 图片未进行缩放
            imageEdit.scaleView(2.0f, point)
        } else {
            imageEdit.centerizeImageOnView()
        }
    }

    private fun handleOnePointGesture(pointBefore: PointF, pointNow: PointF) {
        imageEdit.translateView(pointNow.x - pointBefore.x, pointNow.y - pointBefore.y)
    }

    // 以双指中心的移动作为平移量，以双指距离的变换作为旋转量
    private fun handleTwoPointGesture(
        point1Before: PointF,
        point2Before: PointF,
        point1Now: PointF,
        point2Now: PointF
    ) {
        val centerBefore = PointF(
            (point1Before.x + point2Before.x) / 2f,
            (point1Before.y + point2Before.y) / 2f,
        )
        val centerNow = PointF(
            (point1Now.x + point2Now.x) / 2f,
            (point1Now.y + point2Now.y) / 2f,
        )
        imageEdit.translateView(centerNow.x - centerBefore.x, centerNow.y - centerBefore.y)

        val diffBefore = PointF(
            point2Before.x - point1Before.x,
            point2Before.y - point1Before.y,
        )
        val diffNow = PointF(
            point2Now.x - point1Now.x,
            point2Now.y - point1Now.y,
        )
        val scale = hypot(diffNow.x, diffNow.y) / hypot(diffBefore.x, diffBefore.y)
        imageEdit.scaleView(scale, centerNow)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        imageEdit.setViewSize(w, h)
    }
}
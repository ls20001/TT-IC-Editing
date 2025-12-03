package com.example.tticediting

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 处理裁剪框的拖动和绘制。
 */
class ChopView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private val dragBoxController = DragBoxController()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnTouchListener(dragBoxController)
    }

    fun getChopBox(): RectF {
        return dragBoxController.getDragBox()
    }

    fun setChopBox(box: RectF) {
        dragBoxController.setDragBox(box)
        invalidate()
    }

    fun setImageBound(bound: RectF) {
        dragBoxController.setDragBoxBound(bound)
        invalidate()
    }

    fun setFixAspectRatio(w: Int, h: Int) {
        dragBoxController.setFixAspectRatio(w, h)
        invalidate()
    }

    fun setFreeAspectRatio() {
        dragBoxController.setFreeAspectRatio()
        invalidate()
    }

    // 绘图操作
    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(BACKGROUND_COLOR)

        val chopBox = dragBoxController.getDragBox()
        drawChopFrame(canvas, chopBox)
        drawChopRange(canvas, chopBox)
    }

    private fun drawChopRange(canvas: Canvas, chopBox: RectF) {
        paint.setColor(FOREGROUND_COLOR)
        paint.style = Paint.Style.FILL_AND_STROKE
        canvas.drawRect(chopBox, paint)
    }

    private fun drawChopFrame(canvas: Canvas, chopBox: RectF) {
        paint.setColor(EDGE_COLOR)
        paint.style = Paint.Style.STROKE

        paint.strokeWidth = 10f
        canvas.drawRect(chopBox, paint)

        val paddingX = chopBox.width() / 3f
        val paddingY = chopBox.height() / 3f
        val lines = floatArrayOf(
            chopBox.left, chopBox.top + paddingY * 1f, chopBox.right, chopBox.top + paddingY * 1f,
            chopBox.left, chopBox.top + paddingY * 2f, chopBox.right, chopBox.top + paddingY * 2f,
            chopBox.left + paddingX * 1f, chopBox.top, chopBox.left + paddingX * 1f, chopBox.bottom,
            chopBox.left + paddingX * 2f, chopBox.top, chopBox.left + paddingX * 2f, chopBox.bottom,
        )
        paint.strokeWidth = 6f
        canvas.drawLines(lines, paint)
    }

    companion object {
        const val BACKGROUND_COLOR = 0xCF000000.toInt()
        const val FOREGROUND_COLOR = 0xAFFFFFFF.toInt()
        const val EDGE_COLOR = 0xFFFFFFFF.toInt()
    }
}
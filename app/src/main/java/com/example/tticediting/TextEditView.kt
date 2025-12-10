package com.example.tticediting

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withMatrix

/**
 * 处理文字的绘制和拖动
 */
class TextEditView : View, DragableRotableController.EventHandler {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private var dragableController = DragableRotableController()

    /**
     * 处理文字编辑请求
     */
    fun interface TextEditRequestHandler {
        fun handleTextEdit()
    }

    private var textEditRequestHandler: TextEditRequestHandler? = null

    fun setTextEditRequestHandler(handler: TextEditRequestHandler) {
        textEditRequestHandler = handler
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        dragableController.setEventHandler(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (super.onTouchEvent(event))
            return true
        return dragableController.onTouch(this, event)
    }

    fun getTextRect(): RotRect {
        return dragableController.getDragableRect()
    }

    fun setTextRotation(angle: Float) {
        dragableController.setAngle(angle)
    }

    fun setTextPosition(position: PointF) {
        dragableController.setCenter(position)
    }

    fun setTextContent(text: String) {
        textContent = text
        updateAspectRatio()
    }

    fun setTextSize(textSize: Float) {
        paint.textSize = textSize
        updateAspectRatio()
    }

    fun setTypeface(typeface: Typeface) {
        paint.typeface = typeface
        updateAspectRatio()
    }

    fun setTextColor(color: Int) {
        paint.color = color
    }

    private val paint = Paint()
    private var textContent: String = ""

    private fun updateAspectRatio() {
        val textBound = Rect()
        paint.getTextBounds(textContent, 0, textContent.length, textBound)

        val textWidth = textBound.width().toFloat()
        val textHeight = textBound.height().toFloat()
        dragableController.setSize(textWidth, textHeight)
        dragableController.setFixAspectRatio(textWidth / textHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (textContent.isNotEmpty()) {
            drawText(canvas)
            dragableController.drawController(canvas)
        }
    }

    private fun drawText(canvas: Canvas) {
        val textRect = getTextRect()
        val leftBottom = rotatePoint(
            PointF(textRect.x - textRect.width / 2f, textRect.y + textRect.height / 2f),
            textRect.angle,
            textRect.center()
        )

        // 计算变换矩阵：bound -> (left, bottom)
        val transformation = Matrix()
        transformation.preRotate(textRect.angle)

        val inverse = Matrix()
        transformation.invert(inverse)

        val points = floatArrayOf(0f, textRect.height, leftBottom.x, leftBottom.y)
        inverse.mapPoints(points)
        transformation.preTranslate(points[2] - points[0], points[3] - points[1])

        paint.textSize = textRect.height
        canvas.withMatrix(transformation) {
            drawText(textContent, points[0], points[1], paint)
        }
    }

    override fun onUpdate() {
        postInvalidate()
    }

    override fun onDoubleTap(point: PointF) {
        textEditRequestHandler?.handleTextEdit()
    }
}
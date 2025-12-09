package com.example.tticediting

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MASK
import android.view.View

class TextEditView : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)


    private var hasTextInput = false
    private var textPosition = PointF()
    private var textRotation = 0

    private var pointerId = -1

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (super.onTouchEvent(event))
            return true

        when (event.action and ACTION_MASK) {
            ACTION_DOWN -> {
                return onActionDown(event)
            }
        }
        return false
    }

    private fun onActionDown(event: MotionEvent): Boolean {
        if (!hasTextInput) {
            showDialogForInput()

        }
        return true
    }

    private fun showDialogForInput() {

    }

    fun setFontRotation(angle: Int) {}
}
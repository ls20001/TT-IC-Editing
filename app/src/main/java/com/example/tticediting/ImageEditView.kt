package com.example.tticediting

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ImageEditView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    private val testImage = BitmapFactory.decodeResource(resources, R.drawable.test_image)
    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(testImage, 0.0f, 0.0f, paint)
    }
}
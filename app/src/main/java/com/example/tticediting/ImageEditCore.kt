package com.example.tticediting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap

class ImageEditCore {
    private var image = createBitmap(1, 1)
    private val paint = Paint()

    private var viewTransformation = Matrix()

    fun openImage(image: Bitmap) {
        this.image = image
    }

    fun setViewTransformation(transformation: Matrix) {
        this.viewTransformation = transformation
    }

    fun drawOnCanvas(canvas: Canvas) {
        canvas.drawBitmap(image, viewTransformation, paint)
    }
}
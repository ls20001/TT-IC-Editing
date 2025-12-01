package com.example.tticediting

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import com.example.tticediting.ViewGestureDetector.PointerDragInfo
import com.example.tticediting.geometry.Vector2
import com.example.tticediting.geometry.div
import com.example.tticediting.geometry.minus
import com.example.tticediting.geometry.norm
import com.example.tticediting.geometry.plus
import kotlin.math.min

/**
 * ImageEditView 配套的 ViewModel，保存图像变换数据。
 */
class ImageEditViewModel : ViewModel() {
    private var image: Bitmap = createBitmap(1, 1)
    private var imageWidth = 0f
    private var imageHeight = 0f

    private var viewWidth = 0f
    private var viewHeight = 0f

    private var transformation = Matrix()
    private var zoomLevel = 1.0f  // 图像居中对齐后，附加的缩放系数。

    /**
     * 打开图片并自动将图片居中。
     */
    fun openImage(image: Bitmap) {
        this.image = image
        imageWidth = image.width.toFloat()
        imageHeight = image.height.toFloat()
        centerizeImageOnView()
    }

    /**
     * 设置窗口大小，配合图片居中对齐。
     */
    fun setViewSize(width: Int, height: Int) {
        viewWidth = width.toFloat()
        viewHeight = height.toFloat()
        centerizeImageOnView()
    }

    // 根据窗口大小将图片缩放并居中。
    private fun centerizeImageOnView() {
        val scale = min(viewWidth / imageWidth, viewHeight / imageHeight)

        transformation = Matrix()
        transformation.postTranslate(-imageWidth / 2f, -imageHeight / 2f)
        transformation.postScale(scale, scale)
        transformation.postTranslate(viewWidth / 2f, viewHeight / 2f)
        zoomLevel = 1.0f
    }

    /**
     * 获取当前图像变换矩阵，用于渲染。
     */
    fun getTransformation(): Matrix {
        return transformation
    }

    private var zoomOn = false

    /**
     * 切换图片缩放系数，从默认（1.0）到 IMAGE_SCALE_ZOOM_ON 之间。
     */
    fun switchImageZoom(position: Vector2) {
        val scale = if (zoomOn) {
            zoomOn = false
            1.0f / zoomLevel
        } else {
            zoomOn = true
            IMAGE_SCALE_ZOOM_ON / zoomLevel
        }
        transformation.postScale(scale, scale, position.x, position.y)
        zoomLevel = zoomLevel * scale
    }

    /**
     * 处理单击拖动。
     */
    fun dragImage(pointer: PointerDragInfo) {
        val difference = pointer.current - pointer.before
        transformation.postTranslate(difference.x, difference.y)
        constrainImageBox()
    }

    /**
     * 处理双击缩放和平移，缩放范围为 IMAGE_SCALE_MIN 到 IMAGE_SCALE_MAX 之间。
     */
    fun dragImage(pointer1: PointerDragInfo, pointer2: PointerDragInfo) {
        val centerBefore = (pointer1.before + pointer2.before) / 2f
        transformation.postTranslate(-centerBefore.x, -centerBefore.y)

        val differenceBefore = pointer2.before - pointer1.before
        val differenceCurrent = pointer2.current - pointer1.current

        var scale = norm(differenceCurrent) / norm(differenceBefore)
        if (zoomLevel * scale > IMAGE_SCALE_MAX) {
            scale = IMAGE_SCALE_MAX / zoomLevel
        }
        if (zoomLevel * scale < IMAGE_SCALE_MIN) {
            scale = IMAGE_SCALE_MIN / zoomLevel
        }
        transformation.postScale(scale, scale)
        zoomLevel = zoomLevel * scale

        val centerCurrent = (pointer1.current + pointer2.current) / 2f
        transformation.postTranslate(centerCurrent.x, centerCurrent.y)

        constrainImageBox()
    }

    // 约束图片位置，防止移出边界。
    private fun constrainImageBox() {
        val imageBox = RectF(0f, 0f, imageWidth, imageHeight)
        transformation.mapRect(imageBox)

        val margin = IMAGE_MARGIN_RATI * min(viewWidth, viewHeight)

        val boundLeft = margin
        val boundRight = viewWidth - margin
        val boundTop = margin
        val boundBottom = viewHeight - margin

        if (imageBox.left > boundRight) {
            transformation.postTranslate(boundRight - imageBox.left, 0.0f)
        }
        if (imageBox.right < boundLeft) {
            transformation.postTranslate(boundLeft - imageBox.right, 0.0f)
        }
        if (imageBox.top > boundBottom) {
            transformation.postTranslate(0.0f, boundBottom - imageBox.top)
        }
        if (imageBox.bottom < boundTop) {
            transformation.postTranslate(0.0f, boundTop - imageBox.bottom)
        }
    }

    companion object {
        const val IMAGE_MARGIN_RATI = 0.2f      // 图像拖动边缘范围和屏幕尺寸的比值
        const val IMAGE_SCALE_MIN = 0.5f        // 缩放系数最小值
        const val IMAGE_SCALE_MAX = 2.0f        // 缩放系数最大值
        const val IMAGE_SCALE_ZOOM_ON = 2.0f    // Zoom on 后的缩放系数
    }
}
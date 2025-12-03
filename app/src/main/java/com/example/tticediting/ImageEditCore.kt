package com.example.tticediting

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toRect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private const val TAG = "XX-ImageEditCore"

class ImageEditCore(application: Application) : AndroidViewModel(application) {
    private val mImageUpdate = MutableLiveData<Unit>()
    private val mImageSaveResult = MutableLiveData<Boolean>()

    /**
     * 图片更新时触发。
     */
    val imageUpdate: LiveData<Unit> get() = mImageUpdate

    /**
     * 图片写入完成后触发，返回 true 表示写入成功。
     */
    val imageSaveResult: LiveData<Boolean> get() = mImageSaveResult

    /**
     * 打开图片，并自动将图片居中。采用 IO 线程异步进行读写。
     */
    fun openImageFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            application.contentResolver.openInputStream(uri).use { stream ->
                targetImage = BitmapFactory.decodeStream(stream)
                centerizeImageOnView()
                mImageUpdate.postValue(Unit)
            }
        }
    }

    /**
     * 将图像保存到相册。Android 10 以上，写入公共存储无需申请权限。采用协程进行异步处理，避免阻塞 UI 线程。
     */
    fun saveImageToAlbum() {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = application.contentResolver.insert(EXTERNAL_CONTENT_URI, ContentValues())
            if (uri == null) {
                mImageSaveResult.postValue(false)
                return@launch
            }
            application.contentResolver.openOutputStream(uri)?.use { stream ->
                targetImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            Log.d(TAG, "Image save success: $uri")
            mImageSaveResult.postValue(true)
        }
    }

    // 渲染目标图像
    private var targetImage = createBitmap(1, 1)

    // 图像在 View 上的区域
    private var imageBox = RectF(0f, 0f, 1f, 1f)

    private var viewWidth = 0f
    private var viewHeight = 0f
    private var paint = Paint()

    /**
     * 获取图像当前在屏幕上的区域
     */
    fun getImageBox(): RectF {
        return imageBox
    }

    /**
     * 设置渲染窗口大小
     */
    fun setViewSize(width: Int, height: Int) {
        viewWidth = width.toFloat()
        viewHeight = height.toFloat()
        centerizeImageOnView()
    }

    /**
     * 渲染图像
     */
    fun drawOnCanvas(canvas: Canvas) {
        canvas.drawBitmap(targetImage, null, imageBox, paint)
    }

    // 根据窗口大小将图片缩放并居中。
    fun centerizeImageOnView() {
        val imageAspectRatio = targetImage.width.toFloat() / targetImage.height.toFloat()
        val viewAspectRatio = viewWidth / viewHeight
        if (imageAspectRatio > viewAspectRatio) {
            imageBox.left = 0f
            imageBox.right = viewWidth

            val imageBoxHeight = viewWidth / imageAspectRatio
            imageBox.top = viewHeight / 2f - imageBoxHeight / 2
            imageBox.bottom = viewHeight / 2f + imageBoxHeight / 2

        } else {
            imageBox.top = 0f
            imageBox.bottom = viewHeight

            val imageBoxWidth = viewHeight * imageAspectRatio
            imageBox.left = viewWidth / 2f - imageBoxWidth / 2
            imageBox.right = viewWidth / 2f + imageBoxWidth / 2
        }
        zoomLevel = 1.0f
        mImageUpdate.postValue(Unit)
    }

    /**
     * 图像居中后附加的缩放系数，默认 1.0，范围为 IMAGE_SCALE_MIN 到 IMAGE_SCALE_MAX 之间。
     */
    private var zoomLevel: Float = 1.0f

    fun getZoomLevel(): Float {
        return zoomLevel
    }

    /**
     * 以 center 为中心缩放图片
     */
    fun scaleView(scale: Float, center: PointF = PointF()) {
        val zoomLevelAfter = max(min(zoomLevel * scale, IMAGE_SCALE_MAX), IMAGE_SCALE_MIN)
        val scaleFactory = zoomLevelAfter / zoomLevel

        val transform = Matrix()
        transform.postScale(scaleFactory, scaleFactory, center.x, center.y)
        transform.mapRect(imageBox)
        zoomLevel = zoomLevelAfter

        adjustImageOnView()
        mImageUpdate.value = Unit
    }

    fun translateView(dx: Float, dy: Float) {
        imageBox.left += dx
        imageBox.right += dx
        imageBox.top += dy
        imageBox.bottom += dy
        adjustImageOnView()
        mImageUpdate.value = Unit
    }

    // 约束图片位置，防止移出边界。
    private fun adjustImageOnView() {
        val margin = IMAGE_MARGIN_RATIO * min(viewWidth, viewHeight)

        val boundLeft = margin
        val boundRight = viewWidth - margin
        val boundTop = margin
        val boundBottom = viewHeight - margin

        if (imageBox.left > boundRight) {
            val difference = imageBox.left - boundRight
            imageBox.left -= difference
            imageBox.right -= difference
        }
        if (imageBox.right < boundLeft) {
            val difference = boundLeft - imageBox.right
            imageBox.left += difference
            imageBox.right += difference
        }
        if (imageBox.top > boundBottom) {
            val difference = imageBox.top - boundBottom
            imageBox.top -= difference
            imageBox.bottom -= difference
        }
        if (imageBox.bottom < boundTop) {
            val difference = boundTop - imageBox.bottom
            imageBox.top += difference
            imageBox.bottom += difference
        }
    }

    /**
     * 根据屏幕坐标的 chopBox 裁剪图像
     */
    fun chopImageOnView(chopBox: RectF) {
        viewModelScope.launch {
            val chopOnImage = screenToImage(chopBox).toRect()
            chopOnImage.left = max(chopOnImage.left, 0)
            chopOnImage.top = max(chopOnImage.top, 0)
            chopOnImage.right = min(chopOnImage.right, targetImage.width)
            chopOnImage.bottom = min(chopOnImage.bottom, targetImage.height)

            val image = createBitmap(
                chopOnImage.width(), chopOnImage.height(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(image)
            canvas.drawBitmap(
                targetImage,
                chopOnImage,
                Rect(0, 0, image.width, image.height),
                paint
            )
            targetImage = image
            centerizeImageOnView()
            mImageUpdate.postValue(Unit)
        }
    }

    /**
     * 屏幕坐标转换为 bitmap 坐标
     */
    fun screenToImage(rect: RectF): RectF {
        val imageWidth = targetImage.width.toFloat()
        val imageHeight = targetImage.height.toFloat()
        val scaleX = imageBox.width() / imageWidth
        val scaleY = imageBox.height() / imageHeight
        return RectF(
            (rect.left - imageBox.left) / scaleX,
            (rect.top - imageBox.top) / scaleY,
            (rect.right - imageBox.left) / scaleX,
            (rect.bottom - imageBox.top) / scaleY,
        )
    }

    init {
        // TODO: 测试图片
        targetImage = BitmapFactory.decodeResource(application.resources, R.drawable.test_image)
        mImageUpdate.value = Unit
    }

    companion object {
        const val IMAGE_MARGIN_RATIO = 0.2f     // 图像拖动边缘范围和屏幕尺寸的比值
        const val IMAGE_SCALE_MIN = 0.5f        // 缩放系数最小值
        const val IMAGE_SCALE_MAX = 2.0f        // 缩放系数最大值
    }
}
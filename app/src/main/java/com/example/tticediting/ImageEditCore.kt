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
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toRect
import androidx.core.graphics.toRectF
import androidx.core.graphics.withMatrix
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
                val options = BitmapFactory.Options().apply {
                    inMutable = true
                }
                BitmapFactory.decodeStream(stream, null, options)?.apply {
                    targetImage = this
                    centerizeImageOnView()
                }
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

    // 更新图片，并通知 observer
    private fun updateImage() {
        mImageUpdate.postValue(Unit)
    }

    // 渲染目标图像，确保以可变模式打开
    private var targetImage = createBitmap(1, 1)

    // 自上一次 commit 以来的变换操作，先翻转再旋转
    private var imageRotation = 0  // 旋转角度，仅支持0、90、180、270
    private var imageFlipHorizontal = false
    private var imageFlipVertival = false

    // 图像在 View 上占据的区域，确保其长宽为正
    private var imageBox = RectF(0f, 0f, 1f, 1f)
    private var viewWidth = 0f
    private var viewHeight = 0f

    private var paint = Paint()

    /**
     * 获取图像在 View 上占据的区域
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
        val transformation = Matrix()

        // 处理缩放和翻转
        if (imageRotation % 180 == 0) {
            val scaleFactor = imageBox.width() / targetImage.width
            transformation.preScale(
                scaleFactor * if (imageFlipHorizontal) -1f else 1f,
                scaleFactor * if (imageFlipVertival) -1f else 1f,
            )
        } else {
            val scaleFactor = imageBox.width() / targetImage.height
            transformation.preScale(
                scaleFactor * if (imageFlipVertival) -1f else 1f,
                scaleFactor * if (imageFlipHorizontal) -1f else 1f,
            )
        }

        // 处理旋转
        transformation.preRotate(imageRotation.toFloat())

        // 计算平移量
        val inverse = Matrix()
        transformation.invert(inverse)

        val imageBoxAfter = RectF(imageBox)
        inverse.mapRect(imageBoxAfter)

        transformation.preTranslate(imageBoxAfter.left, imageBoxAfter.top)
        canvas.drawBitmap(targetImage, transformation, paint)
    }

    // 根据窗口大小将图片缩放并居中。
    fun centerizeImageOnView() {
        val imageAspectRatio = getAspectRatio(targetImage.rectF())
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
        updateImage()
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
        updateImage()
    }

    fun translateView(dx: Float, dy: Float) {
        imageBox.left += dx
        imageBox.right += dx
        imageBox.top += dy
        imageBox.bottom += dy
        adjustImageOnView()
        updateImage()
    }

    // 约束图片位置，防止移出边界，不触发  updateImage() 事件
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
            commitUpdate()
            val chopOnImage = screenToImage(chopBox).toRect()
            chopOnImage.left = max(chopOnImage.left, 0)
            chopOnImage.top = max(chopOnImage.top, 0)
            chopOnImage.right = min(chopOnImage.right, targetImage.width)
            chopOnImage.bottom = min(chopOnImage.bottom, targetImage.height)

            val image = createBitmap(chopOnImage.width(), chopOnImage.height())
            val canvas = Canvas(image)
            canvas.drawBitmap(targetImage, chopOnImage, image.rect(), paint)

            targetImage = image
            centerizeImageOnView()
        }
    }

    // 旋转角度，0，90，180，270
    fun rotateImage(angle: Int) {
        if (angle % 180 != 0) {
            val imageBoxCenter = getRectCenter(imageBox)
            val widthHalf = imageBox.width() / 2f
            val heightHalf = imageBox.height() / 2f
            imageBox.left = imageBoxCenter.x - heightHalf
            imageBox.top = imageBoxCenter.y - widthHalf
            imageBox.right = imageBoxCenter.x + heightHalf
            imageBox.bottom = imageBoxCenter.y + widthHalf
        }
        imageRotation = (imageRotation + angle) % 360
        updateImage()
    }

    fun flipImageHorizontal() {
        if (imageRotation % 180 == 0) {
            imageFlipHorizontal = !imageFlipHorizontal
        } else {
            imageFlipVertival = !imageFlipVertival
        }
        updateImage()
    }

    fun flipImageVertical() {
        if (imageRotation % 180 == 0) {
            imageFlipVertival = !imageFlipVertival
        } else {
            imageFlipHorizontal = !imageFlipHorizontal
        }
        updateImage()
    }

    // 将变换结果写入到原图像
    fun commitUpdate() {
        if (imageRotation % 360 == 0 && !imageFlipHorizontal && !imageFlipVertival)
            return

        val transformation = Matrix()

        // 处理翻转
        val image = if (imageRotation % 180 == 0) {
            transformation.preScale(
                if (imageFlipHorizontal) -1f else 1f,
                if (imageFlipVertival) -1f else 1f,
            )
            createBitmap(targetImage.width, targetImage.height)
        } else {
            transformation.preScale(
                if (imageFlipVertival) -1f else 1f,
                if (imageFlipHorizontal) -1f else 1f,
            )
            createBitmap(targetImage.height, targetImage.width)
        }

        // 处理旋转
        transformation.preRotate(imageRotation.toFloat())

        // 计算平移量
        val inverse = Matrix()
        transformation.invert(inverse)

        val imageBoxAfter = RectF(0f, 0f, image.width.toFloat(), image.height.toFloat())
        inverse.mapRect(imageBoxAfter)
        transformation.preTranslate(imageBoxAfter.left, imageBoxAfter.top)

        // 更新图像
        val canvas = Canvas(image)
        canvas.drawBitmap(targetImage, transformation, paint)
        targetImage = image

        imageRotation = 0
        imageFlipHorizontal = false
        imageFlipVertival = false
        centerizeImageOnView()
    }

    fun discardUpdate() {
        imageRotation = 0
        imageFlipHorizontal = false
        imageFlipVertival = false
        centerizeImageOnView()
    }

    fun drawTextOnView(text: String, typeface: Typeface, color: Int, textRect: RotRect) {
        val textRectOnImage = screenToImage(textRect)
        val leftBottom = rotatePoint(
            PointF(
                textRectOnImage.x - textRectOnImage.width / 2f,
                textRectOnImage.y + textRectOnImage.height / 2f
            ),
            textRectOnImage.angle,
            textRectOnImage.center()
        )

        // 计算变换矩阵：bound -> (left, bottom)
        val transformation = Matrix()
        transformation.preRotate(textRect.angle)

        val inverse = Matrix()
        transformation.invert(inverse)

        val points = floatArrayOf(0f, textRect.height, leftBottom.x, leftBottom.y)
        inverse.mapPoints(points)
        transformation.preTranslate(points[2] - points[0], points[3] - points[1])

        paint.typeface = typeface
        paint.textSize = textRectOnImage.height
        paint.color = color
        val canvas = Canvas(targetImage)
        canvas.withMatrix(transformation) {
            drawText(text, points[0], points[1], paint)
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

    fun screenToImage(rect: RotRect): RotRect {
        val imageWidth = targetImage.width.toFloat()
        val imageHeight = targetImage.height.toFloat()
        val scaleX = imageBox.width() / imageWidth
        val scaleY = imageBox.height() / imageHeight
        return RotRect(
            (rect.x - imageBox.left) / scaleX,
            (rect.y - imageBox.top) / scaleY,
            rect.width / scaleX,
            rect.height / scaleY,
            rect.angle
        )
    }

    // init {
    //     // TODO: 测试专用
    //     val options = BitmapFactory.Options().apply {
    //         inMutable = true
    //     }
    //     targetImage = BitmapFactory.decodeResource(
    //         application.resources, R.drawable.test_image, options
    //     )
    //     updateImage()
    // }

    companion object {
        const val IMAGE_MARGIN_RATIO = 0.2f     // 图像拖动边缘范围和屏幕尺寸的比值
        const val IMAGE_SCALE_MIN = 0.5f        // 缩放系数最小值
        const val IMAGE_SCALE_MAX = 2.0f        // 缩放系数最大值

        // 辅助函数
        fun getRectCenter(rect: RectF): PointF {
            return PointF((rect.left + rect.right) / 2f, (rect.top + rect.bottom) / 2f)
        }

        fun getRectCenter(rect: Rect): PointF {
            return getRectCenter(rect.toRectF())
        }

        fun getAspectRatio(rect: RectF): Float {
            return rect.width() / rect.height()
        }

        fun getAspectRatio(rect: Rect): Float {
            return getAspectRatio(rect.toRectF())
        }
    }
}

private fun Bitmap.rectF(): RectF {
    return RectF(0f, 0f, width.toFloat(), height.toFloat())
}

private fun Bitmap.rect(): Rect {
    return Rect(0, 0, width, height)
}
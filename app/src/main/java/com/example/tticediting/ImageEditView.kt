package com.example.tticediting

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.tticediting.ViewGestureDetector.PointerDragInfo
import com.example.tticediting.geometry.Vector2

/**
 * 编辑器主视图，用于显示图片，并实施渲染图像结果。默认进入预览模式，支持手势缩放和平移。
 * 主要功能如下：
 *   1. 单指点击拖动视图
 *   2. 双击缩放
 *   3. 双指缩放查看图像细节。
 *   4. 拖动到边缘时会限制进一步拖动。
 */
class ImageEditView : View, ViewGestureDetector.GestureListener {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    private val imageEdit = ImageEditCore()

    private lateinit var viewModel: ImageEditViewModel

    /**
     * Activity 初始化时调用。
     */
    fun onCreate(activity: ComponentActivity) {
        viewModel = ViewModelProvider(activity)[ImageEditViewModel::class.java]
        setOnTouchListener(ViewGestureDetector(this))
    }

    override fun onDraw(canvas: Canvas) {
        imageEdit.setViewTransformation(viewModel.getTransformation())
        imageEdit.drawOnCanvas(canvas)
    }

    // 处理触控事件。
    override fun onDoubleTap(position: Vector2) {
        viewModel.switchImageZoom(position)
        invalidate()
    }

    override fun onDrag(pointer: PointerDragInfo) {
        viewModel.dragImage(pointer)
        invalidate()
    }

    override fun onDoubleDrag(pointer1: PointerDragInfo, pointer2: PointerDragInfo) {
        viewModel.dragImage(pointer1, pointer2)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewModel.setViewSize(w, h)

        // TODO：此处从测试文件加载图片
        if (fistTimeInit) {
            fistTimeInit = false

            val image = BitmapFactory.decodeResource(resources, R.drawable.test_image)
            imageEdit.openImage(image)
            viewModel.openImage(image)
        }
    }
}

private var fistTimeInit = true
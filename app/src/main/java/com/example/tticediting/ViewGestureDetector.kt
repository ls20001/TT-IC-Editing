package com.example.tticediting

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MASK
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.View
import com.example.tticediting.geometry.Vector2

/**
 * 处理基本手势交互，将 MotionEvent 转换为点击、双击、单指拖动、双指缩放。
 *
 * 对于拖动，操作逻辑如下，其行为与 iOS 相册和 Safari 浏览器一致：
 *   1. 第一个光标按下（ACTION_DOWN），进入单指拖动。
 *   2. 第二个光标按下（ACTION_POINTER_DOWN），进入双指拖动模式。此状态只触发一次，忽略更多的光标。
 *   3. 光标移动（ACTION_MOVE），根据单指和双指拖动模型，分别按第一个或前两个光标进行处理。
 *   4. 光标抬起（ACTION_POINTER_UP），双指模式下，若前两个光标中的一个抬起，进入单指模式，其他光标按下不再进入
 *      双指模式。
 *   5. 所有光标抬起（ACTION_UP），结束跟踪。
 */
class ViewGestureDetector(private var listener: GestureListener? = null) : View.OnTouchListener {
    /**
     * 拖动事件信息，current 为当前指针位置，before 为上一次事件指针的位置，origin 为起始时指针的位置。
     */
    data class PointerDragInfo(val current: Vector2, val before: Vector2, val origin: Vector2)

    interface GestureListener {
        /**
         * 点击时触发，position 为点击的位置。
         */
        fun onTap(position: Vector2) {}

        /**
         * 双击时触发，position 为点击的位置，双击的时间间隔为 DOUBLE_TAP_INTERVAL。
         */
        fun onDoubleTap(position: Vector2) {}

        /**
         * 单指拖动时触发。
         */
        fun onDrag(pointer: PointerDragInfo) {}

        /**
         * 双指拖动时触发。
         */
        fun onDoubleDrag(pointer1: PointerDragInfo, pointer2: PointerDragInfo) {}
    }

    private enum class PointerStatus {
        NONE,
        SINGLE_DOWN,
        DOUBLE_DOWN,
        DOUBLE_UP,
    }

    private var pointerStatus = PointerStatus.NONE

    // 处理双击逻辑
    private var firstDown = false
    private var firstDownTime = 0L

    private var pointer1Position = Vector2(0f, 0f)
    private var pointer1OriginPosition = Vector2(0f, 0f)

    private var pointer2Position = Vector2(0f, 0f)
    private var pointer2OriginPosition = Vector2(0f, 0f)

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v.performClick())
            return true

        when (event.action and ACTION_MASK) {
            ACTION_DOWN -> {
                pointerStatus = PointerStatus.SINGLE_DOWN

                pointer1Position = Vector2(event.x, event.y)
                pointer1OriginPosition = pointer1Position

                if (firstDown && event.eventTime - firstDownTime <= DOUBLE_TAP_INTERVAL) {
                    // 双击事件
                    firstDown = false
                    firstDownTime = event.eventTime
                    listener?.onDoubleTap(pointer1Position)

                } else {
                    // 单击事件
                    firstDown = true
                    firstDownTime = event.eventTime
                    listener?.onTap(pointer1Position)
                }
            }

            ACTION_POINTER_DOWN -> {
                // 处理第二个 pointer 按下，忽略更多的 pointer
                if (pointerStatus == PointerStatus.SINGLE_DOWN) {
                    pointerStatus = PointerStatus.DOUBLE_DOWN
                    pointer2Position = Vector2(event.getX(1), event.getY(1))
                    pointer2OriginPosition = pointer2Position
                }
            }

            ACTION_MOVE -> {
                // 处理单指和双指拖动事件
                if (pointerStatus == PointerStatus.SINGLE_DOWN || pointerStatus == PointerStatus.DOUBLE_UP) {
                    handleSingleDrag(Vector2(event.x, event.y))

                } else if (pointerStatus == PointerStatus.DOUBLE_DOWN) {
                    handleDoubleDrag(
                        Vector2(event.getX(0), event.getY(0)),
                        Vector2(event.getX(1), event.getY(1))
                    )
                }
            }

            ACTION_POINTER_UP -> {
                // 处理 pointer 抬起
                if (pointerStatus == PointerStatus.DOUBLE_DOWN) {
                    val actionIndex = event.actionIndex
                    if (actionIndex == 0 || actionIndex == 1) {
                        pointerStatus = PointerStatus.DOUBLE_UP
                    }
                    if (actionIndex == 0) {
                        pointer1Position = pointer2Position
                    }
                } else if (pointerStatus == PointerStatus.SINGLE_DOWN || pointerStatus == PointerStatus.DOUBLE_UP) {
                    pointerStatus = PointerStatus.NONE
                }
            }

            ACTION_UP -> {
                // 所有 pointer 全部抬起
                pointerStatus = PointerStatus.NONE
            }
        }
        return true
    }

    private fun handleSingleDrag(position: Vector2) {
        val info = PointerDragInfo(position, pointer1Position, pointer1OriginPosition)
        pointer1Position = position
        listener?.onDrag(info)
    }

    private fun handleDoubleDrag(position1: Vector2, position2: Vector2) {
        val info1 = PointerDragInfo(position1, pointer1Position, pointer1OriginPosition)
        val info2 = PointerDragInfo(position2, pointer2Position, pointer2OriginPosition)
        pointer1Position = position1
        pointer2Position = position2
        listener?.onDoubleDrag(info1, info2)
    }

    companion object {
        /**
         * 双击的时间间隔，单位为 ms。
         */
        const val DOUBLE_TAP_INTERVAL = 300L
    }
}
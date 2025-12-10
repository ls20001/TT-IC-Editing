package com.example.tticediting

import android.graphics.PointF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 角度弧度转换
 */
fun degToRad(deg: Float): Float {
    return deg / 180f * PI.toFloat()
}

fun radToDeg(rad: Float): Float {
    return rad / PI.toFloat() * 180f
}

/**
 * 带转角的矩形，旋转中心为矩形形心，通过形心、边长、转角定义
 */
data class RotRect(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f,
    var angle: Float = 0f
) {
    fun center(): PointF {
        return PointF(x, y)
    }
}

/**
 * 点绕 center 旋转
 */
fun rotatePoint(point: PointF, angle: Float, center: PointF = PointF()): PointF {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val cosA = cos(degToRad(angle))
    val sinA = sin(degToRad(angle))
    return PointF(
        center.x + dx * cosA - dy * sinA,
        center.y + dx * sinA + dy * cosA,
    )
}

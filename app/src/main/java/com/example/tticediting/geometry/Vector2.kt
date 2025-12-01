package com.example.tticediting.geometry

import kotlin.math.sqrt

/**
 * 二维向量。
 */
data class Vector2(var x: Float, var y: Float) {
    operator fun plusAssign(v: Vector2) {
        x += v.x
        y += v.y
    }

    operator fun minusAssign(v: Vector2) {
        x -= v.x
        y -= v.y
    }

    operator fun timesAssign(v: Vector2) {
        x *= v.x
        y *= v.y
    }

    operator fun divAssign(v: Vector2) {
        x /= v.x
        y /= v.y
    }
}

operator fun Vector2.unaryPlus() = Vector2(x, y)
operator fun Vector2.unaryMinus() = Vector2(-x, -y)

infix operator fun Vector2.plus(v: Vector2) = Vector2(x + v.x, y + v.y)
infix operator fun Vector2.minus(v: Vector2) = Vector2(x - v.x, y - v.y)
infix operator fun Vector2.times(v: Vector2) = Vector2(x * v.x, y * v.y)
infix operator fun Vector2.div(v: Vector2) = Vector2(x / v.x, y / v.y)

infix operator fun Vector2.plus(v: Float) = Vector2(x + v, y + v)
infix operator fun Vector2.minus(v: Float) = Vector2(x - v, y - v)
infix operator fun Vector2.times(v: Float) = Vector2(x * v, y * v)
infix operator fun Vector2.div(v: Float) = Vector2(x / v, y / v)

infix operator fun Float.plus(v: Vector2) = Vector2(this + v.x, this + v.y)
infix operator fun Float.minus(v: Vector2) = Vector2(this - v.x, this - v.y)
infix operator fun Float.times(v: Vector2) = Vector2(this * v.x, this * v.y)
infix operator fun Float.div(v: Vector2) = Vector2(this / v.x, this / v.y)

fun norm(v: Vector2) = sqrt(v.x * v.x + v.y * v.y)

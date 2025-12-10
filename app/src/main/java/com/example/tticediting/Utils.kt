package com.example.tticediting

/**
 * 生成0xAARRGGBB格式的颜色，忽略高位
 */
fun makeColorARGB(red: Int, green: Int, blue: Int, alpha: Int = 0xFF): Int {
    return ((alpha and 0xFF) shl 24) or ((red and 0xFF) shl 16) or ((green and 0xFF) shl 8) or (blue and 0xFF)
}

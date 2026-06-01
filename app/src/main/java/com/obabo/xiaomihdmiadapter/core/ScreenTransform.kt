package com.obabo.xiaomihdmiadapter.core

data class IntSize2(
    val width: Int,
    val height: Int
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
    }
}

data class StretchTransform(
    val scaleX: Float,
    val scaleY: Float,
    val outputWidth: Int,
    val outputHeight: Int,
    val preservesAspectRatio: Boolean
)

object ScreenTransform {
    fun stretchedFill(source: IntSize2, target: IntSize2): StretchTransform {
        return StretchTransform(
            scaleX = target.width.toFloat() / source.width.toFloat(),
            scaleY = target.height.toFloat() / source.height.toFloat(),
            outputWidth = target.width,
            outputHeight = target.height,
            preservesAspectRatio = false
        )
    }
}

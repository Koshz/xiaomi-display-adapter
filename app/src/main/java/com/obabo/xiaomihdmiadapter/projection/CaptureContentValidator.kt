package com.obabo.xiaomihdmiadapter.projection

object CaptureContentValidator {
    const val FULL_DISPLAY_REQUIRED_MESSAGE = "Full display capture required"
    private const val SUSPICIOUS_SIZE_RATIO = 0.92f

    fun isSuspiciouslySmaller(
        expected: CaptureSpec,
        actualWidth: Int,
        actualHeight: Int
    ): Boolean {
        if (actualWidth <= 0 || actualHeight <= 0) return false

        val widthTooSmall = actualWidth < expected.width * SUSPICIOUS_SIZE_RATIO
        val heightTooSmall = actualHeight < expected.height * SUSPICIOUS_SIZE_RATIO
        return widthTooSmall || heightTooSmall
    }
}

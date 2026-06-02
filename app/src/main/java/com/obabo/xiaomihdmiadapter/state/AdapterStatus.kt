package com.obabo.xiaomihdmiadapter.state

sealed interface AdapterStatus {
    data object Off : AdapterStatus
    data object MissingHdmi : AdapterStatus
    data object PreparingLandscape : AdapterStatus
    data object NeedsCapturePermission : AdapterStatus
    data object Starting : AdapterStatus

    data class On(
        val displayName: String,
        val expectedCaptureWidth: Int,
        val expectedCaptureHeight: Int,
        val actualCaptureWidth: Int? = null,
        val actualCaptureHeight: Int? = null
    ) : AdapterStatus

    data class Error(
        val message: String
    ) : AdapterStatus
}

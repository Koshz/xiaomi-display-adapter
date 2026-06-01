package com.obabo.xiaomihdmiadapter.state

sealed interface AdapterStatus {
    data object Off : AdapterStatus
    data object MissingHdmi : AdapterStatus
    data object NeedsCapturePermission : AdapterStatus
    data object Starting : AdapterStatus

    data class On(
        val displayName: String,
        val captureWidth: Int,
        val captureHeight: Int
    ) : AdapterStatus

    data class Error(
        val message: String
    ) : AdapterStatus
}

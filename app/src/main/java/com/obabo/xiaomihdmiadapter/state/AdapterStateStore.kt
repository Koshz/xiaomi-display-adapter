package com.obabo.xiaomihdmiadapter.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdapterStateStore {
    private val _status = MutableStateFlow<AdapterStatus>(AdapterStatus.Off)
    val status: StateFlow<AdapterStatus> = _status.asStateFlow()

    fun update(status: AdapterStatus) {
        _status.value = status
    }
}

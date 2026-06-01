package com.obabo.xiaomihdmiadapter.state

object AdapterStateReducer {
    fun reduce(current: AdapterStatus, event: ControllerEvent): AdapterStatus {
        return when (event) {
            ControllerEvent.HdmiMissing -> AdapterStatus.MissingHdmi
            ControllerEvent.PreparingLandscape -> AdapterStatus.PreparingLandscape
            ControllerEvent.CapturePermissionRequested -> AdapterStatus.NeedsCapturePermission
            ControllerEvent.PermissionDenied -> AdapterStatus.Error("Screen capture permission was denied")
            ControllerEvent.Starting -> AdapterStatus.Starting
            ControllerEvent.Started -> current
            ControllerEvent.Stopped -> AdapterStatus.Off
            is ControllerEvent.Failed -> AdapterStatus.Error(event.message)
        }
    }
}

sealed interface ControllerEvent {
    data object HdmiMissing : ControllerEvent
    data object PreparingLandscape : ControllerEvent
    data object CapturePermissionRequested : ControllerEvent
    data object PermissionDenied : ControllerEvent
    data object Starting : ControllerEvent
    data object Started : ControllerEvent
    data object Stopped : ControllerEvent
    data class Failed(val message: String) : ControllerEvent
}

package com.bonyad.healthplat.blesdk.model

enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED }

sealed class PairingState {
    object NotPaired : PairingState()
    object PairingRequested : PairingState()
    object Paired : PairingState()
    data class PairingFailed(val error: String) : PairingState()
}

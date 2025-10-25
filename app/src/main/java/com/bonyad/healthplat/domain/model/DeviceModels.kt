package com.bonyad.healthplat.domain.model

data class RegisterDeviceRequest(
    val deviceMac: String,
    val deviceName: String? = null,
    val deviceType: String = "ring"
)

data class RegisterDeviceResponse(
    val success: Boolean,
    val deviceId: String? = null
)

data class DeviceInfoResponse(
    val success: Boolean,
    val data: DeviceData? = null
)

data class DeviceData(
    val deviceId: String,
    val deviceMac: String,
    val firmwareVersion: String? = null,
    val batteryLevel: Int? = null
)
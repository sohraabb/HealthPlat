package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    @SerialName("UserId")
    val userId: String,
    @SerialName("DeviceMac")
    val deviceMac: String,
    @SerialName("DeviceName")
    val deviceName: String? = null,
    @SerialName("DeviceType")
    val deviceType: String = "ring",
    @SerialName("FirmwareVersion")
    val firmwareVersion: String? = null,
    @SerialName("BatteryLevel")
    val batteryLevel: Int? = null,
    @SerialName("IsActive")
    val isActive: Boolean = true
)

@Serializable
data class UpdateDeviceRequest(
    @SerialName("Id")
    val id: Int,
    @SerialName("DeviceName")
    val deviceName: String? = null,
    @SerialName("DeviceType")
    val deviceType: String? = null,
    @SerialName("FirmwareVersion")
    val firmwareVersion: String? = null,
    @SerialName("BatteryLevel")
    val batteryLevel: Int? = null,
    @SerialName("IsActive")
    val isActive: Boolean? = null
)

@Serializable
data class AddUserDeviceRequest(
    @SerialName("UserId")
    val userId: String,
    @SerialName("DeviceMac")
    val deviceMac: String,
    @SerialName("DeviceName")
    val deviceName: String?,
    @SerialName("DeviceType")
    val deviceType: String = "ring",
    @SerialName("FirmwareVersion")
    val firmwareVersion: String?,
    @SerialName("IsActive")
    val isActive: Boolean = true
)

// Legacy model


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
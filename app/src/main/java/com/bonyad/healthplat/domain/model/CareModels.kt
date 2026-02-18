package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============ Request Models ============

@Serializable
data class AddCaregiverRequest(
    @SerialName("PhoneNumber")
    val phoneNumber: String,
    @SerialName("HeartRate")
    val heartRate: Boolean,
    @SerialName("BloodPressure")
    val bloodPressure: Boolean,
    @SerialName("StressLevel")
    val stressLevel: Boolean,
    @SerialName("SleepQuality")
    val sleepQuality: Boolean
)

@Serializable
data class AddCaregiverByScanRequest(
    @SerialName("UserId")
    val userId: String,
    @SerialName("HeartRate")
    val heartRate: Boolean,
    @SerialName("BloodPressure")
    val bloodPressure: Boolean,
    @SerialName("StressLevel")
    val stressLevel: Boolean,
    @SerialName("SleepQuality")
    val sleepQuality: Boolean
)

@Serializable
data class UpdateCaregiverPermissionsRequest(
    @SerialName("HeartRate")
    val heartRate: Boolean,
    @SerialName("BloodPressure")
    val bloodPressure: Boolean,
    @SerialName("StressLevel")
    val stressLevel: Boolean,
    @SerialName("SleepQuality")
    val sleepQuality: Boolean
)

// ============ Response Models ============

@Serializable
data class CaregiverData(
    @SerialName("Id")
    val id: Int,
    @SerialName("PatientId")
    val patientId: String?,
    @SerialName("IsAccepted")
    val isAccepted: Boolean,
    @SerialName("PatientPhoneNumber")
    val patientPhoneNumber: String?,
    @SerialName("PatientName")
    val patientName: String?,
    @SerialName("CaregiverId")
    val caregiverId: String?,
    @SerialName("CaregiverName")
    val caregiverName: String?,
    @SerialName("CaregiverPhoneNumber")
    val caregiverPhoneNumber: String?,
    @SerialName("HeartRate")
    val heartRate: Boolean,
    @SerialName("BloodPressure")
    val bloodPressure: Boolean,
    @SerialName("StressLevel")
    val stressLevel: Boolean,
    @SerialName("SleepQuality")
    val sleepQuality: Boolean,
    @SerialName("CreatedDate")
    val createdDate: String
)

// ============ UI Models ============


data class CaregiverUiModel(
    val id: Int,
    val name: String?,
    val phoneNumber: String,
    val userId: String?,
    val patientId: String?,
    val isPending: Boolean,
    val permissions: CarePermissions
)

data class CarePermissions(
    val heartRate: Boolean = true,
    val bloodPressure: Boolean = true,
    val stressLevel: Boolean = false,
    val sleepQuality: Boolean = false
)

// ============ QR Code Model ============

@Serializable
data class CareQrCodeData(
    @SerialName("userId")
    val userId: String,
    @SerialName("userName")
    val userName: String,
    @SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
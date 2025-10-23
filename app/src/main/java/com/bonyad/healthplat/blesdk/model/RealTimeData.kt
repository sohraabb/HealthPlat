package com.bonyad.healthplat.blesdk.model

data class RealTimeData(
    val heart: Int? = null,
    val step: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)
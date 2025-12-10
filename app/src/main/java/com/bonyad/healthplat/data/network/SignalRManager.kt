package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.TransportEnum
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Singleton
class SignalRManager @Inject constructor(
    private val userPreferences: UserPreferencesDataStore
) {

    private var hubConnection: HubConnection? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    suspend fun connect() {
        val token = userPreferences.getAuthToken().first()
        if (token.isNullOrEmpty()) {
            Timber.e("❌ No access token")
            return
        }

        hubConnection = HubConnectionBuilder
            .create("http://192.168.18.165:7005/hubs")
            .withAccessTokenProvider(Single.defer { Single.just(token) })
            .withTransport(TransportEnum.ALL)
            .build()

        hubConnection?.onClosed {
            _isConnected.value = false
            Timber.e("🔌 SignalR closed: $it")
            reconnect()
        }

        try {
            hubConnection?.start()?.blockingAwait()
            _isConnected.value = true
            Timber.i("✅ SignalR connected!")
        } catch (e: Exception) {
            Timber.e("❌ SignalR connect error: ${e.message}")
            reconnect()
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun sendHeartRate(deviceId: Int, value: Int?) {
        if (_isConnected.value.not()) {
            Timber.e("⚠️ Cannot send HR: not connected")
            return
        }

        val metric = mapOf(
            "DeviceId" to deviceId,
            "MetricType" to 1,        // heart rate type
            "Timestamp" to Clock.System.now().toString(),
            "Value" to value
        )

        try {
            hubConnection?.send("SendMetric", metric)
            Timber.i("📤 Sent HR → $value")
        } catch (e: Exception) {
            Timber.e("❌ Failed to send HR: ${e.message}")
        }
    }

    private fun reconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(4000)
            connect()
        }
    }

    fun disconnect() {
        hubConnection?.stop()
        _isConnected.value = false
    }
}
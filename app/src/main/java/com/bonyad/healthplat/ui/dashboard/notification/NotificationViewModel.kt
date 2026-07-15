package com.bonyad.healthplat.ui.dashboard.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.domain.model.NotificationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.bonyad.healthplat.ui.utils.rtl
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    // TODO: Inject NotificationRepository when backend is ready
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // TODO: Replace with actual API call
                // val notifications = notificationRepository.getNotifications()

                // Mock data for now
                val mockNotifications = listOf(
                    NotificationItem(
                        id = 1,
                        title = "بروزرسانی اپلیکیشن تن‌یار",
                        description = "در نسخه جدید شما میتوانید، با فعال کردن قابلیت‌های جدید، تجربه بهتری داشته باشید.".rtl(),
                        date = "۱۴۰۴/۰۶/۲۰"
                    )
                )

                _uiState.value = _uiState.value.copy(
                    notifications = mockNotifications,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "خطا در دریافت اعلان‌ها"
                )
            }
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            val updatedNotifications = _uiState.value.notifications.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
            _uiState.value = _uiState.value.copy(notifications = updatedNotifications)

            // TODO: Call API to mark notification as read
            // notificationRepository.markAsRead(notificationId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
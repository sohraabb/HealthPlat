package com.bonyad.healthplat.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.bonyad.healthplat.data.local.UserPreferencesDataStore.PreferencesKeys.AUTH_TOKEN
import com.bonyad.healthplat.data.local.UserPreferencesDataStore.PreferencesKeys.REFRESH_TOKEN
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    private object PreferencesKeys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val ACCESS_TOKEN_EXPIRY = longPreferencesKey("access_token_expiry")
        val USER_ID = stringPreferencesKey("user_id")
        val PHONE_NUMBER = stringPreferencesKey("phone_number")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
        val MARKETING_ACCEPTED = booleanPreferencesKey("marketing_accepted")
        val DEVICE_MAC = stringPreferencesKey("device_mac")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val DEVICE_ID = intPreferencesKey("device_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_BIRTH_DATE = stringPreferencesKey("user_birth_date")
        val USER_HEIGHT = intPreferencesKey("user_height")
        val USER_WEIGHT = intPreferencesKey("user_weight")
        val USER_GENDER = stringPreferencesKey("user_gender")

    }

    // ============ Auth Token ============

    suspend fun saveAuthToken(token: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTH_TOKEN] = token
            }
            Timber.d("💾 Auth token saved: ${token.take(30)}...")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save auth token")
        }
    }

    fun getAuthToken(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading auth token")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val token = preferences[PreferencesKeys.AUTH_TOKEN]
                if (token != null) {
                    Timber.d("📖 Read auth token: ${token.take(30)}...")
                } else {
                    Timber.w("📖 No auth token found in storage")
                }
                token
            }
    }

    // ============ Refresh Token ============

    suspend fun saveRefreshToken(token: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.REFRESH_TOKEN] = token
            }
            Timber.d("💾 Refresh token saved: ${token.take(30)}...")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save refresh token")
        }
    }

    fun getRefreshToken(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading refresh token")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val token = preferences[PreferencesKeys.REFRESH_TOKEN]
                if (token != null) {
                    Timber.d("📖 Read refresh token: ${token.take(30)}...")
                } else {
                    Timber.w("📖 No refresh token found in storage")
                }
                token
            }
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expDate: String
    ) {
        val expiryMillis = OffsetDateTime.parse(expDate).toInstant().toEpochMilli()

        dataStore.edit {
            it[PreferencesKeys.AUTH_TOKEN] = accessToken
            it[PreferencesKeys.REFRESH_TOKEN] = refreshToken
            it[PreferencesKeys.ACCESS_TOKEN_EXPIRY] = expiryMillis
        }
    }

    fun getAccessTokenExpiry(): Flow<Long?> =
        dataStore.data.map { it[PreferencesKeys.ACCESS_TOKEN_EXPIRY] }

    // ============ User ID ============

    suspend fun saveUserId(userId: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_ID] = userId
            }
            Timber.d("💾 User ID saved: $userId")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save user ID")
        }
    }

    fun getUserId(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading user ID")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.USER_ID]
            }
    }

    // ============ Phone Number ============

    suspend fun savePhoneNumber(phone: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.PHONE_NUMBER] = phone
            }
            Timber.d("💾 Phone number saved: $phone")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save phone number")
        }
    }

    fun getPhoneNumber(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading phone number")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.PHONE_NUMBER]
            }
    }

    // ============ Onboarding ============

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETE] = complete
        }
    }

    fun isOnboardingComplete(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading onboarding status")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.ONBOARDING_COMPLETE] ?: false
            }
    }

    // ============ Terms & Privacy ============

    suspend fun setTermsAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.TERMS_ACCEPTED] = accepted
        }
    }

    fun isTermsAccepted(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading terms status")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.TERMS_ACCEPTED] ?: false
            }
    }

    suspend fun setMarketingAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MARKETING_ACCEPTED] = accepted
        }
    }

    fun isMarketingAccepted(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading marketing status")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.MARKETING_ACCEPTED] ?: false
            }
    }

    // ============ Personal Info ============

    suspend fun savePersonalInfo(name: String, birthDate: String, height: Int, weight: Int, gender: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_NAME] = name
                preferences[PreferencesKeys.USER_BIRTH_DATE] = birthDate
                preferences[PreferencesKeys.USER_HEIGHT] = height
                preferences[PreferencesKeys.USER_WEIGHT] = weight
                preferences[PreferencesKeys.USER_GENDER] = gender
            }
            Timber.d("💾 Personal info saved: $name")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save personal info")
        }
    }

    fun getUserName(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.USER_NAME]
            }
    }

    fun getUserBirthDate(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.USER_BIRTH_DATE]
        }
    }

    fun getUserHeight(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.USER_HEIGHT]
        }
    }

    fun getUserWeight(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.USER_WEIGHT]
        }
    }

    // ============ Device Info ============

    suspend fun saveDeviceInfo(mac: String, name: String?) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DEVICE_MAC] = mac
                if (name != null) {
                    preferences[PreferencesKeys.DEVICE_NAME] = name
                }
            }
            Timber.d("💾 Device info saved: $mac")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save device info")
        }
    }

    fun getDeviceMac(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading device MAC")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.DEVICE_MAC]
            }
    }

    suspend fun saveDeviceId(id: Int) {
        try {
            dataStore.edit { it[PreferencesKeys.DEVICE_ID] = id }
            Timber.d("💾 Device ID saved: $id")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save device ID")
        }
    }

    fun getDeviceId(): Flow<Int?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { it[PreferencesKeys.DEVICE_ID] }

    // ============ Clear All ============

    suspend fun clearAll() {
        try {
            dataStore.edit { preferences ->
                preferences.clear()
            }
            Timber.i("🗑️ All preferences cleared")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to clear all preferences")
        }
    }

    suspend fun clearAuthOnly() {
        try {
            dataStore.edit {
                it.remove(PreferencesKeys.AUTH_TOKEN)
                it.remove(PreferencesKeys.REFRESH_TOKEN)
            }
            Timber.i("🗑️ Auth tokens cleared (keeping user data)")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to clear auth tokens")
        }
    }
}
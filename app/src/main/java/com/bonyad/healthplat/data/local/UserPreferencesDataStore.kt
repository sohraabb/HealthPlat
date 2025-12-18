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
        val USER_ID = stringPreferencesKey("user_id")
        val PHONE_NUMBER = stringPreferencesKey("phone_number")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
        val MARKETING_ACCEPTED = booleanPreferencesKey("marketing_accepted")
        val DEVICE_MAC = stringPreferencesKey("device_mac")
        val DEVICE_NAME = stringPreferencesKey("device_name")
        val DEVICE_ID = intPreferencesKey("device_id")
    }

    // ============ Auth Token ============

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] = token
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
                val token = preferences[AUTH_TOKEN]
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
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REFRESH_TOKEN] = token
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
                val token = preferences[REFRESH_TOKEN]
                if (token != null) {
                    Timber.d("📖 Read refresh token: ${token.take(30)}...")
                } else {
                    Timber.w("📖 No refresh token found in storage")
                }
                token
            }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTH_TOKEN] = accessToken
                preferences[PreferencesKeys.REFRESH_TOKEN] = refreshToken
            }
            Timber.d("💾 Both tokens saved atomically")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save tokens")
        }
    }

    // ============ User ID ============

    suspend fun saveUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
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
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PHONE_NUMBER] = phone
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
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("user_name")] = name
            preferences[stringPreferencesKey("user_birth_date")] = birthDate
            preferences[intPreferencesKey("user_height")] = height
            preferences[intPreferencesKey("user_weight")] = weight
            preferences[stringPreferencesKey("user_gender")] = gender
        }
    }

    fun getUserName(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("user_name")]
        }
    }


    fun getUserBirthDate(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("user_birth_date")]
        }
    }

    fun getUserHeight(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[intPreferencesKey("user_height")]
        }
    }

    fun getUserWeight(): Flow<Int?> {
        return dataStore.data.map { preferences ->
            preferences[intPreferencesKey("user_weight")]
        }
    }

    // ============ Device Info ============

    suspend fun saveDeviceInfo(mac: String, name: String?) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEVICE_MAC] = mac
            if (name != null) {
                preferences[PreferencesKeys.DEVICE_NAME] = name
            }
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
        dataStore.edit { it[PreferencesKeys.DEVICE_ID] = id }
    }

    fun getDeviceId(): Flow<Int?> = dataStore.data.map { it[PreferencesKeys.DEVICE_ID] }

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
                it.remove(AUTH_TOKEN)
                it.remove(REFRESH_TOKEN)
            }
            Timber.i("🗑️ Auth tokens cleared (keeping user data)")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to clear auth tokens")
        }
    }
}
package com.bonyad.healthplat.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
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
        val USER_LAST_NAME = stringPreferencesKey("user_last_name")
        val USER_BIRTH_DATE = stringPreferencesKey("user_birth_date")
        val USER_HEIGHT = intPreferencesKey("user_height")
        val USER_WEIGHT = intPreferencesKey("user_weight")
        val USER_GENDER = stringPreferencesKey("user_gender")
        val USER_NATIONAL_CODE = stringPreferencesKey("user_national_code")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_DISEASE_IDS = stringPreferencesKey("user_disease_ids") // Stored as comma-separated string
        val LAST_SYNC_DATE = stringPreferencesKey("last_sync_date")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_SYNC_SERVER_TIME = stringPreferencesKey("last_sync_server_time")
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

    suspend fun savePersonalInfo(
        name: String,
        lastName: String,
        birthDate: String,
        height: Int,
        weight: Int,
        gender: String,
        nationalCode: String? = null,
        email: String? = null,
        diseaseIds: List<Int> = emptyList()
    ) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_NAME] = name
                preferences[PreferencesKeys.USER_LAST_NAME] = lastName
                preferences[PreferencesKeys.USER_BIRTH_DATE] = birthDate
                preferences[PreferencesKeys.USER_HEIGHT] = height
                preferences[PreferencesKeys.USER_WEIGHT] = weight
                preferences[PreferencesKeys.USER_GENDER] = gender
                nationalCode?.let { preferences[PreferencesKeys.USER_NATIONAL_CODE] = it }
                email?.let { preferences[PreferencesKeys.USER_EMAIL] = it }
                preferences[PreferencesKeys.USER_DISEASE_IDS] = diseaseIds.joinToString(",")
            }
            Timber.d("💾 Personal info saved: $name $lastName")
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

    fun getUserLastName(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.USER_LAST_NAME]
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

    fun getUserGender(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.USER_GENDER]
            }
    }

    fun getUserNationalCode(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.USER_NATIONAL_CODE]
            }
    }

    fun getUserEmail(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.USER_EMAIL]
            }
    }

    fun getUserDiseaseIds(): Flow<List<Int>> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val idsString = preferences[PreferencesKeys.USER_DISEASE_IDS]
                if (idsString.isNullOrEmpty()) {
                    emptyList()
                } else {
                    idsString.split(",").mapNotNull { it.toIntOrNull() }
                }
            }
    }

    suspend fun saveUserDiseaseIds(diseaseIds: List<Int>) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_DISEASE_IDS] = diseaseIds.joinToString(",")
            }
            Timber.d("💾 Disease IDs saved: $diseaseIds")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save disease IDs")
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

    // ============ Last Sync Date ============

    /**
     * Save the last successful sync date.
     * @param date Format: yyyy-MM-dd — used for gap calculation in syncAllMissingDays()
     * @param serverTime The server's LastSyncedTime string — used for UI display. Falls back to local time if null.
     */
    suspend fun saveLastSyncDate(date: String, serverTime: String? = null) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LAST_SYNC_DATE] = date
                preferences[PreferencesKeys.LAST_SYNC_SERVER_TIME] = serverTime ?: ""
                preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
            }
            Timber.d("💾 Last sync date saved: $date (server time: $serverTime)")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save last sync date")
        }
    }

    /**
     * Get the last successful sync date
     * @return Flow of date string (yyyy-MM-dd) or null if never synced
     */
    fun getLastSyncDate(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading last sync date")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.LAST_SYNC_DATE]
            }
    }

    /**
     * Get the last successful sync timestamp (epoch millis, local fallback)
     */
    fun getLastSyncTimestamp(): Flow<Long?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading last sync timestamp")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP]
            }
    }

    /**
     * Get the server-provided LastSyncedTime string
     * @return Flow of server time string or null if not available
     */
    fun getLastSyncServerTime(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading last sync server time")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.LAST_SYNC_SERVER_TIME]?.ifEmpty { null }
            }
    }

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

    /**
     * Clears all account-specific data on logout.
     * Preserves ONBOARDING_COMPLETE and TERMS_ACCEPTED since those are app-level,
     * not account-level — a new user on the same device shouldn't have to redo them.
     */
    suspend fun clearForLogout() {
        try {
            dataStore.edit {
                it.remove(PreferencesKeys.AUTH_TOKEN)
                it.remove(PreferencesKeys.REFRESH_TOKEN)
                it.remove(PreferencesKeys.ACCESS_TOKEN_EXPIRY)
                it.remove(PreferencesKeys.USER_ID)
                it.remove(PreferencesKeys.PHONE_NUMBER)
                it.remove(PreferencesKeys.USER_NAME)
                it.remove(PreferencesKeys.USER_LAST_NAME)
                it.remove(PreferencesKeys.USER_BIRTH_DATE)
                it.remove(PreferencesKeys.USER_HEIGHT)
                it.remove(PreferencesKeys.USER_WEIGHT)
                it.remove(PreferencesKeys.USER_GENDER)
                it.remove(PreferencesKeys.USER_NATIONAL_CODE)
                it.remove(PreferencesKeys.USER_EMAIL)
                it.remove(PreferencesKeys.USER_DISEASE_IDS)
                it.remove(PreferencesKeys.DEVICE_MAC)
                it.remove(PreferencesKeys.DEVICE_NAME)
                it.remove(PreferencesKeys.DEVICE_ID)
                it.remove(PreferencesKeys.LAST_SYNC_DATE)
                // ONBOARDING_COMPLETE and TERMS_ACCEPTED are intentionally kept
            }
            Timber.i("🗑️ All account data cleared on logout")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to clear account data on logout")
        }
    }
}
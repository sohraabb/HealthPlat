package com.bonyad.healthplat.di

import android.content.Context
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.AIAnalysisApiService
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.data.network.TokenAuthenticator
import com.bonyad.healthplat.data.network.TokenManager
import com.inuker.bluetooth.library.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val MAIN_BASE_URL = "http://192.168.18.165:7005/api/"
    private const val AI_BASE_URL = "http://192.168.18.165:8002/" // Removed 'api' based on docs example, adjust if needed

    // =================================================================
    // 1. Utilities (Json, Logging, Prefs)
    // =================================================================

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferencesDataStore {
        return UserPreferencesDataStore(context)
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    // =================================================================
    // 2. Authentication Components (Interceptor & Authenticator)
    // =================================================================

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        userPreferences: UserPreferencesDataStore
    ): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val path = originalRequest.url.encodedPath

            // Define endpoints that do not need a Token
            val isPublicEndpoint = path.contains("/Auth/login", true) ||
                    path.contains("/Auth/register", true) ||
                    path.contains("/Auth/requestPhoneVerification", true) ||
                    path.contains("/Auth/verifyPhone", true) ||
                    path.contains("/Auth/refresh", true)

            if (isPublicEndpoint) {
                return@Interceptor chain.proceed(originalRequest)
            }

            // Retrieve Token
            val token = runBlocking {
                userPreferences.getAuthToken().first()
            }

            val newRequest = if (!token.isNullOrEmpty()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                // If no token, proceed (might fail with 401, handled by Authenticator or UI)
                originalRequest
            }

            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    fun provideTokenManager(
        userPreferences: UserPreferencesDataStore,
        @Named("RefreshApiService") apiService: HealthPlatApiService
    ): TokenManager {
        return TokenManager(userPreferences, apiService)
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenManager: TokenManager,
        userPreferences: UserPreferencesDataStore
    ): TokenAuthenticator {
        return TokenAuthenticator(tokenManager, userPreferences)
    }

    // =================================================================
    // 3. HTTP Clients
    // =================================================================

    /**
     * Main Client: Used for general API calls.
     * Includes: Logging, Auth Interceptor (adds token), Token Authenticator (refreshes token).
     */
    @Provides
    @Singleton
    @Named("MainOkHttpClient")
    fun provideMainOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Refresh Client: Used specifically for the Token Refresh API call.
     * Includes: Logging only.
     * EXCLUDES: AuthInterceptor (prevents circular loops).
     */
    @Provides
    @Singleton
    @Named("RefreshOkHttp")
    fun provideRefreshOkHttp(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // =================================================================
    // 4. Retrofit Instances
    // =================================================================

    @Provides
    @Singleton
    @Named("MainRetrofit")
    fun provideMainRetrofit(
        @Named("MainOkHttpClient") okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(MAIN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @Named("AIRetrofit")
    fun provideAIRetrofit(
        @Named("MainOkHttpClient") okHttpClient: OkHttpClient, // Uses main client to support Auth if needed
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @Named("RefreshRetrofit")
    fun provideRefreshRetrofit(
        @Named("RefreshOkHttp") okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(MAIN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    // =================================================================
    // 5. API Services
    // =================================================================

    @Provides
    @Singleton
    fun provideHealthPlatApiService(
        @Named("MainRetrofit") retrofit: Retrofit
    ): HealthPlatApiService {
        return retrofit.create(HealthPlatApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("RefreshApiService")
    fun provideRefreshApiService(
        @Named("RefreshRetrofit") retrofit: Retrofit
    ): HealthPlatApiService {
        return retrofit.create(HealthPlatApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAIApiService(
        @Named("AIRetrofit") retrofit: Retrofit
    ): AIAnalysisApiService {
        return retrofit.create(AIAnalysisApiService::class.java)
    }
}
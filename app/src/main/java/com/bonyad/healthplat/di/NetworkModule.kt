package com.bonyad.healthplat.di

import android.content.Context
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.data.network.TokenAuthenticator
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

    private const val BASE_URL = "http://192.168.18.165:7005/api/"

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferencesDataStore {
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
    fun provideAuthInterceptor(
        userPreferences: UserPreferencesDataStore
    ): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val path = originalRequest.url.encodedPath

            val isAuthEndpoint =
                path.contains("/Auth/login", true) ||
                        path.contains("/Auth/register", true) ||
                        path.contains("/Auth/requestPhoneVerification", true) ||
                        path.contains("/Auth/verifyPhone", true) ||
                        path.contains("/Auth/refresh", true)

            if (isAuthEndpoint) {
                Timber.d("🔓 Auth endpoint: $path")
                return@Interceptor chain.proceed(
                    originalRequest.newBuilder()
                        .header("accept", "*/*")
                        .build()
                )
            }

            // ✅ Add detailed logging
            val token = runBlocking {
                val t = userPreferences.getAuthToken().first()
                Timber.d("🔍 Auth Interceptor checking token for: $path")
                Timber.d("   Token exists: ${t != null}")
                Timber.d("   Token value: ${t?.take(30)}...")
                t
            }

            val newRequest = if (!token.isNullOrEmpty()) {
                Timber.d("🔑 Adding Authorization header for: $path")
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("accept", "*/*")
                    .build()
            } else {
                Timber.w("⚠️ No token available for: $path")
                originalRequest.newBuilder()
                    .header("accept", "*/*")
                    .build()
            }

            val response = chain.proceed(newRequest)

            if (response.code == 401) {
                Timber.e("🚫 401 Unauthorized for: $path")
            }

            response
        }
    }

    @Provides
    @Singleton
    @Named("AuthRetrofit")
    fun provideAuthRetrofit(
        json: Json,
        loggingInterceptor: HttpLoggingInterceptor
    ): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        userPreferences: UserPreferencesDataStore,
        @Named("RefreshApiService") apiService: HealthPlatApiService
    ): TokenAuthenticator {
        return TokenAuthenticator(userPreferences, apiService)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
        tokenAuthenticator: TokenAuthenticator // Injected here
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator) // ✅ Attached here
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideHealthPlatApiService(retrofit: Retrofit): HealthPlatApiService {
        return retrofit.create(HealthPlatApiService::class.java)
    }

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


    @Provides
    @Singleton
    @Named("RefreshRetrofit")
    fun provideRefreshRetrofit(
        @Named("RefreshOkHttp") okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
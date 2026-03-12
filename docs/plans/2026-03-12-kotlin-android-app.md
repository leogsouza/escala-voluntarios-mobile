# Kotlin Android App Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers/executing-plans to implement this plan task-by-task.

**Goal:** Replace the React Native codebase with a native Kotlin Android app that authenticates against the Go backend and provides calendar, event detail, and restrictions CRUD screens.

**Architecture:** Single-Activity app using Jetpack Compose for UI, Navigation Compose for routing, Retrofit + OkHttp for networking, and EncryptedSharedPreferences for secure token storage. MVVM pattern with ViewModels and StateFlow. Hilt for dependency injection.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Retrofit 2, OkHttp 3, Hilt, EncryptedSharedPreferences, Kotlin Coroutines, Material 3

---

## Pre-requisites

- Android Studio Hedgehog (2023.1) or newer
- Android SDK 34 (compileSdk), minSdk 26
- The Go backend running at a configurable base URL (set in `local.properties` or `BuildConfig`)

---

## Task 1: Wipe React Native code, scaffold Kotlin project

**Files:**
- Delete: everything except `.git/`, `docs/`, `README.md`
- Create: standard Android Gradle project structure (see steps below)

**Step 1: Nuke React Native files**

```bash
cd ~/projects/escala-voluntarios-mobile
# Remove all RN artifacts, keep git + docs
find . -maxdepth 1 ! -name '.' ! -name '.git' ! -name 'docs' ! -name 'README.md' -exec rm -rf {} +
```

**Step 2: Scaffold the Android project structure**

Create the following directory tree:

```
app/
  src/
    main/
      java/br/com/leonardogsouza/escalav/
        di/
        data/
          remote/
            api/
            dto/
          local/
        domain/
          model/
        ui/
          auth/
          schedule/
          restrictions/
          components/
          theme/
      res/
        values/
        drawable/
      AndroidManifest.xml
  build.gradle.kts
build.gradle.kts
settings.gradle.kts
gradle.properties
gradle/
  wrapper/
    gradle-wrapper.properties
local.properties   (gitignored - contains API_BASE_URL)
```

**Step 3: Write `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "EscalaVoluntarios"
include(":app")
```

**Step 4: Write root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

**Step 5: Write `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.21"
coreKtx = "1.13.1"
lifecycleRuntime = "2.8.6"
activityCompose = "1.9.2"
composeBom = "2024.09.03"
navigationCompose = "2.8.1"
hilt = "2.52"
hiltNavigationCompose = "1.2.0"
retrofit = "2.11.0"
okhttp = "4.12.0"
moshi = "1.15.1"
securityCrypto = "1.1.0-alpha06"
ksp = "2.0.21-1.0.25"
coroutines = "1.8.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntime" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntime" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-moshi = { group = "com.squareup.retrofit2", name = "converter-moshi", version.ref = "retrofit" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
moshi-kotlin = { group = "com.squareup.moshi", name = "moshi-kotlin", version.ref = "moshi" }
moshi-codegen = { group = "com.squareup.moshi", name = "moshi-kotlin-codegen", version.ref = "moshi" }
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

**Step 6: Write `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "br.com.leonardogsouza.escalav"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.leonardogsouza.escalav"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Read API base URL from local.properties
        val localProps = com.android.build.gradle.internal.cxx.configure.gradleLocalProperties(rootDir, providers)
        buildConfigField("String", "API_BASE_URL", "\"${localProps.getProperty("API_BASE_URL", "http://10.0.2.2:8080")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)
    implementation(libs.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.ui.tooling)
}
```

**Step 7: Write `gradle/wrapper/gradle-wrapper.properties`**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

**Step 8: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

**Step 9: Write `local.properties`** (gitignored)

```properties
sdk.dir=/path/to/your/Android/Sdk
API_BASE_URL=http://10.0.2.2:8080
```

> `10.0.2.2` is the Android emulator loopback to the host machine. For a physical device on the same network, use the host's LAN IP.

**Step 10: Add `local.properties` to `.gitignore`**

Create `.gitignore`:
```
local.properties
*.jks
.gradle/
build/
.idea/
*.iml
```

**Step 11: Write `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:name=".EscalaApp"
        android:allowBackup="true"
        android:label="Escala Voluntários"
        android:theme="@style/Theme.EscalaVoluntarios"
        android:usesCleartextTraffic="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

> `usesCleartextTraffic="true"` allows HTTP to the dev backend. Remove for production.

**Step 12: Commit**

```bash
git add -A
git commit -m "chore: replace React Native with Kotlin Android project scaffold"
```

---

## Task 2: Data models + Retrofit API interface

**Files:**
- Create: `app/src/main/java/br/com/leonardogsouza/escalav/data/remote/dto/`
- Create: `app/src/main/java/br/com/leonardogsouza/escalav/data/remote/api/ApiService.kt`

**Step 1: Create DTOs**

`AuthDto.kt`:
```kotlin
package br.com.leonardogsouza.escalav.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthTokens(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshRequest(
    @Json(name = "refresh_token") val refreshToken: String
)
```

`ScheduleDto.kt`:
```kotlin
package br.com.leonardogsouza.escalav.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScheduleDto(
    val id: Int,
    val name: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String,
    val status: String,
    @Json(name = "church_id") val churchId: Int
)

@JsonClass(generateAdapter = true)
data class EventDto(
    val id: Int,
    val date: String,
    val weekday: String,
    val service: String,
    @Json(name = "service_code") val serviceCode: String,
    val time: String,
    @Json(name = "template_id") val templateId: Int?,
    @Json(name = "schedule_id") val scheduleId: Int?,
    @Json(name = "is_special") val isSpecial: Boolean,
    val notes: String
)

@JsonClass(generateAdapter = true)
data class RoleDto(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class PositionDto(
    val id: Int,
    val name: String,
    @Json(name = "role_id") val roleId: Int,
    val role: RoleDto?
)

@JsonClass(generateAdapter = true)
data class VolunteerDto(
    val id: Int,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val active: Boolean?,
    @Json(name = "main_role") val mainRole: RoleDto?,
    @Json(name = "secondary_role") val secondaryRole: RoleDto?
)

@JsonClass(generateAdapter = true)
data class AssignmentDto(
    val id: Int,
    @Json(name = "event_id") val eventId: Int,
    @Json(name = "volunteer_id") val volunteerId: Int,
    @Json(name = "position_id") val positionId: Int,
    val status: String,
    val event: EventDto?,
    val position: PositionDto?,
    val volunteer: VolunteerDto?
)
```

`RestrictionDto.kt`:
```kotlin
package br.com.leonardogsouza.escalav.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RestrictionTypeDto(
    val id: Int,
    val name: String,
    val description: String?
)

@JsonClass(generateAdapter = true)
data class SpecificDateDto(
    val date: String,
    @Json(name = "positionID") val positionId: Int?,
    val notes: String?
)

@JsonClass(generateAdapter = true)
data class DateRangeEntryDto(
    val start: String,
    val end: String,
    @Json(name = "positionID") val positionId: Int?
)

@JsonClass(generateAdapter = true)
data class RestrictionRulesDto(
    val mode: String,
    val operator: String?,
    val weekdays: List<Int>?,
    val periods: List<String>?,
    @Json(name = "specificDates") val specificDates: List<SpecificDateDto>?,
    @Json(name = "dateRanges") val dateRanges: List<DateRangeEntryDto>?
)

@JsonClass(generateAdapter = true)
data class RestrictionDto(
    val id: Int?,
    @Json(name = "volunteer_id") val volunteerId: Int,
    @Json(name = "schedule_id") val scheduleId: Int,
    val description: String,
    @Json(name = "restriction_type_id") val restrictionTypeId: Int,
    @Json(name = "restriction_type") val restrictionType: RestrictionTypeDto?,
    @Json(name = "rules_json") val rulesJson: String?,
    val active: Boolean?,
    val fixed: Boolean?
)

@JsonClass(generateAdapter = true)
data class PaginationInfoDto(
    val page: Int,
    @Json(name = "page_size") val pageSize: Int,
    @Json(name = "total_items") val totalItems: Int,
    @Json(name = "total_pages") val totalPages: Int
)

@JsonClass(generateAdapter = true)
data class PaginatedRestrictionsDto(
    val data: List<RestrictionDto>,
    val pagination: PaginationInfoDto
)
```

**Step 2: Create `ApiService.kt`**

```kotlin
package br.com.leonardogsouza.escalav.data.remote.api

import br.com.leonardogsouza.escalav.data.remote.dto.*
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("login")
    suspend fun login(@Body body: LoginRequest): AuthTokens

    @POST("refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthTokens

    // Schedules
    @GET("schedules/active")
    suspend fun getActiveSchedules(): List<ScheduleDto>

    // Events
    @GET("events/month/{year}/{month}")
    suspend fun getEventsByMonth(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Query("schedule_id") scheduleId: Int? = null
    ): List<EventDto>

    // Assignments
    @GET("assignments/event/{eventId}")
    suspend fun getAssignmentsByEvent(@Path("eventId") eventId: Int): List<AssignmentDto>

    @GET("schedules/{scheduleId}/assignments/published")
    suspend fun getPublishedAssignments(@Path("scheduleId") scheduleId: Int): List<AssignmentDto>

    // Volunteers
    @GET("volunteers/search")
    suspend fun searchVolunteers(@Query("q") query: String): List<VolunteerDto>

    // Restriction types
    @GET("restriction-types")
    suspend fun getRestrictionTypes(): List<RestrictionTypeDto>

    // Restrictions
    @GET("restrictions")
    suspend fun getRestrictions(
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null,
        @Query("schedule_id") scheduleId: Int? = null,
        @Query("volunteer_id") volunteerId: Int? = null
    ): PaginatedRestrictionsDto

    @GET("restrictions/{id}")
    suspend fun getRestrictionById(@Path("id") id: Int): RestrictionDto

    @POST("restrictions")
    suspend fun createRestriction(@Body body: RestrictionDto): RestrictionDto

    @PUT("restrictions/{id}")
    suspend fun updateRestriction(@Path("id") id: Int, @Body body: RestrictionDto): Map<String, String>

    @DELETE("restrictions/{id}")
    suspend fun deleteRestriction(@Path("id") id: Int)
}
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: add DTOs and Retrofit API interface"
```

---

## Task 3: Token storage + Hilt DI module

**Files:**
- Create: `data/local/TokenStore.kt`
- Create: `di/AppModule.kt`
- Create: `EscalaApp.kt`

**Step 1: Create `TokenStore.kt`**

```kotlin
package br.com.leonardogsouza.escalav.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_tokens",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)
        set(v) = prefs.edit().putString(KEY_ACCESS, v).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        set(v) = prefs.edit().putString(KEY_REFRESH, v).apply()

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
```

**Step 2: Create `di/AppModule.kt`**

```kotlin
package br.com.leonardogsouza.escalav.di

import br.com.leonardogsouza.escalav.BuildConfig
import br.com.leonardogsouza.escalav.data.local.TokenStore
import br.com.leonardogsouza.escalav.data.remote.api.ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttp(tokenStore: TokenStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val token = tokenStore.accessToken
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else chain.request()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttp: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
```

**Step 3: Create `EscalaApp.kt`**

```kotlin
package br.com.leonardogsouza.escalav

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EscalaApp : Application()
```

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: add token store, Hilt DI module, and Application class"
```

---

## Task 4: Auth — LoginViewModel + LoginScreen

**Files:**
- Create: `ui/auth/AuthViewModel.kt`
- Create: `ui/auth/LoginScreen.kt`
- Create: `domain/model/UserSession.kt`

**Step 1: Create `UserSession.kt`**

```kotlin
package br.com.leonardogsouza.escalav.domain.model

data class UserSession(
    val userId: Int,
    val username: String,
    val role: String,
    val churchId: Int
)
```

**Step 2: Create `AuthViewModel.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leonardogsouza.escalav.data.local.TokenStore
import br.com.leonardogsouza.escalav.data.remote.api.ApiService
import br.com.leonardogsouza.escalav.data.remote.dto.LoginRequest
import br.com.leonardogsouza.escalav.domain.model.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Base64
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val session: UserSession) : AuthState()
    data class Error(val message: String) : AuthState()
    object LoggedOut : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    // Check if already logged in on startup
    fun checkSession() {
        val token = tokenStore.accessToken ?: return
        val session = decodeJwt(token)
        if (session != null) _state.value = AuthState.Success(session)
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val tokens = api.login(LoginRequest(username, password))
                tokenStore.accessToken = tokens.accessToken
                tokenStore.refreshToken = tokens.refreshToken
                val session = decodeJwt(tokens.accessToken)
                    ?: throw Exception("Token inválido")
                _state.value = AuthState.Success(session)
            } catch (e: Exception) {
                _state.value = AuthState.Error(
                    when {
                        e.message?.contains("401") == true ||
                        e.message?.contains("400") == true -> "Usuário ou senha inválidos"
                        e.message?.contains("Unable to resolve host") == true ||
                        e.message?.contains("timeout") == true -> "Erro de rede. Verifique sua conexão."
                        else -> "Erro ao conectar: ${e.message}"
                    }
                )
            }
        }
    }

    fun logout() {
        tokenStore.clear()
        _state.value = AuthState.LoggedOut
    }

    private fun decodeJwt(token: String): UserSession? = try {
        val payload = token.split(".")[1]
        val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
        val json = JSONObject(decoded)
        UserSession(
            userId = json.getInt("id"),
            username = json.getString("username"),
            role = json.getString("role"),
            churchId = json.getInt("church_id")
        )
    } catch (e: Exception) { null }
}
```

**Step 3: Create `LoginScreen.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthState.Success) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Escala de Voluntários",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Faça login para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuário") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (state is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (state as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(username.trim(), password) },
            enabled = username.isNotBlank() && password.isNotBlank() && state !is AuthState.Loading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (state is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Entrar")
            }
        }
    }
}
```

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: add login screen and AuthViewModel"
```

---

## Task 5: Navigation + MainActivity + Theme

**Files:**
- Create: `ui/theme/Theme.kt`
- Create: `ui/AppNavigation.kt`
- Create: `MainActivity.kt`

**Step 1: Create `ui/theme/Theme.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF1E3A5F)
private val PrimaryContainer = Color(0xFFD6E4FF)
private val Secondary = Color(0xFF3D6B9E)

private val LightColors = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary
)

@Composable
fun EscalaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
```

**Step 2: Create `ui/AppNavigation.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.leonardogsouza.escalav.ui.auth.AuthState
import br.com.leonardogsouza.escalav.ui.auth.AuthViewModel
import br.com.leonardogsouza.escalav.ui.auth.LoginScreen
import br.com.leonardogsouza.escalav.ui.schedule.ScheduleCalendarScreen
import br.com.leonardogsouza.escalav.ui.schedule.DayDetailScreen
import br.com.leonardogsouza.escalav.ui.schedule.EventDetailScreen
import br.com.leonardogsouza.escalav.ui.restrictions.RestrictionsScreen
import br.com.leonardogsouza.escalav.ui.restrictions.RestrictionFormScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Calendar : Screen("calendar")
    object DayDetail : Screen("day/{date}") {
        fun createRoute(date: String) = "day/$date"
    }
    object EventDetail : Screen("event/{eventId}") {
        fun createRoute(id: Int) = "event/$id"
    }
    object Restrictions : Screen("restrictions")
    object NewRestriction : Screen("restrictions/new")
    object EditRestriction : Screen("restrictions/{id}/edit") {
        fun createRoute(id: Int) = "restrictions/$id/edit"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.state.collectAsState()

    LaunchedEffect(Unit) { authViewModel.checkSession() }

    val startDestination = if (authState is AuthState.Success) Screen.Calendar.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Calendar.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Calendar.route) {
            ScheduleCalendarScreen(
                onDayClick = { date -> navController.navigate(Screen.DayDetail.createRoute(date)) },
                onRestrictionsClick = { navController.navigate(Screen.Restrictions.route) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.DayDetail.route) { back ->
            val date = back.arguments?.getString("date") ?: return@composable
            DayDetailScreen(
                date = date,
                onEventClick = { id -> navController.navigate(Screen.EventDetail.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.EventDetail.route) { back ->
            val id = back.arguments?.getString("eventId")?.toIntOrNull() ?: return@composable
            EventDetailScreen(eventId = id, onBack = { navController.popBackStack() })
        }
        composable(Screen.Restrictions.route) {
            RestrictionsScreen(
                onNewRestriction = { navController.navigate(Screen.NewRestriction.route) },
                onEditRestriction = { id -> navController.navigate(Screen.EditRestriction.createRoute(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.NewRestriction.route) {
            RestrictionFormScreen(
                restrictionId = null,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.EditRestriction.route) { back ->
            val id = back.arguments?.getString("id")?.toIntOrNull() ?: return@composable
            RestrictionFormScreen(
                restrictionId = id,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

**Step 3: Create `MainActivity.kt`**

```kotlin
package br.com.leonardogsouza.escalav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.leonardogsouza.escalav.ui.AppNavigation
import br.com.leonardogsouza.escalav.ui.theme.EscalaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EscalaTheme {
                AppNavigation()
            }
        }
    }
}
```

**Step 4: Create `res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.EscalaVoluntarios" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

**Step 5: Commit**

```bash
git add -A
git commit -m "feat: add theme, navigation graph, and MainActivity"
```

---

## Task 6: Calendar screen

**Files:**
- Create: `ui/schedule/ScheduleViewModel.kt`
- Create: `ui/schedule/ScheduleCalendarScreen.kt`

**Step 1: Create `ScheduleViewModel.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leonardogsouza.escalav.data.remote.api.ApiService
import br.com.leonardogsouza.escalav.data.remote.dto.EventDto
import br.com.leonardogsouza.escalav.data.remote.dto.ScheduleDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CalendarUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val schedule: ScheduleDto? = null,
    val eventsByDate: Map<String, List<EventDto>> = emptyMap(),
    val selectedMonth: LocalDate = LocalDate.now().withDayOfMonth(1)
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state

    init { loadScheduleAndEvents() }

    fun changeMonth(month: LocalDate) {
        _state.value = _state.value.copy(selectedMonth = month)
        loadEventsForMonth(month)
    }

    private fun loadScheduleAndEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val schedules = api.getActiveSchedules()
                val schedule = schedules.firstOrNull()
                _state.value = _state.value.copy(schedule = schedule)
                loadEventsForMonth(_state.value.selectedMonth)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    private fun loadEventsForMonth(month: LocalDate) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val scheduleId = _state.value.schedule?.id
                val events = api.getEventsByMonth(month.year, month.monthValue, scheduleId)
                val byDate = events.groupBy { it.date.substring(0, 10) }
                _state.value = _state.value.copy(loading = false, eventsByDate = byDate)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }
}
```

**Step 2: Create `ScheduleCalendarScreen.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(
    onDayClick: (String) -> Unit,
    onRestrictionsClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.schedule?.name ?: "Escala") },
                actions = {
                    IconButton(onClick = onRestrictionsClick) {
                        Icon(Icons.Filled.EventNote, contentDescription = "Restrições")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeMonth(state.selectedMonth.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mês anterior")
                }
                Text(
                    text = "${state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }} ${state.selectedMonth.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { viewModel.changeMonth(state.selectedMonth.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo mês")
                }
            }

            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                CalendarGrid(
                    month = state.selectedMonth,
                    eventsByDate = state.eventsByDate,
                    onDayClick = onDayClick
                )
            }

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    month: LocalDate,
    eventsByDate: Map<String, List<Any>>,
    onDayClick: (String) -> Unit
) {
    val firstDay = month.withDayOfMonth(1)
    val daysInMonth = month.lengthOfMonth()
    // Sunday=0, shift from Java's Monday=1 convention
    val startOffset = (firstDay.dayOfWeek.value % 7)
    val totalCells = startOffset + daysInMonth

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        items(totalCells) { index ->
            if (index < startOffset) {
                Box(modifier = Modifier.aspectRatio(1f))
            } else {
                val day = index - startOffset + 1
                val dateStr = "${month.year}-${month.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                val hasEvents = eventsByDate.containsKey(dateStr)
                val isToday = dateStr == LocalDate.now().toString()

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable(enabled = hasEvents) { onDayClick(dateStr) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (hasEvents) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Step 3: Commit**

```bash
git add -A
git commit -m "feat: add calendar screen with month navigation and event dots"
```

---

## Task 7: Day detail + Event detail screens

**Files:**
- Create: `ui/schedule/DayDetailScreen.kt`
- Create: `ui/schedule/EventDetailScreen.kt`
- Create: `ui/schedule/EventViewModel.kt`

**Step 1: Create `EventViewModel.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leonardogsouza.escalav.data.remote.api.ApiService
import br.com.leonardogsouza.escalav.data.remote.dto.AssignmentDto
import br.com.leonardogsouza.escalav.data.remote.dto.EventDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DayDetailUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val events: List<EventDto> = emptyList()
)

data class EventDetailUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val event: EventDto? = null,
    val assignments: List<AssignmentDto> = emptyList()
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _dayState = MutableStateFlow(DayDetailUiState())
    val dayState: StateFlow<DayDetailUiState> = _dayState

    private val _eventState = MutableStateFlow(EventDetailUiState())
    val eventState: StateFlow<EventDetailUiState> = _eventState

    fun loadEventsForDay(date: String) {
        viewModelScope.launch {
            _dayState.value = DayDetailUiState(loading = true)
            try {
                val parts = date.split("-")
                val events = api.getEventsByMonth(parts[0].toInt(), parts[1].toInt())
                val filtered = events.filter { it.date.startsWith(date) }
                _dayState.value = DayDetailUiState(events = filtered)
            } catch (e: Exception) {
                _dayState.value = DayDetailUiState(error = e.message)
            }
        }
    }

    fun loadEventDetail(eventId: Int) {
        viewModelScope.launch {
            _eventState.value = EventDetailUiState(loading = true)
            try {
                val assignments = api.getAssignmentsByEvent(eventId)
                val event = assignments.firstOrNull()?.event
                _eventState.value = EventDetailUiState(event = event, assignments = assignments)
            } catch (e: Exception) {
                _eventState.value = EventDetailUiState(error = e.message)
            }
        }
    }
}
```

**Step 2: Create `DayDetailScreen.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leonardogsouza.escalav.data.remote.dto.EventDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: String,
    onEventClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val state by viewModel.dayState.collectAsState()

    LaunchedEffect(date) { viewModel.loadEventsForDay(date) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(date) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                state.events.isEmpty() -> Text("Nenhum evento neste dia.", modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.events) { event ->
                        EventCard(event = event, onClick = { onEventClick(event.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: EventDto, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.service, style = MaterialTheme.typography.titleMedium)
            Text(event.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (event.notes.isNotBlank()) {
                Text(event.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
```

**Step 3: Create `EventDetailScreen.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leonardogsouza.escalav.data.remote.dto.AssignmentDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Int,
    onBack: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val state by viewModel.eventState.collectAsState()

    LaunchedEffect(eventId) { viewModel.loadEventDetail(eventId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.event?.service ?: "Evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.event?.let { event ->
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(event.service, style = MaterialTheme.typography.headlineSmall)
                                    Text("Data: ${event.date.substring(0, 10)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Horário: ${event.time}", style = MaterialTheme.typography.bodyMedium)
                                    if (event.notes.isNotBlank()) Text("Obs: ${event.notes}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        item {
                            Text("Voluntários escalados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    items(state.assignments) { assignment ->
                        AssignmentCard(assignment)
                    }
                    if (state.assignments.isEmpty() && !state.loading) {
                        item { Text("Nenhum voluntário escalado.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentCard(assignment: AssignmentDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(assignment.volunteer?.fullName ?: assignment.volunteer?.name ?: "—", style = MaterialTheme.typography.bodyLarge)
                Text(assignment.position?.name ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssignmentStatusChip(assignment.status)
        }
    }
}

@Composable
private fun AssignmentStatusChip(status: String) {
    val (label, containerColor) = when (status.uppercase()) {
        "APPROVED" -> "Aprovado" to MaterialTheme.colorScheme.primaryContainer
        "PENDING" -> "Pendente" to MaterialTheme.colorScheme.tertiaryContainer
        "REJECTED" -> "Rejeitado" to MaterialTheme.colorScheme.errorContainer
        else -> status to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = containerColor, shape = MaterialTheme.shapes.small) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}
```

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: add day detail and event detail screens"
```

---

## Task 8: Restrictions CRUD screens

**Files:**
- Create: `ui/restrictions/RestrictionsViewModel.kt`
- Create: `ui/restrictions/RestrictionsScreen.kt`
- Create: `ui/restrictions/RestrictionFormScreen.kt`

**Step 1: Create `RestrictionsViewModel.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.restrictions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leonardogsouza.escalav.data.local.TokenStore
import br.com.leonardogsouza.escalav.data.remote.api.ApiService
import br.com.leonardogsouza.escalav.data.remote.dto.*
import br.com.leonardogsouza.escalav.ui.auth.AuthViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Base64
import javax.inject.Inject

data class RestrictionsUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val restrictions: List<RestrictionDto> = emptyList(),
    val scheduleId: Int? = null,
    val volunteerId: Int? = null
)

data class RestrictionFormState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val restriction: RestrictionDto? = null,
    val restrictionTypes: List<RestrictionTypeDto> = emptyList(),
    val saved: Boolean = false
)

@HiltViewModel
class RestrictionsViewModel @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _listState = MutableStateFlow(RestrictionsUiState())
    val listState: StateFlow<RestrictionsUiState> = _listState

    private val _formState = MutableStateFlow(RestrictionFormState())
    val formState: StateFlow<RestrictionFormState> = _formState

    private fun getVolunteerIdFromToken(): Int? {
        val token = tokenStore.accessToken ?: return null
        return try {
            val payload = token.split(".")[1]
            val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
            JSONObject(decoded).getInt("id")
        } catch (e: Exception) { null }
    }

    fun loadRestrictions() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(loading = true, error = null)
            try {
                val volunteerId = getVolunteerIdFromToken()
                val schedules = api.getActiveSchedules()
                val scheduleId = schedules.firstOrNull()?.id
                val result = api.getRestrictions(
                    page = 1,
                    pageSize = 50,
                    scheduleId = scheduleId,
                    volunteerId = volunteerId
                )
                _listState.value = RestrictionsUiState(
                    restrictions = result.data,
                    scheduleId = scheduleId,
                    volunteerId = volunteerId
                )
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun loadFormData(restrictionId: Int?) {
        viewModelScope.launch {
            _formState.value = RestrictionFormState(loading = true)
            try {
                val types = api.getRestrictionTypes()
                val restriction = restrictionId?.let { api.getRestrictionById(it) }
                _formState.value = RestrictionFormState(restrictionTypes = types, restriction = restriction)
            } catch (e: Exception) {
                _formState.value = RestrictionFormState(error = e.message)
            }
        }
    }

    fun saveRestriction(data: RestrictionDto, id: Int?) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(saving = true, error = null)
            try {
                if (id == null) api.createRestriction(data)
                else api.updateRestriction(id, data)
                _formState.value = _formState.value.copy(saving = false, saved = true)
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(saving = false, error = e.message)
            }
        }
    }

    fun deleteRestriction(id: Int) {
        viewModelScope.launch {
            try {
                api.deleteRestriction(id)
                loadRestrictions()
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(error = e.message)
            }
        }
    }
}
```

**Step 2: Create `RestrictionsScreen.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.restrictions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leonardogsouza.escalav.data.remote.dto.RestrictionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionsScreen(
    onNewRestriction: () -> Unit,
    onEditRestriction: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: RestrictionsViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()
    var deleteTarget by remember { mutableStateOf<RestrictionDto?>(null) }

    LaunchedEffect(Unit) { viewModel.loadRestrictions() }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir restrição?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    target.id?.let { viewModel.deleteRestriction(it) }
                    deleteTarget = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas Restrições") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewRestriction) {
                Icon(Icons.Filled.Add, contentDescription = "Nova restrição")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                state.restrictions.isEmpty() -> Text("Nenhuma restrição cadastrada.", modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.restrictions, key = { it.id ?: 0 }) { restriction ->
                        RestrictionListItem(
                            restriction = restriction,
                            onEdit = { restriction.id?.let(onEditRestriction) },
                            onDelete = { deleteTarget = restriction }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestrictionListItem(
    restriction: RestrictionDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(restriction.description.ifBlank { "Sem descrição" }, style = MaterialTheme.typography.bodyLarge)
                restriction.restrictionType?.let {
                    Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
```

**Step 3: Create `RestrictionFormScreen.kt`**

```kotlin
package br.com.leonardogsouza.escalav.ui.restrictions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.leonardogsouza.escalav.data.remote.dto.RestrictionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionFormScreen(
    restrictionId: Int?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RestrictionsViewModel = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    var description by remember { mutableStateOf("") }
    var selectedTypeId by remember { mutableStateOf<Int?>(null) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(restrictionId) { viewModel.loadFormData(restrictionId) }

    LaunchedEffect(formState.restriction) {
        formState.restriction?.let {
            description = it.description
            selectedTypeId = it.restrictionTypeId
        }
    }

    LaunchedEffect(formState.saved) {
        if (formState.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (restrictionId == null) "Nova Restrição" else "Editar Restrição") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (formState.loading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Type dropdown
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = formState.restrictionTypes.find { it.id == selectedTypeId }?.name ?: "Selecione o tipo",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de restrição") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        formState.restrictionTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedTypeId = type.id
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                formState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        val scheduleId = listState.scheduleId ?: return@Button
                        val volunteerId = listState.volunteerId ?: return@Button
                        val typeId = selectedTypeId ?: return@Button
                        viewModel.saveRestriction(
                            RestrictionDto(
                                id = formState.restriction?.id,
                                volunteerId = volunteerId,
                                scheduleId = scheduleId,
                                description = description.trim(),
                                restrictionTypeId = typeId,
                                restrictionType = null,
                                rulesJson = formState.restriction?.rulesJson,
                                active = true,
                                fixed = false
                            ),
                            restrictionId
                        )
                    },
                    enabled = description.isNotBlank() && selectedTypeId != null && !formState.saving,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (formState.saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(if (restrictionId == null) "Criar" else "Salvar")
                }
            }
        }
    }
}
```

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: add restrictions list and form screens"
```

---

## Task 9: Update README + push

**Step 1: Rewrite `README.md`**

Update to reflect the Kotlin app — see content below (write directly).

```markdown
# Escala Voluntários — Android

Native Android app for the Escala de Voluntários volunteer scheduling system used by CCB church services.

## Tech Stack
- Kotlin + Jetpack Compose
- Navigation Compose
- Retrofit 2 + Moshi (JSON)
- Hilt (dependency injection)
- EncryptedSharedPreferences (JWT token storage)
- Material 3

## Screens
- **Login** — JWT authentication
- **Calendar** — Monthly view with event dots
- **Day Detail** — List events for a selected day
- **Event Detail** — Event info + volunteer assignment cards
- **Restrictions** — List, create, edit, delete personal availability restrictions

## Setup

### Prerequisites
- Android Studio Hedgehog 2023.1 or newer
- Android SDK 34
- The [escala-voluntarios](https://github.com/leogsouza/escala-voluntarios) Go backend running

### Configuration

Create `local.properties` at project root (gitignored):

```properties
sdk.dir=/path/to/your/Android/Sdk
API_BASE_URL=http://10.0.2.2:8080
```

> `10.0.2.2` is the Android emulator's alias for your host machine's localhost.
> For a physical device on the same LAN, use your machine's local IP (e.g. `192.168.1.x`).

### Build & Run

Open the project in Android Studio and click Run, or:

```bash
./gradlew assembleDebug
```
```

**Step 2: Commit and push**

```bash
git add -A
git commit -m "docs: update README for Kotlin app"
git push origin master
```

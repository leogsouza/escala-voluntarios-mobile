package br.com.leogsouza.escalav.ui.auth

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leogsouza.escalav.data.local.SessionEvents
import br.com.leogsouza.escalav.data.local.TokenStore
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.LoginRequest
import br.com.leogsouza.escalav.data.remote.dto.RefreshRequest
import br.com.leogsouza.escalav.domain.model.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named

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
    @Named("refreshApi") private val refreshApi: ApiService,
    private val tokenStore: TokenStore,
    sessionEvents: SessionEvents
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    private val _forcedLogoutMessage = MutableStateFlow<String?>(null)
    val forcedLogoutMessage: StateFlow<String?> = _forcedLogoutMessage

    // Biometric helpers (read-once properties, no StateFlow needed)
    val biometricEnabled: Boolean get() = tokenStore.biometricEnabled
    val hasStoredSession: Boolean
        get() = tokenStore.accessToken != null || tokenStore.refreshToken != null

    init {
        viewModelScope.launch {
            sessionEvents.forcedLogout.collect {
                _forcedLogoutMessage.value = "Sua sessão expirou. Faça login novamente."
                _state.value = AuthState.LoggedOut
            }
        }
    }

    // ── Session bootstrap ──────────────────────────────────────────────────────
    /**
     * Called on app start. If the access token is still valid, restore the session.
     * If it's expired, silently try to refresh with the refresh token. If refresh
     * fails (or there's no refresh token), leave the user on the login screen.
     */
    fun checkSession() {
        viewModelScope.launch {
            val accessToken = tokenStore.accessToken ?: return@launch
            if (!isTokenExpired(accessToken)) {
                // Fast path: token still valid
                decodeJwt(accessToken)?.let { _state.value = AuthState.Success(it) }
                return@launch
            }
            // Access token expired — try a silent refresh
            tryRefresh()
        }
    }

    // ── Password login ─────────────────────────────────────────────────────────
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _forcedLogoutMessage.value = null
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

    // ── Biometric login ────────────────────────────────────────────────────────
    /**
     * Called after a successful biometric prompt. Restores the session from the
     * stored token (refreshing silently if expired). Disables biometric and shows
     * an error if tokens are completely gone or unrefreshable.
     */
    fun loginWithBiometric() {
        viewModelScope.launch {
            val accessToken = tokenStore.accessToken
            // Fast path: access token present and still valid
            if (accessToken != null && !isTokenExpired(accessToken)) {
                decodeJwt(accessToken)?.let { _state.value = AuthState.Success(it); return@launch }
            }
            // Access token absent (post-logout) or expired — try silent refresh
            val refreshed = tryRefresh()
            if (!refreshed) {
                tokenStore.biometricEnabled = false
                _state.value = AuthState.Error("Sessão expirada. Faça login novamente.")
            }
        }
    }

    fun enableBiometric()  { tokenStore.biometricEnabled = true  }
    fun disableBiometric() { tokenStore.biometricEnabled = false }

    // ── Logout ─────────────────────────────────────────────────────────────────
    fun logout() {
        tokenStore.clearTokens()          // keep biometricEnabled so the prompt re-appears on next login
        _forcedLogoutMessage.value = null
        _state.value = AuthState.LoggedOut
    }

    // ── Private helpers ────────────────────────────────────────────────────────
    /**
     * Calls POST /refresh, updates stored tokens and sets AuthState.Success.
     * Returns true on success, false on any failure.
     */
    private suspend fun tryRefresh(): Boolean {
        val refreshToken = tokenStore.refreshToken ?: return false
        return try {
            val tokens = refreshApi.refresh(RefreshRequest(refreshToken))
            tokenStore.accessToken = tokens.accessToken
            tokenStore.refreshToken = tokens.refreshToken
            val session = decodeJwt(tokens.accessToken) ?: return false
            _state.value = AuthState.Success(session)
            true
        } catch (_: Exception) {
            tokenStore.clearTokens()
            false
        }
    }

    /** Returns true if [token]'s `exp` claim is in the past (or unreadable). */
    private fun isTokenExpired(token: String): Boolean = try {
        val payload = token.split(".")[1]
        val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
        val exp = JSONObject(decoded).getLong("exp")
        System.currentTimeMillis() / 1000 >= exp
    } catch (_: Exception) {
        true
    }

    private fun decodeJwt(token: String): UserSession? = try {
        val payload = token.split(".")[1]
        val decoded = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
        val json = JSONObject(decoded)
        UserSession(
            userId   = json.getInt("id"),
            username = json.getString("username"),
            role     = json.getString("role"),
            churchId = json.getInt("church_id")
        )
    } catch (_: Exception) { null }
}

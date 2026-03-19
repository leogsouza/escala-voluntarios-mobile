package br.com.leogsouza.escalav.ui.auth

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.leogsouza.escalav.data.local.SessionEvents
import br.com.leogsouza.escalav.data.local.TokenStore
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.LoginRequest
import br.com.leogsouza.escalav.domain.model.UserSession
import kotlinx.coroutines.flow.collect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
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
    private val tokenStore: TokenStore,
    sessionEvents: SessionEvents
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    private val _forcedLogoutMessage = MutableStateFlow<String?>(null)
    val forcedLogoutMessage: StateFlow<String?> = _forcedLogoutMessage

    init {
        viewModelScope.launch {
            sessionEvents.forcedLogout.collect {
                _forcedLogoutMessage.value = "Sua sessão expirou. Faça login novamente."
                _state.value = AuthState.LoggedOut
            }
        }
    }

    // Check if already logged in on startup
    fun checkSession() {
        val token = tokenStore.accessToken ?: return
        val session = decodeJwt(token)
        if (session != null) _state.value = AuthState.Success(session)
    }

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

    fun logout() {
        tokenStore.clear()
        _forcedLogoutMessage.value = null
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

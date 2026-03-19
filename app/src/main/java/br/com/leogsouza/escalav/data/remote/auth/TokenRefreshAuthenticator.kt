package br.com.leogsouza.escalav.data.remote.auth

import br.com.leogsouza.escalav.data.local.SessionEvents
import br.com.leogsouza.escalav.data.local.TokenStore
import br.com.leogsouza.escalav.data.remote.api.ApiService
import br.com.leogsouza.escalav.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    @Named("refreshApi") private val refreshApi: ApiService,
    private val tokenStore: TokenStore,
    private val sessionEvents: SessionEvents
) : Authenticator {

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path.endsWith("/login") || path.endsWith("/refresh")) return null
        if (responseCount(response) >= 2) return null

        val currentToken = tokenStore.accessToken
        val requestAuthHeader = response.request.header("Authorization")

        // If another request already refreshed the token, retry with latest access token.
        if (currentToken != null && requestAuthHeader != "Bearer $currentToken") {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        }

        synchronized(refreshLock) {
            val latestToken = tokenStore.accessToken
            if (latestToken != null && requestAuthHeader != "Bearer $latestToken") {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            val refreshToken = tokenStore.refreshToken ?: run {
                tokenStore.clear()
                sessionEvents.emitForcedLogout()
                return null
            }

            return try {
                val tokens = runBlocking {
                    refreshApi.refresh(RefreshRequest(refreshToken))
                }
                tokenStore.accessToken = tokens.accessToken
                tokenStore.refreshToken = tokens.refreshToken

                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .build()
            } catch (_: Exception) {
                tokenStore.clear()
                sessionEvents.emitForcedLogout()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }
}



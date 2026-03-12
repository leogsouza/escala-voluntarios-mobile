package br.com.leogsouza.escalav.domain.model

data class UserSession(
    val userId: Int,
    val username: String,
    val role: String,
    val churchId: Int
)

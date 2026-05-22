package org.example.project.domain.model

sealed interface AuthStatus {
    data object Checking : AuthStatus
    data object Unauthenticated : AuthStatus
    data class Authenticated(val userId: String, val email: String) : AuthStatus
}

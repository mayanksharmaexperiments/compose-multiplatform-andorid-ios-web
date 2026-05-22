package org.example.project.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.example.project.domain.model.AuthStatus

interface AuthRepository {
    val sessionState: StateFlow<AuthStatus>
    suspend fun login(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun resetPassword(email: String)
    suspend fun logout()
}

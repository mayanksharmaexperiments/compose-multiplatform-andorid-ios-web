package org.example.project.data.repository

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.model.AuthStatus
import org.example.project.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val auth: Auth,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AuthRepository {

    private val _sessionState = MutableStateFlow<AuthStatus>(AuthStatus.Checking)
    override val sessionState: StateFlow<AuthStatus> = _sessionState.asStateFlow()

    init {
        externalScope.launch {
            println("AuthRepositoryImpl: Initializing session collection...")
            try {
                auth.sessionStatus.collect { status ->
                    println("AuthRepositoryImpl: Received session status: $status")
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            println("AuthRepositoryImpl: SessionStatus.Authenticated - email: ${status.session.user?.email}")
                            _sessionState.value = AuthStatus.Authenticated(
                                userId = status.session.user?.id ?: "",
                                email = status.session.user?.email ?: ""
                            )
                        }
                        is SessionStatus.NotAuthenticated -> {
                            println("AuthRepositoryImpl: SessionStatus.NotAuthenticated")
                            _sessionState.value = AuthStatus.Unauthenticated
                        }
                        is SessionStatus.Initializing -> {
                            println("AuthRepositoryImpl: SessionStatus.Initializing")
                            _sessionState.value = AuthStatus.Checking
                        }
                        is SessionStatus.RefreshFailure -> {
                            println("AuthRepositoryImpl: SessionStatus.RefreshFailure - status: $status")
                            _sessionState.value = AuthStatus.Unauthenticated
                        }
                    }
                }
            } catch (t: Throwable) {
                println("AuthRepositoryImpl: Exception in sessionStatus flow collection: ${t.message}")
                t.printStackTrace()
            }
        }
    }

    override suspend fun login(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun resetPassword(email: String) {
        auth.resetPasswordForEmail(email = email)
    }

    override suspend fun logout() {
        auth.signOut()
    }
}

---
name: supabase-auth-cmp
description: Enforces best practices for Supabase GoTrue Auth authentication, session persistence, reactive state flows, and error handling in Compose Multiplatform (CMP) projects.
---
# Supabase Authentication in Kotlin Multiplatform (KMP)

This skill provides guidelines and concrete patterns to implement secure user authentication and session management using the official **`supabase-kt` (Auth/GoTrue)** plugin in Compose Multiplatform.

---

## 1. Supabase Auth Configuration
*Ensure the Auth plugin is installed inside the single static `SupabaseClient` instance in your network module.*

```kotlin
package org.example.project.data.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth

fun createSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = "YOUR_URL",
        supabaseKey = "YOUR_KEY"
    ) {
        // Install Auth module for GoTrue services
        install(Auth) {
            // Enable caching of user sessions locally
            alwaysAutoRefresh = true
        }
    }
}
```

---

## 2. Implementing Auth Repository Interfaces
*Decouple auth operations from ViewModels. Declare an `AuthRepository` interface in the Domain layer and implement it in the Data layer.*

### 1. Domain Contract:
```kotlin
package org.example.project.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.example.project.domain.model.UserSession

interface AuthRepository {
    val currentSessionState: StateFlow<AuthStatus>
    suspend fun loginWithEmail(email: String, secret: String)
    suspend fun signupWithEmail(email: String, secret: String)
    suspend fun logout()
}

sealed class AuthStatus {
    object Checking : AuthStatus()
    object Unauthenticated : AuthStatus()
    data class Authenticated(val userId: String, val email: String) : AuthStatus()
}
```

### 2. Data Implementation:
```kotlin
package org.example.project.data.repository

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.example.project.domain.repository.AuthRepository
import org.example.project.domain.repository.AuthStatus

class AuthRepositoryImpl(
    private val auth: Auth,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AuthRepository {

    private val _currentSessionState = MutableStateFlow<AuthStatus>(AuthStatus.Checking)
    override val currentSessionState: StateFlow<AuthStatus> = _currentSessionState.asStateFlow()

    init {
        // Observe reactive session streams
        externalScope.launch {
            auth.sessionFlow.collect { session ->
                if (session == null) {
                    _currentSessionState.value = AuthStatus.Unauthenticated
                } else {
                    _currentSessionState.value = AuthStatus.Authenticated(
                        userId = session.user?.id ?: "",
                        email = session.user?.email ?: ""
                    )
                }
            }
        }
    }

    override suspend fun loginWithEmail(email: String, secret: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = secret
        }
    }

    override suspend fun signupWithEmail(email: String, secret: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = secret
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }
}
```

---

## 3. Session Integration in Presentation
*Leverage the reactive `sessionFlow` in your application navigation layer (e.g. Navigation 3) to automatically route users based on authentication status.*

```kotlin
package org.example.project.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import org.example.project.domain.repository.AuthStatus
import org.example.project.domain.repository.AuthRepository

@Composable
fun AppNavigation(authRepository: AuthRepository = koinInject()) {
    val authStatus = authRepository.currentSessionState.collectAsState().value

    when (authStatus) {
        is AuthStatus.Checking -> SplashScreen()
        is AuthStatus.Unauthenticated -> LoginScreen()
        is AuthStatus.Authenticated -> MainAppDashboard(authStatus.userId)
    }
}
```

---

## Core Constraints and Anti-Patterns
*   ❌ **Leaking Supabase imports:** Do not import `io.github.jan.supabase.gotrue` or provider credentials (e.g., `Email`) directly inside UI composable packages.
*   ❌ **Stale Session references:** Avoid hardcoding manual token caches; rely instead on the built-in Supabase session flow observer to update state changes.
*   ❌ **Exposing Plain passwords:** Never log passwords, tokens, or credential inputs anywhere in the application.

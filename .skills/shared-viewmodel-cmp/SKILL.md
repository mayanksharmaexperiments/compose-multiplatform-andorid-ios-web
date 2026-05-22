---
name: shared-viewmodel-cmp
description: Enforces best practices for shared ViewModels (androidx.lifecycle.ViewModel), reactive state flows, coroutine scopes, and lifecycle-aware state collection in Compose Multiplatform (CMP).
---
# Shared ViewModels in Compose Multiplatform (CMP)

This skill provides guidelines and patterns to build shared ViewModels inside `commonMain` that handle presentation state reactively and platform-independently.

---

## 1. Declarative Multiplatform ViewModel Structure
*Shared ViewModels inherit from `androidx.lifecycle.ViewModel` in commonMain. Use pure Kotlin Flow to expose read-only state and keep state mutations private.*

```kotlin
package org.example.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.model.Client
import org.example.project.domain.repository.ClientRepository

class ClientListViewModel(
    private val clientRepository: ClientRepository
) : ViewModel() {

    // 1. Private mutable state backing
    private val _uiState = MutableStateFlow<ClientListUiState>(ClientListUiState.Loading)
    
    // 2. Public read-only reactive state flow
    val uiState: StateFlow<ClientListUiState> = _uiState.asStateFlow()

    init {
        loadClients()
    }

    fun loadClients() {
        viewModelScope.launch {
            _uiState.value = ClientListUiState.Loading
            try {
                val clients = clientRepository.getClients()
                if (clients.isEmpty()) {
                    _uiState.update { ClientListUiState.Empty }
                } else {
                    _uiState.update { ClientListUiState.Success(clients) }
                }
            } catch (e: Exception) {
                _uiState.update { ClientListUiState.Error(e.message ?: "Unknown error") }
            }
        }
    }
}

// 3. Immutable UI state definition
sealed interface ClientListUiState {
    object Loading : ClientListUiState
    object Empty : ClientListUiState
    data class Success(val clients: List<Client>) : ClientListUiState
    data class Error(val message: String) : ClientListUiState
}
```

---

## 2. Lifecycle-Aware State Collection in Compose
*Collecting StateFlow directly in Compose via `collectAsState()` is an anti-pattern (it continues background polling when the view is invisible on iOS and Android). Always collect using `collectAsStateWithLifecycle()`.*

```kotlin
package org.example.project.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClientListScreen(
    onClientClick: (Long) -> Unit,
    viewModel: ClientListViewModel = koinViewModel()
) {
    // Collect the UI state in a lifecycle-safe manner
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ClientListUiState.Loading -> ShowLoadingSpinner()
        is ClientListUiState.Empty -> ShowEmptyState()
        is ClientListUiState.Success -> ClientListContent(state.clients, onClientClick)
        is ClientListUiState.Error -> ShowErrorAlert(state.message, onRetry = { viewModel.loadClients() })
    }
}
```

---

## 3. Scoping & Memory Deallocation
*Shared ViewModels inside KMP must release native resource allocations (like timers, socket streams, or database listeners) when cleared.*

```kotlin
class ClientChatViewModel(
    private val webSocketRepository: WebSocketRepository
) : ViewModel() {

    init {
        webSocketRepository.connect()
    }

    // Called automatically by the framework when the screen/ViewModel is popped off the stack
    override fun onCleared() {
        super.onCleared()
        // Disconnect and release long-running resources safely
        webSocketRepository.disconnect()
    }
}
```

---

## Core Constraints and Anti-Patterns
*   ❌ **Direct View Imports:** Never import Android UI classes (like `View`, `Context`, `Intent`), iOS elements (`UIViewController`), or Compose packages into shared ViewModel files. ViewModels must remain pure UI-agnostic presentation state engines.
*   ❌ **Using `collectAsState()`:** Never use the standard `collectAsState()` without lifecycle awareness inside screens. It is prone to leaks and background processing drain.
*   ❌ **Thread Blocking:** Do not block threads inside ViewModel mappings; always launch async scopes.

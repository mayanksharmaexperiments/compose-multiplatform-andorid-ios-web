package org.example.project.presentation.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.model.Client
import org.example.project.domain.repository.ClientRepository

class ClientListViewModel(
    private val repository: ClientRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        fetchClients()
    }

    fun fetchClients() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val clients = repository.getClients()
                if (clients.isEmpty()) {
                    _uiState.value = UiState.Empty
                } else {
                    _uiState.value = UiState.Success(clients)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class UiState {
    data object Loading : UiState()
    data object Empty : UiState()
    data class Success(val clients: List<Client>) : UiState()
    data class Error(val message: String) : UiState()
}

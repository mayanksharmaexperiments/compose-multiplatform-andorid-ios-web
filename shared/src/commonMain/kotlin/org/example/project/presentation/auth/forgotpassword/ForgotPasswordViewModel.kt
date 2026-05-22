package org.example.project.presentation.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.repository.AuthRepository

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Email address cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState.Loading
            try {
                authRepository.resetPassword(email)
                _uiState.value = ForgotPasswordUiState.Success
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = ForgotPasswordUiState.Error(e.message ?: "Failed to send reset email.")
            }
        }
    }

    fun clearError() {
        if (_uiState.value is ForgotPasswordUiState.Error) {
            _uiState.value = ForgotPasswordUiState.Idle
        }
    }
}

sealed interface ForgotPasswordUiState {
    data object Idle : ForgotPasswordUiState
    data object Loading : ForgotPasswordUiState
    data object Success : ForgotPasswordUiState
    data class Error(val message: String) : ForgotPasswordUiState
}

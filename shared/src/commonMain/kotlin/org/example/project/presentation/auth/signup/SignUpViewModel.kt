package org.example.project.presentation.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.repository.AuthRepository

class SignUpViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String, passwordConfirm: String) {
        if (email.isBlank() || password.isBlank() || passwordConfirm.isBlank()) {
            _uiState.value = SignUpUiState.Error("All fields are required.")
            return
        }

        if (password != passwordConfirm) {
            _uiState.value = SignUpUiState.Error("Passwords do not match.")
            return
        }

        if (password.length < 6) {
            _uiState.value = SignUpUiState.Error("Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = SignUpUiState.Loading
            try {
                authRepository.signUp(email, password)
                _uiState.value = SignUpUiState.Success
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = SignUpUiState.Error(e.message ?: "Registration failed.")
            }
        }
    }

    fun clearError() {
        if (_uiState.value is SignUpUiState.Error) {
            _uiState.value = SignUpUiState.Idle
        }
    }
}

sealed interface SignUpUiState {
    data object Idle : SignUpUiState
    data object Loading : SignUpUiState
    data object Success : SignUpUiState
    data class Error(val message: String) : SignUpUiState
}

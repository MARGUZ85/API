package com.example.marsphotos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.marsphotos.data.SicenetRepository
import com.example.marsphotos.data.model.LoginResult
import com.example.marsphotos.data.model.SicenetProfile
import kotlinx.coroutines.launch
import com.example.marsphotos.MarsPhotosApplication

sealed interface SicenetUiState {
    object Login : SicenetUiState
    object Loading : SicenetUiState
    data class Success(val profile: SicenetProfile) : SicenetUiState
    data class Error(val message: String) : SicenetUiState
}

class SicenetViewModel(private val repository: SicenetRepository) : ViewModel() {
    
    var sicenetUiState: SicenetUiState by mutableStateOf(SicenetUiState.Login)
        private set

    fun iniciarSesion(user: String, pass: String) {
        viewModelScope.launch {
            sicenetUiState = SicenetUiState.Loading
            when (val result = repository.login(user, pass)) {
                is LoginResult.Success -> {
                    // Login successful, fetch profile
                    getPerfil()
                }
                is LoginResult.Error -> {
                    sicenetUiState = SicenetUiState.Error(result.message)
                }
            }
        }
    }

    private fun getPerfil() {
        viewModelScope.launch {
            val profile = repository.getPerfil()
            sicenetUiState = profile?.let {
                SicenetUiState.Success(it)
            } ?: SicenetUiState.Error("Error: Se esperaba un perfil válido pero se recibió nulo.")
        }
    }

    fun resetToLogin() {
        sicenetUiState = SicenetUiState.Login
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MarsPhotosApplication)
                val repository = application.container.sicenetRepository
                SicenetViewModel(repository)
            }
        }
    }
}

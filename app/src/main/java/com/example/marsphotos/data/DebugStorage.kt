package com.example.marsphotos.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DebugStorage {
    private val _lastResponse = MutableStateFlow("")
    val lastResponse = _lastResponse.asStateFlow()

    fun updateResponse(response: String) {
        val current = _lastResponse.value
        val separator = if (current.isNotEmpty()) "\n\n================================\n\n" else ""
        _lastResponse.value = current + separator + response
    }
    
    fun clear() {
        _lastResponse.value = ""
    }
}

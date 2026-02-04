package com.example.marsphotos.data.model

data class SicenetProfile(
    val name: String,
    val enrollmentId: String, // Matricula
    val career: String,
    val semester: String,
    val specialty: String, // Especialidad
    val earnedCredits: String, // Creditos Acumulados
    val status: String, // Estatus
    val email: String? = null,
    val rawResponse: String = "" // Keeping raw response for debugging since we are guessing fields
)

sealed class LoginResult {
    data class Success(val cookie: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

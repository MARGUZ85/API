package com.example.marsphotos.data.model

/**
 * [MODEL LAYER - ENTITY]
 * Data class inmutable que representa la información del alumno.
 * Actúa como DTO (Data Transfer Object) entre capas.
 */
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

/**
 * [MODEL LAYER - RESULT STATE]
 * Sealed Class (Clase Sellada) para representar una jerarquía restringida de resultados.
 * Permite manejo exhaustivo en sentencias 'when' (es decir, el compilador obliga a manejar todos los casos).
 */
sealed class LoginResult {
    data class Success(val cookie: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

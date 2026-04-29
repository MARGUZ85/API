package com.example.marsphotos.data.model

/**
 * [CAPA DE MODELO - ENTIDAD DE PERFIL]
 * Esta clase de datos inmutable representa toda la información personal y académica
 * básica de un alumno recuperada desde Sicenet.
 * Se utiliza como un 'Objeto de Transferencia de Datos' (DTO) para mover la información
 * desde el Repositorio hasta las pantallas de la Interfaz de Usuario (UI).
 */
data class SicenetProfile(
    val name: String,             // Nombre completo del alumno
    val enrollmentId: String,     // Número de control o Matrícula
    val career: String,           // Carrera que cursa el alumno
    val semester: String,         // Semestre actual (ej: "8")
    val specialty: String,        // Especialidad de la carrera
    val earnedCredits: String,    // Créditos totales acumulados hasta la fecha
    val status: String,           // Situación académica (ej: "Vigente")
    val email: String? = null     // Correo institucional (opcional)
)

/**
 * [CAPA DE MODELO - ESTADO DE LOGIN]
 * Clase Sellada (Sealed Class) que define los posibles resultados de un intento de inicio de sesión.
 * Las clases selladas son ideales para representar estados porque el compilador nos ayuda
 * a manejar todos los casos posibles en un bloque 'when'.
 */
sealed class LoginResult {
    // Indica que el inicio de sesión fue exitoso y contiene la respuesta del servidor
    data class Success(val response: String) : LoginResult()
    
    // Indica que hubo un error (credenciales incorrectas, error de red, etc.)
    data class Error(val message: String) : LoginResult()
}

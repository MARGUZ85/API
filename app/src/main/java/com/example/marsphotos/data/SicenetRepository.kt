package com.example.marsphotos.data

import com.example.marsphotos.data.model.LoginResult
import com.example.marsphotos.data.model.SicenetProfile
import com.example.marsphotos.network.SicenetService

/**
 * [DATA LAYER - REPOSITORY]
 * Implementa el patrón Repository, actuando como 'Single Source of Truth'.
 * Abstrae la complejidad del parseo de datos y manejo de errores de la capa de UI.
 */
class SicenetRepository(private val service: SicenetService) {

    private var cachedLoginResponse: String? = null

    // [DATA TRANSFORMATION]
    // Transforma la respuesta cruda (Raw String) en un objeto de dominio (LoginResult).
    // Encapsula la lógica de validación de respuesta exitosa vs fallida.
    suspend fun login(user: String, pass: String): LoginResult {
        // Clear debug log on new login
        com.example.marsphotos.data.DebugStorage.clear()
        
        val result = service.login(user, pass)
        
        // DEBUG: Capture raw response
        com.example.marsphotos.data.DebugStorage.updateResponse("LOGIN RAW:\n$result")
        println("SicenetRepository: Login Raw Response: $result") 

        if (result == null) return LoginResult.Error("Network Error or Empty Response")

        // CACHE: Save successful (or potential) response for fallback
        cachedLoginResponse = result

        // Check for HTML response
        if (result.trim().startsWith("<html", ignoreCase = true)) {
             return LoginResult.Error("Error: El servidor respondió con HTML (posible bloqueo o error de URL). No se recibió XML.")
        }

        // Check for SOAP Fault
        if (result.contains(":Fault>", ignoreCase = true)) {
             val faultString = result.substringAfter("<faultstring>").substringBefore("</faultstring>")
             return LoginResult.Error("SOAP Fault: $faultString")
        }

        // VALIDATION UPDATE: Check for JSON success inside the XML
        val isValid = result.contains("\"acceso\":true", ignoreCase = true) ||
                      result.contains("\"acceso\": true", ignoreCase = true) ||
                      result.contains("&quot;acceso&quot;:true", ignoreCase = true) ||
                      result.contains("<accesoLoginResult>true</accesoLoginResult>")

        return if (isValid) {
            LoginResult.Success(result)
        } else {
             // Check for empty/self-closing result (common for null returns/failed auth in some setups)
             // Matches <accesoLoginResult /> or <accesoLoginResult/>
             if (result.contains("<accesoLoginResult />") || result.contains("<accesoLoginResult/>")) {
                 return LoginResult.Error("Login Failed: Check your credentials (Matricula/Password). Server returned empty.")
             }

             // Extract failure reason if possible
            val failPattern = "<(?:\\w+:)?accesoLoginResult>(.*?)</(?:\\w+:)?accesoLoginResult>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = failPattern.find(result)

            if (match != null) {
                val content = match.groupValues[1]
                LoginResult.Error("Auth Failed: $content")
            } else {
                // Return a larger snippet to debug standard XML envelope issues
                val snippet = result.take(500).replace("\n", " ") 
                LoginResult.Error("Unknown structure: $snippet")
            }
        }
    }

    suspend fun getPerfil(): SicenetProfile? {
        var xmlResponse = service.getProfile()
        
        // DEBUG: Capture raw response
        com.example.marsphotos.data.DebugStorage.updateResponse("PROFILE RAW:\n${xmlResponse ?: "NULL"}")
        println("SicenetRepository: Profile Raw Response: $xmlResponse") 

        // DETECT FAILURE & FALLBACK
        // If getProfile failed (null, Fault, or HTML error) BUT we have a cached login response, use it!
        val isFailure = xmlResponse == null || 
                        xmlResponse.contains(":Fault>", ignoreCase = true) || 
                        xmlResponse.contains("Server", ignoreCase = true) || // Capture "Server cannot access..."
                        xmlResponse.trim().startsWith("<html", ignoreCase = true)

        if (isFailure && !cachedLoginResponse.isNullOrEmpty()) {
            println("SicenetRepository: getProfile failed. Falling back to Cached Login Response.")
            com.example.marsphotos.data.DebugStorage.updateResponse("FALLBACK WARNING: 'getProfile' failed. Using Cached Login Response.")
            xmlResponse = cachedLoginResponse
        }

        if (xmlResponse == null) return null
        
        return parseProfileFromXml(xmlResponse)
    }

    private fun parseProfileFromXml(xml: String): SicenetProfile {
        // 1. Try to unescape
        var cleanXml = xml
        if (cleanXml.contains("&lt;") && cleanXml.contains("&gt;")) {
            cleanXml = cleanXml.replace("&lt;", "<").replace("&gt;", ">")
        }

        // 2. Identify format (XML vs JSON)
        // Check for JSON-like keys in the response
        val isJson = cleanXml.contains("\":") || cleanXml.contains("\": ")

        val name: String
        val enrollment: String
        val career: String
        val semester: String
        val specialty: String
        val earnedCredits: String
        val status: String

        if (isJson) {
            // Extract using regex for JSON keys
            // Try multiple keys for robustness (Login response uses strPrefix often)
            val n1 = extractJson(cleanXml, "nombre")
            val n2 = extractJson(cleanXml, "strNombre")
            val n3 = extractJson(cleanXml, "nombreCompleto")
            name = listOf(n1, n2, n3).firstOrNull { it != "Unknown" } ?: "Unknown"

            val e1 = extractJson(cleanXml, "matricula")
            val e2 = extractJson(cleanXml, "strMatricula")
            enrollment = listOf(e1, e2).firstOrNull { it != "Unknown" } ?: "Unknown"

            val c1 = extractJson(cleanXml, "carrera")
            val c2 = extractJson(cleanXml, "strCarrera")
            val c3 = extractJson(cleanXml, "programa")
            career = listOf(c1, c2, c3).firstOrNull { it != "Unknown" } ?: "Unknown"
            
            // FORCE DEBUGGING: If name is unknown, put raw json in name to see it
            // val finalName = if (name == "Unknown") "DEBUG: ${cleanXml.take(150)}" else name
            
            val st1 = extractJson(cleanXml, "estatus")
            val st2 = extractJson(cleanXml, "situacion")
            status = listOf(st1, st2).firstOrNull { it != "Unknown" } ?: "Unknown"

            val sp1 = extractJson(cleanXml, "especialidad")
            val sp2 = extractJson(cleanXml, "strEspecialidad")
            specialty = listOf(sp1, sp2).firstOrNull { it != "Unknown" } ?: "Unknown"

            // Semester variations
            val s1 = extractJson(cleanXml, "semestre")
            val s2 = extractJson(cleanXml, "semestreActual")
            val s3 = extractJson(cleanXml, "periodo")
            val s4 = extractJson(cleanXml, "semActual") // Found in debug keys
            semester = listOf(s1, s2, s3, s4).firstOrNull { it != "Unknown" } ?: "Unknown"

            // Credits variations
            val cr1 = extractJson(cleanXml, "creditosAcumulados")
            val cr2 = extractJson(cleanXml, "creditos")
            val cr3 = extractJson(cleanXml, "creditosAprobados")
            val cr4 = extractJson(cleanXml, "totalCreditos")
            val cr5 = extractJson(cleanXml, "cdtosAcumulados") // Found in debug keys
            earnedCredits = listOf(cr1, cr2, cr3, cr4, cr5).firstOrNull { it != "Unknown" } ?: "Unknown"

            // DEBUG: If still unknown, append ALL keys found in the JSON to help us find the right one
            val allKeys = getAllJsonKeys(cleanXml)
            val debugSuffix = if (allKeys.isNotEmpty()) " (Avail: ${allKeys.joinToString(",")})" else ""

            return SicenetProfile(
                name = name,
                enrollmentId = enrollment,
                career = career,
                semester = if (semester == "Unknown") "Unknown$debugSuffix" else semester,
                specialty = specialty,
                earnedCredits = earnedCredits,
                status = status,
                rawResponse = xml
            )

        } else {
            // Standard XML extraction
            // Also try strPrefix for XML just in case
            val n1 = extractTag(cleanXml, "nombre")
            val n2 = extractTag(cleanXml, "strNombre")
            name = if (n1 != "Unknown") n1 else n2
            
            val e1 = extractTag(cleanXml, "matricula")
            val e2 = extractTag(cleanXml, "strMatricula")
            enrollment = if (e1 != "Unknown") e1 else e2

            career = extractTag(cleanXml, "carrera")
            status = extractTag(cleanXml, "estatus")
            specialty = extractTag(cleanXml, "especialidad")
            
            // XML fallback variations
            // Semestre variations based on screenshot and common SOAP patterns
            val s1 = extractTag(cleanXml, "semestre")
            val s2 = extractTag(cleanXml, "semestreActual")
            val s3 = extractTag(cleanXml, "semActual") 
            semester = listOf(s1, s2, s3).firstOrNull { it != "Unknown" } ?: "Unknown"

            // Credits variations based on screenshot "Cdts. Reunidos"
            val c1 = extractTag(cleanXml, "creditosAcumulados")
            val c2 = extractTag(cleanXml, "creditos")
            val c3 = extractTag(cleanXml, "creditosReunidos")
            val c4 = extractTag(cleanXml, "cdtsReunidos")
             val c5 = extractTag(cleanXml, "totalCreditos")
            earnedCredits = listOf(c1, c2, c3, c4, c5).firstOrNull { it != "Unknown" } ?: "Unknown"
            
             return SicenetProfile(
                name = name,
                enrollmentId = enrollment,
                career = career,
                semester = semester,
                specialty = specialty,
                earnedCredits = earnedCredits,
                status = status,
                rawResponse = xml
            )
        }
    }

    private fun extractTag(xml: String, tagName: String): String {
        try {
            // Updated to be CASE INSENSITIVE
            val pattern = "<(?:\\w+:)?$tagName>(.*?)</(?:\\w+:)?$tagName>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val match = pattern.find(xml)
            return match?.groupValues?.get(1)?.trim() ?: "Unknown"
        } catch (e: Exception) {
            return "Error"
        }
    }

    private fun extractJson(text: String, key: String): String {
        try {
            // Matches "key": "value" or "key": 123 ignoring whitespace and quotes around value
            val pattern = "\"$key\"\\s*:\\s*\"?([^\"},]+)\"?".toRegex(RegexOption.IGNORE_CASE)
            return pattern.find(text)?.groupValues?.get(1)?.trim() ?: "Unknown"
        } catch (e: Exception) {
            return "Error"
        }
    }

    private fun getAllJsonKeys(text: String): List<String> {
        try {
            // Find all strings followed by a colon
            val pattern = "\"([^\"]+)\"\\s*:".toRegex()
            return pattern.findAll(text).map { it.groupValues[1] }.toList()
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
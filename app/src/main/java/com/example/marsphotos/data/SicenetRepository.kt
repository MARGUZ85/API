package com.example.marsphotos.data

import com.example.marsphotos.data.model.LoginResult
import com.example.marsphotos.data.model.SicenetProfile
import com.example.marsphotos.network.SicenetService

class SicenetRepository {

    private val service = SicenetService()

    suspend fun login(user: String, pass: String): LoginResult {
        val result = service.login(user, pass)
        println("SicenetRepository: Login Raw Response: $result") // Log for debugging

        if (result == null) return LoginResult.Error("Network Error or Empty Response")

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
        val xmlResponse = service.getProfile() ?: return null
        println("SicenetRepository: Profile Raw Response: $xmlResponse") // Log for debugging
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
            name = extractJson(cleanXml, "nombre")
            enrollment = extractJson(cleanXml, "matricula")
            career = extractJson(cleanXml, "carrera")
            // FORCE DEBUGGING: If name is unknown, put raw json in name to see it
            val finalName = if (name == "Unknown") "DEBUG: ${cleanXml.take(150)}" else name
            
            status = extractJson(cleanXml, "estatus")
            specialty = extractJson(cleanXml, "especialidad")

            // Semester variations
            val s1 = extractJson(cleanXml, "semestre")
            val s2 = extractJson(cleanXml, "semestreActual")
            val s3 = extractJson(cleanXml, "periodo")
            semester = listOf(s1, s2, s3).firstOrNull { it != "Unknown" } ?: "Unknown"

            // Credits variations
            val c1 = extractJson(cleanXml, "creditosAcumulados")
            val c2 = extractJson(cleanXml, "creditos")
            val c3 = extractJson(cleanXml, "creditosAprobados")
            val c4 = extractJson(cleanXml, "totalCreditos")
            earnedCredits = listOf(c1, c2, c3, c4).firstOrNull { it != "Unknown" } ?: "Unknown"

            // DEBUG: If still unknown, append ALL keys found in the JSON to help us find the right one
            val allKeys = getAllJsonKeys(cleanXml)
            val debugSuffix = if (allKeys.isNotEmpty()) " (Avail: ${allKeys.joinToString(",")})" else ""

            return SicenetProfile(
                name = name,
                enrollmentId = enrollment,
                career = career,
                semester = if (semester == "Unknown") "Unknown$debugSuffix" else semester,
                specialty = specialty,
                earnedCredits = if (earnedCredits == "Unknown") "Unknown$debugSuffix" else earnedCredits,
                status = status,
                rawResponse = xml
            )

        } else {
            // Standard XML extraction
            name = extractTag(cleanXml, "nombre")
            enrollment = extractTag(cleanXml, "matricula")
            career = extractTag(cleanXml, "carrera")
            status = extractTag(cleanXml, "estatus")
            specialty = extractTag(cleanXml, "especialidad")
            
            // XML fallback variations
            val s1 = extractTag(cleanXml, "semestre")
            val s2 = extractTag(cleanXml, "semestreActual")
            semester = if (s1 != "Unknown") s1 else s2

            val c1 = extractTag(cleanXml, "creditosAcumulados")
            val c2 = extractTag(cleanXml, "creditos")
            earnedCredits = if (c1 != "Unknown") c1 else c2
            
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
            val pattern = "<(?:\\w+:)?$tagName>(.*?)</(?:\\w+:)?$tagName>".toRegex(RegexOption.DOT_MATCHES_ALL)
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
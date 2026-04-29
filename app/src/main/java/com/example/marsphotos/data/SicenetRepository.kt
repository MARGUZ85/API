package com.example.marsphotos.data

import com.example.marsphotos.data.model.LoginResult
import com.example.marsphotos.data.model.SicenetProfile
import com.example.marsphotos.network.SicenetService

/**
 * [CAPA DE DATOS - REPOSITORIO CENTRAL]
 * Esta clase es el "Director de Orquesta" de los datos online. 
 * Aplica el patrón "Single Source of Truth" (Fuente Única de Verdad).
 * 
 * Qué hace exactamente:
 * 1. Va a la red (usando `SicenetService.kt`) a pedir información cruda.
 * 2. Valida que el servidor de la escuela no haya mandado un error ("Página en mantenimiento").
 * 3. Le da esa info horrible al `SicenetParser.kt` para que la limpie.
 * 4. Pasa esa info limpia a la base de datos o a la pantalla.
 */
class SicenetRepository(private val service: SicenetService) {

    // Variable para almacenar temporalmente la respuesta del último login exitoso.
    // Se usa como respaldo si la petición de 'Perfil' falla, ya que el login suele traer los datos del alumno.
    private var cachedLoginResponse: String? = null

    /**
     * [INICIO DE SESIÓN]
     * Esta es una función SUSPENDIDA. Significa que se ejecuta dentro de una corrutina.
     * No bloquea la aplicación mientras espera la respuesta del servidor.
     * 
     * @param user Matrícula del alumno.
     * @param pass Contraseña del portal Sicenet.
     * @return LoginResult indicando si fue Success (Éxito) o Error.
     */
    suspend fun login(user: String, pass: String): LoginResult {
        // Llamada al servicio de red. Este método también es 'suspend' y corre en Dispatchers.IO
        val result = service.login(user, pass)
        
        println("SicenetRepository: Respuesta Cruda del Login: $result") 

        if (result == null) return LoginResult.Error("Error de Red: El servidor no respondió.")

        // Cacheamos la respuesta por si necesitamos extraer el perfil de aquí más tarde
        cachedLoginResponse = result

        // VALIDACIÓN 1: El servidor a veces redirige a una página HTML de error
        if (result.trim().startsWith("<html", ignoreCase = true)) {
             return LoginResult.Error("Error del Servidor: Se recibió HTML en lugar de XML. Posible bloqueo de sesión.")
        }

        // VALIDACIÓN 2: Detectar fallos estructurales de SOAP (Protocolo de comunicación del servidor)
        if (result.contains(":Fault>", ignoreCase = true)) {
             val faultString = result.substringAfter("<faultstring>").substringBefore("</faultstring>")
             return LoginResult.Error("Error del Servidor (SOAP): $faultString")
        }

        // VALIDACIÓN 3: Verificar acceso concedido. Sicenet usa JSON embebido en XML.
        // Buscamos la clave '"acceso":true' dentro de la cadena de texto.
        val isValid = result.contains("\"acceso\":true", ignoreCase = true) ||
                      result.contains("\"acceso\": true", ignoreCase = true) ||
                      result.contains("&quot;acceso&quot;:true", ignoreCase = true) ||
                      result.contains("<accesoLoginResult>true</accesoLoginResult>")

        return if (isValid) {
            LoginResult.Success(result)
        } else {
             // Si el login falló, intentamos extraer el motivo del error del XML
             if (result.contains("<accesoLoginResult />") || result.contains("<accesoLoginResult/>")) {
                 return LoginResult.Error("Credenciales incorrectas: Verifica tu Matrícula o Contraseña.")
             }

            val failPattern = "<(?:\\w+:)?accesoLoginResult>(.*?)</(?:\\w+:)?accesoLoginResult>".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = failPattern.find(result)

            if (match != null) {
                val content = match.groupValues[1]
                LoginResult.Error("Acceso Denegado: $content")
            } else {
                LoginResult.Error("Estructura de respuesta desconocida. Revisa los logs.")
            }
        }
    }

    /**
     * [OBTENER PERFIL]
     * Obtiene la información académica del alumno.
     * Utiliza una estrategia de respaldo: si falla la consulta directa al perfil,
     * intenta extraer los datos de la respuesta del login que guardamos en caché.
     */
    suspend fun getPerfil(): SicenetProfile? {
        var xmlResponse = service.getProfile()
        
        println("SicenetRepository: Respuesta Cruda del Perfil: $xmlResponse") 

        // ESTRATEGIA DE RESPALDO (FALLBACK)
        // Sicenet a veces bloquea la consulta de perfil justo después del login.
        // Si detectamos fallo, usamos la caché del login.
        val isFailure = xmlResponse == null || 
                        xmlResponse.contains(":Fault>", ignoreCase = true) || 
                        xmlResponse.contains("Server", ignoreCase = true) || 
                        xmlResponse.trim().startsWith("<html", ignoreCase = true)

        if (isFailure && !cachedLoginResponse.isNullOrEmpty()) {
            println("SicenetRepository: Falló getProfile. Usando datos del Login guardados en caché.")
            xmlResponse = cachedLoginResponse
        }

        if (xmlResponse == null) return null
        
        // Delegamos el procesamiento del texto a la función de parseo
        return parseProfileFromXml(xmlResponse)
    }

    /**
     * [PARSEAR PERFIL]
     * Convierte el texto gigante retornado por el servidor en un objeto SicenetProfile limpio.
     * Implementa lógica para manejar tanto respuestas XML como JSON embebido.
     */
    private fun parseProfileFromXml(xml: String): SicenetProfile {
        // Limpiamos los caracteres escapados típicos de XML (&lt; -> <, &gt; -> >)
        var cleanXml = xml
        if (cleanXml.contains("&lt;") && cleanXml.contains("&gt;")) {
            cleanXml = cleanXml.replace("&lt;", "<").replace("&gt;", ">")
        }

        // Identificamos el formato: Si contiene '":' es probable que tenga JSON embebido
        val isJson = cleanXml.contains("\":") || cleanXml.contains("\": ")

        val name: String
        val enrollment: String
        val career: String
        val semester: String
        val specialty: String
        val earnedCredits: String
        val status: String

        if (isJson) {
            // EXTRACCIÓN JSON: Usamos expresiones regulares para buscar las claves.
            // Buscamos múltiples variantes de nombres de claves porque el servidor no es consistente.
            val n1 = extractJson(cleanXml, "nombre")
            val n2 = extractJson(cleanXml, "strNombre")
            val n3 = extractJson(cleanXml, "nombreCompleto")
            name = listOf(n1, n2, n3).firstOrNull { it != "Unknown" } ?: "Desconocido"

            val e1 = extractJson(cleanXml, "matricula")
            val e2 = extractJson(cleanXml, "strMatricula")
            enrollment = listOf(e1, e2).firstOrNull { it != "Unknown" } ?: "Desconocido"

            val c1 = extractJson(cleanXml, "carrera")
            val c2 = extractJson(cleanXml, "strCarrera")
            career = listOf(c1, c2).firstOrNull { it != "Unknown" } ?: "Desconocida"
            
            val st1 = extractJson(cleanXml, "estatus")
            val st2 = extractJson(cleanXml, "situacion")
            status = listOf(st1, st2).firstOrNull { it != "Unknown" } ?: "Desconocido"

            val sp1 = extractJson(cleanXml, "especialidad")
            val sp2 = extractJson(cleanXml, "strEspecialidad")
            specialty = listOf(sp1, sp2).firstOrNull { it != "Unknown" } ?: "Desconocida"

            semester = extractJson(cleanXml, "semestre").let { if(it == "Unknown") extractJson(cleanXml, "semActual") else it }

            earnedCredits = extractJson(cleanXml, "cdtosAcumulados").let { if(it == "Unknown") extractJson(cleanXml, "creditos") else it }

            return SicenetProfile(
                name = name,
                enrollmentId = enrollment,
                career = career,
                semester = semester,
                specialty = specialty,
                earnedCredits = earnedCredits,
                status = status
            )

        } else {
            // EXTRACCIÓN XML: Buscamos etiquetas tradicionales <etiqueta>valor</etiqueta>
            name = extractTag(cleanXml, "nombre").let { if(it == "Unknown") extractTag(cleanXml, "strNombre") else it }
            enrollment = extractTag(cleanXml, "matricula").let { if(it == "Unknown") extractTag(cleanXml, "strMatricula") else it }
            career = extractTag(cleanXml, "carrera")
            status = extractTag(cleanXml, "estatus")
            specialty = extractTag(cleanXml, "especialidad")
            semester = extractTag(cleanXml, "semestre").let { if(it == "Unknown") extractTag(cleanXml, "semActual") else it }
            earnedCredits = extractTag(cleanXml, "cdtsReunidos").let { if(it == "Unknown") extractTag(cleanXml, "creditosAcumulados") else it }
            
             return SicenetProfile(
                name = name,
                enrollmentId = enrollment,
                career = career,
                semester = semester,
                specialty = specialty,
                earnedCredits = earnedCredits,
                status = status
            )
        }
    }

    /**
     * Función interna que usa RegEx para extraer el contenido de una etiqueta XML.
     * Ignora mayúsculas/minúsculas y prefijos de espacio de nombres.
     */
    private fun extractTag(xml: String, tagName: String): String {
        try {
            val pattern = "<(?:\\w+:)?$tagName>(.*?)</(?:\\w+:)?$tagName>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val match = pattern.find(xml)
            return match?.groupValues?.get(1)?.trim() ?: "Unknown"
        } catch (e: Exception) {
            return "Error"
        }
    }

    /**
     * Función interna que usa RegEx para extraer el valor de una clave en una cadena JSON cruda.
     */
    private fun extractJson(text: String, key: String): String {
        try {
            val pattern = "\"$key\"\\s*:\\s*\"?([^\"},]+)\"?".toRegex(RegexOption.IGNORE_CASE)
            return pattern.find(text)?.groupValues?.get(1)?.trim() ?: "Unknown"
        } catch (e: Exception) {
            return "Error"
        }
    }
}
package com.example.marsphotos.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * [CAPA DE RED]
 * Servicio encargado de la comunicación HTTP/SOAP.
 * Implementa el patrón Singleton implícito al ser instanciado una sola vez por el contenedor.
 * Aquí se definen las funciones fundamentales para hablar con el servidor de Sicenet.
 */
class SicenetService {

    // [GESTIÓN de COOKIES - MUY IMPORTANTE]
    // Esta parte es clave para que Sicenet funcione. 
    // Sicenet no usa "Tokens" (JWT) modernos, usa una tecnología antigua.
    // Cuando inicias sesión, Sicenet te da una "Cookie" (una credencial temporal).
    // Este "CookieJar" (Tarro de galletas) atrapa esa credencial y la guarda en la memoria.
    // Luego, en cada petición que hacemos (como pedir calificaciones), OkHttp saca la credencial 
    // de este tarro y la envía automáticamente de regreso al servidor para decir "Soy yo, sigo conectado".
    private val cookieJar = object : CookieJar {
        private val cookieStore = HashMap<String, Cookie>()

        override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
            if (newCookies.isNotEmpty()) {
                synchronized(this) {
                    val logMsg = StringBuilder("COOKIES SAVED for ${url.host}:\n")
                    for (cookie in newCookies) {
                        cookieStore[cookie.name] = cookie
                        logMsg.append("- ${cookie.name}=${cookie.value}\n")
                    }
                    println("SicenetService: $logMsg")
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            synchronized(this) {
                val validCookies = cookieStore.values.toList()
                if (validCookies.isNotEmpty()) {
                    val logMsg = validCookies.joinToString { "${it.name}=${it.value.take(5)}..." }
                    println("SicenetService: Loading cookies: $logMsg")
                    // Opcional: ¿Guardar un registro en memoria interna solo si falla para evitar spam? 
                    // Por ahora, confiamos en los logs de impresión (consola) normales.
                } else {
                    println("SicenetService: NO COOKIES FOUND for ${url.host}")
                }
                return validCookies
            }
        }
    }

    // Configuramos nuestro "Navegador Invisible" (OkHttpClient)
    // Le pasamos nuestro "Tarro de galletas" para que recuerde la sesión.
    // Desactivamos los redireccionamientos automáticos porque Sicenet a veces hace trucos raros con la conexión.
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(false) // Deshabilita redirecciones automáticas para evitar perder datos
        .followSslRedirects(false)
        .build()

    // SOAP 1.1 Media Type
    private val mediaType = "text/xml; charset=utf-8".toMediaType()

    // CORRECTED URL
    private val SERVICE_URL = "https://sicenet.surguanajuato.tecnm.mx/ws/wsalumnos.asmx" 
    private var currentUrl = SERVICE_URL // URL cambiable para manejar redirecciones del servidor

    // [EJECUCIÓN ASÍNCRONA - CORRUTINAS]
    // La palabra clave 'suspend' indica que esta función tiene el superpoder de "pausarse" 
    // mientras espera al servidor de internet, sin trabar ni congelar el teléfono del usuario.
    suspend fun login(user: String, pass: String): String? {
        // withContext(Dispatchers.IO) mueve este trabajo pesado a un hilo secundario especial 
        // para cosas de Input/Output (Entrada/Salida de internet), dejando libre el hilo principal de la pantalla.
        return withContext(Dispatchers.IO) {
            val soapAction = "\"http://tempuri.org/accesoLogin\""
            
            // Este es el idioma original en el que habla Sicenet (SOAP XML).
            // Construimos un "Sobre" (Envelope) digital que contiene nuestra matrícula y contraseña,
            // exactamente igual a como el portal web original de la escuela lo envía a su servidor.
            val soapBody = """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <accesoLogin xmlns="http://tempuri.org/">
      <strMatricula>$user</strMatricula>
      <strContrasenia>$pass</strContrasenia>
      <tipoUsuario>ALUMNO</tipoUsuario>
    </accesoLogin>
  </soap:Body>
</soap:Envelope>"""

            // Restablece la URL a la original en cada intento de login nuevo
            currentUrl = SERVICE_URL 

            val request = Request.Builder()
                .url(currentUrl)
                .post(soapBody.toRequestBody(mediaType))
                .addHeader("SOAPAction", soapAction)
                .build()

            makeNetworkCall(request, soapBody, soapAction) // Pasa los datos extra por si necesita re-intentar
        }
    }

    // Función suspendida para obtener el perfil del alumno.
    suspend fun getProfile(): String? {
        // Ejecutamos en el hilo IO porque es una operación de red que toma tiempo.
        return withContext(Dispatchers.IO) {
            val soapAction = "\"http://tempuri.org/getAlumnoAcademicoWithLineamiento\""
            
            val soapBody = """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getAlumnoAcademicoWithLineamiento xmlns="http://tempuri.org/" />
  </soap:Body>
</soap:Envelope>"""

            val request = Request.Builder()
                .url(currentUrl)
                .post(soapBody.toRequestBody(mediaType))
                .addHeader("SOAPAction", soapAction)
                .build()

            makeNetworkCall(request, soapBody, soapAction)
        }
    }

    // Obtiene la carga académica (materias actuales) mediante una petición SOAP.
    suspend fun getCargaAcademica(): String? {
        return withContext(Dispatchers.IO) {
            val soapAction = "\"http://tempuri.org/getCargaAcademicaByAlumno\""
            
            val soapBody = """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getCargaAcademicaByAlumno xmlns="http://tempuri.org/" />
  </soap:Body>
</soap:Envelope>"""

            val request = Request.Builder()
                .url(currentUrl)
                .post(soapBody.toRequestBody(mediaType))
                .addHeader("SOAPAction", soapAction)
                .build()

            makeNetworkCall(request, soapBody, soapAction)
        }
    }

    // Obtiene el historial académico (Kardex) completo.
    suspend fun getCardex(): String? {
        return withContext(Dispatchers.IO) {
            val soapAction = "\"http://tempuri.org/getAllKardexConPromedioByAlumno\""
            
            val soapBody = """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getAllKardexConPromedioByAlumno xmlns="http://tempuri.org/">
      <aluLineamiento>1</aluLineamiento>
    </getAllKardexConPromedioByAlumno>
  </soap:Body>
</soap:Envelope>"""

            val request = Request.Builder()
                .url(currentUrl)
                .post(soapBody.toRequestBody(mediaType))
                .addHeader("SOAPAction", soapAction)
                .build()

            makeNetworkCall(request, soapBody, soapAction)
        }
    }

    // Obtiene las calificaciones por unidad (parciales).
    suspend fun getCalifUnidades(): String? {
        return withContext(Dispatchers.IO) {
            val soapAction = "\"http://tempuri.org/getCalifUnidadesByAlumno\""
            
            val soapBody = """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getCalifUnidadesByAlumno xmlns="http://tempuri.org/" />
  </soap:Body>
</soap:Envelope>"""

            val request = Request.Builder()
                .url(currentUrl)
                .post(soapBody.toRequestBody(mediaType))
                .addHeader("SOAPAction", soapAction)
                .build()

            makeNetworkCall(request, soapBody, soapAction)
        }
    }

    // Obtiene las calificaciones finales de los semestres.
    suspend fun getCalifFinales(): String? {
        return withContext(Dispatchers.IO) {
            val soapAction = "\"http://tempuri.org/getAllCalifFinalByAlumnos\""
            
            val soapBody = """<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <getAllCalifFinalByAlumnos xmlns="http://tempuri.org/">
      <bytModEducativo>1</bytModEducativo>
    </getAllCalifFinalByAlumnos>
  </soap:Body>
</soap:Envelope>"""

            val request = Request.Builder()
                .url(currentUrl)
                .post(soapBody.toRequestBody(mediaType))
                .addHeader("SOAPAction", soapAction)
                .build()

            makeNetworkCall(request, soapBody, soapAction)
        }
    }

    private fun makeNetworkCall(originalRequest: Request, soapBody: String, soapAction: String): String? {
        try {
            // Primer Intento
            println("SicenetService: Executing request to ${originalRequest.url}")
            var response = client.newCall(originalRequest).execute()
            
            // Maneja redireccionamientos especiales de servidores ASP.NET (Cuando verifican el soporte de cookies)
            if (response.code in 300..399) {
                val location = response.header("Location")
                if (location != null && location.contains("AspxAutoDetectCookieSupport=1")) {
                    println("SicenetService: Detected Cookie Check Redirect to: $location")
                    
                    // Construye una URL completa absoluta para el redireccionamiento
                    val redirectUrl = if (location.startsWith("http")) {
                        location
                    } else {
                        // Resuelve la ruta relativa
                         val base = SERVICE_URL.toHttpUrlOrNull()!!
                         base.resolve(location)?.toString() ?: location
                    }

                    response.close()

                    println("SicenetService: Retrying at $redirectUrl")
                    currentUrl = redirectUrl // Guardamos la nueva URL confirmada
                    
                    val newRequest = originalRequest.newBuilder()
                        .url(currentUrl)
                        .post(soapBody.toRequestBody(mediaType))
                        .build()
                        
                    response = client.newCall(newRequest).execute()
                }
            }

            println("SicenetService: Final Response Code: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                // El cuerpo de la respuesta se lee aquí.
                return responseBody
            } else {
                println("SicenetService: Fallo en la petición: ${response.code} ${response.message}")
                val errorBody = response.body?.string()
                return errorBody 
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("SicenetService: Excepción detectada: ${e.message}")
            return null
        }
    }
}

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
 * [NETWORK LAYER]
 * Service encargado de la comunicación HTTP/SOAP.
 * Implementa el patrón Singleton implícito al ser instanciado una sola vez por el contenedor.
 */
class SicenetService {

    // [COOKIE MANAGEMENT]
    // Implementación anónima de CookieJar para persistencia de sesión en memoria.
    // Es CRÍTICO para servidores ASP.NET que dependen de JSESSIONID o ASP.NET_SessionId.
    // [COOKIE MANAGEMENT]
    // Simplified CookieJar to avoid Host Mismatch issues (e.g. sicenet vs www.sicenet)
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
                    // Optional: Log to debug storage only if things fail, to avoid spam? 
                    // For now, let's trust the print logs, or we can add a summarized line.
                } else {
                    println("SicenetService: NO COOKIES FOUND for ${url.host}")
                    com.example.marsphotos.data.DebugStorage.updateResponse("WARNING: No cookies found for request to ${url.host}")
                }
                return validCookies
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(false) // Disable auto-redirects to prevent POST->GET downgrade
        .followSslRedirects(false)
        .build()

    // SOAP 1.1 Media Type
    private val mediaType = "text/xml; charset=utf-8".toMediaType()

    // CORRECTED URL
    private val SERVICE_URL = "https://sicenet.surguanajuato.tecnm.mx/ws/wsalumnos.asmx" 
    private var currentUrl = SERVICE_URL // Mutable URL to handle redirects/cookieless sessions

    // [ASYNCHRONOUS EXECUTION]
    suspend fun login(user: String, pass: String): String? {
        return withContext(Dispatchers.IO) {
            val soapAction = "\"http://tempuri.org/accesoLogin\""
            
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

            // Reset URL on new login
            currentUrl = SERVICE_URL 

            val request = Request.Builder()
                .url(currentUrl)
                .post(soapBody.toRequestBody(mediaType))
                .addHeader("SOAPAction", soapAction)
                .build()

            makeNetworkCall(request, soapBody, soapAction) // Pass body/action for potential retry
        }
    }

    suspend fun getProfile(): String? {
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
            // First Attempt
            println("SicenetService: Executing request to ${originalRequest.url}")
            var response = client.newCall(originalRequest).execute()
            
            // Handle ASP.NET Cookieless / AutoDetect Redirect
            if (response.code in 300..399) {
                val location = response.header("Location")
                if (location != null && location.contains("AspxAutoDetectCookieSupport=1")) {
                    println("SicenetService: Detected Cookie Check Redirect to: $location")
                    
                    // Construct absolute URL for the redirect
                    val redirectUrl = if (location.startsWith("http")) {
                        location
                    } else {
                        // Resolve relative path
                         val base = SERVICE_URL.toHttpUrlOrNull()!!
                         base.resolve(location)?.toString() ?: location
                    }

                    response.close()

                    println("SicenetService: Retrying at $redirectUrl")
                    currentUrl = redirectUrl // Update class-level URL
                    
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
                // println("SicenetService: Success. Body length: ${responseBody?.length}")
                return responseBody
            } else {
                println("SicenetService: Request failed: ${response.code} ${response.message}")
                val errorBody = response.body?.string()
                // println("SicenetService: Error Body: $errorBody")
                return errorBody 
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("SicenetService: Exception: ${e.message}")
            return null
        }
    }
}

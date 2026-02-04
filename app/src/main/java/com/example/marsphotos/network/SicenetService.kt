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

class SicenetService {

    // Persistent CookieJar implementation
    private val cookieJar = object : CookieJar {
        private val cookieStore = HashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: ArrayList()
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

    suspend fun login(user: String, pass: String): String? {
        return withContext(Dispatchers.IO) {
            val soapAction = "\"http://tempuri.org/accesoLogin\""
            
            // SOAP 1.1 Body - Cleanest standard structure
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

            // Reset URL on new login attempt if needed, or keep previous if sticking to session? 
            // Better to start fresh usually, but if 'Cookieless' is strict, we might need to handle it.
            // For now, let's start fresh:
            currentUrl = SERVICE_URL 

            var request = Request.Builder()
                .url(currentUrl)
                .post(soapBody.toRequestBody(mediaType))
                .addHeader("SOAPAction", soapAction)
                .build()

            try {
                // First Attempt
                println("SicenetService: Attempting login for user: $user")
                var response = client.newCall(request).execute()
                
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
                        
                        request = request.newBuilder()
                            .url(currentUrl)
                            .post(soapBody.toRequestBody(mediaType))
                            .build()
                            
                        response = client.newCall(request).execute()
                    }
                }

                println("SicenetService: Final Response Code: ${response.code}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    println("SicenetService: Login Successful. Body length: ${responseBody?.length}")
                    return@withContext responseBody
                } else {
                    println("SicenetService: Login failed: ${response.code} ${response.message}")
                    val errorBody = response.body?.string()
                    println("SicenetService: Error Body snippet: ${errorBody?.take(200)}")
                    return@withContext errorBody 
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("SicenetService: Login Exception: ${e.message}")
                null
            }
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
                .url(currentUrl) // Use the potentially verified/redirected URL
                .post(soapBody.toRequestBody(mediaType))
                .addHeader("SOAPAction", soapAction)
                .build()


            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    return@withContext response.body?.string()
                } else {
                    println("Profile failed: ${response.code} ${response.message}")
                     val errorBody = response.body?.string()
                    return@withContext errorBody
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Profile Exception: ${e.message}")
                null
            }
        }
    }
}

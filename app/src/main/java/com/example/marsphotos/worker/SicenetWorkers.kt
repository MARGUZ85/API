package com.example.marsphotos.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.marsphotos.data.SicenetLocalRepository
import com.example.marsphotos.data.SicenetParser
import com.example.marsphotos.network.SicenetService
import kotlinx.serialization.InternalSerializationApi

/**
 * [TRABAJO EN SEGUNDO PLANO - LOGIN]
 * Este Worker se encarga de realizar el inicio de sesión y obtener el perfil básico.
 * Al usar CoroutineWorker, permite realizar estas operaciones de red de forma asíncrona.
 */
class SicenetLoginWorker(
    context: Context,
    params: WorkerParameters,
    private val sicenetService: SicenetService,
    private val sicenetLocalRepository: SicenetLocalRepository
) : CoroutineWorker(context, params) {

    // doWork() es la función que ejecuta WorkManager en un hilo secundario.
    override suspend fun doWork(): Result {
        val user = inputData.getString("USER") ?: return Result.failure()
        val pass = inputData.getString("PASS") ?: return Result.failure()

        return try {
            // 1. Intentar inicio de sesión
            val loginResult = sicenetService.login(user, pass)
            
            // Verificamos si la respuesta contiene señales de éxito
            if (loginResult == null || !loginResult.contains("true", ignoreCase = true)) {
                return Result.failure(workDataOf("ERROR" to "Fallo en el inicio de sesión: Credenciales incorrectas"))
            }

            // 2. Intentar obtener el perfil (paso requerido para validar la sesión completa)
            val profileResult = sicenetService.getProfile()
            
            if (profileResult == null) {
                return Result.failure(workDataOf("ERROR" to "No se pudo recuperar la información del perfil"))
            }

            // Pasamos los datos del perfil como salida para que otros Workers o la UI puedan verlos
            val output = workDataOf(
                "KEY_TYPE" to "PROFILE",
                "KEY_DATA" to profileResult
            )

            Result.success(output)

        } catch (e: Exception) {
            Log.e("SicenetLoginWorker", "Error durante login o perfil: ${e.message}")
            Result.failure(workDataOf("ERROR" to (e.message ?: "Error desconocido en LoginWorker")))
        }
    }
}

/**
 * [TRABAJO EN SEGUNDO PLANO - SINCRONIZACIÓN]
 * Este Worker descarga y guarda datos específicos (Kardex, Carga, Calificaciones).
 * Se ejecuta de forma independiente para no bloquear la aplicación.
 */
class SicenetSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val sicenetService: SicenetService,
    private val sicenetLocalRepository: SicenetLocalRepository
) : CoroutineWorker(context, params) {

    /**
     * Tarea principal de sincronización.
     * WorkManager garantiza que esto se ejecute incluso si el usuario sale de la app.
     */
    @OptIn(InternalSerializationApi::class)
    override suspend fun doWork(): Result {
        // Obtenemos qué característica (feature) se debe sincronizar
        val feature = inputData.getString("FEATURE") ?: return Result.failure()

        return try {
            // 1. Descargar los datos crudos (XML) del servidor según la característica
            val resultXml = when (feature) {
                "LOAD" -> sicenetService.getCargaAcademica()
                "CARDEX" -> sicenetService.getCardex()
                "GRADES_UNITS" -> sicenetService.getCalifUnidades()
                "GRADES_FINAL" -> sicenetService.getCalifFinales()
                else -> null
            }

            if (resultXml == null) {
                return Result.failure(workDataOf("ERROR" to "Error de red: No se recibió respuesta para $feature"))
            }

            // 2. Parsear y Guardar los datos en la base de datos local
            when (feature) {
                "LOAD" -> {
                    val entities = SicenetParser.parseAcademicLoad(resultXml)
                    sicenetLocalRepository.saveAcademicLoad(entities)
                }
                "CARDEX" -> {
                    val entities = SicenetParser.parseCardex(resultXml)
                    sicenetLocalRepository.saveCardex(entities)
                }
                "GRADES_UNITS" -> {
                    val entities = SicenetParser.parseUnitGrades(resultXml)
                    sicenetLocalRepository.saveUnitGrades(entities)
                }
                "GRADES_FINAL" -> {
                    val entities = SicenetParser.parseFinalGrades(resultXml)
                    sicenetLocalRepository.saveFinalGrades(entities)
                }
            }
            // Si llegamos aquí, la sincronización fue exitosa
            Result.success()

        } catch (e: Exception) {
            Log.e("SicenetSyncWorker", "Error sincronizando $feature: ${e.message}")
            Result.failure(workDataOf("ERROR" to (e.message ?: "Error desconocido en SyncWorker")))
        }
    }
}

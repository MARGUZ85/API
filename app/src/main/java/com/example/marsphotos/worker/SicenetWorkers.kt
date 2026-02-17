package com.example.marsphotos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.marsphotos.data.SicenetLocalRepository
import com.example.marsphotos.data.SicenetParser
import com.example.marsphotos.network.SicenetService

import android.util.Log

// --- WORKER 1: Network Operations (Login or Fetch) ---

class SicenetLoginWorker(
    context: Context,
    params: WorkerParameters,
    private val sicenetService: SicenetService,
    private val sicenetLocalRepository: SicenetLocalRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val user = inputData.getString("USER") ?: return Result.failure()
        val pass = inputData.getString("PASS") ?: return Result.failure()

        try {
            // 1. Login
            val loginResult = sicenetService.login(user, pass)
            
            // Validate Login
            if (loginResult == null || !loginResult.contains("true", ignoreCase = true)) {
                return Result.failure(Data.Builder().putString("ERROR", "Login Failed").build())
            }

            // 2. Get Profile (As per requirement "a)")
            val profileResult = sicenetService.getProfile()
            
            if (profileResult == null) {
                return Result.failure(Data.Builder().putString("ERROR", "Failed to get Profile").build())
            }

            // Pass Profile Data to Next Worker
            val output = Data.Builder()
                .putString("KEY_TYPE", "PROFILE")
                .putString("KEY_DATA", profileResult)
                .build()

            return Result.success(output)

        } catch (e: Exception) {
            return Result.failure()
        }
    }
}

class SicenetSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val sicenetService: SicenetService,
    private val sicenetLocalRepository: SicenetLocalRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val feature = inputData.getString("FEATURE") ?: return Result.failure()

        return try {
            val resultXml = when (feature) {
                "LOAD" -> sicenetService.getCargaAcademica()
                "CARDEX" -> sicenetService.getCardex()
                "GRADES_UNITS" -> sicenetService.getCalifUnidades()
                "GRADES_FINAL" -> sicenetService.getCalifFinales()
                else -> null
            }

            if (resultXml == null) {
                com.example.marsphotos.data.DebugStorage.updateResponse("ERROR: Network/Service returned null for $feature")
                Log.e("SicenetWorker", "ResultXML is null for $feature")
                return Result.failure(Data.Builder().putString("ERROR", "Network Error").build())
            }

            // DEBUG: Save response
            com.example.marsphotos.data.DebugStorage.updateResponse("Feature: $feature\n\n$resultXml")
            Log.d("SicenetResponse", "Feature: $feature | XML: $resultXml")

            // Parse and Save directly
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

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            com.example.marsphotos.data.DebugStorage.updateResponse("EXCEPTION: ${e.message}")
            Result.failure()
        }
    }
}

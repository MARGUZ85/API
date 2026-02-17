package com.example.marsphotos.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.marsphotos.MarsPhotosApplication
import com.example.marsphotos.data.SicenetParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SicenetSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {

        val feature = inputData.getString("FEATURE")
            ?: return@withContext Result.failure(
                workDataOf("ERROR" to "Feature not provided")
            )

        try {

            val app = applicationContext as MarsPhotosApplication
            val service = app.container.sicenetService
            val localRepository = app.container.sicenetLocalRepository

            val resultXml = when (feature) {
                "LOAD" -> service.getCargaAcademica()
                "CARDEX" -> service.getCardex()
                "GRADES_UNITS" -> service.getCalifUnidades()
                "GRADES_FINAL" -> service.getCalifFinales()
                else -> null
            }

            if (resultXml == null) {
                return@withContext Result.failure(
                    workDataOf("ERROR" to "Network Error")
                )
            }

            when (feature) {
                "LOAD" -> {
                    val entities = SicenetParser.parseAcademicLoad(resultXml)
                    localRepository.saveAcademicLoad(entities)
                }
                "CARDEX" -> {
                    val entities = SicenetParser.parseCardex(resultXml)
                    localRepository.saveCardex(entities)
                }
                "GRADES_UNITS" -> {
                    val entities = SicenetParser.parseUnitGrades(resultXml)
                    localRepository.saveUnitGrades(entities)
                }
                "GRADES_FINAL" -> {
                    val entities = SicenetParser.parseFinalGrades(resultXml)
                    localRepository.saveFinalGrades(entities)
                }
            }

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(
                workDataOf("ERROR" to (e.message ?: "Unknown error"))
            )
        }
    }
}

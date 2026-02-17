package com.example.marsphotos.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.marsphotos.data.SicenetLocalRepository
import com.example.marsphotos.data.SicenetRepository
import com.example.marsphotos.network.SicenetService

class SicenetWorkerFactory(
    private val sicenetService: SicenetService, // Access via SicenetRepository or directly? AppContainer doesn't expose Service publically.
    private val sicenetLocalRepository: SicenetLocalRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        // We will implement the workers next. 
        // For now, prepare the factory logic.
        return when (workerClassName) {
            "com.example.marsphotos.worker.SicenetLoginWorker" -> {
                SicenetLoginWorker(appContext, workerParameters, sicenetService, sicenetLocalRepository)
            }
            "com.example.marsphotos.worker.SicenetSyncWorker" -> {
                SicenetSyncWorker(appContext, workerParameters, sicenetService, sicenetLocalRepository)
            }

            else -> null
        }
    }
}

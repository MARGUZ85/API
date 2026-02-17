package com.example.marsphotos

import android.app.Application
import androidx.work.Configuration
import com.example.marsphotos.data.AppContainer
import com.example.marsphotos.data.DefaultAppContainer
import com.example.marsphotos.worker.SicenetWorkerFactory

class MarsPhotosApplication : Application(), Configuration.Provider {

    /** AppContainer instance used by the rest of classes to obtain dependencies */
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                SicenetWorkerFactory(
                    container.sicenetService,
                    container.sicenetLocalRepository
                )
            )
            .build()
}

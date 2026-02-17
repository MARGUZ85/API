package com.example.marsphotos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.*
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.example.marsphotos.MarsPhotosApplication
import com.example.marsphotos.data.SicenetRepository
import com.example.marsphotos.data.SicenetLocalRepository
import com.example.marsphotos.data.model.LoginResult
import com.example.marsphotos.data.model.SicenetProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.marsphotos.worker.SicenetSyncWorker


sealed interface SicenetUiState {
    object Login : SicenetUiState
    object Loading : SicenetUiState
    data class Success(val profile: SicenetProfile) : SicenetUiState
    data class Error(val message: String) : SicenetUiState
}

enum class SicenetScreen {
    Login,
    Menu,
    Profile,
    Load,
    Cardex,
    GradesUnits,
    GradesFinal,
    Debug
}

class SicenetViewModel(
    private val repository: SicenetRepository,
    private val localRepository: SicenetLocalRepository,
    private val workManager: WorkManager
) : ViewModel() {

    var sicenetUiState by mutableStateOf<SicenetUiState>(SicenetUiState.Login)
        private set

    var currentScreen by mutableStateOf(SicenetScreen.Login)
        private set

    fun navigateTo(screen: SicenetScreen) {
        currentScreen = screen
    }

    val academicLoad = localRepository.academicLoad
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cardex = localRepository.cardex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unitGrades = localRepository.unitGrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val finalGrades = localRepository.finalGrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastUpdateLoad =
        localRepository.getLastUpdate("carga")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastUpdateCardex =
        localRepository.getLastUpdate("cardex")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastUpdateGradesUnits =
        localRepository.getLastUpdate("calif_unidades")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastUpdateGradesFinal =
        localRepository.getLastUpdate("calif_finales")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun iniciarSesion(user: String, pass: String) {
        viewModelScope.launch {
            sicenetUiState = SicenetUiState.Loading

            when (val result = repository.login(user, pass)) {

                is LoginResult.Success -> {
                    val profile = repository.getPerfil()

                    if (profile != null) {
                        sicenetUiState = SicenetUiState.Success(profile)
                        currentScreen = SicenetScreen.Menu
                        // Auto-Sync all features
                        syncAll()
                    } else {
                        sicenetUiState =
                            SicenetUiState.Error("Error getting profile")
                    }
                }

                is LoginResult.Error -> {
                    sicenetUiState =
                        SicenetUiState.Error(result.message)
                }
            }
        }
    }

    var currentWorkId by mutableStateOf<java.util.UUID?>(null)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentWorkInfo: StateFlow<WorkInfo?> = snapshotFlow { currentWorkId }
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else workManager.getWorkInfoByIdFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun syncFeature(feature: String, tag: String) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val data = Data.Builder()
            .putString("FEATURE", feature)
            .build()

        val request = OneTimeWorkRequestBuilder<SicenetSyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag(tag)
            .build()

        // Only track single feature syncs if triggered manually, 
        // or just let the UI observe the last one.
        currentWorkId = request.id 
        workManager.enqueue(request)
    }

    private fun syncAll() {
        val features = listOf(
            "LOAD" to "LOAD",
            "CARDEX" to "CARDEX",
            "GRADES_UNITS" to "GRADES_UNITS",
            "GRADES_FINAL" to "GRADES_FINAL"
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val requests = features.map { (feature, tag) ->
            OneTimeWorkRequestBuilder<SicenetSyncWorker>()
                .setInputData(Data.Builder().putString("FEATURE", feature).build())
                .setConstraints(constraints)
                .addTag(tag)
                .build()
        }

        // Enqueue all unique work
        workManager.enqueue(requests)
        
        // Optionally track the last one or a group? 
        // For simplicity, we just fire them. 
        // The individual screens will update as data arrives in DB.
    }

    // DEBUG
    val lastResponse = com.example.marsphotos.data.DebugStorage.lastResponse
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    companion object {

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                            as MarsPhotosApplication

                SicenetViewModel(
                    repository = application.container.sicenetRepository,
                    localRepository = application.container.sicenetLocalRepository,
                    workManager = WorkManager.getInstance(application)
                )
            }
        }
    }
}



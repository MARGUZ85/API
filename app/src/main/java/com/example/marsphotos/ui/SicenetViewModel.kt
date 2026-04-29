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
    GradesFinal
}

/**
 * El ViewModel es el "Cerebro" de la UI.
 * Aquí se gestiona el estado de las pantallas y se orquestan las corrutinas para la sincronización.
 */
@OptIn(kotlinx.serialization.InternalSerializationApi::class)
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

    // Flujos de datos (Flows) que vienen de la base de datos local (Room).
    // Imagina un "Flow" como una tubería de agua viva conectada a la base de datos.
    // .stateIn convierte esta tubería en un "StateFlow" (un tanque de agua caliente) 
    // que la pantalla (UI) puede ver y reaccionar instantáneamente cuando el agua (los datos) cambian.
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

    // Inicia el proceso de sesión.
    fun iniciarSesion(user: String, pass: String) {
        // viewModelScope.launch lanza una corrutina (un hilo de trabajo ligero) amarrada a esta pantalla.
        // Si el usuario presiona "Atrás" y cierra la app, esta corrutina se cancela automáticamente 
        // para no gastar batería ni memoria a lo tonto.
        viewModelScope.launch {
            // Avisamos a la pantalla que ponga el circulito de "Cargando..."
            sicenetUiState = SicenetUiState.Loading

            // Vamos al Repositorio a intentar el login (esperamos la respuesta sin que la app se trabe)
            when (val result = repository.login(user, pass)) {

                is LoginResult.Success -> {
                    val profile = repository.getPerfil()

                    if (profile != null) {
                        sicenetUiState = SicenetUiState.Success(profile)
                        currentScreen = SicenetScreen.Menu
                        // Sincronización automática de todas las funciones al entrar.
                        syncAll()
                    } else {
                        sicenetUiState =
                            SicenetUiState.Error("Error al obtener el perfil")
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

    // Lanza un trabajo de sincronización para una característica específica usando WorkManager.
    fun syncFeature(feature: String, tag: String) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Solo si hay internet
            .build()
        
        val data = Data.Builder()
            .putString("FEATURE", feature)
            .build()

        val request = OneTimeWorkRequestBuilder<SicenetSyncWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag(tag)
            .build()

        // Guardamos el ID del trabajo actual para que la UI pueda mostrar el progreso.
        currentWorkId = request.id 
        workManager.enqueue(request)
    }

    // Encola todas las tareas de sincronización (Carga, Kardex, etc.) en segundo plano.
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

        // Ejecutamos todos los trabajos en cola (Descargar Carga, Kardex, etc).
        // El WorkManager se encargará de hacer esto de forma invisible en el fondo.
        // Una vez que descargue algo, lo guardará en la Base de Datos (Room),
        // y gracias a nuestra "Tubería" (Flow), la pantalla se actualizará SOLA mágicamente.
        workManager.enqueue(requests)
    }


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



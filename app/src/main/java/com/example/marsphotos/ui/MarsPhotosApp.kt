package com.example.marsphotos.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.marsphotos.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarsPhotosApp() {

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val sicenetViewModel: SicenetViewModel =
        viewModel(factory = SicenetViewModel.Factory)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { MarsTopAppBar(scrollBehavior = scrollBehavior) }
    ) { innerPadding ->

        val contentModifier =
            Modifier.padding(innerPadding).fillMaxSize()

        Surface(
            modifier = contentModifier,
            color = MaterialTheme.colorScheme.background
        ) {

            val uiState = sicenetViewModel.sicenetUiState
            val currentScreen = sicenetViewModel.currentScreen

            when (currentScreen) {

                SicenetScreen.Login -> {
                    when (uiState) {
                        is SicenetUiState.Loading -> {
                            LoadingScreen()
                        }

                        else -> {
                            val errorMsg =
                                (uiState as? SicenetUiState.Error)?.message

                            LoginScreen(
                                onLoginClicked = { u, p ->
                                    sicenetViewModel.iniciarSesion(u, p)
                                },
                                error = errorMsg
                            )
                        }
                    }
                }

                SicenetScreen.Menu -> {
                    SicenetMenuScreen(
                        onOptionSelected = { option ->
                            when (option) {
                                "PROFILE" ->
                                    sicenetViewModel.navigateTo(SicenetScreen.Profile)
                                "LOAD" ->
                                    sicenetViewModel.navigateTo(SicenetScreen.Load)
                                "CARDEX" ->
                                    sicenetViewModel.navigateTo(SicenetScreen.Cardex)
                                "GRADES_UNITS" ->
                                    sicenetViewModel.navigateTo(SicenetScreen.GradesUnits)
                                "GRADES_FINAL" ->
                                    sicenetViewModel.navigateTo(SicenetScreen.GradesFinal)
                            }
                        }
                    )
                }

                SicenetScreen.Profile -> {

                    val profile =
                        (uiState as? SicenetUiState.Success)?.profile

                    if (profile != null) {
                        Column {
                            Button(
                                onClick = {
                                    sicenetViewModel.navigateTo(SicenetScreen.Menu)
                                },
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("Regresar")
                            }

                            ProfileScreen(profile = profile)
                        }
                    } else {
                        Text("No Profile Data")
                        Button(
                            onClick = {
                                sicenetViewModel.navigateTo(SicenetScreen.Menu)
                            }
                        ) {
                            Text("Back")
                        }
                    }
                }

                SicenetScreen.Load -> {
                    CargaAcademicaScreen(
                        viewModel = sicenetViewModel,
                        onBack = {
                            sicenetViewModel.navigateTo(SicenetScreen.Menu)
                        }
                    )
                }

                SicenetScreen.Cardex -> {
                    CardexScreen(
                        viewModel = sicenetViewModel,
                        onBack = {
                            sicenetViewModel.navigateTo(SicenetScreen.Menu)
                        }
                    )
                }

                SicenetScreen.GradesUnits -> {
                    UnitGradesScreen(
                        viewModel = sicenetViewModel,
                        onBack = {
                            sicenetViewModel.navigateTo(SicenetScreen.Menu)
                        }
                    )
                }

                SicenetScreen.GradesFinal -> {
                    FinalGradesScreen(
                        viewModel = sicenetViewModel,
                        onBack = {
                            sicenetViewModel.navigateTo(SicenetScreen.Menu)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarsTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = "SICENET Alumnos",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        modifier = modifier
    )
}

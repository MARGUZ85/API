package com.example.marsphotos.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marsphotos.ui.screens.LoginScreen
import com.example.marsphotos.ui.screens.ProfileScreen
import com.example.marsphotos.ui.screens.LoadingScreen
import com.example.marsphotos.ui.screens.ErrorScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarsPhotosApp() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val sicenetViewModel: SicenetViewModel = viewModel(factory = SicenetViewModel.Factory)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { MarsTopAppBar(scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val uiState = sicenetViewModel.sicenetUiState
            
            // Apply innerPadding to content to avoid overlap with TopBar
            val contentModifier = Modifier.padding(innerPadding).fillMaxSize()

            when (uiState) {
                is SicenetUiState.Login -> {
                    LoginScreen(
                        onLoginClicked = { u, p -> sicenetViewModel.iniciarSesion(u, p) },
                        modifier = contentModifier
                    )
                }
                is SicenetUiState.Loading -> {
                    LoadingScreen(modifier = contentModifier)
                }
                is SicenetUiState.Success -> {
                    ProfileScreen(
                        profile = uiState.profile,
                        modifier = contentModifier
                    )
                }
                is SicenetUiState.Error -> {
                    // Reuse LoginScreen but pass the error
                    LoginScreen(
                        onLoginClicked = { u, p -> sicenetViewModel.iniciarSesion(u, p) },
                        error = uiState.message,
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarsTopAppBar(scrollBehavior: TopAppBarScrollBehavior, modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            Text(
                text = "SICENET Alumnos",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        modifier = modifier
    )
}
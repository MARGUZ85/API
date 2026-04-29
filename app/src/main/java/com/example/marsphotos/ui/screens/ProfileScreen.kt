package com.example.marsphotos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marsphotos.data.model.SicenetProfile

/**
 * Esta función Composable define cómo se ve la pantalla de Perfil.
 * Es una función "pura" que solo se encarga de pintar los datos que recibe.
 * No realiza lógica de negocio ni llamadas de red.
 */
@Composable
fun ProfileScreen(
    profile: SicenetProfile,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Perfil Académico",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        ProfileItem("Nombre", profile.name)
        ProfileItem("Matrícula", profile.enrollmentId)
        ProfileItem("Estatus", profile.status)
        ProfileItem("Carrera", profile.career)
        ProfileItem("Especialidad", profile.specialty)
        ProfileItem("Semestre", profile.semester)
        ProfileItem("Créditos Acumulados", profile.earnedCredits)



        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

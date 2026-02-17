package com.example.marsphotos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.marsphotos.ui.SicenetViewModel
import androidx.compose.foundation.background
import androidx.compose.material3.Divider
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun SicenetMenuScreen(
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MenuButton("Perfil") { onOptionSelected("PROFILE") }
        Spacer(Modifier.height(16.dp))
        MenuButton("Carga Académica") { onOptionSelected("LOAD") }
        Spacer(Modifier.height(16.dp))
        MenuButton("Cardex") { onOptionSelected("CARDEX") }
        Spacer(Modifier.height(16.dp))
        MenuButton("Calif. Por Unidades") { onOptionSelected("GRADES_UNITS") }
        Spacer(Modifier.height(16.dp))
        MenuButton("Calif. Finales") { onOptionSelected("GRADES_FINAL") }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onOptionSelected("DEBUG") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Ver Respuesta Servidor (Debug)")
        }
    }
}

@Composable
fun DebugScreen(
    viewModel: SicenetViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val response by viewModel.lastResponse.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Respuesta del Servidor (DEBUG)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onBack) {
            Text("Regresar")
        }
        Spacer(Modifier.height(8.dp))
        
        androidx.compose.foundation.lazy.LazyColumn {
            item {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = response.ifEmpty { "Sin respuesta capturada aún." },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(text)
    }
}

@Composable
fun SyncStatus(
    workInfo: androidx.work.WorkInfo?,
    modifier: Modifier = Modifier
) {
    if (workInfo != null) {
        when (workInfo.state) {
            androidx.work.WorkInfo.State.RUNNING -> {
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sincronizando...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            androidx.work.WorkInfo.State.SUCCEEDED -> {
                Text(
                    "Sincronización Exitosa",
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = modifier.fillMaxWidth()
                )
            }
            androidx.work.WorkInfo.State.FAILED -> {
                val error = workInfo.outputData.getString("ERROR") ?: "Error desconocido"
                Text(
                    "Error: $error",
                    color = androidx.compose.ui.graphics.Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = modifier.fillMaxWidth()
                )
            }
            else -> {}
        }
    }
}

@Composable
fun CargaAcademicaScreen(
    viewModel: SicenetViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val load by viewModel.academicLoad.collectAsState()
    val lastUpdate by viewModel.lastUpdateLoad.collectAsState()
    val workInfo by viewModel.currentWorkInfo.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        Button(onClick = { viewModel.syncFeature("LOAD", "LOAD") }) {
            Text("Sincronizar Carga")
        }
        
        SyncStatus(workInfo = workInfo)

        lastUpdate?.let {
            Text(
                "Actualizado: ${
                    SimpleDateFormat("dd/MM/yyyy HH:mm")
                        .format(Date(it))
                }",
                 style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Materias: ${load.size}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xFF2E7D32))
                .padding(4.dp)
        ) {
            TableCell(text = "MATERIA", weight = 2f, isHeader = true)
            TableCell(text = "GPO", weight = 0.5f, isHeader = true)
            TableCell(text = "PROFESOR", weight = 1.5f, isHeader = true)
            TableCell(text = "HORARIO", weight = 1.5f, isHeader = true)
            TableCell(text = "AULA", weight = 0.5f, isHeader = true)
        }

        LazyColumn {
            items(load) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    TableCell(text = item.materia, weight = 2f)
                    TableCell(text = item.grupo, weight = 0.5f)
                    TableCell(text = item.profesor, weight = 1.5f)
                    TableCell(
                        text = "${item.lunes} ${item.martes} ${item.miercoles} ${item.jueves} ${item.viernes}", 
                        weight = 1.5f
                    )
                    TableCell(text = item.aula, weight = 0.5f)
                }
                Divider(thickness = 0.5.dp, color = androidx.compose.ui.graphics.Color.LightGray)
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onBack) {
            Text("Regresar")
        }
    }
}

@Composable
fun CardexScreen(
    viewModel: SicenetViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardex by viewModel.cardex.collectAsState()
    val lastUpdate by viewModel.lastUpdateCardex.collectAsState()
    val workInfo by viewModel.currentWorkInfo.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        Button(onClick = { viewModel.syncFeature("CARDEX", "CARDEX") }) {
            Text("Sincronizar Kardex")
        }
        
        SyncStatus(workInfo = workInfo)

        lastUpdate?.let {
            Text(
                "Actualizado: ${
                    SimpleDateFormat("dd/MM/yyyy HH:mm")
                        .format(Date(it))
                }",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xFF2E7D32)) // Sicenet Green
                .padding(4.dp)
        ) {
            TableCell(text = "CVE", weight = 0.7f, isHeader = true)
            TableCell(text = "MATERIA", weight = 2f, isHeader = true)
            TableCell(text = "CALIF", weight = 0.8f, isHeader = true)
            TableCell(text = "SEM", weight = 0.6f, isHeader = true)
            TableCell(text = "AÑO", weight = 0.8f, isHeader = true)
            TableCell(text = "OBS", weight = 0.8f, isHeader = true)
        }

        LazyColumn {
            items(cardex) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    TableCell(text = item.clave, weight = 0.7f)
                    TableCell(text = item.materia, weight = 2f)
                    TableCell(text = item.calificacion, weight = 0.8f)
                    TableCell(text = item.semestre.toString(), weight = 0.6f)
                    TableCell(text = item.anio.toString(), weight = 0.8f)
                    TableCell(text = item.observacion, weight = 0.8f) 
                }
                Divider(thickness = 0.5.dp, color = androidx.compose.ui.graphics.Color.LightGray)
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onBack) {
            Text("Regresar")
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(2.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        color = if (isHeader) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        fontSize = if (isHeader) 10.sp else 10.sp
    )
}

@Composable
fun UnitGradesScreen(
    viewModel: SicenetViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grades by viewModel.unitGrades.collectAsState()
    val lastUpdate by viewModel.lastUpdateGradesUnits.collectAsState()
    val workInfo by viewModel.currentWorkInfo.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        Button(onClick = { viewModel.syncFeature("GRADES_UNITS", "GRADES_UNITS") }) {
            Text("Sincronizar Unidades")
        }

        SyncStatus(workInfo = workInfo)

        lastUpdate?.let {
            Text(
                "Actualizado: ${
                    SimpleDateFormat("dd/MM/yyyy HH:mm")
                        .format(Date(it))
                }",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xFF2E7D32))
                .padding(4.dp)
        ) {
            TableCell(text = "MATERIA", weight = 2f, isHeader = true)
            TableCell(text = "U1", weight = 0.5f, isHeader = true)
            TableCell(text = "U2", weight = 0.5f, isHeader = true)
            TableCell(text = "U3", weight = 0.5f, isHeader = true)
            TableCell(text = "PF", weight = 0.5f, isHeader = true)
        }

        LazyColumn {
            items(grades) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    TableCell(text = item.materia, weight = 2f)
                    TableCell(text = item.u1, weight = 0.5f)
                    TableCell(text = item.u2, weight = 0.5f)
                    TableCell(text = item.u3, weight = 0.5f)
                    TableCell(text = item.pf, weight = 0.5f)
                }
                Divider(thickness = 0.5.dp, color = androidx.compose.ui.graphics.Color.LightGray)
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onBack) {
            Text("Regresar")
        }
    }
}

@Composable
fun FinalGradesScreen(
    viewModel: SicenetViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grades by viewModel.finalGrades.collectAsState()
    val lastUpdate by viewModel.lastUpdateGradesFinal.collectAsState()
    val workInfo by viewModel.currentWorkInfo.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        Button(onClick = { viewModel.syncFeature("GRADES_FINAL", "GRADES_FINAL") }) {
            Text("Sincronizar Finales")
        }
        
        SyncStatus(workInfo = workInfo)

        lastUpdate?.let {
            Text(
                "Actualizado: ${
                    SimpleDateFormat("dd/MM/yyyy HH:mm")
                        .format(Date(it))
                }",
                 style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xFF2E7D32))
                .padding(4.dp)
        ) {
            TableCell(text = "MATERIA", weight = 2f, isHeader = true)
            TableCell(text = "CALIF", weight = 0.5f, isHeader = true)
            TableCell(text = "OBSERVACION", weight = 1.5f, isHeader = true)
        }

        LazyColumn {
            items(grades) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    TableCell(text = item.materia, weight = 2f)
                    TableCell(text = item.calif, weight = 0.5f)
                    TableCell(text = item.observacion, weight = 1.5f)
                }
                Divider(thickness = 0.5.dp, color = androidx.compose.ui.graphics.Color.LightGray)
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onBack) {
            Text("Regresar")
        }
    }
}

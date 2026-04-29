package com.example.marsphotos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.serialization.InternalSerializationApi
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

@OptIn(InternalSerializationApi::class)
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


        // Área de tabla con desplazamiento (scroll)
        Box(
            modifier = Modifier
                .weight(1f) // Ocupa el espacio restante, empujando el botón hacia abajo
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Column(modifier = Modifier.width(1000.dp)) { // Ancho fijo grande para permitir el desplazamiento horizontal
                // Encabezado de la Tabla
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

                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Regresar")
        }
    }
}

@OptIn(InternalSerializationApi::class)
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


        // Área de tabla desplazable
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Column(modifier = Modifier.width(1000.dp)) {
                // Encabezado de la Tabla
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color(0xFF2E7D32)) // Verde Sicenet
                        .padding(4.dp)
                ) {
                    TableCell(text = "CVE", weight = 0.7f, isHeader = true)
                    TableCell(text = "MATERIA", weight = 2f, isHeader = true)
                    TableCell(text = "CALIF", weight = 0.8f, isHeader = true)
                    TableCell(text = "SEM", weight = 0.6f, isHeader = true)
                    TableCell(text = "AÑO", weight = 0.8f, isHeader = true)
                    TableCell(text = "OBS", weight = 0.8f, isHeader = true)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
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
        color = if (isHeader) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        fontSize = if (isHeader) 10.sp else 10.sp
    )
}

@OptIn(InternalSerializationApi::class)
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


        // Área de tabla con desplazamiento (scroll)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Column(modifier = Modifier.width(1200.dp)) {
                // Encabezado de la Tabla
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color(0xFF2E7D32))
                        .padding(4.dp)
                ) {
                    TableCell(text = "MATERIA", weight = 2f, isHeader = true)
                    TableCell(text = "PROFESOR", weight = 1.5f, isHeader = true)
                    TableCell(text = "U1", weight = 0.4f, isHeader = true)
                    TableCell(text = "U2", weight = 0.4f, isHeader = true)
                    TableCell(text = "U3", weight = 0.4f, isHeader = true)
                    TableCell(text = "PF", weight = 0.4f, isHeader = true)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(grades) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            TableCell(text = item.materia, weight = 2f)
                            TableCell(text = item.profesor, weight = 1.5f)
                            TableCell(text = item.u1, weight = 0.4f)
                            TableCell(text = item.u2, weight = 0.4f)
                            TableCell(text = item.u3, weight = 0.4f)
                            TableCell(text = item.pf, weight = 0.4f)
                        }
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Regresar")
        }
    }
}

@OptIn(InternalSerializationApi::class)
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


        // Área de tabla con desplazamiento (scroll)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Column(modifier = Modifier.width(1100.dp)) {
                // Encabezado de la Tabla
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Color(0xFF2E7D32))
                        .padding(4.dp)
                ) {
                    TableCell(text = "MATERIA", weight = 2f, isHeader = true)
                    TableCell(text = "PROFESOR", weight = 1.5f, isHeader = true)
                    TableCell(text = "CALIF", weight = 0.5f, isHeader = true)
                    TableCell(text = "OBSERVACION", weight = 1.5f, isHeader = true)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(grades) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            TableCell(text = item.materia, weight = 2f)
                            TableCell(text = item.profesor, weight = 1.5f)
                            TableCell(text = item.calif, weight = 0.5f)
                            TableCell(text = item.observacion, weight = 1.5f)
                        }
                        Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Regresar")
        }
    }
}

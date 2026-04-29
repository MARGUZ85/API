package com.example.marsphotos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi /**
 * [CAPA DE DATOS - ENTIDADES DE PERSISTENCIA]
 * Estas clases definen la estructura de las tablas en la base de datos local (Room).
 * Se marcan como @Serializable para permitir su transporte fácil si fuera necesario.
 */

// --- Tabla: Carga Académica ---
@Entity(tableName = "academic_load")
@Serializable
data class AcademicLoadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,               // ID único generado automáticamente

    val materia: String = "",      // Nombre de la asignatura
    val grupo: String = "",        // Grupo asignado
    val profesor: String = "",     // Nombre del docente
    val lunes: String = "",        // Horarios por día
    val martes: String = "",
    val miercoles: String = "",
    val jueves: String = "",
    val viernes: String = "",
    val sabado: String = "",
    val domingo: String = "",
    val creditos: Int = 0,         // Valor en créditos
    val aula: String = "",         // Aula física o virtual
    val estadoMateria: String = "" // Estado (ej: "Cursando")
)


@InternalSerializationApi // --- Tabla: Kardex (Historial Académico) ---
@Entity(tableName = "cardex")
@Serializable
data class CardexEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val materia: String = "",
    val clave: String = "",        // Clave oficial de la materia
    val creditos: Int = 0,
    val calificacion: String = "", // Nota obtenida
    val evaluacion: String = "",   // Tipo de evaluación (ej: "Ordinaria")
    val semestre: Int = 0,         // Número de semestre en que se cursó
    val anio: Int = 0,             // Periodo/Año
    val observacion: String = ""   // Observaciones adicionales
)


@InternalSerializationApi // --- Tabla: Calificaciones por Unidad (Parciales) ---
@Entity(tableName = "unit_grades")
@Serializable
data class UnitGradesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val materia: String = "",
    val profesor: String = "",     // Nombre del docente (Agregado)
    val u1: String = "",           // Calificaciones individuales del 1 al 13
    val u2: String = "",
    val u3: String = "",
    val u4: String = "",
    val u5: String = "",
    val u6: String = "",
    val u7: String = "",
    val u8: String = "",
    val u9: String = "",
    val u10: String = "",
    val u11: String = "",
    val u12: String = "",
    val u13: String = "",
    val act: String = "",          // Actitud o participación
    val pf: String = ""            // Promedio Final parcial
)


@InternalSerializationApi // --- Tabla: Calificaciones Finales ---
@Entity(tableName = "final_grades")
@Serializable
data class FinalGradesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val materia: String = "",
    val profesor: String = "",     // Nombre del docente (Agregado)
    val calif: String = "",
    val observacion: String = ""
)


// --- Tabla: Registro de Metadatos (Última Actualización) ---
@Entity(tableName = "last_update_log")
data class LastUpdateEntity(
    @PrimaryKey
    val feature: String,           // Nombre de la sección (ej: "cardex")

    val timestamp: Long = 0L       // Fecha y hora en milisegundos de la última sync
)

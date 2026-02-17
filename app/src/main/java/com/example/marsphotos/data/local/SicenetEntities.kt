package com.example.marsphotos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// -------------------------
// Academic Load
// -------------------------

@Entity(tableName = "academic_load")
@Serializable
data class AcademicLoadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val materia: String = "",
    val grupo: String = "",
    val profesor: String = "",
    val lunes: String = "",
    val martes: String = "",
    val miercoles: String = "",
    val jueves: String = "",
    val viernes: String = "",
    val sabado: String = "",
    val domingo: String = "",
    val creditos: Int = 0,
    val aula: String = "",
    val estadoMateria: String = ""
)


// -------------------------
// Cardex
// -------------------------

@Entity(tableName = "cardex")
@Serializable
data class CardexEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val materia: String = "",
    val clave: String = "",
    val creditos: Int = 0,
    val calificacion: String = "",
    val evaluacion: String = "",
    val semestre: Int = 0,
    val anio: Int = 0,
    val observacion: String = ""
)


// -------------------------
// Unit Grades
// -------------------------

@Entity(tableName = "unit_grades")
@Serializable
data class UnitGradesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val materia: String = "",
    val u1: String = "",
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
    val act: String = "",
    val pf: String = ""
)


// -------------------------
// Final Grades
// -------------------------

@Entity(tableName = "final_grades")
@Serializable
data class FinalGradesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val materia: String = "",
    val calif: String = "",
    val observacion: String = ""
)


// -------------------------
// Last Update Metadata
// -------------------------

@Entity(tableName = "last_update_log")
data class LastUpdateEntity(
    @PrimaryKey
    val feature: String,

    val timestamp: Long = 0L
)

package com.example.marsphotos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.InternalSerializationApi

/**
 * [CAPA DE DATOS - DAO (Data Access Object)]
 * ¡Esta es la Base de Datos Local! Usamos "Room" (la tecnología oficial de Android).
 * En lugar de escribir código SQL manual complicado, solo definimos "Funciones" y Room
 * se encarga de crear las tablas de SQLite y guardar las cosas adentro de nuestro teléfono celular.
 */
@OptIn(kotlinx.serialization.InternalSerializationApi::class)
@Dao
interface SicenetDao {

    // --- CARGA ACADÉMICA ---

    @OptIn(InternalSerializationApi::class)
    @Query("SELECT * FROM academic_load")
    fun getAcademicLoad(): Flow<List<AcademicLoadEntity>>

    /**
     * Función SUSPENDIDA para insertar datos. Solo se puede llamar desde una corrutina.
     */
    @OptIn(InternalSerializationApi::class)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicLoad(load: List<AcademicLoadEntity>)

    @Query("DELETE FROM academic_load")
    suspend fun clearAcademicLoad()


    // --- KARDEX (CARDEX) ---

    @OptIn(InternalSerializationApi::class)
    @Query("SELECT * FROM cardex ORDER BY semestre DESC")
    fun getCardex(): Flow<List<CardexEntity>>

    @OptIn(InternalSerializationApi::class)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardex(cardex: List<CardexEntity>)

    @Query("DELETE FROM cardex")
    suspend fun clearCardex()


    // --- CALIFICACIONES POR UNIDAD ---

    @Query("SELECT * FROM unit_grades")
    fun getUnitGrades(): Flow<List<UnitGradesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitGrades(grades: List<UnitGradesEntity>)

    @Query("DELETE FROM unit_grades")
    suspend fun clearUnitGrades()


    // --- CALIFICACIONES FINALES ---

    @Query("SELECT * FROM final_grades")
    fun getFinalGrades(): Flow<List<FinalGradesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinalGrades(grades: List<FinalGradesEntity>)

    @Query("DELETE FROM final_grades")
    suspend fun clearFinalGrades()


    // --- METADATOS (ÚLTIMA ACTUALIZACIÓN) ---

    @Query("SELECT timestamp FROM last_update_log WHERE feature = :feature LIMIT 1")
    fun getLastUpdate(feature: String): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastUpdate(entity: LastUpdateEntity)
}

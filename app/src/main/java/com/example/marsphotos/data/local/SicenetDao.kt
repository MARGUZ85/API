package com.example.marsphotos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SicenetDao {

    // -------------------------
    // Academic Load
    // -------------------------

    @Query("SELECT * FROM academic_load")
    fun getAcademicLoad(): Flow<List<AcademicLoadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicLoad(load: List<AcademicLoadEntity>)

    @Query("DELETE FROM academic_load")
    suspend fun clearAcademicLoad()


    // -------------------------
    // Cardex
    // -------------------------

    @Query("SELECT * FROM cardex ORDER BY semestre DESC")
    fun getCardex(): Flow<List<CardexEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardex(cardex: List<CardexEntity>)

    @Query("DELETE FROM cardex")
    suspend fun clearCardex()


    // -------------------------
    // Unit Grades
    // -------------------------

    @Query("SELECT * FROM unit_grades")
    fun getUnitGrades(): Flow<List<UnitGradesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitGrades(grades: List<UnitGradesEntity>)

    @Query("DELETE FROM unit_grades")
    suspend fun clearUnitGrades()


    // -------------------------
    // Final Grades
    // -------------------------

    @Query("SELECT * FROM final_grades")
    fun getFinalGrades(): Flow<List<FinalGradesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinalGrades(grades: List<FinalGradesEntity>)

    @Query("DELETE FROM final_grades")
    suspend fun clearFinalGrades()


    // -------------------------
    // Metadata (Last Update)
    // -------------------------

    @Query("SELECT timestamp FROM last_update_log WHERE feature = :feature LIMIT 1")
    fun getLastUpdate(feature: String): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastUpdate(entity: LastUpdateEntity)
}

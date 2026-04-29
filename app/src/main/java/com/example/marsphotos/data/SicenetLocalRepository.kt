package com.example.marsphotos.data

import com.example.marsphotos.data.local.AcademicLoadEntity
import com.example.marsphotos.data.local.CardexEntity
import com.example.marsphotos.data.local.FinalGradesEntity
import com.example.marsphotos.data.local.LastUpdateEntity
import com.example.marsphotos.data.local.SicenetDao
import com.example.marsphotos.data.local.UnitGradesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.InternalSerializationApi

class SicenetLocalRepository(private val sicenetDao: SicenetDao) {

    // Academic Load
    @OptIn(InternalSerializationApi::class)
    val academicLoad: Flow<List<AcademicLoadEntity>> = sicenetDao.getAcademicLoad()

    @OptIn(InternalSerializationApi::class)

    suspend fun saveAcademicLoad(load: List<AcademicLoadEntity>) {
        sicenetDao.clearAcademicLoad()
        sicenetDao.insertAcademicLoad(load)
        sicenetDao.setLastUpdate(LastUpdateEntity("carga", System.currentTimeMillis()))
    }

    // Cardex
    @OptIn(InternalSerializationApi::class)
    val cardex: Flow<List<CardexEntity>> = sicenetDao.getCardex()

    @OptIn(InternalSerializationApi::class)
    suspend fun saveCardex(data: List<CardexEntity>) {
        sicenetDao.clearCardex()
        sicenetDao.insertCardex(data)
        sicenetDao.setLastUpdate(LastUpdateEntity("cardex", System.currentTimeMillis()))
    }

    // Unit Grades
    @OptIn(InternalSerializationApi::class)
    val unitGrades: Flow<List<UnitGradesEntity>> = sicenetDao.getUnitGrades()

    @OptIn(InternalSerializationApi::class)
    suspend fun saveUnitGrades(grades: List<UnitGradesEntity>) {
        sicenetDao.clearUnitGrades()
        sicenetDao.insertUnitGrades(grades)
        sicenetDao.setLastUpdate(LastUpdateEntity("calif_unidades", System.currentTimeMillis()))
    }

    // Final Grades
    @OptIn(InternalSerializationApi::class)
    val finalGrades: Flow<List<FinalGradesEntity>> = sicenetDao.getFinalGrades()

    @OptIn(InternalSerializationApi::class)
    suspend fun saveFinalGrades(grades: List<FinalGradesEntity>) {
        sicenetDao.clearFinalGrades()
        sicenetDao.insertFinalGrades(grades)
        sicenetDao.setLastUpdate(LastUpdateEntity("calif_finales", System.currentTimeMillis()))
    }

    // Metadata
    fun getLastUpdate(feature: String): Flow<Long?> = sicenetDao.getLastUpdate(feature)
}

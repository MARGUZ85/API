package com.example.marsphotos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AcademicLoadEntity::class,
        CardexEntity::class,
        UnitGradesEntity::class,
        FinalGradesEntity::class,
        LastUpdateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SicenetDatabase : RoomDatabase() {

    abstract fun sicenetDao(): SicenetDao

    companion object {

        @Volatile
        private var instance: SicenetDatabase? = null

        fun getDatabase(context: Context): SicenetDatabase {
            return instance ?: synchronized(this) {
                val database = Room.databaseBuilder(
                    context.applicationContext,
                    SicenetDatabase::class.java,
                    "sicenet_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                instance = database
                database
            }
        }
    }
}

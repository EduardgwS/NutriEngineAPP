package com.explosionlab.nutriengine.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.explosionlab.nutriengine.data.local.dao.ConsumoDao
import com.explosionlab.nutriengine.data.local.entity.AlimentoEntity
import com.explosionlab.nutriengine.data.local.entity.ConsumoDiarioEntity
import com.explosionlab.nutriengine.data.local.entity.ListaEntity

@Database(
    entities = [
        ConsumoDiarioEntity::class,
        ListaEntity::class,
        AlimentoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun consumoDao(): ConsumoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriengine_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

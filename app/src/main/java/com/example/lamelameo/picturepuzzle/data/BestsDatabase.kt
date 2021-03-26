package com.example.lamelameo.picturepuzzle.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [BestData::class], version = 1, exportSchema = false)
abstract class BestsDatabase : RoomDatabase() {

    abstract val bestDao: BestDataDao

    // TODO: can be used to prepopulate database with entries for default images - need to change best data type to float
    private class BestDatabaseCallback(private val scope: CoroutineScope, private val data: List<Int>): RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database -> scope.launch {
                val bestDataDao = database.bestDao
                // populate database with empty entries for default images
                val inf = Float.POSITIVE_INFINITY
                data.forEach() { id -> bestDataDao.newEntry(BestData("default_$id", -1, -1)) }
            }}
        }
    }

    companion object {
        @Volatile private var INSTANCE: BestsDatabase? = null
        fun getInstance(context: Context): BestsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, BestsDatabase::class.java,
                    "puzzle_best_database").fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                    instance
                }
            }
        }
}

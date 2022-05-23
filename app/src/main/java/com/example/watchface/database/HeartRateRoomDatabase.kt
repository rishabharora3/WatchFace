/**
 * Author: Rishabh Arora
 */
package com.example.watchface.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.watchface.database.dao.HeartRateDao
import com.example.watchface.database.entity.HeartRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [HeartRate::class], version = 1)
abstract class HeartRateRoomDatabase : RoomDatabase() {

    abstract fun heartRateDao(): HeartRateDao

    companion object {
        @Volatile
        private var INSTANCE: HeartRateRoomDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): HeartRateRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HeartRateRoomDatabase::class.java,
                    "heart_rate_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(HeartRateDatabaseCallback(scope))
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class HeartRateDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.heartRateDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(heartRateDao: HeartRateDao) {
            // Start the app with a clean database every time.
//            heartRateDao.deleteAll()
        }
    }
}

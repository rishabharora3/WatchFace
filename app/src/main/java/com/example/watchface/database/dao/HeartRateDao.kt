/**
 * Author: Rishabh Arora
 */
package com.example.watchface.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.watchface.database.entity.HeartRate


@Dao
interface HeartRateDao {
    @Query("SELECT * FROM heart_rate_table ORDER BY timestamp DESC LIMIT 1")
    fun getLastHeartRate(): List<HeartRate>

    @Query("SELECT * FROM heart_rate_table ORDER BY timestamp")
    fun getHeartRateData(): List<HeartRate>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(heartRate: HeartRate)

    @Query("DELETE FROM heart_rate_table")
    suspend fun deleteAll()
}

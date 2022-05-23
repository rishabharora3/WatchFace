/**
 * Author: Rishabh Arora
 */
package com.example.watchface.database

import androidx.annotation.WorkerThread
import com.example.watchface.database.dao.HeartRateDao
import com.example.watchface.database.entity.HeartRate

class HeartRateRepository(private val heartRateDao: HeartRateDao) {

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insert(heartRate: HeartRate) {
        heartRateDao.insert(heartRate)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun deleteAll() {
        heartRateDao.deleteAll()
    }
}

/**
 * Author: Rishabh Arora
 */
package com.example.watchface.database

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class HeartRateApplication(applicationContext: Context){
    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { HeartRateRoomDatabase.getDatabase(applicationContext, applicationScope) }
    val repository by lazy { HeartRateRepository(database.heartRateDao()) }
}

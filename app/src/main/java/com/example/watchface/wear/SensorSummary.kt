/**
 * Author: Rishabh Arora
 */
package com.example.watchface.wear

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import com.example.watchface.R


@RequiresPermission(Manifest.permission.BODY_SENSORS)
fun Context.sensorSummary(): String {
    val sensorManager = getSystemService<SensorManager>()!!
    val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
    val numberOfSensorsOnDevice = sensorList.size
    return getString(R.string.sensor_summary, numberOfSensorsOnDevice)
}

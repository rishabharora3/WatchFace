/**
 * Author: Rishabh Arora
 */
package com.example.watchface.wear

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import androidx.concurrent.futures.await
import androidx.core.app.ActivityCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPoint
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import com.example.watchface.R
import com.example.watchface.database.entity.HeartRate
import com.example.watchface.database.HeartRateApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*


/**
 * Updates heart rate in milliseconds for interactive mode. Updates once every 10 second to store the
 * heart rate in room database
 */
private var INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0


class MyWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: Engine) : Handler() {
        private val mWeakReference: WeakReference<Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine(), UpdateUI {

        private var mRegisteredTimeZoneReceiver = false
        private var mCenterX: Float = 0F
        private var mCenterY: Float = 0F

        private lateinit var paintText: Paint

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mBackgroundBitmap: Bitmap
        private lateinit var mGrayBackgroundBitmap: Bitmap

        private var mAmbient: Boolean = false
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false
        private var permission: Boolean = false
        private var registered: Boolean = false
        private var repository = HeartRateApplication(applicationContext).repository
        private var database = HeartRateApplication(applicationContext).database
        private var heartRateLast: Double = -1.0
        private var hasHeartRate: Boolean = false

        /* Handler to update the time once a second in interactive mode. */
        private val mUpdateTimeHandler = EngineHandler(this)

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                invalidate()
            }
        }

        private var data: List<DataPoint> = ArrayList()
        private val scope: CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val healthClient = HealthServices.getClient(applicationContext /*context*/)
        private val heartRateCallback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DataType, availability: Availability) {
                if (availability is DataTypeAvailability) {
                    Log.d("availability", availability.toString())
                }
            }

            override fun onData(data: List<DataPoint>) {
                this@Engine.data = data
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@MyWatchFace)
                    .setAcceptsTapEvents(true)
                    .build()
            )
            initializeBackground()
            initializeWatchFace()
        }

        private suspend fun hasHeartRateCapability(): Boolean {
            val capabilities = healthClient.measureClient.capabilities.await()
            return (DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure)
        }

        private fun promptUserForSensorPermission() {
            val sensorPermissionApproved =
                ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.BODY_SENSORS
                ) ==
                        PackageManager.PERMISSION_GRANTED
            if (sensorPermissionApproved) {
                permission = true
            } else {
                // Launch Activity to grant sensor permissions.
                startActivity(
                    Intent(this@MyWatchFace, RequestPermissionActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(RequestPermissionActivity.EXTRA_PROMPT_PERMISSION_FROM_PHONE, true)
                        putExtra(RequestPermissionActivity.EXTRA_PROMPT_PERMISSION_FROM_PHONE, true)
                    }
                )
            }
        }

        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }
            mBackgroundBitmap =
                BitmapFactory.decodeResource(resources, R.drawable.heart)
        }

        private fun initializeWatchFace() {
            /* Set defaults for colors */
            checkPermission()
            paintText = Paint()
            paintText.style = Paint.Style.FILL
            paintText.strokeWidth = 3f
            paintText.color = Color.WHITE
            paintText.textSize = 20F
            RequestPermissionActivity.storeInstance(this)
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            mBurnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode
            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f
            mCenterY = height / 2f
            /* Scale loaded background image (more efficient) if surface dimensions change. */
            val scale = width.toFloat() / mBackgroundBitmap.width.toFloat()

            mBackgroundBitmap = Bitmap.createScaledBitmap(
                mBackgroundBitmap,
                (mBackgroundBitmap.width * scale).toInt(),
                (mBackgroundBitmap.height * scale).toInt(), true
            )

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don"t want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren"t
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            if (!mBurnInProtection && !mLowBitAmbient) {
                initGrayBackgroundBitmap()
            }
        }

        private fun initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                mBackgroundBitmap.width,
                mBackgroundBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(mGrayBackgroundBitmap)
            val grayPaint = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(colorMatrix)
            grayPaint.colorFilter = filter
            canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, grayPaint)
        }

        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            scope.launch {
                promptUserForSensorPermission()
            }
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK)
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            }
        }

        private fun drawWatchFace(canvas: Canvas) {
            checkIfHeartRateServicesAvailable()
            if (permission) {
                registerCallback()
                val bpm = showHeartRate(canvas)
                getLastHeartRate()
                storeHeartRate(bpm)
                showLastHeartRate(canvas)
            } else {
                drawPermissionText(canvas)
                checkPermission()
            }
            canvas.save()
            /* Restore the canvas" original orientation. */
            canvas.restore()
        }

        private fun checkIfHeartRateServicesAvailable() {
            scope.launch {
                hasHeartRate = hasHeartRateCapability()
            }
            if (!hasHeartRate) {
                Log.d("Watchface", "drawWatchFace: No heart rate services available")
            }
        }

        private fun showLastHeartRate(canvas: Canvas) {
            if (heartRateLast != -1.0) {
                canvas.drawText(
                    "Last: " + String.format("%.1f", heartRateLast),
                    mCenterX - 70,
                    mCenterY + 40,
                    paintText
                )
            }
        }

        private fun getLastHeartRate() {
            scope.launch {
                val data = database.heartRateDao().getLastHeartRate()
                if (data.isNotEmpty())
                    heartRateLast = data.first().heartRate
            }
        }

        private fun storeHeartRate(bpm: Double) {
            scope.launch {
                repository.insert(
                    HeartRate(
                        (System.currentTimeMillis() / 1000).toString(),
                        bpm
                    )
                )
            }
        }

        private fun showHeartRate(canvas: Canvas): Double {
            val bpm = if (data.isNotEmpty()) data.last().value.asDouble() else 0.0
            canvas.drawText(
                "Heart Rate: " + String.format("%.1f", bpm),
                mCenterX - 70,
                mCenterY,
                paintText
            )
            return bpm
        }

        private fun checkPermission() {
            val sensorPermissionApproved =
                ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.BODY_SENSORS
                ) ==
                        PackageManager.PERMISSION_GRANTED
            if (sensorPermissionApproved) {
                permission = true
                updateUi()
            }
        }

        private fun drawPermissionText(canvas: Canvas) {
            canvas.drawText(
                "Please allow",
                mCenterX - 70,
                mCenterY - 30,
                paintText
            )
            canvas.drawText(
                "permission\n",
                mCenterX - 70,
                mCenterY,
                paintText
            )
            canvas.drawText(
                "by clicking here\n",
                mCenterX - 70,
                mCenterY + 30,
                paintText
            )
        }

        private fun registerCallback() {
            if (!registered) {
                registered = true
                scope.launch {
                    healthClient.measureClient
                        .registerCallback(DataType.HEART_RATE_BPM, heartRateCallback)
                        .await()
                }
                scope.launch {
                    repository.deleteAll()
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MyWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@MyWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !mAmbient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }

        override fun updateUi() {
            INTERACTIVE_UPDATE_RATE_MS = 10000
        }
    }

    interface UpdateUI {
        fun updateUi()
    }
}
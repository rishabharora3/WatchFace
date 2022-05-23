/**
 * Author: Rishabh Arora
 */
package com.example.watchface.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.setPadding
import com.example.watchface.R
import com.example.watchface.databinding.ActivityMainBinding
import com.google.android.gms.wearable.CapabilityClient.OnCapabilityChangedListener
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.example.watchface.util.Constants
import kotlin.math.roundToInt
import kotlin.math.sqrt

class RequestPermissionActivity :
    AppCompatActivity(),
    OnCapabilityChangedListener,
    OnMessageReceivedListener {

    private lateinit var binding: ActivityMainBinding
    private var phoneInfoPermissionApproved = false
    private var isPhoneRequestingPermission = false
    private var askedForPermissionOnBehalfOfPhone = false
    private var phoneNodeId: String? = null
    private val requestPermissionLauncher = registerForActivityResult(
        RequestPermission()
    ) {
        onPermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        phoneInfoPermissionApproved = false

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            binding.wearBodySensorsPermissionButton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_very_satisfied, 0, 0, 0
            )
        }
        askedForPermissionOnBehalfOfPhone =
            savedInstanceState?.getBoolean(
                ASKED_PERMISSION_ON_BEHALF_OF_PHONE,
                askedForPermissionOnBehalfOfPhone
            ) ?: askedForPermissionOnBehalfOfPhone

        checkForRemotePermissionRequest()

        binding.wearBodySensorsPermissionButton.setOnClickListener {
            logToUi(getString(R.string.requested_local_permission))
            requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }

        if (resources.configuration.isScreenRound) {
            binding.scrollingContentContainer.doOnPreDraw {
                // Calculate the padding necessary to make the scrolling content fit in a square
                // inscribed on a round screen.
                it.setPadding((it.width / 2.0 * (1.0 - 1.0 / sqrt(2.0))).roundToInt())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
        Wearable.getCapabilityClient(this).removeListener(this)
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        Wearable.getCapabilityClient(this).addListener(
            this, Constants.CAPABILITY_PHONE_APP
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ASKED_PERMISSION_ON_BEHALF_OF_PHONE, askedForPermissionOnBehalfOfPhone)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        askedForPermissionOnBehalfOfPhone = false

        checkForRemotePermissionRequest()
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        Log.d(TAG, "onCapabilityChanged(): $capabilityInfo")
        phoneNodeId = capabilityInfo.nodes.firstOrNull()?.id
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived(): $messageEvent")
        val messagePath = messageEvent.path
        if (messagePath == Constants.MESSAGE_PATH_WEAR) {
            val dataMap = DataMap.fromByteArray(messageEvent.data)
            val commType = dataMap.getInt(Constants.KEY_COMM_TYPE, 0)
            when (commType) {
                Constants.COMM_TYPE_RESPONSE_PERMISSION_REQUIRED -> {
                    phoneInfoPermissionApproved = false
                }
                Constants.COMM_TYPE_RESPONSE_USER_APPROVED_PERMISSION -> {
                    phoneInfoPermissionApproved = true
                    logToUi(getString(R.string.user_approved_remote_permission))
                }
                Constants.COMM_TYPE_RESPONSE_USER_DENIED_PERMISSION -> {
                    phoneInfoPermissionApproved = false
                    logToUi(getString(R.string.user_denied_remote_permission))
                }
                Constants.COMM_TYPE_RESPONSE_DATA -> {
                    phoneInfoPermissionApproved = true
                    val phoneSummary = dataMap.getString(Constants.KEY_PAYLOAD)!!
                    logToUi(phoneSummary)
                }
            }
        }
    }

    /**
     * A helper function to launch the permission dialog on behalf of the phone.
     */
    private fun checkForRemotePermissionRequest() {
        isPhoneRequestingPermission = intent.getBooleanExtra(
            EXTRA_PROMPT_PERMISSION_FROM_PHONE, false
        )
        if (isPhoneRequestingPermission && !askedForPermissionOnBehalfOfPhone) {
            launchPermissionDialogForPhone()
        }
    }

    private fun onPermissionResult() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val sensorSummary = this.sensorSummary()

            binding.wearBodySensorsPermissionButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_very_satisfied, 0, 0, 0
            )

            logToUi(sensorSummary)
            if (isPhoneRequestingPermission) {
                isPhoneRequestingPermission = false
                askedForPermissionOnBehalfOfPhone = true
            }
            engine.updateUi()
            finish()
        } else {
            binding.wearBodySensorsPermissionButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_very_dissatisfied, 0, 0, 0
            )
            if (isPhoneRequestingPermission) {
                // Resets so this isn't triggered every time permission is changed in app.
                isPhoneRequestingPermission = false
                askedForPermissionOnBehalfOfPhone = true
            }
        }

    }

    private fun launchPermissionDialogForPhone() {
        Log.d(TAG, "launchPermissionDialogForPhone()")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
    }


    private fun logToUi(message: String) {
        runOnUiThread {
            if (message.isNotEmpty()) {
                Log.d(TAG, message)
                binding.output.text = message
            }
        }
    }

    companion object {
        fun storeInstance(engine: MyWatchFace.Engine) {
            this.engine = engine
        }

        private lateinit var engine: MyWatchFace.Engine
        private const val TAG = "MainWearActivity"

        private const val ASKED_PERMISSION_ON_BEHALF_OF_PHONE = "AskedPermissionOnBehalfOfPhone"

        const val EXTRA_PROMPT_PERMISSION_FROM_PHONE =
            "com.example.android.wearable.runtimepermissions.extra.PROMPT_PERMISSION_FROM_PHONE"
    }
}

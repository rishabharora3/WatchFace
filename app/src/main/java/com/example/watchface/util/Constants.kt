/**
 * Author: Rishabh Arora
 */
package com.example.watchface.util

object Constants {
    const val KEY_COMM_TYPE = "communicationType"
    const val KEY_PAYLOAD = "payload"

    // Responses
    const val COMM_TYPE_RESPONSE_PERMISSION_REQUIRED = 1001
    const val COMM_TYPE_RESPONSE_USER_APPROVED_PERMISSION = 1002
    const val COMM_TYPE_RESPONSE_USER_DENIED_PERMISSION = 1003
    const val COMM_TYPE_RESPONSE_DATA = 1004

    const val CAPABILITY_PHONE_APP = "phone_app_runtime_permissions"

    // Wear
    const val CAPABILITY_WEAR_APP = "wear_app_runtime_permissions"
    const val MESSAGE_PATH_WEAR = "/wear_message_path"
}

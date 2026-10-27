package com.campsitereservationapp.service

import android.content.Context
import java.util.UUID

/**
 * Provides a unique, permanent ID for this device.
 *
 * **Purpose:** identifies the user by their device, so they never need a
 * username or password (US 01.09.01).
 *
 * **Design rationale:** generates a random ID on first launch and saves it
 * in SharedPreferences, so the same ID comes back every time the app opens.
 * Kept in its own class so the storage method can change without affecting
 * the rest of the app.
 *
 * **Outstanding issues:** uninstalling the app (or clearing its data)
 * creates a new ID, so the user starts as a new profile. The user will become inaccessible.
 * TODO: Ask the TA if this is intended behaviour
 *
 * @param context used to reach the app's private storage
 */
class DeviceIdProvider(context: Context) {

    private val prefs = context.getSharedPreferences("device", Context.MODE_PRIVATE)

    /**
     * Returns this device's ID, creating and saving one on first use.
     *
     * @return the same ID every time, for as long as the app stays installed
     */
    fun getDeviceId(): String =
        prefs.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { newId ->
                prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}
package com.campsitereservationapp

import android.app.Application
import com.campsitereservationapp.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The app's Application class, created once when the app starts.
 *
 * **Purpose:** holds the [AppContainer] and makes sure a [model.User]
 * exists for this device as soon as the app opens.
 *
 * **Design rationale:** the Application lives as long as the app does,
 * which makes it the natural owner of app-wide objects and startup work.
 *
 * **Outstanding issues:** None.
 */
class CampsiteApp : Application() {

    /** The app-wide container of shared repositories. */
    val container by lazy { AppContainer(this) }

    // A coroutine scope that lives as long as the app, for startup work.
    private val appScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // First launch: creates an entrant for this device.
        // Later launches: finds the existing user.
        appScope.launch {
            container.userRepository.getOrCreate(container.currentUserId)
        }
    }
}
package com.campsitereservationapp

import android.app.Application
import android.util.Log
import com.campsitereservationapp.data.AppContainer
import com.campsitereservationapp.data.SampleData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The app's Application class, created once when the app starts.
 *
 * **Purpose:** holds the [AppContainer] and makes sure a [com.campsitereservationapp.model.User]
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
    // Errors are logged instead of crashing the app (for example, no network).
    private val appScope = CoroutineScope(
        SupervisorJob() + CoroutineExceptionHandler { _, error ->
            Log.e("CampsiteApp", "Startup work failed", error)
        }
    )

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // TODO: remove sample data seeding before the final release.
            container.campsiteRepository.seedIfEmpty(SampleData.campsites)
            // First launch: creates an entrant for this device.
            // Later launches: finds the existing user.
            container.userRepository.getOrCreate(container.currentUserId)
        }
    }
}
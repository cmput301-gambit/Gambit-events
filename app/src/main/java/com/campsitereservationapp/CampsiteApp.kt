package com.campsitereservationapp

import android.app.Application
import com.campsitereservationapp.data.AppContainer

/**
 * The app's Application class, created once when the app starts.
 *
 * **Purpose:** holds the [AppContainer] so ViewModel factories can reach it.
 *
 * **Design rationale:** the Application lives as long as the app does,
 * which makes it the natural owner of app-wide objects.
 *
 * **Outstanding issues:** None.
 */
class CampsiteApp : Application() {
    /** The app-wide container of shared repositories. */
    val container by lazy { AppContainer() }
}
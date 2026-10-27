package com.campsitereservationapp.data

/**
 * Creates and holds the app's shared repositories.
 *
 * **Purpose:** gives every ViewModel the same repository instances, so
 * data added on one screen shows up on every other screen.
 *
 * **Design rationale:** manual dependency injection, as in Google's Android
 * architecture guide. Each repository is created once, on first use.
 *
 * **Outstanding issues:** TODO: add a line per repository as each is created.
 */
class AppContainer {
    /** Shared repository for campsites. */
    val campsiteRepository by lazy { CampsiteRepository() }

    /** Shared repository for users. */

    /** Shared repository for lotteries. */

    /** Shared repository for reservations. */

    /** Shared repository for images. */

    /** Shared repository for waiting lists. */
}

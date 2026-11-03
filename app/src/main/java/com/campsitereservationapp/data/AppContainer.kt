package com.campsitereservationapp.data

import android.content.Context
import com.campsitereservationapp.service.DeviceIdProvider

/**
 * Creates and holds the app's shared repositories.
 *
 * **Purpose:** gives every ViewModel the same repository instances, so
 * data added on one screen shows up on every other screen.
 *
 * **Design rationale:** manual dependency injection, as in Google's Android
 * architecture guide. Each repository is created once, on first use, and
 * this is the one place that decides which data source each one uses.
 *
 * **Outstanding issues:** lotteries are kept in memory until their Firestore
 * mapping exists. TODO: add reservationRepository once it is created.
 */
class AppContainer(context: Context) {

    /** Shared repository for campsites. */
    val campsiteRepository by lazy {
        CampsiteRepository(CampsiteRepository.firestoreDataSource())
    }

    /** Shared repository for users. */
    val userRepository by lazy {
        UserRepository(UserRepository.firestoreDataSource())
    }

    /** Shared repository for notifications. */
    val notificationRepository by lazy {
        NotificationRepository(NotificationRepository.firestoreDataSource())
    }

    /** Shared repository for lotteries. */
    val lotteryRepository by lazy {
        LotteryRepository(MockDataSource(), campsiteRepository)
    }

    /** The current user's ID, which is this device's ID (US 01.09.01). */
    val currentUserId: String by lazy { DeviceIdProvider(context).getDeviceId() }
}

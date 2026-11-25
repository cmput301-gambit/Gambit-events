package com.campsitereservationapp.data

import android.content.Context
import com.campsitereservationapp.service.CascadeDeleter
import com.campsitereservationapp.service.DeviceIdProvider
import com.campsitereservationapp.service.NotificationHandler

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
 * **Outstanding issues:** None.
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

    /** Creates and saves win/lose notifications after a lottery draw. */
    val notificationHandler by lazy { NotificationHandler(notificationRepository) }

    /** Shared repository for lotteries. */
    val lotteryRepository by lazy {
        LotteryRepository(LotteryRepository.firestoreDataSource(), campsiteRepository, notificationHandler)
    }


    /** Shared repository for reservations. */
    val reservationRepository by lazy {
        ReservationRepository(ReservationRepository.firestoreDataSource())
    }

    /** Shared repository for ratings*/
    val ratingRepository by lazy {
        RatingRepository(RatingRepository.firestoreDataSource())
    }

    /** Shared repository for owner and entrant messages about reservations. */
    val messageRepository by lazy {
        MessageRepository(MessageRepository.firestoreDataSource())
    }

    /**
     * Deletes profiles, owners and campsites together with everything that
     * depends on them (US 01.08.01, 03.02.01, 03.07.01).
     */
    val cascadeDeleter by lazy {
        CascadeDeleter(
            userRepository,
            campsiteRepository,
            lotteryRepository,
            reservationRepository,
            notificationRepository,
            ratingRepository,
            messageRepository,
        )
    }

    /** The current user's ID, which is this device's ID (US 01.09.01). */
    val currentUserId: String by lazy { DeviceIdProvider(context).getDeviceId() }
}
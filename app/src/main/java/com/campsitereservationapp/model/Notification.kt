package com.campsitereservationapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

/**
 * The kinds of lottery result an entrant can be notified about.
 */
enum class NotificationType {
    LOTTERY_WON,
    LOTTERY_LOST,
}

/**
 * A message telling an entrant how a lottery turned out.
 *
 * **Purpose:** the data behind win and lose notifications
 * (US 01.03.01, 01.03.02).
 *
 * **Design rationale:** plain data class with no Android UI or Firestore
 * code, so it is easy to unit test. It refers to the recipient and lottery
 * by ID, like [Entrant] does, so each piece of data has one source of truth.
 * Dates use [LocalDate], like DateRange in the data package.
 *
 * **Outstanding issues:** TODO delivery (in-app list or system
 * notification) is not built yet, and Lottery doesn't exist yet.
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property recipientId ID of the [User] who receives this notification
 * @property lotteryId ID of the lottery this result is about
 * @property type whether the entrant won or lost
 * @property message the text shown to the entrant
 * @property createdAt the date the notification was created
 * @property read true once the entrant has opened it
 */

data class Notification(
    override val id: String = "",
    val recipientId: String = "",
    val lotteryId: String = "",
    val type: NotificationType = NotificationType.LOTTERY_WON,
    val message: String = "",
    val createdAt: LocalDate = LocalDate.of(1970, 1, 1),
    val read: Boolean = false,
) : Identifiable {

    /**
     * Returns a copy of this notification marked as read.
     *
     * @return the updated notification; this one is not changed
     */
    fun markRead(): Notification = copy(read = true)

    /**
     * Checks whether this notification may be sent to the given user.
     *
     * @param user the user to check
     * @return true if the user is the recipient and has notifications on
     */
    fun canBeDeliveredTo(user: User): Boolean =
        user.id == recipientId && user.notificationsEnabled

    companion object {
        /**
         * Creates a notification telling an entrant they won a lottery.
         *
         * @param id unique ID for the notification
         * @param recipientId ID of the winning user
         * @param lotteryId ID of the lottery
         * @param campsiteName name of the campsite, shown in the message
         * @param createdAt the date the notification is created
         * @return a new unread win notification
         */
        fun won(
            id: String,
            recipientId: String,
            lotteryId: String,
            campsiteName: String,
            createdAt: LocalDate,
        ): Notification = Notification(
            id = id,
            recipientId = recipientId,
            lotteryId = lotteryId,
            type = NotificationType.LOTTERY_WON,
            message = "You won the lottery for $campsiteName! Open the app to reserve your dates.",
            createdAt = createdAt,
        )

        /**
         * Creates a notification telling an entrant they were not selected.
         *
         * @param id unique ID for the notification
         * @param recipientId ID of the user who lost
         * @param lotteryId ID of the lottery
         * @param campsiteName name of the campsite, shown in the message
         * @param createdAt the date the notification is created
         * @return a new unread lose notification
         */
        fun lost(
            id: String,
            recipientId: String,
            lotteryId: String,
            campsiteName: String,
            createdAt: LocalDate,
        ): Notification = Notification(
            id = id,
            recipientId = recipientId,
            lotteryId = lotteryId,
            type = NotificationType.LOTTERY_LOST,
            message = "You were not selected in the lottery for $campsiteName.",
            createdAt = createdAt,
        )
    }
}
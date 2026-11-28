package com.campsitereservationapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * The kinds of notification an entrant can receive.
 */
enum class NotificationType {
    LOTTERY_WON,
    LOTTERY_LOST,

    /** A message a campsite owner sent to a lottery's waiting list (US 02.10.01). */
    OWNER_MESSAGE,
}

/**
 * Errors that can occur when validating a [Notification].
 */
enum class NotificationError {
    BLANK_MESSAGE,
    MESSAGE_TOO_LONG,
    BLANK_RECIPIENT,
    BLANK_SENDER,
    SENT_TO_SELF,
}

/**
 * A notification shown in an entrant's inbox.
 *
 * **Purpose:** the data behind win and lose notifications
 * (US 01.03.01, 01.03.02) and owner messages to a lottery's waiting list
 * (US 02.10.01). Owner messages record who sent them, which lottery they are
 * about and which send they belong to, so an owner can later list what they
 * sent (US 02.10.02).
 *
 * **Design rationale:** plain data class with no Android UI or Firestore
 * code, so it is easy to unit test. It refers to the recipient and lottery
 * by ID, like [Entrant] does, so each piece of data has one source of truth.
 * Dates use [LocalDate], like DateRange in the data package. Owner messages
 * also carry [sentAt] to the second, because several can be sent in one day.
 * Use [won], [lost] and [fromOwner] to build one.
 *
 * **Outstanding issues:** [createdAt] has no time, so win/lose notifications
 * from the same day can't be ordered. When a user is deleted, notifications
 * they sent as an owner are kept (only the ones they received are deleted).
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property recipientId ID of the [User] who receives this notification
 * @property lotteryId ID of the lottery this notification is about
 * @property type what kind of notification this is
 * @property message the text shown to the entrant
 * @property createdAt the date the notification was created
 * @property read true once the entrant has opened it
 * @property senderId ID of the owner who sent it; blank for win/lose
 * notifications, which the app sends
 * @property batchId shared by every notification from one owner send; blank
 * for win/lose notifications
 * @property sentAt when an owner message was sent, to the second; null for
 * win/lose notifications
 */

data class Notification(
    override val id: String = "",
    val recipientId: String = "",
    val lotteryId: String = "",
    val type: NotificationType = NotificationType.LOTTERY_WON,
    val message: String = "",
    val createdAt: LocalDate = LocalDate.of(1970, 1, 1),
    val read: Boolean = false,
    val senderId: String = "",
    val batchId: String = "",
    val sentAt: LocalDateTime? = null,
) : Identifiable {

    /**
     * When this notification happened, for sorting newest first. Owner
     * messages use [sentAt]; win/lose notifications have no time, so they
     * count as the start of [createdAt]. That means an owner message sorts
     * above a win/lose notification from the same day.
     */
    val sortTime: LocalDateTime
        get() = sentAt ?: createdAt.atStartOfDay()

    /**
     * Validates the notification fields and returns a list of errors found.
     * A sender is only required for owner messages.
     *
     * @return list of [NotificationError] violations
     */
    fun validate(): List<NotificationError> {
        val errors = mutableListOf<NotificationError>()
        if (message.isBlank()) errors.add(NotificationError.BLANK_MESSAGE)
        if (message.trim().length > MAX_LENGTH) errors.add(NotificationError.MESSAGE_TOO_LONG)
        if (recipientId.isBlank()) errors.add(NotificationError.BLANK_RECIPIENT)
        if (type == NotificationType.OWNER_MESSAGE && senderId.isBlank()) {
            errors.add(NotificationError.BLANK_SENDER)
        }
        if (senderId.isNotBlank() && senderId == recipientId) errors.add(NotificationError.SENT_TO_SELF)
        return errors
    }

    /**
     * Checks whether this notification has everything it needs to be sent.
     *
     * @return true if the notification has no validation errors, false otherwise
     */
    fun isValid(): Boolean = validate().isEmpty()

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
        /** The longest owner message allowed, in characters (US 02.10.01). */
        const val MAX_LENGTH = 500

        /**
         * Creates a message from a campsite owner to one entrant on a
         * lottery's waiting list (US 02.10.01).
         *
         * @param id unique ID for the notification
         * @param recipientId ID of the entrant receiving it
         * @param lotteryId ID of the lottery whose waiting list it was sent to
         * @param senderId ID of the owner sending it
         * @param text what the message says; extra spaces are trimmed
         * @param batchId shared by every notification in this send
         * @param sentAt when it was sent; cut to whole seconds
         * @return a new unread owner message
         */
        fun fromOwner(
            id: String,
            recipientId: String,
            lotteryId: String,
            senderId: String,
            text: String,
            batchId: String,
            sentAt: LocalDateTime,
        ): Notification {
            val sentAtSeconds = sentAt.truncatedTo(ChronoUnit.SECONDS)
            return Notification(
                id = id,
                recipientId = recipientId,
                lotteryId = lotteryId,
                type = NotificationType.OWNER_MESSAGE,
                message = text.trim(),
                createdAt = sentAtSeconds.toLocalDate(),
                senderId = senderId,
                batchId = batchId,
                sentAt = sentAtSeconds,
            )
        }

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
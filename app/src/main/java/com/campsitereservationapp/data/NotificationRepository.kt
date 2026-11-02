package com.campsitereservationapp.data

import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Stores and provides [Notification] objects.
 *
 * **Purpose:** lets entrants see the notifications sent to them, lets them
 * mark notifications as read, and lets admins review every notification
 * (US 01.03.01, 01.03.02, and the admin notification log).
 *
 * **Design rationale:** extends [Repository] like every other repository.
 * Originally planned as a read-only NotificationLog; became a repository
 * because notifications need their read status updated. Each user only
 * downloads their own notifications, because [observeForUser] filters in
 * the database. Sorting happens on the device so Firestore doesn't need an
 * extra index. Its companion object converts notifications to and from
 * Firestore documents.
 *
 * **Outstanding issues:** createdAt has no time, so same-day notifications
 * can't be ordered.
 *
 * @param dataSource where notifications are kept, such as [firestoreDataSource]
 * or an [InMemoryDataSource] in tests
 */
class NotificationRepository(dataSource: DataSource<Notification>) :
    Repository<Notification>(dataSource) {

    /**
     * Live list of the notifications sent to one user, newest first.
     *
     * @param userId ID of the user who received the notifications
     * @return a stream that updates whenever this user receives a notification
     * or one of theirs is marked as read
     */
    fun observeForUser(userId: String): Flow<List<Notification>> =
        observeWhere(Filter("recipientId", userId) { it.recipientId == userId })
            .map { notifications -> notifications.sortedByDescending { it.createdAt } }

    /**
     * Live count of a user's unread notifications, for an inbox badge.
     *
     * @param userId ID of the user who received the notifications
     * @return a stream that emits the number of unread notifications
     */
    fun observeUnreadCount(userId: String): Flow<Int> =
        observeForUser(userId).map { notifications -> notifications.count { !it.read } }

    /**
     * Marks a notification as read and saves it.
     *
     * @param notification the notification the user opened
     * @return the saved notification, now marked as read
     * @throws IllegalArgumentException if the notification no longer exists
     */
    suspend fun markRead(notification: Notification): Notification =
        if (notification.read) notification else update(notification.markRead())

    companion object {
        /**
         * Creates the Firestore data source for notifications, used by [AppContainer].
         *
         * @return a data source backed by the "notifications" collection
         */
        fun firestoreDataSource(): FirestoreDataSource<Notification> =
            FirestoreDataSource("notifications", ::toMap, ::fromMap)

        /**
         * Converts a notification into the fields saved in Firestore.
         *
         * The ID is not included because it is the document ID. The type is
         * saved by name (such as "LOTTERY_WON") and the date as "yyyy-MM-dd" text.
         *
         * @param notification the notification to convert
         * @return the fields to save
         */
        fun toMap(notification: Notification): Map<String, Any?> = mapOf(
            "recipientId" to notification.recipientId,
            "lotteryId" to notification.lotteryId,
            "type" to notification.type.name,
            "message" to notification.message,
            "createdAt" to notification.createdAt.toString(),
            "read" to notification.read,
        )

        /**
         * Rebuilds a notification from a Firestore document.
         *
         * Missing fields fall back to the same defaults as [Notification]'s
         * constructor, except the type: a missing or unknown type throws,
         * so the document is skipped rather than shown as the wrong result.
         *
         * @param id the document ID, used as the notification ID
         * @param data the saved fields
         * @return the rebuilt notification
         * @throws IllegalArgumentException if the type is missing or unknown
         */
        fun fromMap(id: String, data: Map<String, Any?>): Notification = Notification(
            id = id,
            recipientId = data["recipientId"] as? String ?: "",
            lotteryId = data["lotteryId"] as? String ?: "",
            type = NotificationType.valueOf(data["type"] as? String ?: ""),
            message = data["message"] as? String ?: "",
            createdAt = (data["createdAt"] as? String)?.let(LocalDate::parse)
                ?: LocalDate.of(1970, 1, 1),
            read = data["read"] as? Boolean ?: false,
        )
    }
}
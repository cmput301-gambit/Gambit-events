package com.campsitereservationapp.data

import android.app.Notification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

package com.campsitereservationapp.data

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Stores and provides [Notification] objects.
 *
 * **Purpose:** lets entrants see the notifications sent to them, lets them
 * mark notifications as read, and lets admins review every notification
 * (US 01.03.01, 01.03.02, and the admin notification log).
 *
 * **Design rationale:** extends [Repository] like every other repository.
 * Originally planned as a read-only NotificationLog; became a repository
 * because notifications need their read status updated. Notifications are
 * written by the NotificationHandler and never edited, except for their
 * read status.
 *
 * **Outstanding issues:** in memory until Firestore storage is merged;
 * observeForUser will then filter in the database instead of on the device.
 * createdAt has no time, so same-day notifications can't be ordered.
 */
@RequiresApi(Build.VERSION_CODES.O)
class NotificationRepository : Repository<Notification>() {

    /**
     * Live list of the notifications sent to one user, newest first.
     *
     * @param userId ID of the user who received the notifications
     * @return a stream that updates whenever this user receives a notification
     * or one of theirs is marked as read
     */
    fun observeForUser(userId: String): Flow<List<Notification>> =
        observeAll().map { notifications ->
            notifications
                .filter { it.recipientId == userId }
                .sortedByDescending { it.createdAt }
        }

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
}
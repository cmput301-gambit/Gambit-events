package com.campsitereservationapp.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.model.Notification
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Everything the inbox screen needs to draw.
 *
 * @property notifications the current user's notifications, newest first
 * @property unreadCount how many of them haven't been opened yet
 */
data class InboxUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
)

/**
 * Holds the state for the entrant's notification inbox.
 *
 * **Purpose:** shows the current user their win and lose notifications and
 * marks one as read when they open it (US 01.03.01, 01.03.02).
 *
 * **Design rationale:** MVVM, like the other screens. Receives
 * [NotificationRepository] from the AppContainer through [Factory] and
 * subscribes to [NotificationRepository.observeForUser], so new
 * notifications appear on their own without refreshing.
 *
 * **Outstanding issues:** none.
 *
 * @param notificationRepository the shared repository notifications come from
 * @param currentUserId ID of the user whose inbox this is
 */
class InboxViewModel(
    private val notificationRepository: NotificationRepository,
    private val currentUserId: String,
) : ViewModel() {

    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */
    val uiState: StateFlow<InboxUiState> =
        notificationRepository.observeForUser(currentUserId)
            .map { notifications ->
                InboxUiState(
                    notifications = notifications,
                    unreadCount = notifications.count { !it.read },
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InboxUiState(),
            )

    /**
     * Called by the screen when the user taps a notification. Marks it as
     * read, which updates [uiState] on its own through the live stream.
     *
     * @param notification the notification the user opened
     */
    fun onNotificationTapped(notification: Notification) {
        viewModelScope.launch {
            try {
                notificationRepository.markRead(notification)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The notification was deleted or couldn't be saved. The inbox
                // keeps showing it as unread, which is safe.
            }
        }
    }

    companion object {
        /**
         * Builds this ViewModel with the shared repository and the current
         * user's ID from the AppContainer.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                InboxViewModel(
                    app.container.notificationRepository,
                    app.container.currentUserId,
                )
            }
        }
    }
}
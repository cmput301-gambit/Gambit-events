package com.campsitereservationapp.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.Notification
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

/**
 * One message an owner sent to a lottery's waiting list.
 *
 * @property batchId shared by every notification in this send
 * @property message what the message said
 * @property campsiteNames the lottery's campsite names joined with commas, or
 * [SentNotificationsViewModel.DELETED_LOTTERY] if the lottery was deleted
 * @property recipientCount how many entrants it went to
 * @property sentAt when it was sent
 * @property lotteryDeleted true if the lottery no longer exists
 */
data class SentNotification(
    val batchId: String,
    val message: String,
    val campsiteNames: String,
    val recipientCount: Int,
    val sentAt: LocalDateTime,
    val lotteryDeleted: Boolean = false,
)

/**
 * Everything the sent notifications screen needs to draw.
 *
 * @property sends the current owner's sends, newest first
 * @property isLoading true until the first data arrives, so the screen
 * doesn't wrongly say nothing was sent while loading
 */
data class SentNotificationsUiState(
    val sends: List<SentNotification> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Holds the state for the owner's list of sent notifications.
 *
 * **Purpose:** lets a campsite owner see the messages they sent to their
 * lotteries' waiting lists (US 02.10.02).
 *
 * **Design rationale:** MVVM, like the other screens. Each send saves one
 * notification per entrant, all sharing a batchId, so they are grouped back
 * into one row per send. Combines the live streams of the owner's sent
 * notifications, their lotteries and the campsites, so the list updates on
 * its own. A send whose lottery was deleted is still shown, marked
 * "Deleted lottery".
 *
 * **Outstanding issues:** when an entrant deletes their profile, the
 * notifications they received are deleted too, so a send's recipient count
 * can go down later.
 *
 * @param notificationRepository where the sent notifications come from
 * @param lotteryRepository used to find each send's lottery
 * @param campsiteRepository used to show campsite names
 * @param currentUserId the owner viewing the screen
 */
class SentNotificationsViewModel(
    notificationRepository: NotificationRepository,
    lotteryRepository: LotteryRepository,
    campsiteRepository: CampsiteRepository,
    currentUserId: String,
) : ViewModel() {

    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */
    val uiState: StateFlow<SentNotificationsUiState> =
        combine(
            notificationRepository.observeSentBy(currentUserId),
            lotteryRepository.listLotteries(ownerId = currentUserId),
            campsiteRepository.observeAll(),
        ) { notifications, lotteries, campsites ->
            SentNotificationsUiState(
                sends = groupSends(notifications, lotteries, campsites),
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SentNotificationsUiState(),
        )

    companion object {
        /** Shown instead of campsite names when a send's lottery was deleted. */
        const val DELETED_LOTTERY = "Deleted lottery"

        /**
         * Groups notifications into one row per send, newest first.
         * Notifications without a batchId are treated as their own send.
         *
         * @param notifications the owner's sent notifications
         * @param lotteries the owner's lotteries that still exist
         * @param campsites campsites to take names from
         * @return one [SentNotification] per send, newest first
         */
        fun groupSends(
            notifications: List<Notification>,
            lotteries: List<Lottery>,
            campsites: List<Campsite>,
        ): List<SentNotification> {
            val lotteriesById = lotteries.associateBy { it.id }
            val campsiteNamesById = campsites.associate { it.id to it.name }
            return notifications
                .groupBy { it.batchId.ifBlank { it.id } }
                .map { (batchId, batch) ->
                    val first = batch.first()
                    val lottery = lotteriesById[first.lotteryId]
                    SentNotification(
                        batchId = batchId,
                        message = first.message,
                        campsiteNames = lottery?.let { namesFor(it, campsiteNamesById) } ?: DELETED_LOTTERY,
                        recipientCount = batch.map { it.recipientId }.distinct().size,
                        sentAt = batch.maxOf { it.sortTime },
                        lotteryDeleted = lottery == null,
                    )
                }
                .sortedWith(compareByDescending<SentNotification> { it.sentAt }.thenBy { it.batchId })
        }

        private fun namesFor(lottery: Lottery, campsiteNamesById: Map<String, String>): String =
            lottery.campsiteIds
                .mapNotNull { campsiteNamesById[it] }
                .joinToString(", ")
                .ifBlank { "Unnamed campsite" }

        /**
         * Builds this ViewModel with the shared repositories and the current
         * user's ID from the AppContainer.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                SentNotificationsViewModel(
                    app.container.notificationRepository,
                    app.container.lotteryRepository,
                    app.container.campsiteRepository,
                    app.container.currentUserId,
                )
            }
        }
    }
}

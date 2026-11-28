package com.campsitereservationapp.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.service.NotificationHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Everything the notify waitlist screen needs to draw.
 *
 * @property isLoading true until the lottery has been looked up
 * @property blockedReason why this user can't message this waitlist, or null if they can
 * @property campsiteNames names of the campsites in the lottery, joined with commas
 * @property waitlistCount how many entrants are on the waiting list
 * @property draft the message being typed
 * @property error a message about the last send attempt, or null
 * @property result what the last successful send did, such as "Sent to 3 entrants.", or null
 * @property isSending true while a message is being sent, so a second tap is ignored
 */
data class NotifyWaitlistUiState(
    val isLoading: Boolean = true,
    val blockedReason: String? = null,
    val campsiteNames: String = "",
    val waitlistCount: Int = 0,
    val draft: String = "",
    val error: String? = null,
    val result: String? = null,
    val isSending: Boolean = false,
) {
    /** True when the message box and Send button should be usable. */
    val canSend: Boolean
        get() = !isLoading && blockedReason == null && !isSending
}

/**
 * Holds the state for sending a message to a lottery's waiting list.
 *
 * **Purpose:** lets a campsite owner send a notification to everyone on one
 * of their lottery's waiting lists (US 02.10.01). Each notification records
 * the sender, the lottery, a shared batch ID and the send time, so the owner
 * can later see what they sent (US 02.10.02).
 *
 * **Design rationale:** MVVM, like the other screens, and built the same
 * way as ConversationViewModel. Only the lottery's owner can send. The
 * lottery is looked up again before each send, so the owner check and the
 * waiting list are current. Saving is done by [NotificationHandler], which
 * also skips entrants who turned notifications off. Only one send runs at a
 * time, so double-tapping Send sends one batch. Messages can be sent at any
 * lottery stage.
 *
 * **Outstanding issues:**
 * - Notifications are saved one at a time. If saving fails partway through,
 *   some entrants already have the message, and sending again gives them a
 *   second copy.
 * - When an owner's profile is deleted, the notifications they sent are kept;
 *   only notifications a user received are deleted with them.
 * - [Notification.createdAt] has no time, so win/lose notifications from the
 *   same day can't be ordered. Owner messages use [Notification.sentAt].
 *
 * @param lotteryRepository where the lottery and its waiting list come from
 * @param campsiteRepository used to show the lottery's campsite names
 * @param userRepository used to check which entrants have notifications on
 * @param notificationHandler builds and saves the notifications
 * @param currentUserId the person using the phone
 */
class NotifyWaitlistViewModel(
    private val lotteryRepository: LotteryRepository,
    private val campsiteRepository: CampsiteRepository,
    private val userRepository: UserRepository,
    private val notificationHandler: NotificationHandler,
    private val currentUserId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotifyWaitlistUiState())

    /** The current screen state. The screen redraws every time it changes. */
    val uiState: StateFlow<NotifyWaitlistUiState> = _uiState.asStateFlow()

    // Set once the lottery has loaded and the current user owns it.
    private var lotteryId: String? = null

    /**
     * Looks up the lottery and checks the current user owns it. Called by
     * the screen when it opens.
     *
     * @param lotteryId the lottery whose waiting list will be messaged
     */
    fun load(lotteryId: String) {
        viewModelScope.launch { loadNow(lotteryId) }
    }

    /**
     * Called when the text in the message box changes.
     *
     * @param text what the owner typed
     */
    fun onDraftChanged(text: String) {
        _uiState.update { it.copy(draft = text, error = null, result = null) }
    }

    /**
     * Called when the owner taps Send. Sends the draft to the waiting list,
     * or shows why it couldn't be sent. Taps while a send is already running
     * are ignored.
     */
    fun send() {
        viewModelScope.launch { sendNow() }
    }

    private suspend fun loadNow(id: String) {
        try {
            val lottery = lotteryRepository.get(id)
            if (lottery == null) {
                _uiState.value = blocked(LOTTERY_GONE)
                return
            }
            if (lottery.ownerId != currentUserId) {
                _uiState.value = blocked(NOT_OWNER)
                return
            }
            val names = lottery.campsiteIds
                .mapNotNull { campsiteRepository.get(it)?.name }
                .joinToString(", ")
                .ifBlank { "Unnamed campsite" }
            lotteryId = lottery.id
            _uiState.update {
                it.copy(
                    isLoading = false,
                    blockedReason = null,
                    campsiteNames = names,
                    waitlistCount = recipientsOf(lottery).size,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.value = blocked("Couldn't load this lottery. Check your connection and try again.")
        }
    }

    private suspend fun sendNow() {
        val id = lotteryId ?: return
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(isSending = true, error = null, result = null) }
        try {
            val text = _uiState.value.draft.trim()
            if (text.isBlank()) {
                showError("Write a message first.")
                return
            }
            if (text.length > Notification.MAX_LENGTH) {
                showError("Messages can be up to ${Notification.MAX_LENGTH} characters.")
                return
            }
            val latest = lotteryRepository.get(id)
            if (latest == null) {
                stop(LOTTERY_GONE)
                return
            }
            if (latest.ownerId != currentUserId) {
                stop(NOT_OWNER)
                return
            }
            val recipientIds = recipientsOf(latest)
            _uiState.update { it.copy(waitlistCount = recipientIds.size) }
            if (recipientIds.isEmpty()) {
                showError("No one is on this waitlist yet, so there's no one to notify.")
                return
            }
            val users = recipientIds.mapNotNull { userRepository.get(it) }
            val result = notificationHandler.sendWaitlistMessage(latest, currentUserId, text, users)
            if (result.sent.isEmpty()) {
                showError(
                    if (result.skippedOptedOut > 0) {
                        "Everyone on this waitlist has turned off notifications."
                    } else {
                        "No one on this waitlist can receive notifications right now."
                    }
                )
                return
            }
            _uiState.update {
                it.copy(draft = "", error = null, result = successText(result.sent.size, result.skippedOptedOut))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError("Couldn't send. Check your connection and try again.")
        } finally {
            _uiState.update { it.copy(isSending = false) }
        }
    }

    private fun recipientsOf(lottery: Lottery): List<String> =
        notificationHandler.waitlistRecipientIds(lottery, currentUserId)

    // Blocks the screen for good, so later taps on Send do nothing.
    private fun stop(reason: String) {
        lotteryId = null
        _uiState.value = blocked(reason)
    }

    private fun blocked(reason: String) = NotifyWaitlistUiState(isLoading = false, blockedReason = reason)

    private fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    companion object {
        private const val LOTTERY_GONE = "This lottery no longer exists."
        private const val NOT_OWNER = "Only this lottery's owner can notify its waitlist."

        // Such as "Sent to 3 entrants (1 skipped: notifications off)."
        private fun successText(sent: Int, skipped: Int): String {
            val entrants = if (sent == 1) "1 entrant" else "$sent entrants"
            return if (skipped > 0) {
                "Sent to $entrants ($skipped skipped: notifications off)."
            } else {
                "Sent to $entrants."
            }
        }

        /**
         * Builds this ViewModel with the shared repositories, the notification
         * handler and the current user's ID from the AppContainer.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                NotifyWaitlistViewModel(
                    app.container.lotteryRepository,
                    app.container.campsiteRepository,
                    app.container.userRepository,
                    app.container.notificationHandler,
                    app.container.currentUserId,
                )
            }
        }
    }
}

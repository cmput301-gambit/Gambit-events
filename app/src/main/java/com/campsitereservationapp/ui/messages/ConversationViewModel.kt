package com.campsitereservationapp.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.MessageRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Message
import com.campsitereservationapp.model.Reservation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

/**
 * One message as the conversation screen shows it.
 *
 * @property id the message's ID
 * @property text what the message says
 * @property sentAt when it was sent, as text like "2026-11-24 20:45"
 * @property isMine true if the person using the phone sent it
 */
data class ConversationMessage(
    val id: String,
    val text: String,
    val sentAt: String,
    val isMine: Boolean,
)

/**
 * Everything the conversation screen needs to draw.
 *
 * @property isLoading true until the reservation has been looked up
 * @property blockedReason why this user can't use this conversation, or null if they can
 * @property campsiteName name of the reserved campsite
 * @property otherPartyName who the user is talking to
 * @property messages the conversation, oldest first
 * @property draft the message being typed
 * @property error a message about the last send, or null
 */
data class ConversationUiState(
    val isLoading: Boolean = true,
    val blockedReason: String? = null,
    val campsiteName: String = "",
    val otherPartyName: String = "",
    val messages: List<ConversationMessage> = emptyList(),
    val draft: String = "",
    val error: String? = null,
) {
    /** True when the message box should be usable. */
    val canSend: Boolean
        get() = !isLoading && blockedReason == null
}

/**
 * Holds the state for the conversation about one reservation.
 *
 * **Purpose:** lets a campsite owner message the entrant who reserved
 * their campsite (US 02.12.01). The same screen works for the entrant
 * replying (US 01.14.01), because it works out who is who from the
 * reservation.
 *
 * **Design rationale:** MVVM, like the other screens. Only the campsite's
 * owner and the entrant who made the reservation can open it. It watches
 * [MessageRepository.observeForReservation], so new messages appear on
 * their own, and marks messages sent to this user as read when they show.
 *
 * **Outstanding issues:** cancelled reservations can't be messaged about.
 * There are no push notifications for new messages, only the screen.
 *
 * @param reservationRepository where the reservation comes from
 * @param campsiteRepository used to find the campsite and its owner
 * @param userRepository used to show the other person's name
 * @param messageRepository where messages are sent and read
 * @param currentUserId the person using the phone
 */
class ConversationViewModel(
    private val reservationRepository: ReservationRepository,
    private val campsiteRepository: CampsiteRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())

    /** The current screen state. The screen redraws every time it changes. */
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private var reservation: Reservation? = null
    private var ownerId: String = ""
    private var isOwner: Boolean = false
    private var watchJob: Job? = null

    /**
     * Looks up the reservation and starts showing its messages. Called by
     * the screen when it opens.
     *
     * @param reservationId the reservation the conversation is about
     */
    fun load(reservationId: String) {
        viewModelScope.launch { loadNow(reservationId) }
    }

    /**
     * Called when the text in the message box changes.
     *
     * @param text what the user typed
     */
    fun onDraftChanged(text: String) {
        _uiState.update { it.copy(draft = text, error = null) }
    }

    /**
     * Called when the user taps Send. Sends the draft, or shows why it
     * couldn't be sent.
     */
    fun send() {
        viewModelScope.launch { sendNow() }
    }

    private suspend fun loadNow(reservationId: String) {
        val found = reservationRepository.get(reservationId)
        if (found == null) {
            _uiState.value = blocked("This reservation no longer exists.")
            return
        }
        val campsite = campsiteRepository.get(found.campsiteId)
        if (campsite == null) {
            _uiState.value = blocked("The campsite for this reservation no longer exists.")
            return
        }
        val owner = currentUserId == campsite.ownerId
        val entrant = currentUserId == found.entrantId
        if (!owner && !entrant) {
            _uiState.value = blocked("Only the campsite owner and the guest can see this conversation.")
            return
        }
        if (campsite.ownerId == found.entrantId) {
            _uiState.value = blocked("You can't message yourself.")
            return
        }
        if (!found.isActive) {
            _uiState.value = blocked("This reservation was cancelled.")
            return
        }

        reservation = found
        ownerId = campsite.ownerId
        isOwner = owner
        val otherId = if (owner) found.entrantId else campsite.ownerId
        val otherName = userRepository.get(otherId)?.name?.takeIf { it.isNotBlank() }
            ?: if (owner) "Guest" else "Campsite owner"

        _uiState.update {
            it.copy(
                isLoading = false,
                blockedReason = null,
                campsiteName = campsite.name,
                otherPartyName = otherName,
            )
        }
        watchMessages(found.id)
    }

    private fun watchMessages(reservationId: String) {
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            messageRepository.observeForReservation(reservationId).collect { messages ->
                _uiState.update { state -> state.copy(messages = messages.map { it.toShown() }) }
                messages.filter { it.recipientId == currentUserId && !it.read }.forEach { markRead(it) }
            }
        }
    }

    private suspend fun markRead(message: Message) {
        try {
            messageRepository.markRead(message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Couldn't save the read status. It stays unread, which is safe.
        }
    }

    private suspend fun sendNow() {
        val current = reservation ?: return
        val text = _uiState.value.draft
        if (text.isBlank()) {
            showError("Write a message first.")
            return
        }
        val id = messageRepository.newId()
        val message = if (isOwner) {
            Message.fromOwner(id, current, ownerId, text)
        } else {
            Message.fromEntrant(id, current, ownerId, text)
        }
        try {
            val sent = messageRepository.send(message)
            if (sent == null) {
                showError("Messages can be up to ${Message.MAX_LENGTH} characters.")
            } else {
                _uiState.update { it.copy(draft = "", error = null) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError("Couldn't send. Check your connection and try again.")
        }
    }

    private fun Message.toShown() = ConversationMessage(
        id = id,
        text = text,
        sentAt = sentAt.format(TIME_FORMAT),
        isMine = senderId == currentUserId,
    )

    private fun blocked(reason: String) = ConversationUiState(isLoading = false, blockedReason = reason)

    private fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    companion object {
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        /**
         * Builds this ViewModel with the shared repositories and the current
         * user's ID from the AppContainer.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                ConversationViewModel(
                    app.container.reservationRepository,
                    app.container.campsiteRepository,
                    app.container.userRepository,
                    app.container.messageRepository,
                    app.container.currentUserId,
                )
            }
        }
    }
}
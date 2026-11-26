package com.campsitereservationapp.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * One entrant staying at the owner's campsite.
 *
 * @property reservationId ID of their reservation, used to open the conversation
 * @property guestName the entrant's name, or "Guest" if they haven't set one
 * @property dates the reserved dates as text, like "2026-12-02 to 2026-12-04"
 */
data class GuestItem(
    val reservationId: String,
    val guestName: String,
    val dates: String,
)

/**
 * Everything the owner's guest list needs to draw.
 *
 * @property isLoading true until the first data arrives
 * @property guests the campsite's current and upcoming guests, earliest first
 * @property blockedReason why this user can't see the list, or null if they can
 */
data class OwnerGuestsUiState(
    val isLoading: Boolean = true,
    val guests: List<GuestItem> = emptyList(),
    val blockedReason: String? = null,
)

/**
 * Holds the state for the list of entrants a campsite owner can message.
 *
 * **Purpose:** shows the owner who has an active reservation at one of
 * their campsites, so they can pick someone to message (US 02.12.01).
 * Only the campsite's owner can see the list; anyone else, or a campsite
 * that no longer exists, gets a message explaining why instead.
 *
 * **Design rationale:** MVVM, like the other screens. Checks the campsite's
 * owner first, so guest names and dates are never loaded for someone else.
 * Then combines the live reservation and user streams, so the list updates
 * on its own when someone reserves or cancels. Reservations that have
 * already ended are left out, since the story is about users who are reserving.
 *
 * **Outstanding issues:** loads every user to look up names, which is fine
 * for this app's size. This is a messaging list only; the owner's full
 * reservation lists are separate stories (02.07.01 to 02.07.03).
 *
 * @param reservationRepository where the campsite's reservations come from
 * @param userRepository used to show entrants' names
 * @param campsiteRepository used to check who owns the campsite
 * @param currentUserId the person using the phone; must own the campsite
 * @param today used to hide reservations that have ended; tests pass a fixed date
 */
class OwnerGuestsViewModel(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val campsiteRepository: CampsiteRepository,
    private val currentUserId: String,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(OwnerGuestsUiState())

    /** The current screen state. The screen redraws every time it changes. */
    val uiState: StateFlow<OwnerGuestsUiState> = _uiState.asStateFlow()

    private var watchJob: Job? = null

    /**
     * Starts showing the guests of one campsite. Called by the screen when it
     * opens. If the campsite is missing or the current user doesn't own it,
     * shows why instead of the list.
     *
     * @param campsiteId the owner's campsite
     */
    fun load(campsiteId: String) {
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            val campsite = campsiteRepository.get(campsiteId)
            if (campsite == null) {
                _uiState.value = blocked("This campsite no longer exists.")
                return@launch
            }
            if (campsite.ownerId != currentUserId) {
                _uiState.value = blocked("Only this campsite's owner can message its guests.")
                return@launch
            }
            combine(
                reservationRepository.observeForCampsite(campsiteId),
                userRepository.observeAll(),
            ) { reservations, users ->
                val namesById = users.associate { it.id to it.name }
                OwnerGuestsUiState(
                    isLoading = false,
                    guests = reservations
                        .filter { it.isActive && !it.dates.finish.isBefore(today) }
                        .map {
                            GuestItem(
                                reservationId = it.id,
                                guestName = namesById[it.entrantId].takeUnless { name -> name.isNullOrBlank() } ?: "Guest",
                                dates = "${it.dates.start} to ${it.dates.finish}",
                            )
                        },
                )
            }.collect { _uiState.value = it }
        }
    }

    private fun blocked(reason: String) = OwnerGuestsUiState(isLoading = false, blockedReason = reason)

    companion object {
        /**
         * Builds this ViewModel with the shared repositories and the current
         * user's ID from the AppContainer.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                OwnerGuestsViewModel(
                    app.container.reservationRepository,
                    app.container.userRepository,
                    app.container.campsiteRepository,
                    app.container.currentUserId,
                )
            }
        }
    }
}
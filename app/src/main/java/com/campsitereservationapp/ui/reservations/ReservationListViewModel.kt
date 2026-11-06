package com.campsitereservationapp.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.model.Reservation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * One row in the reservation list: a reservation and the name of its campsite.
 *
 * @property reservation the reservation itself
 * @property campsiteName the campsite's name, shown on the card
 */
data class ReservationListItem(
    val reservation: Reservation,
    val campsiteName: String,
)

/**
 * What the reservation list screen shows.
 *
 * @property reservations the entrant's active, upcoming reservations
 * @property isLoading true until the first data arrives
 */
data class ReservationListUiState(
    val reservations: List<ReservationListItem> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel for the entrant's reservation list (US 01.06.01, 01.07.01).
 *
 * **Purpose:** shows the current entrant's active reservations with their
 * campsite names, and cancels a reservation when asked.
 *
 * **Design rationale:** combines two live streams (this entrant's
 * reservations and all campsites), so the list updates by itself when a
 * reservation is made or cancelled, or a campsite is renamed. Cancelling
 * goes through [ReservationRepository.cancel], which saves the change as
 * one step and frees the dates for other winners.
 *
 * **Outstanding issues:** a failed cancel isn't shown to the user; the
 * reservation simply stays in the list.
 *
 * @param reservationRepository where reservations are read and cancelled
 * @param campsiteRepository used to show each reservation's campsite name
 * @param entrantId the current entrant's userId
 * @param today used to hide reservations that have already ended; tests
 * pass a fixed date
 */
class ReservationListViewModel(
    private val reservationRepository: ReservationRepository,
    campsiteRepository: CampsiteRepository,
    entrantId: String,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {

    /** The current screen state, updated whenever reservations or campsites change. */
    val uiState: StateFlow<ReservationListUiState> =
        combine(
            reservationRepository.observeForEntrant(entrantId),
            campsiteRepository.observeAll(),
        ) { reservations, campsites ->
            val namesById = campsites.associate { it.id to it.name }
            ReservationListUiState(
                reservations = reservations
                    .filter { it.isActive && !it.dates.finish.isBefore(today) }
                    .map { ReservationListItem(it, namesById[it.campsiteId] ?: "Removed campsite") },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReservationListUiState(),
        )

    /**
     * Cancels one of the entrant's reservations (US 01.07.01).
     *
     * @param reservationId the reservation to cancel
     */
    fun cancel(reservationId: String) {
        viewModelScope.launch {
            try {
                reservationRepository.cancel(reservationId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Couldn't save (for example, no network). The reservation
                // stays in the list, which is the truth.
            }
        }
    }

    companion object {
        /** Builds this ViewModel with the shared repositories from the AppContainer. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                ReservationListViewModel(
                    app.container.reservationRepository,
                    app.container.campsiteRepository,
                    app.container.currentUserId,
                )
            }
        }
    }
}
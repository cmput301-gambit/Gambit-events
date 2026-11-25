package com.campsitereservationapp.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Reservation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Owner reservation list item representation.
 *
 * @property reservation the reservation entity
 * @property campsiteName resolved display name of the campsite
 * @property guestName resolved display name of the entrant/guest
 */
data class OwnerReservationItem(
    val reservation: Reservation,
    val campsiteName: String,
    val guestName: String,
)

/**
 * UI state for owner reservation list screen.
 *
 * @property items filtered list of owner reservation items
 * @property isLoading true while loading data
 */
data class OwnerReservationListUiState(
    val items: List<OwnerReservationItem> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel for the campsite owner reservation list (US 02.07.01).
 *
 * **Purpose:** displays active upcoming reservations for campsites owned specifically
 * by the logged-in owner, resolving campsite and guest names cleanly.
 *
 * **Design rationale:** MVVM. Combines live streams from [ReservationRepository],
 * [CampsiteRepository], and [UserRepository] to filter reservations by owner campsite IDs
 * and resolve human-readable display names instead of raw Firestore IDs.
 *
 * **Outstanding issues:** none.
 *
 * @property reservationRepository shared repository providing reservation records
 * @property campsiteRepository shared repository providing campsite records
 * @property userRepository shared repository providing user profile records
 * @property currentUserId ID of the currently logged-in campsite owner
 * @property today reference date for filtering active upcoming reservations
 */
class OwnerReservationListViewModel(
    private val reservationRepository: ReservationRepository,
    private val campsiteRepository: CampsiteRepository,
    private val userRepository: UserRepository,
    private val currentUserId: String,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    /**
     * Observable state stream emitting owner reservations matching current owner.
     */
    val uiState: StateFlow<OwnerReservationListUiState> = combine(
        reservationRepository.observeAll(),
        campsiteRepository.observeAll(),
        userRepository.observeAll(),
    ) { reservations, campsites, users ->
        val campsiteNames = campsites.associate { it.id to (if (it.name.isNotBlank()) it.name else "Campsite") }
        val userNames = users.associate { it.id to (if (it.name.isNotBlank()) it.name else "Guest") }
        val ownerCampsiteIds = campsites.filter { it.ownerId == currentUserId }.map { it.id }.toSet()

        val activeOwnerItems = reservations
            .filter { res ->
                res.isActive &&
                !res.dates.finish.isBefore(today) &&
                res.campsiteId in ownerCampsiteIds
            }
            .map { res ->
                OwnerReservationItem(
                    reservation = res,
                    campsiteName = campsiteNames[res.campsiteId] ?: "Campsite",
                    guestName = userNames[res.entrantId] ?: "Guest",
                )
            }
            .sortedBy { it.reservation.dates.start }

        OwnerReservationListUiState(
            items = activeOwnerItems,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OwnerReservationListUiState(),
    )

    companion object {
        /**
         * Factory building [OwnerReservationListViewModel] with dependencies from [CampsiteApp.container].
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                OwnerReservationListViewModel(
                    app.container.reservationRepository,
                    app.container.campsiteRepository,
                    app.container.userRepository,
                    app.container.currentUserId,
                )
            }
        }
    }
}

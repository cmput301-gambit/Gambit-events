package com.campsitereservationapp.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.model.Reservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Properties:
 */

data class OwnerReservationItem(
    val reservation: Reservation,
    val campsiteName: String,
    val guestName: String,
)

data class OwnerReservationListUiState(
    val items: List<OwnerReservationItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
)

/**
 * Owner Reservation List: Owner is able to see a dropdown menu of different reservations
 *
 * Purpose: Shows the reservations made at current owners campsite
 * Design Rationale:
 * Outstanding Issues
 *
 * @param ReservationRepository
 * @param CampsiteRepository
 * @param currentUserId
 * @param today
 */

class OwnerReservationListViewModel(
    reservationRepository: ReservationRepository,
    campsiteRepository: CampsiteRepository,
    currentUserId: String,
    today: LocalDate = LocalDate.now(),
    ): ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<OwnerReservationListUiState> =
        reservationRepository.observeAll()
            .map { reservations ->
                OwnerReservationListUiState(
                    items = reservations
                        .filter { it.isActive && !it.dates.finish.isBefore(today) }
                        .map { OwnerReservationItem(it, it.campsiteId, it.entrantId) },
                    isLoading = false,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = OwnerReservationListUiState(),
            )
    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                OwnerReservationListViewModel(
                    app.container.reservationRepository,
                    app.container.campsiteRepository,
                    app.container.currentUserId,
                )
            }
        }
    }

}
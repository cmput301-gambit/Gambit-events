package com.campsitereservationapp.ui.reservations

import androidx.lifecycle.ViewModel
//import com.campsitereservationapp.data.ReservationRepository
//import com.campsitereservationapp.model.Reservation

data class ReservationListUiState(
    //val reservations: List<Reservation> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Reservation View Model (US 01.06.01 & US 01.07.01)
 *
 * Purpose:
 *
 * Design Rationale:
 *
 * Outstanding Issues
 *
 *
 */

class ReservationListViewModel (
    //private val repo: ReservationRepository,
    entrantID: String
): ViewModel() {
    //val reservations: StateFlow<List<Reservation>> =
    //repo.observeForEntrant(entrantID)
        //.map {list -> list.filter { it.isActive}}
        //.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun cancel(reservationId: String) {
        //viewModelScope.launch {repo.cancel(reservationId)}
    }
}
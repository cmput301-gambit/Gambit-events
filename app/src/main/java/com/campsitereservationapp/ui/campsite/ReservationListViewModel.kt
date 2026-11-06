package com.campsitereservationapp.ui.campsite

import android.adservices.adid.AdId
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
//import com.campsitereservationapp.data.ReservationRepository
//import com.campsitereservationapp.model.Reservation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
package com.campsitereservationapp.ui.owner

import androidx.lifecycle.ViewModel
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

/**
 * Owner Reservation List: Owner is able to see a dropdown menu of different reservations
 *
 * Purpose
 * Design Rationale
 * Outstanding Issues
 *
 * Param
 */

data class OwnerReservationItem(
    val reservation: Reservation,
    val campsiteName: String,
    val guestName: String,
)

data class OwnerReservationListUiState(
    val items: List<OwnerReservationItem> = emptyList(),
    val query: String = "",
)
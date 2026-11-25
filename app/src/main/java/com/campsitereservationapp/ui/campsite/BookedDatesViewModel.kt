@file:Suppress("NewApi")

package com.campsitereservationapp.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Item representing one booked date entry for a campsite.
 *
 * @property reservationId unique ID of the reservation
 * @property entrantName display name of the entrant who booked the dates
 * @property dateRangeText formatted date range text (e.g., "July 1st–3rd" or "Jul 1–Jul 3")
 * @property startDate start date of the booking
 * @property endDate finish date of the booking
 * @property daysOfMonth list of day-of-month integers covered by this booking
 */
data class BookedDateItem(
    val reservationId: String,
    val entrantName: String,
    val dateRangeText: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysOfMonth: List<Int> = emptyList()
)

/**
 * UI state for the booked dates screen.
 *
 * @property campsiteId ID of the viewed campsite
 * @property campsiteName display name of the viewed campsite
 * @property bookedDatesList list of active bookings with entrant names and date ranges
 * @property bookedDaysOfMonth set of day-of-month numbers currently booked in the active month
 * @property monthName display name of the month (e.g., "July 2026")
 * @property isLoading true while data is loading
 */
data class BookedDatesUiState(
    val campsiteId: String = "",
    val campsiteName: String = "",
    val bookedDatesList: List<BookedDateItem> = emptyList(),
    val bookedDaysOfMonth: Set<Int> = emptySet(),
    val monthName: String = "",
    val isLoading: Boolean = true
)

/**
 * ViewModel for viewing currently booked dates for a campsite (US 02.06.01).
 *
 * **Purpose:** allows campsite owners to view all active bookings and taken date ranges
 * for a specific campsite.
 *
 * **Design rationale:** MVVM. Combines live streams from [ReservationRepository],
 * [UserRepository], and [CampsiteRepository] to map reservations to entrant names and
 * calculate booked calendar days dynamically.
 *
 * **Outstanding issues:** none.
 *
 * @property reservationRepository shared repository for reservations
 * @property userRepository shared repository for resolving entrant names
 * @property campsiteRepository shared repository for resolving campsite names
 * @property campsiteId ID of the campsite whose booked dates are being viewed
 */
class BookedDatesViewModel(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val campsiteRepository: CampsiteRepository,
    private val campsiteId: String
) : ViewModel() {

    private val viewedCampsiteId = MutableStateFlow(campsiteId)

    /**
     * Observable stream emitting the current booked dates UI state.
     */
    val uiState: StateFlow<BookedDatesUiState> = combine(
        reservationRepository.observeAll(),
        userRepository.observeAll(),
        campsiteRepository.observeAll(),
        viewedCampsiteId
    ) { reservations, users, campsites, currentCampId ->
        val campsite = campsites.find { it.id == currentCampId }
        val activeReservations = reservations.filter { it.campsiteId == currentCampId && it.isActive }
        val userNames = users.associate { it.id to (if (it.name.isNotBlank()) it.name else "Entrant") }

        val today = LocalDate.now()
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        val monthNameText = today.format(monthFormatter)

        val bookedItems = activeReservations.map { res ->
            val entrantName = userNames[res.entrantId] ?: "Entrant"
            val formattedDates = formatBookedDates(res.dates.start, res.dates.finish)
            val daysList = calculateDaysInMonth(res.dates.start, res.dates.finish, today.monthValue, today.year)
            BookedDateItem(
                reservationId = res.id,
                entrantName = entrantName,
                dateRangeText = formattedDates,
                startDate = res.dates.start,
                endDate = res.dates.finish,
                daysOfMonth = daysList
            )
        }.sortedBy { it.startDate }

        val allBookedDays = bookedItems.flatMap { it.daysOfMonth }.toSet()

        BookedDatesUiState(
            campsiteId = currentCampId,
            campsiteName = campsite?.name ?: "Campsite",
            bookedDatesList = bookedItems,
            bookedDaysOfMonth = allBookedDays,
            monthName = monthNameText,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookedDatesUiState(campsiteId = campsiteId)
    )

    /**
     * Updates the viewed campsite ID if changed dynamically.
     *
     * @param newCampsiteId new campsite ID to load
     */
    fun setCampsiteId(newCampsiteId: String) {
        viewedCampsiteId.update { newCampsiteId }
    }

    companion object {
        /**
         * Factory building [BookedDatesViewModel] for a specific campsite ID.
         *
         * @param campsiteId ID of the campsite whose booked dates to view
         */
        fun provideFactory(campsiteId: String) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                BookedDatesViewModel(
                    app.container.reservationRepository,
                    app.container.userRepository,
                    app.container.campsiteRepository,
                    campsiteId
                )
            }
        }
    }
}

/**
 * Formats start and finish dates into a readable date range string.
 */
private fun formatBookedDates(start: LocalDate, finish: LocalDate): String {
    val monthName = start.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val startDay = start.dayOfMonth
    val finishDay = finish.dayOfMonth

    return if (start.month == finish.month && start.year == finish.year) {
        "$monthName ${startDay}${ordinalSuffix(startDay)}–${finishDay}${ordinalSuffix(finishDay)}"
    } else {
        "${start.month.name.substring(0, 3)} $startDay – ${finish.month.name.substring(0, 3)} $finishDay"
    }
}

/**
 * Returns ordinal suffix for a day of month (e.g. 1st, 2nd, 3rd, 4th).
 */
private fun ordinalSuffix(day: Int): String {
    if (day in 11..13) return "th"
    return when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

/**
 * Calculates days of the given month/year that overlap with the date range.
 */
private fun calculateDaysInMonth(start: LocalDate, finish: LocalDate, month: Int, year: Int): List<Int> {
    val days = mutableListOf<Int>()
    var current = start
    while (!current.isAfter(finish)) {
        if (current.monthValue == month && current.year == year) {
            days.add(current.dayOfMonth)
        }
        current = current.plusDays(1)
    }
    return days
}

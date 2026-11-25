@file:Suppress("NewApi")

package com.campsitereservationapp.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.Filter
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Item representing one booked date entry for a campsite.
 *
 * @property reservationId unique ID of the reservation
 * @property entrantName display name of the entrant who booked the dates
 * @property dateRangeText formatted date range text (e.g., "Dec 3–5" or "Dec 30 – Jan 2")
 * @property startDate start date of the booking
 * @property endDate finish date of the booking
 * @property daysOfMonth list of day-of-month integers covered by this booking in the active month
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
 * @property bookedDatesList list of active upcoming bookings with entrant names and date ranges
 * @property bookedDaysOfMonth set of day-of-month numbers currently booked in the displayed month
 * @property displayedYearMonth current year and month being displayed on the calendar
 * @property monthName formatted display text for the month (e.g., "December 2026")
 * @property blanksBeforeFirstDay number of empty weekday cells before day 1 (Sunday = 0, Monday = 1, ...)
 * @property daysInMonth total number of days in the displayed month
 * @property isLoading true while data is loading
 */
data class BookedDatesUiState(
    val campsiteId: String = "",
    val campsiteName: String = "",
    val bookedDatesList: List<BookedDateItem> = emptyList(),
    val bookedDaysOfMonth: Set<Int> = emptySet(),
    val displayedYearMonth: YearMonth = YearMonth.now(),
    val monthName: String = "",
    val blanksBeforeFirstDay: Int = 0,
    val daysInMonth: Int = 31,
    val isLoading: Boolean = true
)

/**
 * ViewModel for viewing currently booked dates for a campsite (US 02.06.01).
 *
 * **Purpose:** allows campsite owners to view all active upcoming bookings and taken date ranges
 * for a specific campsite, with previous/next month calendar navigation.
 *
 * **Design rationale:** MVVM. Filters reservations on the server/repository layer using
 * `observeWhere(Filter("campsiteId", ...))` to fetch only reservations for the target campsite.
 * Defaults the active calendar month to the earliest upcoming booking (or current month if none),
 * and calculates grid weekday offsets (`blanks`) so calendar days sit under the correct weekday column.
 *
 * **Outstanding issues:** none.
 *
 * @property reservationRepository shared repository for reservations
 * @property userRepository shared repository for resolving entrant names
 * @property campsiteRepository shared repository for resolving campsite names
 * @property campsiteId ID of the campsite whose booked dates are being viewed
 * @property today reference date for filtering past bookings (defaults to [LocalDate.now])
 */
class BookedDatesViewModel(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
    private val campsiteRepository: CampsiteRepository,
    private val campsiteId: String,
    private val today: LocalDate = LocalDate.now()
) : ViewModel() {

    private val selectedYearMonth = MutableStateFlow<YearMonth?>(null)

    /**
     * Observable stream emitting the current booked dates UI state.
     */
    val uiState: StateFlow<BookedDatesUiState> = combine(
        reservationRepository.observeWhere(Filter("campsiteId", campsiteId) { it.campsiteId == campsiteId }),
        userRepository.observeAll(),
        campsiteRepository.observeAll(),
        selectedYearMonth
    ) { reservations, users, campsites, userSelectedMonth ->
        val campsite = campsites.find { it.id == campsiteId }
        val upcomingReservations = reservations.filter { it.isActive && !it.dates.finish.isBefore(today) }

        // Default to month of earliest upcoming booking, or current month if none
        val activeYearMonth = userSelectedMonth ?: upcomingReservations
            .map { YearMonth.from(it.dates.start) }
            .minOrNull() ?: YearMonth.from(today)

        val entrantIds = upcomingReservations.map { it.entrantId }.toSet()
        val userNames = users
            .filter { it.id in entrantIds }
            .associate { it.id to (if (it.name.isNotBlank()) it.name else "Entrant") }

        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
        val monthNameText = activeYearMonth.format(monthFormatter)

        val bookedItems = upcomingReservations.map { res ->
            val entrantName = userNames[res.entrantId] ?: "Entrant"
            val formattedDates = formatBookedDates(res.dates.start, res.dates.finish)
            val daysList = calculateDaysInYearMonth(res.dates.start, res.dates.finish, activeYearMonth)
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

        // Weekday grid alignment: Sunday = 0, Monday = 1, ... Saturday = 6
        val firstOfMonth = activeYearMonth.atDay(1)
        val blanks = firstOfMonth.dayOfWeek.value % 7

        BookedDatesUiState(
            campsiteId = campsiteId,
            campsiteName = campsite?.name ?: "Campsite",
            bookedDatesList = bookedItems,
            bookedDaysOfMonth = allBookedDays,
            displayedYearMonth = activeYearMonth,
            monthName = monthNameText,
            blanksBeforeFirstDay = blanks,
            daysInMonth = activeYearMonth.lengthOfMonth(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookedDatesUiState(campsiteId = campsiteId)
    )

    /**
     * Navigates the calendar view to the previous month.
     */
    fun onPreviousMonth() {
        val current = uiState.value.displayedYearMonth
        selectedYearMonth.update { current.minusMonths(1) }
    }

    /**
     * Navigates the calendar view to the next month.
     */
    fun onNextMonth() {
        val current = uiState.value.displayedYearMonth
        selectedYearMonth.update { current.plusMonths(1) }
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
 * Formats start and finish dates into a readable date range string (e.g. "Dec 3–5" or "Dec 30 – Jan 2").
 */
private fun formatBookedDates(start: LocalDate, finish: LocalDate): String {
    val startMonth = start.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val finishMonth = finish.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    return if (start.month == finish.month && start.year == finish.year) {
        "$startMonth ${start.dayOfMonth}–${finish.dayOfMonth}"
    } else if (start.year == finish.year) {
        "$startMonth ${start.dayOfMonth} – $finishMonth ${finish.dayOfMonth}"
    } else {
        "$startMonth ${start.dayOfMonth}, ${start.year} – $finishMonth ${finish.dayOfMonth}, ${finish.year}"
    }
}

/**
 * Calculates days of the given [YearMonth] that overlap with the date range.
 */
private fun calculateDaysInYearMonth(start: LocalDate, finish: LocalDate, yearMonth: YearMonth): List<Int> {
    val days = mutableListOf<Int>()
    var current = start
    while (!current.isAfter(finish)) {
        if (YearMonth.from(current) == yearMonth) {
            days.add(current.dayOfMonth)
        }
        current = current.plusDays(1)
    }
    return days
}

package com.campsitereservationapp.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.Reservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Everything the reserve screen needs to draw.
 *
 * @property isLoading true until the lottery and campsite have been looked up
 * @property campsiteName name of the campsite being reserved
 * @property blockedReason why this user can't reserve, or null if they can
 * @property freeRanges the date ranges at the campsite that nobody has booked
 * @property checkInText the check-in date as typed, like 2026-12-10
 * @property checkOutText the check-out date as typed
 * @property error a message about the last attempt, or null
 * @property confirmation the saved reservation once booking worked, or null
 */
data class ReserveUiState(
    val isLoading: Boolean = true,
    val campsiteName: String = "",
    val blockedReason: String? = null,
    val freeRanges: List<DateRange> = emptyList(),
    val checkInText: String = "",
    val checkOutText: String = "",
    val error: String? = null,
    val confirmation: Reservation? = null,
) {
    /** True when the form should be usable. */
    val canReserve: Boolean
        get() = !isLoading && blockedReason == null && confirmation == null
}

/**
 * Holds the state for the screen where a lottery winner reserves dates.
 *
 * **Purpose:** shows a winner which dates are free at their campsite and
 * lets them reserve some, then confirms the reservation
 * (US 01.04.01, 01.05.01).
 *
 * **Design rationale:** MVVM, like the other screens. Only winners of a
 * drawn lottery can reserve, and each winner gets one active reservation
 * per lottery. The clash check itself lives in [ReservationRepository], so
 * dates can't be booked twice.
 *
 * **Outstanding issues:** does not check the lottery's booking period
 * (reservationPeriod) yet, and uses the first campsite if a lottery has
 * several. Dates are typed in as text for now. Reservations are not saved
 * between app restarts until the Firestore mapping is added.
 *
 * @param lotteryRepository where the lottery comes from
 * @param campsiteRepository where the campsite comes from
 * @param reservationRepository where reservations are saved
 * @param currentUserId the entrant using the phone
 */
class ReserveViewModel(
    private val lotteryRepository: LotteryRepository,
    private val campsiteRepository: CampsiteRepository,
    private val reservationRepository: ReservationRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReserveUiState())

    /** The current screen state. The screen redraws every time it changes. */
    val uiState: StateFlow<ReserveUiState> = _uiState.asStateFlow()

    private var lotteryId: String? = null
    private var campsiteId: String? = null

    /**
     * Looks up the lottery and campsite and works out which dates are free.
     * Called by the screen when it opens.
     *
     * @param lotteryId the lottery the user won
     */
    fun load(lotteryId: String) {
        viewModelScope.launch { loadNow(lotteryId) }
    }

    /**
     * Called when the check-in text changes.
     *
     * @param text what the user typed
     */
    fun onCheckInChanged(text: String) {
        _uiState.update { it.copy(checkInText = text, error = null) }
    }

    /**
     * Called when the check-out text changes.
     *
     * @param text what the user typed
     */
    fun onCheckOutChanged(text: String) {
        _uiState.update { it.copy(checkOutText = text, error = null) }
    }

    /**
     * Called when the user taps Reserve. Checks the typed dates, saves the
     * reservation, and shows the confirmation, or shows what went wrong.
     */
    fun reserve() {
        viewModelScope.launch { reserveNow() }
    }

    private suspend fun loadNow(lotteryId: String) {
        this.lotteryId = lotteryId
        val lottery = lotteryRepository.get(lotteryId)
        if (lottery == null) {
            _uiState.value = blocked("This lottery no longer exists.")
            return
        }

        // One active reservation per win, so show it if they already have one.
        val existing = reservationRepository.getAll().firstOrNull {
            it.lotteryId == lotteryId && it.entrantId == currentUserId && it.isActive
        }
        var campsite: Campsite? = null
        for (id in lottery.campsiteIds) {
            campsite = campsiteRepository.get(id)
            if (campsite != null) break
        }
        campsiteId = campsite?.id
        val name = campsite?.name.orEmpty()

        if (existing != null) {
            _uiState.value = ReserveUiState(isLoading = false, campsiteName = name, confirmation = existing)
            return
        }
        if (lottery.stage != LotteryStatus.CLOSED) {
            _uiState.value = blocked("This lottery hasn't been drawn yet.", name)
            return
        }
        if (currentUserId !in lottery.waitingList.entrantWinners) {
            _uiState.value = blocked("Only lottery winners can reserve dates.", name)
            return
        }
        if (campsite == null) {
            _uiState.value = blocked("The campsite for this lottery no longer exists.")
            return
        }
        val availability = campsite.availability
        if (availability == null) {
            _uiState.value = blocked("This campsite has no available dates yet.", name)
            return
        }

        val free = freeRanges(availability, reservationRepository.bookedDates(campsite.id))
        _uiState.value = if (free.isEmpty()) {
            blocked("No dates are left at this campsite.", name)
        } else {
            ReserveUiState(isLoading = false, campsiteName = name, freeRanges = free)
        }
    }

    private suspend fun reserveNow() {
        val state = _uiState.value
        val reservingLotteryId = lotteryId ?: return
        val reservingCampsiteId = campsiteId ?: return
        if (!state.canReserve) return

        val checkIn = parseDate(state.checkInText)
        val checkOut = parseDate(state.checkOutText)
        if (checkIn == null || checkOut == null) {
            showError("Enter both dates as yyyy-mm-dd, like 2026-12-10.")
            return
        }
        if (checkOut.isBefore(checkIn)) {
            showError("Check-out can't be before check-in.")
            return
        }
        val insideFreeRange = state.freeRanges.any {
            !checkIn.isBefore(it.start) && !checkOut.isAfter(it.finish)
        }
        if (!insideFreeRange) {
            showError("Pick dates that fall inside one of the free ranges.")
            return
        }

        val saved = reservationRepository.reserve(
            Reservation(
                id = reservationRepository.newId(),
                entrantId = currentUserId,
                campsiteId = reservingCampsiteId,
                lotteryId = reservingLotteryId,
                dates = DateRange(checkIn, checkOut),
            )
        )
        if (saved == null) {
            // Someone booked these dates after the screen loaded, so refresh the free dates.
            loadNow(reservingLotteryId)
            showError("Those dates were just taken. Pick other dates.")
        } else {
            _uiState.update { it.copy(confirmation = saved, error = null) }
        }
    }

    private fun blocked(reason: String, campsiteName: String = "") =
        ReserveUiState(isLoading = false, campsiteName = campsiteName, blockedReason = reason)

    private fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    private fun parseDate(text: String): LocalDate? =
        try {
            LocalDate.parse(text.trim())
        } catch (e: DateTimeParseException) {
            null
        }

    companion object {
        /**
         * Works out which dates in a campsite's availability are not booked.
         *
         * @param availability the dates the campsite can be reserved
         * @param booked the date ranges already taken
         * @return the unbooked stretches, earliest first, first and last days included
         */
        fun freeRanges(availability: DateRange, booked: List<DateRange>): List<DateRange> {
            val ranges = mutableListOf<DateRange>()
            var runStart: LocalDate? = null
            var day = availability.start
            while (!day.isAfter(availability.finish)) {
                val taken = booked.any { !day.isBefore(it.start) && !day.isAfter(it.finish) }
                if (!taken && runStart == null) {
                    runStart = day
                } else if (taken && runStart != null) {
                    ranges.add(DateRange(runStart, day.minusDays(1)))
                    runStart = null
                }
                day = day.plusDays(1)
            }
            if (runStart != null) ranges.add(DateRange(runStart, availability.finish))
            return ranges
        }

        /**
         * Builds this ViewModel with the shared repositories and the current
         * user's ID from the AppContainer.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                ReserveViewModel(
                    app.container.lotteryRepository,
                    app.container.campsiteRepository,
                    app.container.reservationRepository,
                    app.container.currentUserId,
                )
            }
        }
    }
}
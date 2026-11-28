package com.campsitereservationapp.ui.reservations

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Csv
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.ReservationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * Everything the reserve screen needs to draw.
 *
 * @property isLoading true until the lottery and campsite have been looked up
 * @property campsiteName name of the campsite being reserved
 * @property campsiteLocation where the campsite is
 * @property campsiteCapacity how many guests the campsite holds, as text
 * @property blockedReason why this user can't reserve, or null if they can
 * @property freeRanges the date ranges the winner can book: inside the campsite's
 * availability and the lottery's reservation period, and not already booked
 * @property checkIn the check-in date picked so far, or null
 * @property checkOut the check-out date picked so far, or null
 * @property error a message about the last attempt, or null
 * @property confirmation the saved reservation once booking worked, or null
 * @property sendCSV toggles whether to give a download prompt of a CSV after reservation has been confirmed.
 * @property csvRequested true when a CSV export has been requested by a successful reservation.
 * @property entrantName the reserving entrant's profile name, shown on the confirmation card (M34)
 * @property campsiteRules the campsite's rules and policies the entrant must agree to (US 02.11.01)
 * @property rulesAccepted true once the entrant has ticked "Read & Agree to Rules and Policy"
 */
data class ReserveUiState(
    val isLoading: Boolean = true,
    val campsiteName: String = "",
    val campsiteLocation: String = "",
    val campsiteCapacity: String = "",
    val blockedReason: String? = null,
    val freeRanges: List<DateRange> = emptyList(),
    val checkIn: LocalDate? = null,
    val checkOut: LocalDate? = null,
    val error: String? = null,
    val confirmation: Reservation? = null,
    val sendCSV: Boolean = false,
    val csvRequested: Boolean = false,
    val entrantName: String = "",
    val campsiteRules: String = "",
    val rulesAccepted: Boolean = false,
) {
    /** True when the form should be usable. */
    val canReserve: Boolean
        get() = !isLoading && blockedReason == null && confirmation == null

    /**
     * True when the confirm button can be tapped: the form is usable and the
     * entrant has agreed to the campsite's rules and policies (US 02.11.01).
     */
    val canConfirm: Boolean
        get() = canReserve && rulesAccepted
}

/**
 * Holds the state for the screen where a lottery winner reserves dates.
 *
 * **Purpose:** shows a winner which dates are free at their campsite and
 * lets them reserve some, then confirms the reservation
 * (US 01.04.01, 01.05.01). Before confirming, the entrant must read and agree
 * to the campsite's rules and policies (US 02.11.01).
 *
 * **Design rationale:** MVVM, like the other screens. Only winners of a
 * drawn lottery can reserve, and each winner gets one active reservation
 * per lottery. The dates a winner can pick are the campsite's availability
 * limited to the lottery's reservation period, minus dates already booked.
 * The clash check itself lives in [ReservationRepository], so dates can't
 * be booked twice. Dates come from the same date range picker as the
 * campsite search ([com.campsitereservationapp.ui.discovery.DateFilterMenu]), which can't limit the days you can
 * select, so this class checks that the picked dates fall inside a free
 * range. The rules gate lives in [ReserveUiState.rulesAccepted] and is checked
 * again in [reserve], so a reservation can't be saved without agreement even if
 * the screen's disabled button were bypassed.
 *
 * **Outstanding issues:** uses the first campsite if a lottery has
 * several. Reservations are not saved between app restarts until the
 * Firestore mapping is added.
 *
 * @param lotteryRepository where the lottery comes from
 * @param campsiteRepository where the campsite comes from
 * @param reservationRepository where reservations are saved
 * @param userRepository where the entrant's profile name comes from
 * @param currentUserId the entrant using the phone
 */
class ReserveViewModel(
    private val lotteryRepository: LotteryRepository,
    private val campsiteRepository: CampsiteRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository,
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
     * Called when the user confirms a date range in the picker. If only one
     * day was picked, the end date is null and that day is used for both
     * check-in and check-out.
     *
     * @param checkIn the first day picked, or null if nothing was picked
     * @param checkOut the last day picked, or null for a single day
     */
    fun onDatesPicked(checkIn: LocalDate?, checkOut: LocalDate?) {
        _uiState.update { it.copy(checkIn = checkIn, checkOut = checkOut ?: checkIn, error = null) }
    }

    /**
     * Called when the entrant ticks or unticks "Read & Agree to Rules and
     * Policy" (US 02.11.01).
     *
     * @param accepted true if the entrant agrees to the campsite's rules
     */
    fun onRulesAcceptedChange(accepted: Boolean) {
        _uiState.update { it.copy(rulesAccepted = accepted, error = null) }
    }

    /**
     * Toggles whether a CSV of the reservation is offered once it is confirmed.
     *
     * @param newState true to offer the CSV download
     */
    fun onToggleReceiveCSV(newState: Boolean){
        _uiState.update { it.copy(sendCSV = newState) }
    }

    /**
     * Called when the user taps Reserve. Checks the picked dates, saves the
     * reservation, and shows the confirmation, or shows what went wrong.
     */
    fun reserve() {
        viewModelScope.launch { reserveNow() }
    }

    /**
     * Called by the UI when the CSV export launcher has been triggered.
     */
    fun onCsvLaunched() {
        _uiState.update { it.copy(csvRequested = false) }
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
        val location = campsite?.location.orEmpty()
        val capacity = campsite?.capacity?.toString().orEmpty()
        val rules = campsite?.rules.orEmpty()
        val entrantName = userRepository.get(currentUserId)?.name.orEmpty()

        if (existing != null) {
            _uiState.value = ReserveUiState(
                isLoading = false,
                campsiteName = name,
                campsiteLocation = location,
                campsiteCapacity = capacity,
                confirmation = existing,
            )
            return
        }
        // A rejected booking can't be remade; tell the entrant why (US 02.08.01).
        val wasRejected = reservationRepository.getAll().any {
            it.lotteryId == lotteryId && it.entrantId == currentUserId &&
                it.status == ReservationStatus.REJECTED
        }
        if (wasRejected) {
            _uiState.value = blocked("The campsite owner rejected your reservation.", name)
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

        // Only dates inside both the campsite's availability and the booking period.
        val bookable = overlap(availability, lottery.reservationPeriod)
        if (bookable == null) {
            _uiState.value = blocked("No dates are open for booking at this campsite.", name)
            return
        }
        val free = freeRanges(bookable, reservationRepository.bookedDates(campsite.id))
        _uiState.value = if (free.isEmpty()) {
            blocked("No dates are left at this campsite.", name)
        } else {
            ReserveUiState(
                isLoading = false,
                campsiteName = name,
                campsiteLocation = location,
                campsiteCapacity = capacity,
                freeRanges = free,
                entrantName = entrantName,
                campsiteRules = rules,
            )
        }
    }

    private suspend fun reserveNow() {
        val state = _uiState.value
        val reservingLotteryId = lotteryId ?: return
        val reservingCampsiteId = campsiteId ?: return
        if (!state.canReserve) return
        if (!state.rulesAccepted) {
            showError("Read and agree to the campsite rules and policies first.")
            return
        }

        val checkIn = state.checkIn
        val checkOut = state.checkOut
        if (checkIn == null || checkOut == null) {
            showError("Choose your check-in and check-out dates first.")
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
            _uiState.update { it.copy(rulesAccepted = state.rulesAccepted) }
            showError("Those dates were just taken. Pick other dates.")
        } else {
            _uiState.update { it.copy(confirmation = saved, error = null, csvRequested = it.sendCSV) }
        }
    }

    /**
     * Called when the user makes a reservation and has opted into receiving a CSV confirmation.
     * Creates a CSV with the relevant info and prompts the user to specify a location to save the file to.
     *
     * @param context necessary variable to initiate
     * @param uri address for the new CSV
     */
    fun sendCSV(context: Context, uri: Uri){
        if (uiState.value.confirmation == null) return

        val appContext = context.applicationContext

        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val csvString = createCSVString()
                val stream = appContext.contentResolver.openOutputStream(uri, "wt") //Opening stream to write to
                    ?: throw IOException("openOutputStream returned null for $uri")
                //Actually writing to that location
                stream.use { out ->
                    out.write(csvString.toByteArray(Charsets.UTF_8))
                    out.flush()
                }

                "Exported" //This is the result

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {
                Log.e("CsvExport", "Export failed", e)

                "Export failed: ${e.message}" //This is the result with an error message.

            }

            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, result, Toast.LENGTH_LONG).show() //Giving user quick message with the results attached.
            }
        }
    }

    private fun createCSVString(): String {
        val confirmation = uiState.value.confirmation ?: return ""
        val csv = Csv(listOf(
            "Reservation ID",
            "Campsite Name",
            "Check-in Date",
            "Check-out Date",
        ))

        csv.addRow(listOf(
            confirmation.id,
            uiState.value.campsiteName,
            confirmation.dates.start.toString(),
            confirmation.dates.finish.toString(),
        ))

        return csv.toString()
    }

    private fun blocked(reason: String, campsiteName: String = "") =
        ReserveUiState(isLoading = false, campsiteName = campsiteName, blockedReason = reason)

    private fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    companion object {
        /**
         * Works out which dates in a range are not booked.
         *
         * @param availability the dates that could be reserved
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
         * Finds the days two date ranges have in common.
         *
         * @param a the first range
         * @param b the second range
         * @return the shared days, or null if the ranges don't overlap
         */
        fun overlap(a: DateRange, b: DateRange): DateRange? {
            val start = if (a.start.isAfter(b.start)) a.start else b.start
            val finish = if (a.finish.isBefore(b.finish)) a.finish else b.finish
            return if (start.isAfter(finish)) null else DateRange(start, finish)
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
                    app.container.userRepository,
                    app.container.currentUserId,
                )
            }
        }
    }
}

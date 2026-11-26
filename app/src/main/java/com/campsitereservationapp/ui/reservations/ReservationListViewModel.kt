package com.campsitereservationapp.ui.campsite

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
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.model.Csv
import com.campsitereservationapp.model.Reservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
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
 * @property showPast true when the list shows past reservations instead of active ones
 * @property isLoading true until the first data arrives
 * @property sendCSV true if the user checked the CSV export box
 * @property csvRequested true when a CSV export has been requested by a successful cancellation
 */
data class ReservationListUiState(
    val reservations: List<ReservationListItem> = emptyList(),
    val showPast: Boolean = false,
    val isLoading: Boolean = true,
    val sendCSV: Boolean = false,
    val csvRequested: Boolean = false,
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

    private val showPast = MutableStateFlow(false)

    //Checkbox state
    private val isSendCSV = MutableStateFlow(false)
    private val csvRequested = MutableStateFlow(false)

    /** The current screen state, updated whenever reservations or campsites change. */
    val uiState: StateFlow<ReservationListUiState> =
        combine(
            reservationRepository.observeForEntrant(entrantId),
            campsiteRepository.observeAll(),
            showPast,
            isSendCSV,
            csvRequested,
        ) { reservations, campsites, past, csv, csvReq ->
            val namesById = campsites.associate { it.id to it.name }
            val shown = reservations
                .filter {
                    it.isActive &&
                            if (past) it.dates.finish.isBefore(today) else !it.dates.finish.isBefore(today)
                }
                .let { list ->
                    if (past) list.sortedByDescending { it.dates.start } else list
                }
            ReservationListUiState(
                reservations = shown.map {
                    ReservationListItem(it, namesById[it.campsiteId] ?: "Removed campsite")
                },
                showPast = past,
                isLoading = false,
                sendCSV = csv,
                csvRequested = csvReq,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReservationListUiState(),
        )

    /**
     * @param past true to show reservations that have already ended
     */
    fun onShowPastChange(past: Boolean) {
        showPast.value = past
    }

    /**
     * Cancels one of the entrant's reservations (US 01.07.01).
     *
     * @param reservationId the reservation to cancel
     */
    fun cancel(reservationId: String) {
        viewModelScope.launch {
            try {
                reservationRepository.cancel(reservationId)
                csvRequested.update { isSendCSV.value }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Couldn't save (for example, no network). The reservation
                // stays in the list, which is the truth.
            } finally {
                isSendCSV.update { false }
            }
        }
    }

    fun onCsvLaunched() {
        csvRequested.update { false }
        onToggleReceiveCSV(false)
    }

    fun onToggleReceiveCSV(newState: Boolean){
        isSendCSV.update { newState }
    }

    /**
     * Called when the user makes a reservation and has opted into receiving a CSV confirmation.
     * Creates a CSV with the relevant info and prompts the user to specify a location to save the file to.
     *
     * @param context necessary variable to initiate
     * @param uri address for the new CSV
     * @param reservation the reservation being exported
     */
    fun sendCSV(context: Context, uri: Uri, reservation: ReservationListItem?){
        if (reservation == null) return

        val appContext = context.applicationContext

        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val csvString = createCSVString(reservation)
                val stream = appContext.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IOException("openOutputStream returned null for $uri")
                stream.use { out ->
                    out.write(csvString.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                "Exported"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CsvExport", "Export failed", e)
                "Export failed: ${e.message}"
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, result, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createCSVString(reservation: ReservationListItem?): String {
        if (reservation == null) return ""

        val csv = Csv(listOf(
            "Reservation ID",
            "Campsite Name",
            "Check-in Date",
            "Check-out Date",
        ))

        csv.addRow(listOf(
            reservation.reservation.id,
            reservation.campsiteName,
            reservation.reservation.dates.start.toString(),
            reservation.reservation.dates.finish.toString(),
        ))

        return csv.toString()
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

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
 * @property isLoading true until the first data arrives
 */
data class ReservationListUiState(
    val reservations: List<ReservationListItem> = emptyList(),
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

    //Checkbox state
    private val isSendCSV = MutableStateFlow(false)
    private val csvRequested = MutableStateFlow(false)
    /** The current screen state, updated whenever reservations or campsites change. */
    val uiState: StateFlow<ReservationListUiState> =
        combine(
            reservationRepository.observeForEntrant(entrantId),
            campsiteRepository.observeAll(),
            isSendCSV,
            csvRequested,
        ) { reservations, campsites, csv, csvReq ->
            val namesById = campsites.associate { it.id to it.name }
            ReservationListUiState(
                reservations = reservations
                    .filter { it.isActive && !it.dates.finish.isBefore(today) }
                    .map { ReservationListItem(it, namesById[it.campsiteId] ?: "Removed campsite") },
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
            }
        }
    }

    fun onCsvLaunched() {
        csvRequested.update { false }
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
     */
    fun sendCSV(context: Context, uri: Uri, reservation: ReservationListItem?){

        if(reservation == null) return

        val appContext = context.applicationContext

        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val csvString = createCSVString(reservation)
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

    private fun createCSVString(reservation: ReservationListItem?): String {
        if(reservation == null) return ""

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
package com.campsitereservationapp.ui.discovery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteFilter
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Lottery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Everything the campsite details screen needs to make and manage their status in a given
 * lottery, along with description display.
 *
 * The screen reads only this class. It never talks to a repository directly.
 *
 * @property mainButtonState determines the text displayed and behaviour of the join/leave waitlist button
 */
data class CampsiteDetailsUiState(
    val mainButtonState: CampsiteDetailsMainButtonState = CampsiteDetailsMainButtonState.JOIN,
    val viewedCampsiteId: String = "",
    val viewedCampsite: Campsite = Campsite()
)

/**
 * Holds the state for the entrant's campsite details screen.
 *
 * **Purpose:** provides details on a given campsite, and also allows the user to join or leave the campsites waitlist.
 * (US 01.01.05, 01.01.06, 01.02.01, 01.02.02).
 *
 * **Design rationale:** MVVM. Receives its repository from the
 * [com.campsitereservationapp.data.AppContainer] through [Factory], and
 * subscribes to the repository's live stream, so the list updates on its own
 * whenever campsite or lottery data changes.
 *
 * **Outstanding issues:** joining and leaving lotteries still need the required functions
 * The lotteryRepository still needs to be added
 *
 * @property LotteryRepository The shared repository lotteries come from
 * @property campsiteRepository The shared repository campsites come from. Required here because the campsite can only be passed
 * by value between this models screen and the last screen [CampsiteListScreen].
 */
class CampsiteDetailsViewModel(
    private val lotteryRepository: LotteryRepository,
    private val campsiteRepository: CampsiteRepository,
    private val currentEntrantId: String,
) : ViewModel() {

    //ID of the currently viewed campsite, passed from the previous screen.
    private val viewedCampsiteId: MutableStateFlow<String> = MutableStateFlow("")

    //Enum of the main button of this screen.
    private val mainButtonState : MutableStateFlow<CampsiteDetailsMainButtonState> = MutableStateFlow(
        CampsiteDetailsMainButtonState.JOIN
    )

    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */
    val uiState: StateFlow<CampsiteDetailsUiState> =

        combine(
            lotteryRepository.observeAll(), // could be changed to an observeWhere() call
            mainButtonState,
            viewedCampsiteId,
        ) { lotteries, newButtonState, campsiteId,  ->
            //Log.d("CampsiteDetails", "Fetching campsite with id='$campsiteId'")
            // Turn raw data into exactly what the screen needs to draw.
            CampsiteDetailsUiState(
                newButtonState,
                campsiteId,
                viewedCampsite = if (campsiteId.isBlank()) Campsite()
                else campsiteRepository.get(campsiteId) ?: Campsite() //Returns an empty campsite if no campsite is found.
            )
        }
            // stateIn() turns the stream into a StateFlow the screen can read.
            .stateIn(
                // Tie the subscription to this ViewModel's lifetime.
                // It's cancelled automatically when the ViewModel is cleared.
                scope = viewModelScope,
                // Stop the listener once the screen is gone for 5 seconds
                started = SharingStarted.WhileSubscribed(5_000),
                // What the screen shows before any data arrives.
                // Its isLoading = true lets the screen show a spinner.
                initialValue = CampsiteDetailsUiState(),
            )

    /**
     * Called by the screen whenever the main button is pressed. Contains all behaviour for joining and leaving
     * a campsite lottery.
     *
     * Updating [mainButtonState] makes combine() recalculate the state,
     * so the buttons text and behaviour are updated.
     */
    fun updateButtonState() {
        viewModelScope.launch {
            try {
                val camp = campsiteRepository.get(viewedCampsiteId.value)
                val lottery = lotteryRepository
                    .listLotteries(campsiteId = viewedCampsiteId.value)
                    .first()
                    .firstOrNull()
                //Case where either a campsite stopped existing or its availability is gone, or no lottery can be found
                if (camp?.availability == null || lottery == null) {
                    //println("BAD CAMP OR LOTTERY")
                    mainButtonState.update { CampsiteDetailsMainButtonState.UNAVAILABLE }
                }
                //Separating behaviour based on current button dialog
                //No case for UNAVAILABLE, not sure if anything should be there.
                when (mainButtonState.value) {

                    CampsiteDetailsMainButtonState.JOIN -> {
                        //println("UPDATING TO JOIN")
                        mainButtonState.update{
                            //println("Lottery add entrant : ${lottery}")
                            if (lottery?.addEntrant(currentEntrantId) ?: false) CampsiteDetailsMainButtonState.LEAVE
                            else CampsiteDetailsMainButtonState.UNAVAILABLE
                        }
                    }
                    CampsiteDetailsMainButtonState.LEAVE -> {
                        mainButtonState.update{
                            if (lottery?.removeEntrant(currentEntrantId) ?: false) CampsiteDetailsMainButtonState.JOIN
                            else CampsiteDetailsMainButtonState.UNAVAILABLE
                        }
                    }
                    else -> {
                        //Should something be here?
                    }
                }
                //Error handling, should only be necessary while going through test cases
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CampsiteDetails", "Failed to update lottery state", e)
                mainButtonState.value = CampsiteDetailsMainButtonState.UNAVAILABLE
            }
        }
    }
    /**
     * Called by the screen on the first load.
     *
     * Updating [newCampsiteId] makes combine() recalculate the state,
     * automatically making this screen display the correct info. Also updates the [mainButtonState] to be
     * accurate to the users current state relative to the viewed campsite.
     *
     * @param newCampsiteId the id of the campsite to display for this screen
     */
    fun updateViewedCampSiteId(newCampsiteId: String){
        viewedCampsiteId.update { newCampsiteId }
        viewModelScope.launch {
            //Might be overkill for getting a campsite
            val camp: Campsite = if (viewedCampsiteId.value.isBlank()) Campsite()
            else campsiteRepository.get(viewedCampsiteId.value) ?: Campsite()

            val lottery = lotteryRepository
                .listLotteries(campsiteId = viewedCampsiteId.value)
                .first()
                .firstOrNull()

            //Passed date or no lottery available for the camp
            if (camp.availability == null || lottery == null) {
                //println(lottery)
                mainButtonState.update { CampsiteDetailsMainButtonState.UNAVAILABLE }
            }else{
                if(lottery.waitingList.entrantList.contains(currentEntrantId))
                mainButtonState.update { CampsiteDetailsMainButtonState.LEAVE }
                else mainButtonState.update { CampsiteDetailsMainButtonState.JOIN }
            }
        }

    }

    /**
     * THIS IS WIP, not final as the rating screen has no mockup presently.
     * Called by the screen whenever the user submits a rating.
     *
     * Updating [newRating] makes combine() recalculate the state,
     * automatically hiding the rating popup after making a rating, and loading the rating to the
     * correct repository location.
     *
     * @param newRating the rating given by the user
     */
    fun onCreateRating(newRating: Float){
        //adds a rating to the campsite
    }

    /**
     * Deletes a campsite from the repository (admin functionality, US 03.04.01).
     *
     * @param campsiteId ID of the campsite to delete
     */
    fun deleteCampsite(campsiteId: String) {
        viewModelScope.launch {
            try {
                campsiteRepository.delete(campsiteId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CampsiteDetails", "Failed to delete campsite $campsiteId", e)
            }
        }
    }


    companion object {
        /**
         * Builds this ViewModel with the shared repository from the AppContainer.
         *
         * Android hands every factory the Application object under
         * APPLICATION_KEY. Because the manifest registers CampsiteApp, we can
         * cast it and reach the container. The ViewModel itself never sees
         * the container; it only receives the repository it needs.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                CampsiteDetailsViewModel(
                    app.container.lotteryRepository,
                    app.container.campsiteRepository,
                    app.container.currentUserId
               )
            }
        }
    }
}
package com.campsitereservationapp.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteFilter
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.model.Campsite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

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
    //private val lotteryRepository: LotteryRepository, //TODO: Implement when lotteryRepository is added.
    private val campsiteRepository: CampsiteRepository
) : ViewModel() {

    //ID of the currently viewed campsite, passed from the previous screen.
    private val viewedCampsiteId: MutableStateFlow<String> = MutableStateFlow("")

    //Enum of the main button of this screen.
    private val mainButtonState : MutableStateFlow<CampsiteDetailsMainButtonState> = MutableStateFlow(
        CampsiteDetailsMainButtonState.JOIN
        //TODO: Change this to be either JOIN or LEAVE depending on whether the user is already in that lottery.
        //Should be done after lottery is implemented.
    )

    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */
    val uiState: StateFlow<CampsiteDetailsUiState> =

        combine(
            //lotteryRepository.(This campsites lottery).observeAll(), // live campsite list from the repository
            mainButtonState,
            viewedCampsiteId,
        ) { /*lotteries,*/ newButtonState, campsiteId,  ->

            // Turn raw data into exactly what the screen needs to draw.
            CampsiteDetailsUiState(
                newButtonState,
                campsiteId,
                viewedCampsite = campsiteRepository.get(campsiteId) ?: Campsite() //Returns an empty campsite if no campsite is found.
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
     * Called by the screen whenever the main button is pressed.
     *
     * Updating [mainButtonState] makes combine() recalculate the state,
     * so the buttons text and behaviour are updated.
     * //TODO: Many changes required to properly implement joining and leaving
     */
    fun updateButtonState() {
        //Case where the campsite suddenly doesn't have a lottery (ex. the date passed)
        if(false){ //TODO: This should check if the given campsite has a joinable lottery
            mainButtonState.update { CampsiteDetailsMainButtonState.UNAVAILABLE }
            return
        }
        when (mainButtonState.value) {
            CampsiteDetailsMainButtonState.JOIN -> {
                //Try to join the campsite lottery
                //TODO: Add joining campsite lottery, and check for if that was successful before changing button
                mainButtonState.update {
                    CampsiteDetailsMainButtonState.LEAVE
                }
            }
            //Try to leave the campsite lottery
            //TODO: Add method of leaving campsite lottery here, and check for if that was successful before changing button
            CampsiteDetailsMainButtonState.LEAVE -> {
                mainButtonState.update {
                    CampsiteDetailsMainButtonState.JOIN
                }
            }
            else -> {
                return //This case should ideally be inaccessible
            }
        }

    }
    /**
     * Called by the screen on the first load.
     *
     * Updating [viewedCampsite] makes combine() recalculate the state,
     * automatically making this screen display the correct info.
     *
     * @param newCampsiteId the id of the campsite to display for this screen
     */
    fun updateViewedCampSiteId(newCampsiteId: String){
        viewedCampsiteId.update { newCampsiteId }
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
                    //TODO: enable once lotteryRepository is implemented.
                    //app.container.lotteryRepository,
                    app.container.campsiteRepository
               )
            }
        }
    }
}
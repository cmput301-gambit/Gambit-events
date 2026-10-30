package com.campsitereservationapp.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteFilter
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the campsite details screen needs to make and manage their status in a given
 * lottery, along with description display.
 *
 * The screen reads only this class. It never talks to a repository directly.
 *
 * @property mainButtonState determines the text displayed and behaviour of the join/leave waitlist button
 */
data class EntrantHomeUiState(
    val entrantName: String = ""
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
 * @property LotteryRepository The shared repository campsites come from
 */
class EntrantHomeScreenViewModel(
    private val userRepository: UserRepository,
    private val currentEntrantId: String,
) : ViewModel() {

    private val entrantName : MutableStateFlow<String> = MutableStateFlow<String>(
        currentEntrantId
    )
    // State that belongs to this screen only: what the user typed.
    // Private and mutable, so only this ViewModel can change it.
    init {
        viewModelScope.launch {
            val user: User = userRepository.get(currentEntrantId) ?: User()
            entrantName.update { user.name }
        }
    }
    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */



    val uiState: StateFlow<EntrantHomeUiState> =

        combine(
            entrantName,
        ) { /*lotteries,*/ name,  ->

            // Turn raw data into exactly what the screen needs to draw.
            EntrantHomeUiState(
                name[0], //Not sure why this needs to be indexed at 0...
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
                initialValue = EntrantHomeUiState(),
            )

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
//    suspend fun updateUserDisplayedName(){
//
//    }


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
                EntrantHomeScreenViewModel(
                    //TODO: enable once lotteryRepository is implemented.
                    //app.container.lotteryRepository,
                    app.container.userRepository,
                    app.container.currentUserId
                )
            }
        }
    }
}
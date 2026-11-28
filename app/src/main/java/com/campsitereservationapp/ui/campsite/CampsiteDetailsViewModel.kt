package com.campsitereservationapp.ui.campsite

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Rating
import com.campsitereservationapp.service.CascadeDeleter

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
 * @property ratingCount how many campers have rated the viewed campsite
 * @property reviews the written reviews for the viewed campsite, plus the user's own rating
 * even without text so they can delete it; the user's own comes first (US 01.13.02)
 */
data class CampsiteDetailsUiState(
    val mainButtonState: CampsiteDetailsMainButtonState = CampsiteDetailsMainButtonState.JOIN,
    val viewedCampsiteId: String = "",
    val viewedCampsite: Campsite = Campsite(),
    val viewedCampsiteScore: Float = 0f,
    val giveRatingPopupOpen: Boolean = false,
    val hasGivenRating: Boolean = false,
    val ratingCount: Int = 0,
    val reviews: List<CampsiteReview> = emptyList(),
)

/**
 * One written review as campers see it on the campsite details screen (US 01.13.02).
 *
 * @property rating the rating holding the stars and review text
 * @property authorName the reviewer's name, or "Camper" if they haven't set one
 * @property isMine true if the current user wrote it
 */
data class CampsiteReview(
    val rating: Rating,
    val authorName: String,
    val isMine: Boolean = false,
)

// Rating info for the viewed campsite, combined into the screen state.
private data class RatingInfo(
    val popupOpen: Boolean,
    val hasRated: Boolean,
    val score: Float,
    val count: Int,
    val reviews: List<CampsiteReview>,
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
 *
 *
 * @property [LotteryRepository] The shared repository lotteries come from
 * @property campsiteRepository The shared repository campsites come from. Required here because the campsite can only be passed
 * @property giveRatingPopupOpen Toggles the popup for giving a campsite a rating.
 * @property hasGivenRating Tracks if the user is allowed to give this campsite a rating
 * @property campsiteScore the score of the viewed campsite
 * @property cascadeDeleter deletes a campsite along with its lotteries, reservations and ratings
 * by value between this models screen and the last screen [com.campsitereservationapp.ui.discovery.CampsiteListScreen].
 * @property userRepository provides reviewers' names for the reviews list (US 01.13.02)
 */
class CampsiteDetailsViewModel(
    private val lotteryRepository: LotteryRepository,
    private val campsiteRepository: CampsiteRepository,
    private val ratingRepository: RatingRepository,
    private val currentEntrantId: String,
    private val cascadeDeleter: CascadeDeleter,
    private val userRepository: UserRepository,
) : ViewModel() {

    //ID of the currently viewed campsite, passed from the previous screen.
    private val viewedCampsiteId: MutableStateFlow<String> = MutableStateFlow("")

    private val giveRatingPopupOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)


    //Enum of the main button of this screen.
    private val mainButtonState : MutableStateFlow<CampsiteDetailsMainButtonState> = MutableStateFlow(
        CampsiteDetailsMainButtonState.JOIN
    )

    // Live rating info for the viewed campsite: whether the popup is open,
    // whether this user already rated it, its average score, and the written
    // reviews. Built from the live ratings, so it updates as soon as anyone
    // (including this user) rates or reviews.
    private val popupState = combine(
        giveRatingPopupOpen,
        ratingRepository.observeAll(),
        viewedCampsiteId,
        userRepository.observeAll(),
    ) { ratingOpen, ratings, campsiteId, users ->
        val forCampsite = ratings.filter { it.campsiteId == campsiteId }
        val names = users.associate { it.id to it.name }
        RatingInfo(
            popupOpen = ratingOpen,
            hasRated = forCampsite.any { it.userId == currentEntrantId },
            score = forCampsite.map { it.score }.average().toFloat().takeUnless { it.isNaN() } ?: 0f,
            count = forCampsite.size,
            reviews = forCampsite
                .filter { it.hasReview || it.userId == currentEntrantId }
                .map {
                    CampsiteReview(
                        rating = it,
                        authorName = names[it.userId]?.ifBlank { null } ?: "Camper",
                        isMine = it.userId == currentEntrantId,
                    )
                }
                .sortedByDescending { it.isMine },
        )
    }
    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */
    val uiState: StateFlow<CampsiteDetailsUiState> =

        combine(
            lotteryRepository.observeAll(), // could be changed to an observeWhere() call
            mainButtonState,
            viewedCampsiteId,
            popupState,
        ) { lotteries, newButtonState, campsiteId, ratingInfo ->
            //Log.d("CampsiteDetails", "Fetching campsite with id='$campsiteId'")
            // Turn raw data into exactly what the screen needs to draw.
            CampsiteDetailsUiState(
                newButtonState,
                campsiteId,
                viewedCampsite = if (campsiteId.isBlank()) Campsite()
                else campsiteRepository.get(campsiteId) ?: Campsite(), //Returns an empty campsite if no campsite is found.
                viewedCampsiteScore = ratingInfo.score,
                giveRatingPopupOpen = ratingInfo.popupOpen,
                hasGivenRating = ratingInfo.hasRated,
                ratingCount = ratingInfo.count,
                reviews = ratingInfo.reviews,
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
                        val joined = lottery?.let { lotteryRepository.joinWaitingList(it.id, currentEntrantId) }
                        mainButtonState.value =
                            if (joined != null) CampsiteDetailsMainButtonState.LEAVE
                            else CampsiteDetailsMainButtonState.UNAVAILABLE
                    }
                    CampsiteDetailsMainButtonState.LEAVE -> {
                        val left = lottery?.let { lotteryRepository.leaveWaitingList(it.id, currentEntrantId) }
                        mainButtonState.value =
                            if (left != null) CampsiteDetailsMainButtonState.JOIN
                            else CampsiteDetailsMainButtonState.UNAVAILABLE
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

    suspend fun getLotteryIdByCampsiteId(campsiteId: String): String {
        Log.d("campsitew", campsiteId)
        val lotteryId: String = lotteryRepository.getLotteryFromCampsiteId(campsiteId=campsiteId).first().firstOrNull()?.id.toString()
        Log.d("campsitew", lotteryId)
        return lotteryId
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
     *
     * Toggles the [giveRatingPopupOpen] variable and makes the state recalculate.
     *
     */
    fun toggleRatingPopup(){
        giveRatingPopupOpen.update { !giveRatingPopupOpen.value }
    }

    /**
     *
     * Updating [newRating] makes combine() recalculate the state,
     * automatically hiding the rating popup after making a rating, and loading the rating to the
     * correct repository location. The written review is optional (US 01.13.02): it is trimmed,
     * cut to [Rating.MAX_REVIEW_LENGTH], and saved blank when the user only gave stars.
     *
     * @param newRating the rating given by the user
     * @param review the user's optional written review
     */
    fun onCreateRating(newRating: Float, review: String = ""){
        giveRatingPopupOpen.update { false } //will always close the popup
        val campsiteId = viewedCampsiteId.value
        viewModelScope.launch {
            try {
                ratingRepository.add(
                    Rating(
                        // Same ID every time for this user and campsite, so a
                        // second rating is refused instead of duplicated.
                        id = RatingRepository.idFor(currentEntrantId, campsiteId),
                        userId = currentEntrantId,
                        campsiteId = campsiteId,
                        score = newRating,
                        review = review.trim().take(Rating.MAX_REVIEW_LENGTH),
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Already rated, or no connection. Nothing was saved, so the
                // Rate button stays (unless they already rated) and they can retry.
                Log.w("CampsiteDetails", "Rating not saved", e)
            }
        }
    }

    /**
     * Deletes the current user's own rating and review of the viewed campsite
     * (US 01.13.02). The average updates by itself, and the Rate and review
     * button comes back so they can rate again.
     */
    fun deleteMyRating() {
        val campsiteId = viewedCampsiteId.value
        if (campsiteId.isBlank()) return
        viewModelScope.launch {
            try {
                ratingRepository.delete(RatingRepository.idFor(currentEntrantId, campsiteId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("CampsiteDetails", "Review not deleted", e)
            }
        }
    }

    /**
     * Deletes a campsite along with its lotteries, reservations and ratings
     * (admin functionality, US 03.04.01).
     *
     * @param campsiteId ID of the campsite to delete
     */
    fun deleteCampsite(campsiteId: String) {
        viewModelScope.launch {
            try {
                cascadeDeleter.deleteCampsite(campsiteId)
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
                    app.container.ratingRepository,
                    app.container.currentUserId,
                    app.container.cascadeDeleter,
                    app.container.userRepository,
               )
            }
        }
    }
}
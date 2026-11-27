package com.campsitereservationapp.ui.owner

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
import com.campsitereservationapp.ui.campsite.CampsiteFormUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One of the owner's lotteries and who has joined it.
 *
 * @property lotteryId ID of the lottery
 * @property campsiteNames names of the campsites in the lottery, joined with commas
 * @property entrantNames names of everyone on the waitlist (their ID if they have no name)
 */
data class LotteryWaitlist(
    val lotteryId: String,
    val campsiteNames: String,
    val entrantNames: List<String>,
)

/**
 * Everything the owner waitlist screen needs to draw.
 *
 * @property lotteries the current owner's lotteries and their waitlists
 * @property isLoading true until the first data arrives, so the screen doesn't
 *   wrongly say there are no lotteries while loading
 * @property lotteryPendingRemoval the lottery the owner tapped Remove on, waiting
 *  for them to confirm; null when no confirmation popup is showing
 *
 */
data class OwnerWaitlistUiState(
    val lotteries: List<LotteryWaitlist> = emptyList(),
    val isLoading: Boolean = true,
    val lotteryPendingRemoval: String? = null,
)

/**
 * Holds the state for the owner's lottery waitlist screen.
 *
 * **Purpose:** shows a campsite owner who has joined the waitlist for each
 * of their lotteries (US 02.09.01).
 *
 * **Design rationale:** MVVM, like the other screens. Combines the live
 * streams from the lottery, campsite and user repositories, so the list
 * updates on its own when someone joins or leaves. Starts in a loading
 * state until the first data arrives.
 *
 * **Outstanding issues:** lotteries are still kept in memory until their
 * Firestore mapping exists. Loads all users to look up names, which is fine
 * for this app's size.
 *
 * @param lotteryRepository where the owner's lotteries come from
 * @param campsiteRepository used to show campsite names
 * @param userRepository used to turn entrant IDs into names
 * @param currentOwnerId ID of the owner viewing the screen
 */
class OwnerWaitlistViewModel(
    val lotteryRepository: LotteryRepository,
    val campsiteRepository: CampsiteRepository,
    userRepository: UserRepository,
    val currentOwnerId: String,
    val reservationRepository: ReservationRepository
) : ViewModel() {

    // The lottery the admin tapped Remove on, until they confirm or cancel.
    private val pendingRemovalId = MutableStateFlow<String?>(null)

    /**
     * Removes a lottery from the repository by its ID.
     *
     * @param lotteryId ID of the lottery to delete
     */
    fun removeLottery(lotteryId: String) {
        viewModelScope.launch {
            lotteryRepository.get(lotteryId) ?: return@launch
            val campsiteIds = lotteryRepository.listLotteries(lotteryId=lotteryId).first().first().campsiteIds
            campsiteIds.forEach { campsiteId ->
                updateCampsite(campsiteId, ::removeAvailability)
            }
            reservationRepository.deleteByLottery(lotteryId)
            lotteryRepository.delete(lotteryId)
        }
    }

    /**
     * Called when the owner taps Remove. Opens the confirmation popup
     * instead of deleting straight away.
     *
     * @param lotteryId the id of the lottery the owner wants to remove.
     */
    fun requestRemoval(lotteryId: String) {
        pendingRemovalId.value = lotteryId
    }

    /**
     * Called when the owner taps Cancel or closes the popup. Nothing is deleted.
     */
    fun cancelRemoval() {
        pendingRemovalId.value = null
    }

    /**
     * Called when the owner confirms. Deletes the lottery waiting for
     * confirmation and closes the popup. Does nothing if no removal is pending.
     */
    fun confirmRemoval() {
        val lottery = pendingRemovalId.value ?: return
        pendingRemovalId.value = null
        removeLottery(lottery)
    }


    /**
     * Removes availability by sending an update to the state flow of the [CampsiteFormUiState].
     *
     * @param _uiStateC is the `MutableStateFlow<CampsiteFormUiState>` which will run '.update {}'
     * */
    private fun removeAvailability(_uiStateC: MutableStateFlow<CampsiteFormUiState>?=null) {
        _uiStateC?.update { it.copy(availability = null)}
    }

    /**
     * Updates a campsite based on the passed campsite's id. This is a general update function and will
     *  execute any specific update function passed as long as it takes a `MutableStateFlow<CampsiteFormUiState>`
     *  to accommodate the _uiState which will be passed into it.
     *
     *  @param campsiteId The id of the campsite to be updated
     *  @param update The update function to be executed by [updateCampsite]. Update function requires
     *  taking `MutableStateFlow<CampsiteFormUiState>` as an argument so _uiState may be supplied by [updateCampsite]
     *
     * */
    suspend fun updateCampsite(campsiteId: String, update: (MutableStateFlow<CampsiteFormUiState>?)-> Unit) {
        val _uiStateC = MutableStateFlow(CampsiteFormUiState(isLoading = true))
        val uiStateC: StateFlow<CampsiteFormUiState> = _uiStateC.asStateFlow()
        val loaded = campsiteRepository.get(campsiteId)
        var originalCampsite: Campsite? = null
        originalCampsite = loaded
        // Load in original campsite
        if (loaded != null) {
            _uiStateC.update {
                it.copy(
                    isLoading = false,
                    loadError = false,
                    name = loaded.name,
                    description = loaded.description,
                    location = loaded.location,
                    capacity = loaded.capacity.toString(),
                    tags = loaded.tags,
                    latitude = loaded.latitude,
                    longitude = loaded.longitude,
                    availability = loaded.availability
                )
            }
        }
        val stateC = _uiStateC.value

        // Update campsite
        update(_uiStateC)

        val baseCampsite = originalCampsite ?: Campsite(
            id = campsiteId,
            ownerId = currentOwnerId
        )

        val campsite = baseCampsite.copy(
            name = stateC.name.trim(),
            description = stateC.description.trim(),
            location = stateC.location.trim(),
            capacity = stateC.capacity.toInt(),
            tags = stateC.tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            latitude = stateC.latitude,
            longitude = stateC.longitude,
            availability = uiStateC.value.availability,
        )
        _uiStateC.update { it.copy(isSaving = true, saveError = false) }
        viewModelScope.launch {
            campsiteRepository.update(campsite)
            _uiStateC.update {
                it.copy(
                    isSaving = false,
                    isSaved = true,
                    errors = emptyList(),
                    saveError = false
                )
            }
        }
    }

    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */
    val uiState: StateFlow<OwnerWaitlistUiState> =
        combine(
            lotteryRepository.listLotteries(ownerId = currentOwnerId),
            campsiteRepository.observeAll(),
            userRepository.observeAll(),
            pendingRemovalId,
        ) { lotteries, campsites, users, pending ->
            val namesById = users.associate { it.id to it.name }
            OwnerWaitlistUiState(
                lotteries = lotteries.map { lottery ->
                    LotteryWaitlist(
                        lotteryId = lottery.id,
                        campsiteNames = campsites
                            .filter { it.id in lottery.campsiteIds }
                            .joinToString(", ") { it.name }
                            .ifBlank { "Unnamed campsite" },
                        entrantNames = lottery.waitingList.entrantList.map { id ->
                            namesById[id].takeUnless { it.isNullOrBlank() } ?: id
                        },

                    )
                },
                isLoading = false,
                lotteryPendingRemoval = pending
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OwnerWaitlistUiState(),
        )




    companion object {
        /**
         * Builds this ViewModel with the shared repositories and the current
         * user's ID from the AppContainer.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                OwnerWaitlistViewModel(
                    app.container.lotteryRepository,
                    app.container.campsiteRepository,
                    app.container.userRepository,
                    app.container.currentUserId,
                    app.container.reservationRepository
                )
            }
        }
    }
}
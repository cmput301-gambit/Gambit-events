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
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryError
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.WaitingList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

// TODO: UPDATE COMMENTS

/**
 * UI state for the campsite registration/edit form.
 *
 * @property isLoading true while loading existing campsite data for editing
 * @property loadError true if loading existing campsite data failed (null or exception)
 * @property isSaving true while the campsite is being saved to the repository
 * @property saveError true if saving to the repository failed with an exception
 * @property isSaved true if the campsite was successfully saved
 * @property name display name of the campsite
 * @property description description / amenities of the campsite
 * @property location textual location of the campsite
 * @property capacity maximum occupancy entered as a string
 * @property tags list of tags associated with the campsite
 * @property latitude optional latitude map coordinate
 * @property longitude optional longitude map coordinate
 * @property availability optional reservation availability date range
 * @property errors list of validation errors encountered during save attempts
 */

data class LotteryFormUiState(
    val isLoading: Boolean = false,
    val loadError: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val isSaved: Boolean = false,
    val campsiteIds: MutableList<String> = mutableListOf(),
    val registrationPeriod: DateRange = DateRange(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5)),
    val reservationPeriod: DateRange = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10)),
    val winnerAmount: String = "",
    val waitlistCapacity: String = "",
    val lotteryStage: String = "",
    val errors: List<LotteryError> = emptyList()
)

/**
 * Holds the state for the lottery campsite selection and details editing form.
 *
 * **Purpose:** allows campsite owners to register a new lottery (US 02.04.02, US 02.04.03, US 02.04.07)
 * or update details about an existing campsite US (02.04.06).
 *
 * **Design rationale:** MVVM. Receives its repositories and current user ID from the
 * [com.campsitereservationapp.data.AppContainer] through [Factory], handling validation,
 * persisting new or updated lotteries with stable ID generation for retry safety, preserving
 * unedited fields via copy, handling async load and save errors safely.
 *
 * **Outstanding issues:**
 *
 * @property lotteryRepository the shared repository campsites are added/updated in
 * @property currentUserId ID of the owner registering or updating the campsite
 * @property editingLotteryId optional ID of an existing lottery being edited, or null for creation
 */
class LotteryFormViewModel(
    private val lotteryRepository: LotteryRepository,
    private val campsiteRepository: CampsiteRepository,
    private val currentUserId: String,
    private val editingLotteryId: String? = null,
) : ViewModel() {
    private val campsiteIds = mutableListOf<String>()

    private val _uiState = MutableStateFlow(LotteryFormUiState(isLoading = editingLotteryId != null))

    /**
     * Observable stream of the current form UI state.
     */
    val uiState: StateFlow<LotteryFormUiState> = _uiState.asStateFlow()

    private var originalLottery: Lottery? = null

    // Stable ID generation across save retries
    private val newLotteryId by lazy { lotteryRepository.newId() }

    init {
        if (editingLotteryId != null) {
            viewModelScope.launch {
                try {

                    val loaded = lotteryRepository.get(editingLotteryId)
                    originalLottery = loaded
                    if (loaded != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadError = false,
                                campsiteIds = loaded.campsiteIds,
                                registrationPeriod = loaded.registrationPeriod,
                                reservationPeriod = loaded.reservationPeriod,
                                winnerAmount = loaded.winnerAmount.toString(),
                                waitlistCapacity = loaded.waitingList.entrantCap.toString(),
                                lotteryStage = loaded.stage.toString()
                            )
                        }
                        Log.d("viewModel", "Loaded Lottery: ${loaded.toString()}")
                    } else {
                        _uiState.update { it.copy(isLoading = false, loadError = true) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, loadError = true) }
                }
            }
        }
    }

    /**
     * Returns a list of campsites based on the owner's id [currentUserId].
     *
     * */
    suspend fun getCampsites(): List<Campsite> {
        return campsiteRepository.getByOwner(currentUserId)
    }

    /**
     * Updates the lottery's assigned winner amount from input in the form state.
     *
     * @param winnerAmount new winner amount as a string
     */
    fun onWinnerAmountChange(winnerAmount: String) {
        _uiState.update { it.copy(winnerAmount = winnerAmount) }
    }

    /**
     * Updates the lottery's assigned waitlist capacity from input in the form state.
     *
     * @param waitlistCapacity new waitlist capacity as a string
     */
    fun onWaitlistCapacityChange(waitlistCapacity: String) {
        _uiState.update { it.copy(waitlistCapacity = waitlistCapacity) }
    }

    /**
     * Updates the lottery's assigned stage from input in the form state.
     *
     * @param lotteryStage new lottery stage as a string.
     */
    fun onStageChange(lotteryStage: String) {
        _uiState.update { it.copy(lotteryStage = lotteryStage)}
    }

    /**
     * @return Returns the registration date range as a [DateRange]
     *
     */
    fun getRegistrationRange(): DateRange {
        return uiState.value.registrationPeriod
    }

    /**
     * @return Returns the reservation date range as a [DateRange]
     *
     */
    fun getReservationRange(): DateRange {
        return uiState.value.reservationPeriod
    }

    /**
     * @return Returns the waitinglist capacity as it should be displayed on the form. In this case, whenever
     * the capacity is -1 or "" it will return "". This is because during saving anything other than
     * a positive integer will throw an error while a blank value should be able to pass and just
     * set the entrant cap to -1 (no cap).
     *
     */
    fun getWaitinglistCapacity(): String {
        if (uiState.value.waitlistCapacity=="-1" || uiState.value.waitlistCapacity=="") {
            return ""
        }
        return uiState.value.waitlistCapacity
    }


    /**
     * Creates a copy of the list of campsiteIds and tries to remove the passed [campsiteId].
     *
     *  @param campsiteId A campsite id string of the newly removed campsite.
     *  @return Returns true if successfully removed, false otherwise.
     * */
    fun onCampsitesChangeRemove(campsiteId: String): Boolean {
        val campsiteIdsCopy = uiState.value.campsiteIds.toMutableList()
        var bool = campsiteIdsCopy.remove(campsiteId)
        _uiState.update { it.copy(campsiteIds=campsiteIdsCopy)}
        return bool
    }

    /**
     * Creates a copy of the list of campsiteIds and tries to add the passed [campsiteId].
     *
     *  @param campsiteId A campsite id string of the newly added campsite.
     *  @return Returns true
     * */
    fun onCampsitesChangeAdd(campsiteId: String): Boolean {
        val campsiteIdsCopy = uiState.value.campsiteIds.toMutableList()
        var bool = campsiteIdsCopy.add(campsiteId)
        _uiState.update { it.copy(campsiteIds=campsiteIdsCopy)}
        return bool
    }

    /**
     *
     * @return Returns the mutable string list of campsiteIds.
     * */
    fun returnCampsiteIds(): MutableList<String> {
        return uiState.value.campsiteIds
    }
    
    /**
     * Updates the lottery's registration period when called.
     *
     * @param registrationPeriod new DateRange to update the state's registration period.
     */
    fun onRegistrationPeriodChange(registrationPeriod: DateRange) {
        _uiState.update { it.copy(registrationPeriod = registrationPeriod) }
    }

    /**
     * Updates the lottery's reservation period when called.
     *
     * @param reservationPeriod new DateRange to update the state's reservation period.
     */
    fun onReservationPeriodChange(reservationPeriod: DateRange) {
        _uiState.update { it.copy(reservationPeriod = reservationPeriod) }
    }


    /**
     * Returns whether the passed campsiteId is associated with a lottery. We can do this by just
     * getting the very first lottery listed with the campsiteId as an argument.
     * @param campsiteId campsiteId of the campsite to determine if it is already in a lottery.
     * @return Returns true if it is already in a lottery, false otherwise.
     */
    suspend fun campsiteHasLottery(campsiteId: String): Boolean {
        val lottery: Lottery? = lotteryRepository.listLotteries(campsiteId=campsiteId).first().firstOrNull()
        return (lottery != null)
    }

   

    /**
     * Validates form inputs, trims text values, parses capacity, and saves the campsite
     * to the repository (adding a new one or updating an existing one, preserving unedited fields
     * and maintaining stable IDs for retry safety). Prevents saving if loading is active or
     * encountered a load error. Promotes the user to a campsite owner upon new registration.
     * Sets [LotteryFormUiState.isSaving] during the operation and [LotteryFormUiState.isSaved]
     * upon success, or [LotteryFormUiState.saveError] on exception.
     */
    fun save() {
        Log.d("viewModel", "Saving Model")
        val state = _uiState.value
        if (state.isLoading || state.loadError || state.isSaving || state.isSaved) return

        val parsedWinnerAmount = state.winnerAmount.toIntOrNull()

        var parsedWaitlistCapacity = state.waitlistCapacity.toIntOrNull()
        // This allows default argument (-1) and an empty string to pass while other characters would
        // be set to null which would throw an error and not allow the user to save until cleared.
        if (state.waitlistCapacity == "") {
            parsedWaitlistCapacity = -1
        }

        val baseLottery = originalLottery ?: Lottery(
            id = newLotteryId,
            ownerId = currentUserId
        )

        var convertedStage = LotteryStatus.DRAFT
        if (state.lotteryStage == "OPEN") {
            convertedStage = LotteryStatus.OPEN
        }
        Log.d("viewModel", "state.campsiteIds: ${state.campsiteIds}")
        val lottery = baseLottery.copy(
            campsiteIds = state.campsiteIds,
            registrationPeriod = state.registrationPeriod,
            reservationPeriod = state.reservationPeriod,
            winnerAmount = parsedWinnerAmount ?: 0,
            waitingList = WaitingList(entrantCap = parsedWaitlistCapacity ?: -2),
            stage = convertedStage
        )

        val errors = lottery.validate().toMutableList()
        if (parsedWinnerAmount == null) {
            errors.remove(LotteryError.WINNER_AMOUNT_LESS_THAN_ONE)
            errors.add(LotteryError.CAPACITY_NOT_A_NUMBER)
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors.distinct()) }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveError = false) }
        viewModelScope.launch {
            try {
                if (editingLotteryId != null) {
                    lotteryRepository.update(lottery)
                } else {
                    if (lotteryRepository.get(lottery.id) != null) {
                        lotteryRepository.update(lottery)
                    } else {
                        lotteryRepository.add(lottery)
                    }
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true, errors = emptyList(), saveError = false) }
                Log.d("viewModel", "campsites after update: ${uiState.value.campsiteIds}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }

    companion object {
        /**
         * Provides a ViewModel factory for [CampsiteFormViewModel], supporting both
         * registration mode and editing mode for an existing campsite.
         */
        fun provideFactory(editingLotteryId: String? = null) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                LotteryFormViewModel(
                    lotteryRepository = app.container.lotteryRepository,
                    campsiteRepository = app.container.campsiteRepository,
                    currentUserId = app.container.currentUserId,
                    editingLotteryId = editingLotteryId
                )
            }
        }

        /** Default factory for creating new campsites. */
        val Factory = provideFactory(null)
    }
}

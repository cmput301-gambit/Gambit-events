package com.campsitereservationapp.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

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
    val errors: List<LotteryError> = emptyList()
)

/**
 * Holds the state for the campsite registration and editing form.
 *
 * **Purpose:** allows campsite owners to register a new campsite (US 02.01.01, US 02.01.02)
 * or update details about an existing campsite (US 02.01.03 / #20).
 *
 * **Design rationale:** MVVM. Receives its repositories and current user ID from the
 * [com.campsitereservationapp.data.AppContainer] through [Factory], handling validation,
 * persisting new or updated campsites with stable ID generation for retry safety, preserving
 * unedited fields via copy, handling async load and save errors safely, and promoting users
 * to campsite owners upon registration.
 *
 * **Outstanding issues:** image upload and map picking not yet integrated.
 *
 * @property lotteryRepository the shared repository campsites are added/updated in
 * @property currentUserId ID of the owner registering or updating the campsite
 * @property editingLotteryId optional ID of an existing lottery being edited, or null for creation
 */
class LotteryFormViewModel(
    private val lotteryRepository: LotteryRepository,
    private val currentUserId: String,
    private val editingLotteryId: String? = null,
) : ViewModel() {

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
                            )
                        }
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
     * Updates the lottery's assigned campsites from input in the form state.
     *
     * @param campsiteIds new mutable list of campsite ids
     */
    fun onCampsiteIdsChange(campsiteIds: MutableList<String>) {
        _uiState.update { it.copy(campsiteIds = campsiteIds) }
    }

    /**
     * Updates the lottery's assigned campsites from input in the form state.
     *
     * @param campsiteIds new mutable list of campsite ids
     */
    fun onWinnerAmountChange(winnerAmount: String) {
        _uiState.update { it.copy(winnerAmount = winnerAmount) }
    }
    
    /**
     * Updates the lottery's assigned campsites from input in the form state.
     *
     * @param campsiteIds new mutable list of campsite ids
     */
    fun onRegistrationPeriodChange(registrationPeriod: DateRange) {
        _uiState.update { it.copy(registrationPeriod = registrationPeriod) }
    }

    /**
     * Updates the lottery's assigned campsites from input in the form state.
     *
     * @param campsiteIds new mutable list of campsite ids
     */
    fun onReservationPeriodChange(reservationPeriod: DateRange) {
        _uiState.update { it.copy(reservationPeriod = reservationPeriod) }
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
        val state = _uiState.value
        if (state.isLoading || state.loadError || state.isSaving || state.isSaved) return

        val parsedWinnerAmount = state.winnerAmount.toIntOrNull()
        val baseLottery = originalLottery ?: Lottery(
            id = newLotteryId,
            ownerId = currentUserId
        )

        val lottery = baseLottery.copy(
            campsiteIds = state.campsiteIds,
            registrationPeriod = state.registrationPeriod,
            reservationPeriod = state.reservationPeriod,
            winnerAmount = parsedWinnerAmount ?: 0,
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
                    currentUserId = app.container.currentUserId,
                    editingLotteryId = editingLotteryId
                )
            }
        }

        /** Default factory for creating new campsites. */
        val Factory = provideFactory(null)
    }
}

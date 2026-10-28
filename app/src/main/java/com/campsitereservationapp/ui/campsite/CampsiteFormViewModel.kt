package com.campsitereservationapp.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * UI state for the campsite registration form.
 *
 * @property name display name of the campsite
 * @property description description / amenities of the campsite
 * @property location textual location of the campsite
 * @property capacity maximum occupancy entered as a string
 * @property tags list of tags associated with the campsite
 * @property latitude optional latitude map coordinate
 * @property longitude optional longitude map coordinate
 * @property errors list of validation errors encountered during save attempts
 * @property isSaved true if the campsite was successfully registered and saved
 */
data class CampsiteFormUiState(
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val capacity: String = "",
    val tags: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val errors: List<CampsiteError> = emptyList()
)

/**
 * Holds the state for the campsite registration form.
 *
 * **Purpose:** allows campsite owners to register a new campsite and provide its details
 * (US 02.01.01, 02.01.02).
 *
 * **Design rationale:** MVVM. Receives its repository and current user ID from the
 * [com.campsitereservationapp.data.AppContainer] through [Factory], handling validation
 * and persisting new campsites to the repository.
 *
 * **Outstanding issues:** image upload and map picking not yet integrated.
 *
 * @property campsiteRepository the shared repository campsites are added to
 * @property currentUserId ID of the owner registering the campsite
 */
class CampsiteFormViewModel(
    private val campsiteRepository: CampsiteRepository,
    private val currentUserId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CampsiteFormUiState())

    /**
     * Observable stream of the current form UI state.
     */
    val uiState: StateFlow<CampsiteFormUiState> = _uiState.asStateFlow()

    /**
     * Updates the campsite name input in the form state.
     *
     * @param name new campsite name
     */
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    /**
     * Updates the campsite description/amenities input in the form state.
     *
     * @param description new campsite description
     */
    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    /**
     * Updates the campsite location input in the form state.
     *
     * @param location new campsite location
     */
    fun onLocationChange(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    /**
     * Updates the campsite capacity input in the form state.
     *
     * @param capacity new capacity string
     */
    fun onCapacityChange(capacity: String) {
        _uiState.update { it.copy(capacity = capacity) }
    }

    /**
     * Updates the list of tags associated with the campsite.
     *
     * @param tags new list of tags
     */
    fun onTagsChange(tags: List<String>) {
        _uiState.update { it.copy(tags = tags) }
    }

    /**
     * Updates the map location (latitude and longitude) of the campsite.
     *
     * @param lat latitude coordinate, or null
     * @param lng longitude coordinate, or null
     */
    fun onMapLocationChange(lat: Double?, lng: Double?) {
        _uiState.update { it.copy(latitude = lat, longitude = lng) }
    }

    /**
     * Validates form inputs, trims text values, parses capacity, and saves the new campsite
     * to the repository if valid. Sets [CampsiteFormUiState.isSaving] during the operation
     * and [CampsiteFormUiState.isSaved] upon success. Populates validation errors otherwise.
     */
    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return

        val parsedCapacity = state.capacity.toIntOrNull()
        val campsite = Campsite(
            id = campsiteRepository.newId(),
            ownerId = currentUserId,
            name = state.name.trim(),
            description = state.description.trim(),
            location = state.location.trim(),
            capacity = parsedCapacity ?: 0,
            tags = state.tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            latitude = state.latitude,
            longitude = state.longitude,
        )

        val errors = campsite.validate().toMutableList()
        if (parsedCapacity == null) {
            errors.remove(CampsiteError.NON_POSITIVE_CAPACITY)
            errors.add(CampsiteError.CAPACITY_NOT_A_NUMBER)
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors.distinct()) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                campsiteRepository.add(campsite)
                _uiState.update { it.copy(isSaving = false, isSaved = true, errors = emptyList()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                // TODO: expose a save-failed error to the UI
            }
        }
    }

    companion object {
        /**
         * ViewModel factory for [CampsiteFormViewModel], injecting repository and current user ID
         * from [CampsiteApp] container.
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                CampsiteFormViewModel(
                    campsiteRepository = app.container.campsiteRepository,
                    currentUserId = app.container.currentUserId
                )
            }
        }
    }
}

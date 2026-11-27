package com.campsitereservationapp.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.LotteryStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Item representation for an owner's campsite, including active lottery status.
 *
 * @property campsite the campsite entity
 * @property hasActiveLottery true if the campsite is currently part of an active/open lottery
 * @property posterImage the campsite's poster as Base64 JPEG text, or null if it has none
 */
data class OwnerCampsiteItem(
    val campsite: Campsite,
    val hasActiveLottery: Boolean,
    val posterImage: String? = null,
)

/**
 * UI state for the owner's campsite list screen.
 *
 * @property campsites list of campsites registered by the current owner with active lottery flags
 * @property isLoading true while loading the list
 */
data class OwnerCampsiteListUiState(
    val campsites: List<OwnerCampsiteItem> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for the owner's campsite list screen (US 02.03.01).
 *
 * **Purpose:** connects to [CampsiteRepository.observeByOwner], [LotteryRepository.observeAll]
 * and [ImageRepository.observeByUploader] (the owner's campsite posters)
 * to display all campsites registered by the currently logged-in owner and determine active lottery status.
 *
 * **Design rationale:** MVVM. Subscribes to live owner campsite and lottery data so the list
 * updates automatically whenever campsites or lotteries are created, edited, or drawn.
 *
 * **Outstanding issues:** none.
 *
 * @property campsiteRepository the shared repository providing campsite data
 * @property lotteryRepository the shared repository providing lottery data
 * @property imageRepository the shared repository providing campsite posters
 * @property currentUserId ID of the logged-in campsite owner
 */
class OwnerCampsiteListViewModel(
    private val campsiteRepository: CampsiteRepository,
    private val lotteryRepository: LotteryRepository,
    private val imageRepository: ImageRepository,
    private val currentUserId: String
) : ViewModel() {

    /**
     * Observable stream emitting campsites registered by [currentUserId] with active lottery status.
     */
    val uiState: StateFlow<OwnerCampsiteListUiState> = combine(
        campsiteRepository.observeByOwner(currentUserId),
        lotteryRepository.observeAll(),
        imageRepository.observeByUploader(currentUserId),
    ) { campsites, lotteries, images ->
        val posters = images.associate { it.id to it.data }
        val activeCampsiteIds = lotteries
            .filter { it.stage == LotteryStatus.OPEN }
            .flatMap { it.campsiteIds }
            .toSet()

        val items = campsites.map { campsite ->
            OwnerCampsiteItem(
                campsite = campsite,
                hasActiveLottery = campsite.id in activeCampsiteIds,
                posterImage = campsite.posterImageId?.let { posters[it] },
            )
        }
        OwnerCampsiteListUiState(campsites = items, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OwnerCampsiteListUiState()
    )

    companion object {
        /**
         * ViewModel factory for [OwnerCampsiteListViewModel] initialized with dependencies from [CampsiteApp.container].
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                OwnerCampsiteListViewModel(
                    app.container.campsiteRepository,
                    app.container.lotteryRepository,
                    app.container.imageRepository,
                    app.container.currentUserId
                )
            }
        }
    }
}

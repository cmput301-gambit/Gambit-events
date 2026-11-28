package com.campsitereservationapp.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * One rating as the admin sees it: the rating plus the names of the campsite
 * and the person who left it.
 *
 * @property rating the rating itself
 * @property campsiteName the rated campsite's name, or "Deleted campsite" if it no longer exists
 * @property raterName the rater's name; null if they haven't set one or their profile is gone
 */
data class AdminRatingRow(
    val rating: Rating,
    val campsiteName: String,
    val raterName: String? = null,
)

/**
 * UI state for the admin rating list screen.
 *
 * @property ratings every rating, grouped by campsite name
 * @property isLoading true until the first list of ratings arrives
 * @property ratingPendingRemoval the rating the admin tapped Remove on, waiting
 * for them to confirm; null when no popup is showing
 * @property errorMessage shown when a removal failed, for example with no network
 */
data class AdminRatingListUiState(
    val ratings: List<AdminRatingRow> = emptyList(),
    val isLoading: Boolean = true,
    val ratingPendingRemoval: AdminRatingRow? = null,
    val errorMessage: String? = null,
)

/**
 * ViewModel for the admin rating list screen (US 03.09.01).
 *
 * **Purpose:** lets an admin see every campsite rating and remove ones that
 * break the app's rules, after confirming in a popup.
 *
 * **Design rationale:** MVVM, following [AdminCampsiteListViewModel].
 * Combines the live ratings, campsites and users streams so each rating shows
 * its campsite's name and who left it (name and user ID, so the admin can
 * spot one person rating many campsites unfairly), and the list updates as
 * soon as anything changes. Removing
 * a rating needs no cascade (nothing points at a rating), so it calls
 * [RatingRepository.delete] directly. Campsite averages update by themselves
 * because the campsite screens observe ratings live.
 *
 * **Outstanding issues:** the admin screens don't yet check that the current
 * user is actually an admin. Written reviews (US 01.13.02) are part of each
 * [Rating], so they show here too and are removed along with their rating.
 *
 * @property ratingRepository the shared repository providing ratings
 * @property campsiteRepository the shared repository providing campsite names
 * @property userRepository the shared repository providing raters' names
 */
class AdminRatingListViewModel(
    private val ratingRepository: RatingRepository,
    private val campsiteRepository: CampsiteRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val pendingRemoval = MutableStateFlow<AdminRatingRow?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    /** Live screen state: every rating with its campsite name, plus any popup or error. */
    val uiState: StateFlow<AdminRatingListUiState> =
        combine(
            ratingRepository.observeAll(),
            campsiteRepository.observeAll(),
            userRepository.observeAll(),
            pendingRemoval,
            errorMessage,
        ) { ratings, campsites, users, pending, error ->
            val campsiteNames = campsites.associate { it.id to it.name }
            val userNames = users.associate { it.id to it.name.ifBlank { null } }
            AdminRatingListUiState(
                ratings = ratings
                    .map {
                        AdminRatingRow(
                            rating = it,
                            campsiteName = campsiteNames[it.campsiteId] ?: DELETED_CAMPSITE,
                            raterName = userNames[it.userId],
                        )
                    }
                    .sortedWith(compareBy({ it.campsiteName.lowercase() }, { it.rating.score })),
                isLoading = false,
                ratingPendingRemoval = pending,
                errorMessage = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminRatingListUiState(),
        )

    /**
     * Called when the admin taps Remove. Opens the popup instead of deleting straight away.
     *
     * @param row the rating the admin wants to remove
     */
    fun requestRemoval(row: AdminRatingRow) {
        pendingRemoval.value = row
    }

    /** Called when the admin taps Cancel or closes the popup. Nothing is removed. */
    fun cancelRemoval() {
        pendingRemoval.value = null
    }

    /** Hides the error message. */
    fun dismissError() {
        errorMessage.value = null
    }

    /**
     * Called when the admin confirms. Deletes the rating waiting in the popup
     * and closes it. Does nothing if no removal is pending.
     */
    fun confirmRemoval() {
        val row = pendingRemoval.value ?: return
        pendingRemoval.value = null
        viewModelScope.launch {
            try {
                ratingRepository.delete(row.rating.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove rating ${row.rating.id}", e)
                errorMessage.value = "Couldn't remove the rating. Check your connection and try again."
            }
        }
    }

    companion object {
        private const val TAG = "AdminRatingList"

        /** Shown instead of a name when a rating's campsite no longer exists. */
        const val DELETED_CAMPSITE = "Deleted campsite"

        /**
         * Factory providing [AdminRatingListViewModel] initialized with [CampsiteApp.container].
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                AdminRatingListViewModel(
                    app.container.ratingRepository,
                    app.container.campsiteRepository,
                    app.container.userRepository,
                )
            }
        }
    }
}

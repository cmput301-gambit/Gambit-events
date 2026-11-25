package com.campsitereservationapp.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.User
import com.campsitereservationapp.service.CascadeDeleter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * What the admin is about to remove, shown in the "Are you sure?" popup.
 */
sealed interface ProfileRemoval {
    /** The user whose data will be removed. */
    val user: User

    /** Delete the whole profile and everything it owns (US 03.02.01). */
    data class WholeProfile(override val user: User) : ProfileRemoval

    /** Take away the owner role and delete their campsites and lotteries (US 03.07.01). */
    data class OwnerRole(override val user: User) : ProfileRemoval
}

/**
 * UI state for the admin profile list screen.
 *
 * @property users every profile in the app
 * @property currentUserId the admin's own ID, so the screen can hide Remove on their own card
 * @property isLoading true until the first list of users arrives
 * @property pendingRemoval what the admin tapped Remove on, waiting for them to
 * confirm; null when no popup is showing
 * @property errorMessage shown when a removal failed, for example with no network
 */
data class AdminProfileListUiState(
    val users: List<User> = emptyList(),
    val currentUserId: String = "",
    val isLoading: Boolean = true,
    val pendingRemoval: ProfileRemoval? = null,
    val errorMessage: String? = null,
)

/**
 * ViewModel for the admin profile list screen (US 03.02.01, 03.07.01).
 *
 * **Purpose:** lets an admin see every profile and either remove a whole
 * profile or remove a rule-breaking campsite owner, after confirming in a popup.
 *
 * **Design rationale:** MVVM, following [AdminCampsiteListViewModel]: the
 * pending removal lives here so the confirm and cancel steps can be unit
 * tested. The actual cleanup is done by [CascadeDeleter], the same code an
 * entrant uses to delete their own profile (US 01.08.01). Admins can't
 * remove themselves here, so they can't lock themselves out by accident.
 *
 * **Outstanding issues:** the admin screens don't yet check that the current
 * user is actually an admin. Browsing profiles (US 03.05.01) may want a
 * nicer card layout on top of this list.
 *
 * @property userRepository the shared repository providing profiles
 * @property cascadeDeleter removes a profile or owner along with their data
 * @property currentUserId the ID of the admin using the screen
 */
class AdminProfileListViewModel(
    private val userRepository: UserRepository,
    private val cascadeDeleter: CascadeDeleter,
    private val currentUserId: String,
) : ViewModel() {

    private val pendingRemoval = MutableStateFlow<ProfileRemoval?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    /** Live screen state: every profile, plus any popup or error. */
    val uiState: StateFlow<AdminProfileListUiState> =
        combine(userRepository.observeAll(), pendingRemoval, errorMessage) { users, pending, error ->
            AdminProfileListUiState(
                users = users.sortedBy { it.name.ifBlank { it.id }.lowercase() },
                currentUserId = currentUserId,
                isLoading = false,
                pendingRemoval = pending,
                errorMessage = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdminProfileListUiState(currentUserId = currentUserId),
        )

    /**
     * Called when the admin taps "Remove profile". Opens the popup instead
     * of deleting straight away. Ignored for the admin's own profile.
     *
     * @param user the profile to remove
     */
    fun requestProfileRemoval(user: User) {
        if (user.id == currentUserId) return
        pendingRemoval.value = ProfileRemoval.WholeProfile(user)
    }

    /**
     * Called when the admin taps "Remove as owner". Opens the popup instead
     * of deleting straight away. Ignored for non-owners and the admin's own profile.
     *
     * @param user the campsite owner to remove
     */
    fun requestOwnerRemoval(user: User) {
        if (user.id == currentUserId || !user.isOwner) return
        pendingRemoval.value = ProfileRemoval.OwnerRole(user)
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
     * Called when the admin confirms. Runs the removal waiting in the popup
     * and closes it. Does nothing if no removal is pending.
     */
    fun confirmRemoval() {
        val removal = pendingRemoval.value ?: return
        pendingRemoval.value = null
        viewModelScope.launch {
            try {
                when (removal) {
                    is ProfileRemoval.WholeProfile -> cascadeDeleter.deleteProfile(removal.user.id)
                    is ProfileRemoval.OwnerRole -> cascadeDeleter.removeOwner(removal.user.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove ${removal.user.id}", e)
                errorMessage.value = "Couldn't finish removing ${displayName(removal.user)}. " +
                    "Check your connection and try again."
            }
        }
    }

    companion object {
        private const val TAG = "AdminProfileList"

        /**
         * The name to show for a user, falling back to their ID when they
         * haven't filled in a name yet.
         *
         * @param user the user to name
         * @return their name, or their ID if the name is blank
         */
        fun displayName(user: User): String = user.name.ifBlank { user.id }

        /**
         * Factory providing [AdminProfileListViewModel] initialized with [CampsiteApp.container].
         */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CampsiteApp
                AdminProfileListViewModel(
                    app.container.userRepository,
                    app.container.cascadeDeleter,
                    app.container.currentUserId,
                )
            }
        }
    }
}

package com.campsitereservationapp.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campsitereservationapp.CampsiteApp
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.User
import com.campsitereservationapp.service.CascadeDeleter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Everything the entrant home screen needs to properly address the current user.
 *
 * The screen reads only this class. It never talks to a repository directly.
 *
 * @property currentUser the user whose settings are shown
 * @property isUpdateUserOpen true while the edit profile popup is showing
 * @property isDeleteConfirmOpen true while the "Delete your account?" popup is showing
 * @property accountDeleted true once the profile has been deleted, so the screen can leave
 * @property deleteError shown when deleting the profile failed, for example with no network
 */
data class EntrantSettingsState(
    val currentUser: User = User(),
    val currentProfileImage: String? = null,
    val isLoading: Boolean = false,
    val isUpdateUserOpen: Boolean = false,
    val isDeleteConfirmOpen: Boolean = false,
    val accountDeleted: Boolean = false,
    val deleteError: String? = null,
    val updateError: String? = null,
)

/**
 * Holds the state for the entrant home screen.
 *
 * **Purpose:** provides an entry point for many subsequent screens covering most of the
 * entrant User Stories.
 *
 * **Design rationale:** MVVM. Receives its repository and user ID from the
 * [com.campsitereservationapp.data.AppContainer] through [Factory], and
 * subscribes to the repository's live stream, so the screen adapts correctly to the current user.
 *
 * Deleting the account (US 01.08.01) asks for confirmation first, then hands
 * the cleanup to [CascadeDeleter], the same code admins use (US 03.02.01).
 *
 * **Outstanding issues:** Currently the default screen, when it should be determined by the
 * current users roles. Because a profile is tied to the device ID, opening the
 * app again after deleting starts a fresh, empty profile. The screen will not update if the displayed image is removed by the admin.
 *
 * @property userRepository The shared repository users come from
 * @property currentUserId The id of the current user as a string
 * @property cascadeDeleter deletes the profile and everything that belongs to it;
 * null in tests that don't delete, in which case Delete Account does nothing
 */
class EntrantSettingsViewModel(
    private val userRepository: UserRepository,
    private val currentUserId: String,
    private val cascadeDeleter: CascadeDeleter? = null,
    private val imageRepository: ImageRepository,
) : ViewModel() {

    //Name to be displayed on screen
//    private val currentUser : MutableStateFlow<User> = MutableStateFlow(
//        User()
//    )

//    private val isUpdateUserOpen : MutableStateFlow<Boolean> = MutableStateFlow(
//        false
//    )

    // Account deletion steps (US 01.08.01).
//    private val isDeleteConfirmOpen = MutableStateFlow(false)
//    private val accountDeleted = MutableStateFlow(false)
//    private val deleteError = MutableStateFlow<String?>(null)

    private var isProfileImageUpdated: Boolean = false
    private var newProfileImageId: String? = null
    //Updating the user based on values from the userRepository. When the first time launch screen
    //is done, this should always return a real name instead of a blank or the device ID.
    init {
        viewModelScope.launch {
            val user: User = userRepository.getOrCreate(currentUserId)
            val img: String? = loadProfilePicture(user.profileImageId)
            _uiState.update { it.copy(currentUser = user, isLoading = false, currentProfileImage = img) }
        }
    }

    val _uiState = MutableStateFlow(EntrantSettingsState( isLoading = true))

    /**
     * The current screen state. The screen collects this and redraws
     * every time it changes.
     */
    val uiState: StateFlow<EntrantSettingsState> = _uiState.asStateFlow()

//        combine(
//            userRepository.observe(currentUserId),
//            isUpdateUserOpen,
//            isDeleteConfirmOpen,
//            accountDeleted,
//            deleteError,
//        ) { newUser, updateOpen, deleteOpen, deleted, error ->
//            EntrantSettingsState(
//                currentUser = newUser ?: User(),
//                isUpdateUserOpen = updateOpen,
//                isDeleteConfirmOpen = deleteOpen,
//                accountDeleted = deleted,
//                deleteError = error,
//            )
//        }
//            .stateIn(
//                scope = viewModelScope,
//                started = SharingStarted.WhileSubscribed(5_000),
//                initialValue = EntrantSettingsState(),
//            )

    fun updateUserDetails(
            newName: String? = null,
            newPhone: String? = null,
            newEmail: String? = null,
            newProfileImageId: String? = null,
            newNotificationsEnabled: Boolean? = null,
            ){
        viewModelScope.launch {
            try{
                val current = userRepository.getOrCreate(currentUserId)

                userRepository.update(
                    current.copy(
                        name = newName ?: current.name,
                        phone = newPhone ?: current.phone,
                        email = newEmail ?: current.email,
                        profileImageId = newProfileImageId ?: current.profileImageId,
                        notificationsEnabled = newNotificationsEnabled ?: current.notificationsEnabled
                    )
                )

                _uiState.update { it.copy(currentUser = userRepository.getOrCreate(currentUserId)) }

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("EntrantSettings", "Failed to update profile $currentUserId")
                    _uiState.update { it.copy(updateError = "Couldn't update your account. Check your connection and try again.") }
                    //deleteError.value = "Couldn't delete your account. Check your connection and try again."
                }
            }
        }

    /**
     * Shows a newly picked poster in the form. It's uploaded when the form is saved.
     *
     * @param jpegBytes the shrunk JPEG picture, from
     *   [com.campsitereservationapp.ui.components.loadJpegBytes]
     */
//    fun onProfilePicturePicked(jpegBytes: ByteArray) {
//        prof = true
//        newProfileImageId = imageRepository.newId()
//        _uiState.update { it.copy(posterImage = Image.encode(jpegBytes), saveError = false) }
//    }

    fun toggleReceiveNotifications(){
        viewModelScope.launch {
            updateUserDetails(
                newNotificationsEnabled = !userRepository.getOrCreate(currentUserId).notificationsEnabled
            )
        }
    }

    fun toggleUpdateUserOpen(){
        _uiState.update { it.copy(isUpdateUserOpen = !_uiState.value.isUpdateUserOpen) }
    }

    /**
     * Called when the user taps Delete Account. Opens the confirmation popup
     * instead of deleting straight away (US 01.08.01).
     */
    fun requestDeleteAccount() {
        _uiState.update {
            it.copy(
                deleteError = null,
                isDeleteConfirmOpen = true
            )
        }
//        deleteError.value = null
//        isDeleteConfirmOpen.value = true
    }

    /** Called when the user taps Cancel or closes the popup. Nothing is deleted. */
    fun cancelDeleteAccount() {
        //isDeleteConfirmOpen.value = false
        _uiState.update { it.copy(isDeleteConfirmOpen = false) }
    }

    /** Hides the "couldn't delete" message. */
    fun dismissDeleteError() {
        //deleteError.value = null
        _uiState.update { it.copy(deleteError = null) }
    }

    /**
     * Called when the user confirms. Deletes their profile along with their
     * waiting list spots, reservations, messages, notifications and ratings
     * (and any campsites they own), then marks the account as deleted so the
     * screen can leave.
     */
    fun confirmDeleteAccount() {
        val deleter = cascadeDeleter ?: return
        _uiState.update { it.copy(isDeleteConfirmOpen = false) }
        //isDeleteConfirmOpen.value = false
        viewModelScope.launch {
            try {
                deleter.deleteProfile(currentUserId)
                _uiState.update { it.copy(accountDeleted = true) }
                //accountDeleted.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EntrantSettings", "Failed to delete profile $currentUserId", e)
                _uiState.update { it.copy(deleteError = "Couldn't delete your account. Check your connection and try again.") }
                //deleteError.value = "Couldn't delete your account. Check your connection and try again."
            }
        }
    }

    // Loads a saved poster's picture. Old campsites may hold a phone-only
    // link instead of an image ID; those show no poster.
    private suspend fun loadProfilePicture(imageId: String?): String? = try {
        imageRepository.find(imageId)?.data
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
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
                EntrantSettingsViewModel(
                    app.container.userRepository,
                    app.container.currentUserId,
                    app.container.cascadeDeleter,
                    app.container.imageRepository,
                )
            }
        }
    }
}
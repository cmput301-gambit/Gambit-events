package com.campsitereservationapp.model

/**
 * A user that represents the person navigating the site.
 *
 * **Purpose:** contains all important data the makes up the users personal information.
 *
 * **Design rationale:** plain data class with no Firestore code, so it's
 * easy to unit test. CampsiteRepository handles saving and loading.
 *
 * **Outstanding issues:**
 * - Need to resolve safe method of retrieving the device ID to create the user during runtime.
 * - Needs an image to be stored here to complete the user profile.
 * - Does user only need a name field or both first and last name fields?
 * - Is the current interpretation of how the user class interacts with [Entrant], [CampsiteOwner]
 * and [Admin] correct?
 *
 * @property id unique ID tied to the users device ID.
 * @property firstName first name of the user.
 * @property lastName last name of the user.
 * @property email email of the user.
 * @property phoneNumber phone number of the user.
 * @property profileImage profile photo of the user.
 * @constructor Creates a user. Every field has a default.
 */
class User(
    override val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImage: String? = null,
    //TODO: Implement the three classes that this class could own.
    //val entrant: Entrant? = null,
    //val campsiteOwner: CampsiteOwner? = null,
    //val admin: Admin? = null,
) : Identifiable {

    /**
     * Checks whether this user has everything it needs to be saved.
     *
     * A valid user has a non-blank [id]. All other fields can be optionally
     * empty strings or null.
     *
     * @return true if the user can be saved, false otherwise
     */
    fun isValid(): Boolean {
        return id.isNotBlank()
    }

    fun updateProfileInformation(): Unit{
        //TODO: Confirm how this should function while maintaining MVC.
    }
}
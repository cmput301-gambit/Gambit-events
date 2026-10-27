package com.campsitereservationapp.model

/**
 * A person using the app, identified by their device.
 *
 * **Purpose:** holds profile information, notification settings and the
 * roles this person has (US 01.09.01, 01.07.03, 03.11.01).
 *
 * **Design rationale:** roles are composed, not inherited. A role is present
 * when its property is non-null, so one user can be an entrant, an owner and
 * an admin at the same time without separate accounts.
 *
 * **Outstanding issues:** TODO profile image upload not yet supported.
 *
 * @property id the device ID, also used as the Firestore document ID
 * @property name display name
 * @property email contact email, may be blank
 * @property phone contact phone number, may be blank
 * @property profileImageId ID of the profile [Image], or null if none
 * @property notificationToken this device's push notification token, or null
 * @property notificationsEnabled false if the user opted out of notifications
 * @property entrant the entrant role, or null if the user doesn't have it
 * @property owner the campsite owner role, or null if the user doesn't have it
 * @property admin the admin role, or null if the user doesn't have it
 */
data class User(
    override val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageId: String? = null,
    val notificationToken: String? = null,
    val notificationsEnabled: Boolean = true,
    val entrant: Entrant? = null,
    val owner: CampsiteOwner? = null,
    val admin: Admin? = null,
) : Identifiable {

    /** True if this user can join lotteries. */
    val isEntrant: Boolean get() = entrant != null

    /** True if this user can list campsites and run lotteries. */
    val isOwner: Boolean get() = owner != null

    /** True if this user can moderate the app. */
    val isAdmin: Boolean get() = admin != null

    /**
     * Returns a copy of this user with the campsite owner role added.
     *
     * @return the updated user; unchanged if they were already an owner
     */
    fun withOwnerRole(): User = if (isOwner) this else copy(owner = CampsiteOwner(id))

    /**
     * Returns a copy of this user with updated profile information.
     *
     * @param name the new display name
     * @param email the new email
     * @param phone the new phone number
     * @return the updated user
     */
    fun withProfile(name: String, email: String, phone: String): User =
        copy(name = name.trim(), email = email.trim(), phone = phone.trim())

    companion object {
        /**
         * Creates a brand new user for a device, with the entrant role.
         *
         * @param deviceId the device's unique ID
         * @return a new user who can start joining lotteries right away
         */
        fun newEntrant(deviceId: String): User =
            User(id = deviceId, entrant = Entrant(deviceId))
    }
}
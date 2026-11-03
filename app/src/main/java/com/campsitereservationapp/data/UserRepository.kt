package com.campsitereservationapp.data

import com.campsitereservationapp.model.Admin
import com.campsitereservationapp.model.CampsiteOwner
import com.campsitereservationapp.model.Entrant
import com.campsitereservationapp.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores and provides [User] objects.
 *
 * **Purpose:** saves, loads, updates and deletes users, and creates a user
 * the first time a device opens the app (US 01.09.01).
 *
 * **Design rationale:** extends [Repository], which already covers save,
 * load, update, delete and list; this class adds user-specific queries.
 * Its companion object converts users to and from Firestore documents.
 * Roles are saved as true/false fields and rebuilt as role objects, because
 * each role currently only holds the user's ID.
 *
 * **Outstanding issues:** None
 *
 * @param dataSource where users are kept, such as [firestoreDataSource]
 * or an [InMemoryDataSource] in tests
 */
class UserRepository(dataSource: DataSource<User>) : Repository<User>(dataSource) {

    /**
     * Loads the user for a device, creating a new entrant if this device
     * has never been seen before.
     *
     * @param deviceId the device's unique ID
     * @return the existing or newly created user
     */
    suspend fun getOrCreate(deviceId: String): User =
        get(deviceId) ?: User.newEntrant(deviceId).also { add(it) }

    /**
     * Promotes a user to a campsite owner, creating an entrant profile if none exists.
     *
     * @param id the user's unique ID
     * @return the updated user with the owner role
     */
    suspend fun promoteToOwner(id: String): User {
        val user = getOrCreate(id)
        return if (user.isOwner) user
        else update(user.withOwnerRole())
    }

    /**
     * Live view of one user, for screens like the profile page.
     *
     * @param id the user's ID
     * @return a stream that emits the user (or null if deleted) every time they change
     */
    fun observe(id: String): Flow<User?> =
        observeAll().map { users -> users.find { it.id == id } }

    companion object {
        /**
         * Creates the Firestore data source for users, used by [AppContainer].
         *
         * @return a data source backed by the "users" collection
         */
        fun firestoreDataSource(): FirestoreDataSource<User> =
            FirestoreDataSource("users", ::toMap, ::fromMap)

        /**
         * Converts a user into the fields saved in Firestore.
         *
         * The ID is not included because it is the document ID. Each role is
         * saved as a true/false field.
         *
         * @param user the user to convert
         * @return the fields to save
         */
        fun toMap(user: User): Map<String, Any?> = mapOf(
            "name" to user.name,
            "email" to user.email,
            "phone" to user.phone,
            "profileImageId" to user.profileImageId,
            "notificationToken" to user.notificationToken,
            "notificationsEnabled" to user.notificationsEnabled,
            "isEntrant" to user.isEntrant,
            "isOwner" to user.isOwner,
            "isAdmin" to user.isAdmin,
        )

        /**
         * Rebuilds a user from a Firestore document.
         *
         * Missing fields fall back to the same defaults as [User]'s constructor,
         * so a missing role field means the user doesn't have that role.
         *
         * @param id the document ID, used as the user ID
         * @param data the saved fields
         * @return the rebuilt user
         */
        fun fromMap(id: String, data: Map<String, Any?>): User = User(
            id = id,
            name = data["name"] as? String ?: "",
            email = data["email"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            profileImageId = data["profileImageId"] as? String,
            notificationToken = data["notificationToken"] as? String,
            notificationsEnabled = data["notificationsEnabled"] as? Boolean ?: true,
            entrant = if (data["isEntrant"] == true) Entrant(id) else null,
            owner = if (data["isOwner"] == true) CampsiteOwner(id) else null,
            admin = if (data["isAdmin"] == true) Admin(id) else null,
        )
    }
}
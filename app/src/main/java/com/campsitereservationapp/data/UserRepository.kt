package com.campsitereservationapp.data

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
 *
 * **Outstanding issues:** backed by an in-memory list seeded from
 * [SampleData] until Firestore is connected.
 */
class UserRepository : Repository<User>(SampleData.users) {

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
     * Live view of one user, for screens like the profile page.
     *
     * @param id the user's ID
     * @return a stream that emits the user (or null if deleted) every time they change
     */
    fun observe(id: String): Flow<User?> =
        observeAll().map { users -> users.find { it.id == id } }
}
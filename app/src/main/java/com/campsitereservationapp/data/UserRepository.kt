package com.campsitereservationapp.data

import com.campsitereservationapp.model.User

/**
 * Stores and provides [User] objects.
 *
 * **Purpose:** the single source of user data for every screen,
 * including user-specific queries like finding a user's roles.
 *
 * **Design rationale:** extends [Repository], which holds the shared
 * storage code; this class only adds user-specific queries.
 *
 * **Outstanding issues:** backed by an in-memory list seeded from
 * [SampleData] until Firestore is connected.
 * - No way of getting by a role, needs to be done after method of identifying a
 * users role is solidified.
 */
class UserRepository : Repository<User>(SampleData.users) {

    /**
     * Retrieves a user by their ID.
     *
     * @param [id] is to search users by
     * @return a single [User] with that [id] or null.
     */
    suspend fun getById(id: String): User? = getAll().firstOrNull() {it.id == id}


    //suspend fun getByRole([Some argument]): List<User> = getAll().filter {it.hasRole([Some argument])}

}
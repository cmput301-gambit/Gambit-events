package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores and provides [Campsite] objects.
 *
 * **Purpose:** the single source of campsite data for every screen,
 * including campsite-specific queries like finding an owner's campsites.
 *
 * **Design rationale:** extends [Repository], which holds the shared
 * storage code; this class only adds campsite-specific queries.
 *
 * **Outstanding issues:** backed by an in-memory list seeded from
 * [SampleData] until Firestore is connected.
 */
class CampsiteRepository : Repository<Campsite>(SampleData.campsites) {

    /**
     * Retrieves a static list of campsites owned by a specific owner.
     *
     * @param ownerId ID of the owner whose campsites to return
     * @return a list of campsites belonging to [ownerId]
     */
    suspend fun getByOwner(ownerId: String): List<Campsite> =
        getAll().filter { it.ownerId == ownerId }

    /**
     * Live list of the campsites owned by one owner.
     *
     * @param ownerId ID of the owner whose campsites to return
     * @return a stream that emits the owner's campsites every time
     * campsite data changes
     */
    fun observeByOwner(ownerId: String): Flow<List<Campsite>> =
        observeAll().map { campsites ->              // for each box on the belt...
            campsites.filter { it.ownerId == ownerId } // ...keep only this owner's campsites
        }
}
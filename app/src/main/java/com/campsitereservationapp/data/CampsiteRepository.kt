package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite

/**
 * A repository for Campsite objects.
 *
 * **Purpose:**
 *
 * **Design rationale:** <One line: the pattern it follows or why it's built this way.>
 *
 * **Outstanding issues:** TODO: A temporary stand in until firebase is set up. Must be replaced later.
 */
class CampsiteRepository : Repository<Campsite>(SampleData.campsites) {
    suspend fun getByOwner(ownerId: String) = getAll().filter { it.ownerId == ownerId }
}
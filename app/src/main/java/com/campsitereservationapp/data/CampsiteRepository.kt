package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Stores and provides [Campsite] objects.
 *
 * **Purpose:** the single source of campsite data for every screen,
 * including campsite-specific queries like finding an owner's campsites.
 *
 * **Design rationale:** extends [Repository], which holds the shared rules;
 * this class only adds campsite-specific queries. Its companion object
 * converts campsites to and from Firestore documents, so all campsite
 * storage details live in this one file.
 *
 * **Outstanding issues:** None.
 *
 * @param dataSource where campsites are kept, such as [firestoreDataSource]
 * or an [InMemoryDataSource] in tests
 */
class CampsiteRepository(dataSource: DataSource<Campsite>) : Repository<Campsite>(dataSource) {

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
     * @return a stream that emits the owner's campsites every time they change
     */
    fun observeByOwner(ownerId: String): Flow<List<Campsite>> =
        observeWhere(Filter("ownerId", ownerId) { it.ownerId == ownerId })

    companion object {
        /**
         * Creates the Firestore data source for campsites, used by [AppContainer].
         *
         * @return a data source backed by the "campsites" collection
         */
        fun firestoreDataSource(): FirestoreDataSource<Campsite> =
            FirestoreDataSource("campsites", ::toMap, ::fromMap)

        /**
         * Converts a campsite into the fields saved in Firestore.
         *
         * The ID is not included because it is the document ID. Dates are
         * saved as "yyyy-MM-dd" text so they are readable in the console.
         *
         * @param campsite the campsite to convert
         * @return the fields to save
         */
        fun toMap(campsite: Campsite): Map<String, Any?> = mapOf(
            "ownerId" to campsite.ownerId,
            "name" to campsite.name,
            "description" to campsite.description,
            "location" to campsite.location,
            "capacity" to campsite.capacity,
            "posterImageId" to campsite.posterImageId,
            "tags" to campsite.tags,
            "latitude" to campsite.latitude,
            "longitude" to campsite.longitude,
            "availabilityStart" to campsite.availability?.start?.toString(),
            "availabilityFinish" to campsite.availability?.finish?.toString(),
        )

        /**
         * Rebuilds a campsite from a Firestore document.
         *
         * Missing fields fall back to the same defaults as [Campsite]'s constructor.
         *
         * @param id the document ID, used as the campsite ID
         * @param data the saved fields
         * @return the rebuilt campsite
         */
        fun fromMap(id: String, data: Map<String, Any?>): Campsite = Campsite(
            id = id,
            ownerId = data["ownerId"] as? String ?: "",
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            location = data["location"] as? String ?: "",
            capacity = (data["capacity"] as? Number)?.toInt() ?: 0,
            posterImageId = data["posterImageId"] as? String,
            tags = (data["tags"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
            latitude = (data["latitude"] as? Number)?.toDouble(),
            longitude = (data["longitude"] as? Number)?.toDouble(),
            availability = readDateRange(data["availabilityStart"], data["availabilityFinish"]),
        )

        // Builds a DateRange only when both dates were saved.
        private fun readDateRange(start: Any?, finish: Any?): DateRange? {
            val startText = start as? String ?: return null
            val finishText = finish as? String ?: return null
            return DateRange(LocalDate.parse(startText), LocalDate.parse(finishText))
        }
    }
}

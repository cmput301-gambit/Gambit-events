package com.campsitereservationapp.data

import com.campsitereservationapp.R
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Rating
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Stores and provides [Rating] objects.
 *
 * **Purpose:** the single source of rating data for every screen,
 * including rating-specific queries like finding a user's ratings for a specific campsite.
 *
 * **Design rationale:** extends [Repository], which holds the shared rules;
 * this class only adds rating-specific queries. Its companion object
 * converts ratings to and from Firestore documents, so all rating
 * storage details live in this one file.
 *
 * **Outstanding issues:** None.
 *
 * @param dataSource where ratings are kept, such as [firestoreDataSource]
 * or an [InMemoryDataSource] in tests
 */
class RatingRepository(dataSource: DataSource<Rating>) : Repository<Rating>(dataSource) {

    /**
     * Retrieves a static list of ratings for a specific campsite owned by a specific user.
     *
     * @param campId ID of the camp that the rating is discussing
     * @param userId ID of the user who made the rating
     * @return a list of all ratings by a given user for a given campsite
     */
    suspend fun getForCampByUser(campId: String, userId: String): List<Rating> =
        getAll().filter { it.userId == userId && it.campsiteId == campId }

    /**
     * Live list of the ratings for one campsite.
     *
     * @param campId ID of the campsite that those ratings are for
     * @return a stream that emits the campsites ratings every time they change
     */
    fun observeByCampsite(campId: String): Flow<List<Rating>> =
        observeWhere(Filter("campsiteId", campId) { it.campsiteId == campId })

    suspend fun averageRatingByCampsite(campId: String): Float =
        getAll()
            .filter { it.campsiteId == campId }
            .map { it.score }
            .takeIf { it.isNotEmpty() }
            ?.average()?.toFloat() ?: 0f

    /**
     * Deletes every rating a user left, used when their profile is deleted
     * (US 01.08.01, 03.02.01).
     *
     * @param userId the ID of the deleted user
     */
    suspend fun deleteByUser(userId: String) {
        getAll().filter { it.userId == userId }.forEach { delete(it.id) }
    }

    /**
     * Deletes every rating for a campsite, used when the campsite is deleted.
     *
     * @param campId the ID of the deleted campsite
     */
    suspend fun deleteByCampsite(campId: String) {
        getAll().filter { it.campsiteId == campId }.forEach { delete(it.id) }
    }

    companion object {
        /**
         * The ID for one user's rating of one campsite. Every user gets the
         * same ID for the same campsite, so [add] refuses a second rating
         * (US 01.13.01: each user rates a campsite once).
         *
         * @param userId the user giving the rating
         * @param campsiteId the campsite being rated
         * @return the rating's ID
         */
        fun idFor(userId: String, campsiteId: String): String = "${userId}_$campsiteId"

        /**
         * Creates the Firestore data source for ratings, used by [AppContainer].
         *
         * @return a data source backed by the "ratings" collection
         */
        fun firestoreDataSource(): FirestoreDataSource<Rating> =
            FirestoreDataSource("ratings", ::toMap, ::fromMap)

        /**
         * Converts a rating into the fields saved in Firestore.
         *
         * The ID is not included because it is the document ID.
         *
         * @param rating the rating to convert
         * @return the fields to save
         */
        fun toMap(rating: Rating): Map<String, Any?> = mapOf(
            "userId" to rating.userId,
            "campsiteId" to rating.campsiteId,
            "score" to rating.score,
            "review" to rating.review,
        )

        /**
         * Rebuilds a rating from a Firestore document.
         *
         * Missing fields fall back to the same defaults as [Rating]'s constructor.
         *
         * @param id the document ID, used as the rating ID
         * @param data the saved fields
         * @return the rebuilt rating
         */
        fun fromMap(id: String, data: Map<String, Any?>): Rating = Rating(
            id = id,
            userId = data["userId"] as? String ?: "",
            campsiteId = data["campsiteId"] as? String ?: "",
            score = (data["score"] as? Number)?.toFloat() ?: 0f,
            review = data["review"] as? String ?: "",
        )
    }
}

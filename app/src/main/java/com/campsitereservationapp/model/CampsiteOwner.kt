package com.campsitereservationapp.model

/**
 * The role that lets a [User] list campsites and run lotteries.
 *
 * **Purpose:** marks a user as a campsite owner and holds owner-specific checks.
 *
 * **Design rationale:** composed into [User]. Owned campsites are found by
 * querying campsites whose ownerId matches [userId], not stored as a list
 * here, so the two can never disagree.
 *
 * **Outstanding issues:** None.
 *
 * @property userId ID of the [User] this role belongs to
 */
data class CampsiteOwner(
    val userId: String,
) {
    /**
     * Checks whether this owner owns a campsite.
     *
     * @param campsite the campsite to check
     * @return true if the campsite belongs to this owner
     */
    fun owns(campsite: Campsite): Boolean = campsite.ownerId == userId
}
package com.campsitereservationapp.model

/**
 * The role that lets a [User] browse campsites and join lotteries.
 *
 * **Purpose:** marks a user as an entrant and holds entrant-specific checks.
 *
 * **Design rationale:** composed into [User] (see User's KDoc). Lotteries
 * and reservations reference the entrant by [userId] rather than being
 * stored here, so each piece of data has one source of truth.
 *
 * **Outstanding issues:** TODO add waiting-list checks once Lottery exists.
 *
 * @property userId ID of the [User] this role belongs to
 */
data class Entrant(
    val userId: String,
)
package com.campsitereservationapp.model

/**
 * The role that lets a [User] moderate campsites, profiles and images.
 *
 * **Purpose:** marks a user as an admin.
 *
 * **Design rationale:** composed into [User] alongside the other roles, so
 * an admin can also be an entrant or owner on the same profile (US 03.11.01).
 *
 * **Outstanding issues:** None.
 *
 * @property userId ID of the [User] this role belongs to
 */
data class Admin(
    val userId: String,
)
package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.User

/**
 * Realistic example data shared by repositories, previews and tests.
 *
 * **Purpose:** gives every screen and test the same believable data
 * before Firestore is connected.
 *
 * **Design rationale:** one shared object so sample data isn't
 * duplicated across files; stable IDs so tests can look items up.
 *
 * **Outstanding issues:** add users, lotteries and reservations as
 * those models are created.
 */
object SampleData {
    val campsites = listOf(
        Campsite(
            id = "campsite-pine-ridge",
            ownerId = "owner-dana",
            name = "Pine Ridge",
            description = "Shaded sites near the river, fire pits and pit toilets.",
            location = "Hinton, AB",
            capacity = 6,
        ),
        Campsite(
            id = "campsite-lakeside-loop",
            ownerId = "owner-dana",
            name = "Lakeside Loop",
            description = "Lakefront sites with a boat launch and showers.",
            location = "Sylvan Lake, AB",
            capacity = 4,
        ),
        Campsite(
            id = "campsite-aspen-hollow",
            ownerId = "owner-marcus",
            name = "Aspen Hollow",
            description = "Quiet walk-in tent sites in an aspen grove.",
            location = "Elk Island, AB",
            capacity = 2,
        ),

        // Add sample data for other classes here such as lotteries, reservations, users, etc.
    )

    val users = listOf(User(
        id = "owner-noah",
        name = "Noah Walters",
        email = "noah.ex@example.com",
        phoneNumber = "780-555-0101",
        profileImage = "profiles/owner-noah.jpg",
    ),
        User(
            id = "owner-marcus",
            name = "Marcus Delaney",
            email = "marcus.delaney@example.com",
            phoneNumber = "587-555-0102",
            profileImage = "profiles/owner-marcus.jpg",
        ),
        User(
            id = "entrant-liam",
            name = "Liam Tremblay",
            email = "liam.tremblay@example.com",
            phoneNumber = "780-555-0104",
            profileImage = "profiles/entrant-liam.jpg",
        ),
        User(
            id = "admin-karl",
            name = "Karl Markowski",
            email = "kjmarkow@example.com",
            phoneNumber = "403-555-0105",
            profileImage = null,
        ),
    )
}
package com.campsitereservationapp.data

import com.campsitereservationapp.model.Admin
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteOwner
import com.campsitereservationapp.model.Entrant
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
            ownerId = "owner-bobby",
            name = "Pine Ridge",
            description = "Shaded sites near the river, fire pits and pit toilets.",
            location = "Hinton, AB",
            capacity = 6,
        ),
        Campsite(
            id = "campsite-lakeside-loop",
            ownerId = "owner-bobby",
            name = "Lakeside Loop",
            description = "Lakefront sites with a boat launch and showers.",
            location = "Sylvan Lake, AB",
            capacity = 4,
        ),
        Campsite(
            id = "campsite-aspen-hollow",
            ownerId = "owner-jill",
            name = "Aspen Hollow",
            description = "Quiet walk-in tent sites in an aspen grove.",
            location = "Elk Island, AB",
            capacity = 2,
        ),
        // Add sample data for other classes here such as lotteries, reservations, users, etc.
    )
    val users = listOf(
        User(
            id = "owner-bobby", name = "Bobby Smith", email = "bobby@example.com",
            entrant = Entrant("owner-bobby"), owner = CampsiteOwner("owner-bobby"),
        ),
        User(
            id = "owner-jill", name = "Jill Mason", phone = "780-555-0142",
            owner = CampsiteOwner("owner-jill"),
        ),
        User(
            id = "entrant-priya", name = "Priya Shah", email = "priya@example.com",
            entrant = Entrant("entrant-priya"),
        ),
        User(
            id = "admin-sam", name = "Sam Okafor",
            entrant = Entrant("admin-sam"), admin = Admin("admin-sam"),
        ),
    )
}
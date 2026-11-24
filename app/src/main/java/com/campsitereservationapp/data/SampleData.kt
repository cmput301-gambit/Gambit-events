package com.campsitereservationapp.data

import com.campsitereservationapp.model.Admin
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteOwner
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Entrant
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.User
import com.campsitereservationapp.model.WaitingList
import java.time.LocalDate

/**
 * Realistic example data shared by repositories, previews and tests.
 *
 * **Purpose:** gives tests, previews and a fresh database the same
 * believable data.
 *
 * **Design rationale:** one shared object so sample data isn't
 * duplicated across files; stable IDs so tests can look items up. Dates
 * are relative to today, and each campsite's availability covers its
 * lottery's booking period, so a sample winner always has dates to book.
 *
 * **Outstanding issues:** dates are fixed at the moment the data is seeded
 * into Firestore, so the seeded copies go out of date over time.
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
            availability = DateRange(LocalDate.now().minusDays(7), LocalDate.now().plusDays(30))
        ),
        Campsite(
            id = "campsite-aspen-hollow",
            ownerId = "owner-jill",
            name = "Aspen Hollow",
            description = "Quiet walk-in tent sites in an aspen grove.",
            location = "Elk Island, AB",
            capacity = 2,
            availability = DateRange(LocalDate.now().minusDays(7), LocalDate.now().plusDays(30))
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
        User(
            id = "2f9e63b7-4130-45af-b310-905cae77b937", name = "Noah Walters",
            entrant = Entrant(userId = "2f9e63b7-4130-45af-b310-905cae77b937")
        )
    )

    val lotteries = listOf(
        Lottery("lottery-a", "owner-bobby",
            mutableListOf("campsite-lakeside-loop"),
            DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7)),
            DateRange(LocalDate.now().plusDays(8), LocalDate.now().plusDays(12)),
            stage = LotteryStatus.OPEN
            ),
        Lottery("lottery-b", "owner-bobby",
            mutableListOf("campsite-aspen-hollow"),
            DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(7)),
            DateRange(LocalDate.now().plusDays(8), LocalDate.now().plusDays(12)),
            stage = LotteryStatus.OPEN,
            waitingList = WaitingList(mutableListOf("entrant-user"))
        )
    )
}
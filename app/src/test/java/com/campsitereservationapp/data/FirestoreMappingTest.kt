package com.campsitereservationapp.data

import com.campsitereservationapp.model.Admin
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteOwner
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Entrant
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.NotificationType
import com.campsitereservationapp.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for the Firestore conversion functions in each repository's
 * companion object. Does not test FirestoreDataSource itself, which
 *  * needs a running database.
 *
 * **Purpose:** checks that campsites, users and notifications survive being
 * saved to Firestore and loaded back without losing or changing anything.
 *
 * **Design rationale:** toMap and fromMap are plain functions, so they can be
 * tested on the computer without Firebase. Round-trip tests (object, to map,
 * back to object) catch typos in field names on either side. Extra tests feed
 * in the exact shapes Firestore really returns, such as whole numbers as Long.
 *
 * **Outstanding issues:** None.
 */
class FirestoreMappingTest {

    // ---------- Campsite ----------

    private val mooseMeadow = Campsite(
        id = "campsite-moose-meadow",
        ownerId = "owner-jill",
        name = "Moose Meadow",
        description = "Riverside tent pads with bear lockers.",
        location = "Jasper, AB",
        capacity = 3,
        posterImageId = "image-moose-poster",
        tags = listOf("riverside", "tent-only"),
        latitude = 52.8737,
        longitude = -118.0814,
        availability = DateRange(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 8, 31)),
    )

    @Test
    fun campsite_roundTrip_keepsEveryField() {
        val map = CampsiteRepository.toMap(mooseMeadow)

        assertEquals(mooseMeadow, CampsiteRepository.fromMap(mooseMeadow.id, map))
    }

    @Test
    fun campsite_roundTrip_withNoOptionalFields() {
        val pineRidge = SampleData.campsites.first { it.id == "campsite-pine-ridge" }

        val map = CampsiteRepository.toMap(pineRidge)

        assertEquals(pineRidge, CampsiteRepository.fromMap(pineRidge.id, map))
    }

    @Test
    fun campsite_toMap_doesNotStoreTheId() {
        assertFalse("id" in CampsiteRepository.toMap(mooseMeadow))
    }

    @Test
    fun campsite_fromMap_acceptsWholeNumbersAsLong() {
        // Firestore always returns whole numbers as Long, even if saved as Int.
        val fromFirestore = mapOf("name" to "Moose Meadow", "capacity" to 3L)

        assertEquals(3, CampsiteRepository.fromMap("campsite-moose-meadow", fromFirestore).capacity)
    }

    @Test
    fun campsite_fromMap_withOnlyOneDate_hasNoAvailability() {
        val fromFirestore = mapOf("name" to "Moose Meadow", "availabilityStart" to "2027-06-01")

        assertNull(CampsiteRepository.fromMap("campsite-moose-meadow", fromFirestore).availability)
    }

    // ---------- User ----------

    @Test
    fun user_roundTrip_keepsAllThreeRoles() {
        val sam = User(
            id = "admin-sam",
            name = "Sam Okafor",
            email = "sam@example.com",
            phone = "780-555-0142",
            notificationsEnabled = false,
            entrant = Entrant("admin-sam"),
            owner = CampsiteOwner("admin-sam"),
            admin = Admin("admin-sam"),
        )

        val map = UserRepository.toMap(sam)

        assertEquals(sam, UserRepository.fromMap(sam.id, map))
    }

    @Test
    fun user_roundTrip_worksForEverySampleUser() {
        for (user in SampleData.users) {
            assertEquals(user, UserRepository.fromMap(user.id, UserRepository.toMap(user)))
        }
    }

    @Test
    fun user_fromMap_withNoRoleFields_hasNoRoles() {
        val user = UserRepository.fromMap("device-123", mapOf("name" to "Priya Shah"))

        assertFalse(user.isEntrant)
        assertFalse(user.isOwner)
        assertFalse(user.isAdmin)
    }

    // ---------- Notification ----------

    private val priyaWon = Notification.won(
        id = "notification-1",
        recipientId = "entrant-priya",
        lotteryId = "lottery-summer-2027",
        campsiteName = "Moose Meadow",
        createdAt = LocalDate.of(2027, 5, 15),
    )

    @Test
    fun notification_roundTrip_keepsEveryField() {
        val map = NotificationRepository.toMap(priyaWon)

        assertEquals(priyaWon, NotificationRepository.fromMap(priyaWon.id, map))
    }

    @Test
    fun notification_roundTrip_keepsReadStatus() {
        val read = priyaWon.markRead()

        assertTrue(NotificationRepository.fromMap(read.id, NotificationRepository.toMap(read)).read)
    }

    @Test
    fun notification_savesTypeAsText() {
        assertEquals("LOTTERY_WON", NotificationRepository.toMap(priyaWon)["type"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun notification_fromMap_withUnknownType_isRejected() {
        // Better to skip a broken notification than to tell someone they won.
        NotificationRepository.fromMap("notification-1", mapOf("type" to "LOTTERY_MAYBE"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun notification_fromMap_withMissingType_isRejected() {
        NotificationRepository.fromMap("notification-1", mapOf("message" to "Hello"))
    }
}
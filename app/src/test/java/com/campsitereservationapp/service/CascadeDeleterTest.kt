package com.campsitereservationapp.service

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MessageRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.CampsiteOwner
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Entrant
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.Message
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.Rating
import com.campsitereservationapp.model.Reservation
import com.campsitereservationapp.model.SenderRole
import com.campsitereservationapp.model.User
import com.campsitereservationapp.model.WaitingList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [CascadeDeleter].
 *
 * **Purpose:** checks that deleting a profile (US 01.08.01, 03.02.01), removing
 * an owner (US 03.07.01) and deleting a campsite clean up everything that
 * points at them, and leave everyone else's data alone.
 *
 * **Design rationale:** every repository is backed by [MockDataSource], so
 * nothing touches Firestore. The data is a small but realistic world: Priya
 * is an entrant with a waiting list spot, a lottery win, a reservation, a
 * message thread, a notification, a rating and a profile picture; Bobby owns
 * two campsites; Jill owns one and shares a lottery with Bobby. Pine Ridge and
 * Aspen Hollow have posters.
 *
 * **Outstanding issues:** None.
 */
class CascadeDeleterTest {
    private lateinit var users: UserRepository
    private lateinit var campsites: CampsiteRepository
    private lateinit var lotteries: LotteryRepository
    private lateinit var reservations: ReservationRepository
    private lateinit var notifications: NotificationRepository
    private lateinit var ratings: RatingRepository
    private lateinit var messages: MessageRepository
    private lateinit var images: ImageRepository
    private lateinit var deleter: CascadeDeleter

    private val today = LocalDate.of(2026, 11, 25)
    private val registration = DateRange(today.plusDays(1), today.plusDays(5))
    private val stay = DateRange(today.plusDays(10), today.plusDays(12))

    private val priya = User(
        "entrant-priya", name = "Priya Shah", profileImageId = "image-priya-profile",
        entrant = Entrant("entrant-priya"),
    )
    private val noah = User("entrant-noah", name = "Noah Walters", entrant = Entrant("entrant-noah"))
    private val bobby = User(
        "owner-bobby", name = "Bobby Smith",
        entrant = Entrant("owner-bobby"), owner = CampsiteOwner("owner-bobby"),
    )
    private val jill = User("owner-jill", name = "Jill Mason", owner = CampsiteOwner("owner-jill"))

    private val pineRidge = Campsite(
        "campsite-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge", capacity = 6,
        posterImageId = "image-pine-ridge",
    )
    private val lakesideLoop = Campsite("campsite-lakeside-loop", ownerId = "owner-bobby", name = "Lakeside Loop", capacity = 4)
    private val aspenHollow = Campsite(
        "campsite-aspen-hollow", ownerId = "owner-jill", name = "Aspen Hollow", capacity = 2,
        posterImageId = "image-aspen-hollow",
    )

    // The first bytes of a JPEG file, enough to stand in for a real photo.
    private val jpegData = Image.encode(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10))
    private val pineRidgePoster = Image("image-pine-ridge", "owner-bobby", ImageKind.CAMPSITE, "campsite-pine-ridge", jpegData)
    private val aspenHollowPoster = Image("image-aspen-hollow", "owner-jill", ImageKind.CAMPSITE, "campsite-aspen-hollow", jpegData)
    private val priyaProfilePicture = Image("image-priya-profile", "entrant-priya", ImageKind.PROFILE, "entrant-priya", jpegData)

    // Priya and Noah are waiting for Pine Ridge.
    private val pineRidgeLottery = Lottery(
        "lottery-pine-ridge", ownerId = "owner-bobby", campsiteIds = mutableListOf("campsite-pine-ridge"),
        registrationPeriod = registration, reservationPeriod = stay, stage = LotteryStatus.OPEN,
        waitingList = WaitingList(entrantList = listOf("entrant-priya", "entrant-noah")),
    )

    // Jill's lottery that also lists Bobby's Lakeside Loop. Priya already won it.
    private val sharedLottery = Lottery(
        "lottery-shared", ownerId = "owner-jill",
        campsiteIds = mutableListOf("campsite-aspen-hollow", "campsite-lakeside-loop"),
        registrationPeriod = registration, reservationPeriod = stay, stage = LotteryStatus.CLOSED,
        waitingList = WaitingList(
            entrantList = listOf("entrant-priya", "entrant-noah"),
            entrantWinners = listOf("entrant-priya"),
            entrantLosers = listOf("entrant-noah"),
        ),
    )

    private val priyaAtAspen = Reservation(
        "reservation-priya-aspen", entrantId = "entrant-priya",
        campsiteId = "campsite-aspen-hollow", lotteryId = "lottery-shared", dates = stay,
    )
    private val noahAtLakeside = Reservation(
        "reservation-noah-lakeside", entrantId = "entrant-noah",
        campsiteId = "campsite-lakeside-loop", lotteryId = "lottery-shared", dates = stay,
    )

    private val jillToPriya = Message(
        "message-jill-priya", reservationId = "reservation-priya-aspen",
        senderId = "owner-jill", recipientId = "entrant-priya", senderRole = SenderRole.OWNER,
        text = "Check-in is after 2 pm.",
    )
    private val bobbyToNoah = Message(
        "message-bobby-noah", reservationId = "reservation-noah-lakeside",
        senderId = "owner-bobby", recipientId = "entrant-noah", senderRole = SenderRole.OWNER,
        text = "The boat launch is closed this weekend.",
    )

    private val priyaWon = Notification.won("notification-priya", "entrant-priya", "lottery-shared", "Aspen Hollow", today)
    private val noahLost = Notification.lost("notification-noah", "entrant-noah", "lottery-shared", "Aspen Hollow", today)

    private val priyaRatesAspen = Rating("entrant-priya_campsite-aspen-hollow", "entrant-priya", "campsite-aspen-hollow", 4f)
    private val noahRatesPine = Rating("entrant-noah_campsite-pine-ridge", "entrant-noah", "campsite-pine-ridge", 5f)
    private val noahRatesAspen = Rating("entrant-noah_campsite-aspen-hollow", "entrant-noah", "campsite-aspen-hollow", 3f)

    @Before
    fun setUp() {
        users = UserRepository(MockDataSource(listOf(priya, noah, bobby, jill)))
        campsites = CampsiteRepository(MockDataSource(listOf(pineRidge, lakesideLoop, aspenHollow)))
        lotteries = LotteryRepository(MockDataSource(listOf(pineRidgeLottery, sharedLottery)), campsites)
        reservations = ReservationRepository(MockDataSource(listOf(priyaAtAspen, noahAtLakeside)))
        notifications = NotificationRepository(MockDataSource(listOf(priyaWon, noahLost)))
        ratings = RatingRepository(MockDataSource(listOf(priyaRatesAspen, noahRatesPine, noahRatesAspen)))
        messages = MessageRepository(MockDataSource(listOf(jillToPriya, bobbyToNoah)))
        images = ImageRepository(MockDataSource(listOf(pineRidgePoster, aspenHollowPoster, priyaProfilePicture)))
        deleter = CascadeDeleter(users, campsites, lotteries, reservations, notifications, ratings, messages, images)
    }

    // ---- Deleting an entrant's profile (US 01.08.01, 03.02.01) ----

    @Test
    fun deleteProfile_removesTheProfile() = runTest {
        deleter.deleteProfile("entrant-priya")

        assertNull(users.get("entrant-priya"))
    }

    @Test
    fun deleteProfile_removesThemFromEveryWaitingListAndResult() = runTest {
        deleter.deleteProfile("entrant-priya")

        val open = lotteries.get("lottery-pine-ridge")!!.waitingList
        val closed = lotteries.get("lottery-shared")!!.waitingList
        assertEquals(listOf("entrant-noah"), open.entrantList)
        assertEquals(listOf("entrant-noah"), closed.entrantList)
        assertTrue(closed.entrantWinners.isEmpty())
        assertEquals(listOf("entrant-noah"), closed.entrantLosers)
    }

    @Test
    fun deleteProfile_removesTheirReservationsMessagesNotificationsAndRatings() = runTest {
        deleter.deleteProfile("entrant-priya")

        assertNull(reservations.get("reservation-priya-aspen"))
        assertNull(messages.get("message-jill-priya"))
        assertNull(notifications.get("notification-priya"))
        assertNull(ratings.get("entrant-priya_campsite-aspen-hollow"))
    }

    @Test
    fun deleteProfile_leavesEveryoneElsesDataAlone() = runTest {
        deleter.deleteProfile("entrant-priya")

        assertNotNull(users.get("entrant-noah"))
        assertNotNull(reservations.get("reservation-noah-lakeside"))
        assertNotNull(messages.get("message-bobby-noah"))
        assertNotNull(notifications.get("notification-noah"))
        assertEquals(2, ratings.getAll().size)
        assertEquals(3, campsites.getAll().size)
        assertEquals(2, lotteries.getAll().size)
    }

    @Test
    fun deleteProfile_twice_isSafe() = runTest {
        deleter.deleteProfile("entrant-priya")
        deleter.deleteProfile("entrant-priya")

        assertNull(users.get("entrant-priya"))
        assertEquals(3, users.getAll().size)
    }

    // ---- Deleting an owner's profile ----

    @Test
    fun deleteProfile_ofOwner_alsoRemovesTheirCampsitesAndLotteries() = runTest {
        deleter.deleteProfile("owner-bobby")

        assertNull(users.get("owner-bobby"))
        assertEquals(listOf("campsite-aspen-hollow"), campsites.getAll().map { it.id })
        assertNull(lotteries.get("lottery-pine-ridge"))
        // Jill's shared lottery stays, minus Bobby's campsite.
        assertEquals(listOf("campsite-aspen-hollow"), lotteries.get("lottery-shared")!!.campsiteIds)
    }

    // ---- Removing a rule-breaking owner (US 03.07.01) ----

    @Test
    fun removeOwner_deletesTheirCampsitesAndLotteriesButKeepsTheEntrant() = runTest {
        val updated = deleter.removeOwner("owner-bobby")!!

        assertFalse(updated.isOwner)
        assertTrue(updated.isEntrant)
        assertNotNull(users.get("owner-bobby"))
        assertTrue(campsites.getByOwner("owner-bobby").isEmpty())
        assertNull(lotteries.get("lottery-pine-ridge"))
    }

    @Test
    fun removeOwner_cleansUpBookingsAndRatingsForTheirCampsites() = runTest {
        deleter.removeOwner("owner-bobby")

        assertNull(reservations.get("reservation-noah-lakeside"))
        assertNull(messages.get("message-bobby-noah"))
        assertNull(ratings.get("entrant-noah_campsite-pine-ridge"))
        // Bookings and ratings for Jill's campsite are untouched.
        assertNotNull(reservations.get("reservation-priya-aspen"))
        assertNotNull(ratings.get("entrant-noah_campsite-aspen-hollow"))
    }

    @Test
    fun removeOwner_ofSomeoneWhoIsNotAnOwner_returnsNull() = runTest {
        assertNull(deleter.removeOwner("entrant-priya"))
        assertEquals(priya, users.get("entrant-priya"))
    }

    @Test
    fun removeOwner_ofMissingUser_returnsNull() = runTest {
        assertNull(deleter.removeOwner("nobody"))
    }

    // ---- Deleting a single campsite ----

    @Test
    fun deleteCampsite_removesItsLotteryReservationsAndRatings() = runTest {
        deleter.deleteCampsite("campsite-aspen-hollow")

        assertNull(campsites.get("campsite-aspen-hollow"))
        assertNull(reservations.get("reservation-priya-aspen"))
        assertNull(messages.get("message-jill-priya"))
        assertTrue(ratings.getAll().none { it.campsiteId == "campsite-aspen-hollow" })
        assertEquals(listOf("campsite-lakeside-loop"), lotteries.get("lottery-shared")!!.campsiteIds)
    }

    @Test
    fun deleteCampsite_alsoRemovesItsPoster() = runTest {
        deleter.deleteCampsite("campsite-aspen-hollow")

        assertNull(images.get("image-aspen-hollow"))
        assertNotNull(images.get("image-pine-ridge"))
    }

    @Test
    fun deleteProfile_alsoRemovesTheirProfilePicture() = runTest {
        deleter.deleteProfile("entrant-priya")

        assertNull(images.get("image-priya-profile"))
        assertEquals(2, images.getAll().size)
    }

    @Test
    fun removeOwner_alsoRemovesTheirCampsitePosters() = runTest {
        deleter.removeOwner("owner-bobby")

        assertNull(images.get("image-pine-ridge"))
        assertNotNull(images.get("image-aspen-hollow"))
    }

    // ---- Deleting a single image (US 03.03.01) ----

    @Test
    fun deleteImage_ofCampsitePoster_clearsItFromTheCampsite() = runTest {
        deleter.deleteImage("image-pine-ridge")

        assertNull(images.get("image-pine-ridge"))
        assertNull(campsites.get("campsite-pine-ridge")!!.posterImageId)
        assertEquals("Pine Ridge", campsites.get("campsite-pine-ridge")!!.name)
        assertEquals("image-aspen-hollow", campsites.get("campsite-aspen-hollow")!!.posterImageId)
    }

    @Test
    fun deleteImage_ofProfilePicture_clearsItFromTheProfile() = runTest {
        deleter.deleteImage("image-priya-profile")

        assertNull(images.get("image-priya-profile"))
        assertNull(users.get("entrant-priya")!!.profileImageId)
        assertEquals("Priya Shah", users.get("entrant-priya")!!.name)
    }

    @Test
    fun deleteImage_ofMissingImage_changesNothing() = runTest {
        deleter.deleteImage("image-nobody")

        assertEquals(3, images.getAll().size)
        assertEquals(pineRidge, campsites.get("campsite-pine-ridge"))
    }
}

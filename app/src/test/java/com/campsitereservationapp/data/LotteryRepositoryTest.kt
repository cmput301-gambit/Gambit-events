package com.campsitereservationapp.data

import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.WaitingList
import com.campsitereservationapp.service.NotificationHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Unit tests for [LotteryRepository].
 *
 * **Purpose:** verifies that [LotteryRepository] correctly handles saving, updating,
 * and deleting lotteries, including campsite deletions via [LotteryRepository.deleteByCampsite],
 * and leaving a waiting list (US 01.02.02).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LotteryRepositoryTest {

    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var lotteryRepository: LotteryRepository

    private val regPeriod = DateRange(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5))
    private val resPeriod = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10))

    @Before
    fun setUp() {
        campsiteRepository = CampsiteRepository(MockDataSource())
        lotteryRepository = LotteryRepository(MockDataSource(), campsiteRepository)
    }

    @Test
    fun deleteByCampsite_deletesLottery_whenOnlyThatCampsiteIsInLottery() = runTest {
        val lottery = Lottery(
            id = "lottery-101",
            ownerId = "owner-dana",
            campsiteIds = listOf("camp-alpha"),
            registrationPeriod = regPeriod,
            reservationPeriod = resPeriod,
            stage = LotteryStatus.OPEN
        )
        lotteryRepository.add(lottery)

        assertEquals(1, lotteryRepository.getAll().size)

        lotteryRepository.deleteByCampsite("camp-alpha")

        assertTrue(lotteryRepository.getAll().isEmpty())
    }

    @Test
    fun deleteByCampsite_removesCampsiteId_whenMultiCampsiteLottery() = runTest {
        val lottery = Lottery(
            id = "lottery-102",
            ownerId = "owner-dana",
            campsiteIds = listOf("camp-alpha", "camp-beta"),
            registrationPeriod = regPeriod,
            reservationPeriod = resPeriod,
            stage = LotteryStatus.OPEN
        )
        lotteryRepository.add(lottery)

        lotteryRepository.deleteByCampsite("camp-alpha")

        val remaining = lotteryRepository.getAll()
        assertEquals(1, remaining.size)
        assertEquals(listOf("camp-beta"), remaining[0].campsiteIds)
    }

    @Test
    fun deleteByCampsite_doesNotAffectUnrelatedLotteries() = runTest {
        val lottery1 = Lottery(
            id = "lottery-1",
            ownerId = "owner-dana",
            campsiteIds = listOf("camp-alpha"),
            registrationPeriod = regPeriod,
            reservationPeriod = resPeriod
        )
        val lottery2 = Lottery(
            id = "lottery-2",
            ownerId = "owner-dana",
            campsiteIds = listOf("camp-beta"),
            registrationPeriod = regPeriod,
            reservationPeriod = resPeriod
        )
        lotteryRepository.add(lottery1)
        lotteryRepository.add(lottery2)

        lotteryRepository.deleteByCampsite("camp-alpha")

        val remaining = lotteryRepository.getAll()
        assertEquals(1, remaining.size)
        assertEquals("lottery-2", remaining[0].id)
    }

    private fun openLotteryWith(entrants: List<String>) = Lottery(
        id = "lottery-moose", ownerId = "owner-jill", campsiteIds = listOf("camp-alpha"),
        registrationPeriod = regPeriod, reservationPeriod = resPeriod,
        stage = LotteryStatus.OPEN, winnerAmount = 1,
        waitingList = WaitingList(entrantList = entrants, entrantCap = 2, isFull = entrants.size == 2),
    )

    @Test
    fun leaveWaitingList_removesTheEntrantAndSaves() = runTest {
        lotteryRepository.add(openLotteryWith(listOf("entrant-priya", "entrant-noah")))

        lotteryRepository.leaveWaitingList("lottery-moose", "entrant-priya")

        val saved = lotteryRepository.get("lottery-moose")!!
        assertEquals(listOf("entrant-noah"), saved.waitingList.entrantList)
        assertFalse(saved.waitingList.isFull)
    }

    @Test
    fun leaveWaitingList_freesASpotForSomeoneElse() = runTest {
        lotteryRepository.add(openLotteryWith(listOf("entrant-priya", "entrant-noah")))
        assertNull(lotteryRepository.joinWaitingList("lottery-moose", "entrant-ali"))

        lotteryRepository.leaveWaitingList("lottery-moose", "entrant-priya")

        assertNotNull(lotteryRepository.joinWaitingList("lottery-moose", "entrant-ali"))
    }

    @Test
    fun leaveWaitingList_whenNotOnTheList_returnsNull() = runTest {
        lotteryRepository.add(openLotteryWith(listOf("entrant-noah")))

        assertNull(lotteryRepository.leaveWaitingList("lottery-moose", "entrant-priya"))
    }
    // ================== Automatic Draw Test ==================

    private val drawDay = LocalDate.of(2027, 5, 15)

    private val notificationRepository = NotificationRepository(MockDataSource())

    private fun drawingRepository() = LotteryRepository(
        MockDataSource(),
        CampsiteRepository(MockDataSource(SampleData.campsites)),
        NotificationHandler(notificationRepository),
    )

    private fun summerLottery(registrationEnds: LocalDate) = Lottery(
        id = "lottery-pine-summer", ownerId = "owner-bobby",
        campsiteIds = listOf("campsite-pine-ridge"),
        registrationPeriod = DateRange(LocalDate.of(2027, 5, 1), registrationEnds),
        reservationPeriod = DateRange(LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 30)),
        stage = LotteryStatus.OPEN, winnerAmount = 2,
        waitingList = WaitingList(
            entrantList = listOf("entrant-priya", "entrant-noah", "entrant-ali", "entrant-ben", "entrant-sara"),
        ),
    )

    @Test
    fun drawDueLotteries_drawsEndedLotteryAndNotifiesEveryEntrant() = runTest {
        val repository = drawingRepository()
        repository.add(summerLottery(registrationEnds = drawDay.minusDays(1)))

        repository.drawDueLotteries(drawDay)

        val drawn = repository.get("lottery-pine-summer")!!
        assertEquals(LotteryStatus.CLOSED, drawn.stage)
        assertTrue(drawn.sentNotifications)
        assertEquals(2, drawn.waitingList.entrantWinners.size)
        val notifications = notificationRepository.getAll()
        assertEquals(5, notifications.size)
        assertEquals(2, notifications.count { it.type == NotificationType.LOTTERY_WON })
    }

    @Test
    fun drawDueLotteries_leavesLotteriesStillTakingRegistrations() = runTest {
        val repository = drawingRepository()
        repository.add(summerLottery(registrationEnds = drawDay))

        repository.drawDueLotteries(drawDay)

        assertEquals(LotteryStatus.OPEN, repository.get("lottery-pine-summer")!!.stage)
        assertTrue(notificationRepository.getAll().isEmpty())
    }

    @Test
    fun drawDueLotteries_calledTwice_drawsAndNotifiesOnlyOnce() = runTest {
        // Like two phones opening the app on draw day.
        val repository = drawingRepository()
        repository.add(summerLottery(registrationEnds = drawDay.minusDays(1)))

        repository.drawDueLotteries(drawDay)
        val firstWinners = repository.get("lottery-pine-summer")!!.waitingList.entrantWinners
        repository.drawDueLotteries(drawDay)

        assertEquals(firstWinners, repository.get("lottery-pine-summer")!!.waitingList.entrantWinners)
        assertEquals(5, notificationRepository.getAll().size)
    }

    @Test
    fun drawDueLotteries_withoutHandler_changesNothing() = runTest {
        lotteryRepository.add(summerLottery(registrationEnds = drawDay.minusDays(1)))

        assertFalse(lotteryRepository.drawDueLotteries(drawDay))
        assertEquals(LotteryStatus.OPEN, lotteryRepository.get("lottery-pine-summer")!!.stage)
    }
}

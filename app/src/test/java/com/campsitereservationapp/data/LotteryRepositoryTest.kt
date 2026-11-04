package com.campsitereservationapp.data

import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.WaitingList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/**
 * Unit tests for [LotteryRepository].
 *
 * **Purpose:** verifies that [LotteryRepository] correctly handles saving, updating,
 * and deleting lotteries, including campsite deletions via [LotteryRepository.deleteByCampsite].
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

    private fun openLottery(waitingList: WaitingList = WaitingList(), winnerAmount: Int = 1) =
        Lottery("lottery-moose", ownerId = testCampsite.ownerId,
            campsiteIds = listOf(testCampsite.id), registrationPeriod = registrationPeriod,
            reservationPeriod = reservationPeriod, stage = LotteryStatus.OPEN,
            winnerAmount = winnerAmount, waitingList = waitingList)

    @Test
    fun addEntrantOpen() {
        val joined = openLottery().withEntrant(entrantId)

        assertTrue(entrantId in joined!!.waitingList.entrantList)
    }

    @Test
    fun addEntrantNotOpen() {
        val draft = openLottery().copy(stage = LotteryStatus.DRAFT)

        assertNull(draft.withEntrant(entrantId))
    }

    @Test
    fun removeEntrantOpen() {
        val lottery = openLottery(WaitingList(entrantList = listOf(entrantId)))

        val left = lottery.withoutEntrant(entrantId)

        assertFalse(entrantId in left!!.waitingList.entrantList)
    }

    @Test
    fun removeEntrantNotOpen() {
        val closed = openLottery(WaitingList(entrantList = listOf(entrantId)))
            .copy(stage = LotteryStatus.CLOSED)

        assertNull(closed.withoutEntrant(entrantId))
    }

    @Test
    fun drawn_picksExactlyWinnerAmountDistinctWinners() {
        val entrants = listOf("entrant-priya", "entrant-noah", "entrant-ali", "entrant-ben", "entrant-sara")
        val lottery = openLottery(WaitingList(entrantList = entrants), winnerAmount = 2)
            .copy(stage = LotteryStatus.DRAWN)

        val drawn = lottery.drawn(Random(42))!!

        assertEquals(LotteryStatus.CLOSED, drawn.stage)
        assertEquals(2, drawn.waitingList.entrantWinners.toSet().size)
        assertEquals(3, drawn.waitingList.entrantLosers.size)
        assertEquals(entrants.toSet(), (drawn.waitingList.entrantWinners + drawn.waitingList.entrantLosers).toSet())
    }

    @Test
    fun drawn_withMoreSpotsThanEntrants_everyoneWins() {
        val lottery = openLottery(
            WaitingList(
                entrantList = listOf(
                    "entrant-priya",
                    "entrant-noah"
                )
            ), winnerAmount = 5)
            .copy(stage = LotteryStatus.DRAWN)

        val drawn = lottery.drawn()!!

        assertEquals(2, drawn.waitingList.entrantWinners.size)
        assertTrue(drawn.waitingList.entrantLosers.isEmpty())
    }

    @Test
    fun drawn_whenNotReadyToDraw_returnsNull() {
        assertNull(openLottery().drawn())
    }

    @Test
    fun markNotified_onlyOnce() {
        val closed = openLottery().copy(stage = LotteryStatus.CLOSED)

        val notified = closed.markNotified()!!

        assertTrue(notified.sentNotifications)
        assertNull(notified.markNotified())
    }

    private fun openLotteryWith(entrants: List<String>) = Lottery(
        id = "lottery-moose", ownerId = "owner-jill", campsiteIds = listOf("camp-alpha"),
        registrationPeriod = DateRange(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5)),
        reservationPeriod = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(8)),
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
}

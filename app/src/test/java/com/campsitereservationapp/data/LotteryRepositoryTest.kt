package com.campsitereservationapp.data

import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

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

    @Test
    fun deleteByCampsite_deletesLottery_whenOnlyThatCampsiteIsInLottery() = runTest {
        val lottery = Lottery(
            id = "lottery-101",
            ownerId = "owner-dana",
            campsiteIds = mutableListOf("camp-alpha"),
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
            campsiteIds = mutableListOf("camp-alpha", "camp-beta"),
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
            campsiteIds = mutableListOf("camp-alpha"),
            registrationPeriod = regPeriod,
            reservationPeriod = resPeriod
        )
        val lottery2 = Lottery(
            id = "lottery-2",
            ownerId = "owner-dana",
            campsiteIds = mutableListOf("camp-beta"),
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
}

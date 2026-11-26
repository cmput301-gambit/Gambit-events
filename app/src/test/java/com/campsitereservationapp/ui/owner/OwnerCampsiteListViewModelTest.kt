package com.campsitereservationapp.ui.owner

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.ImageRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MockDataSource
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [OwnerCampsiteListViewModel].
 *
 * **Purpose:** verifies that the owner campsite list view model correctly filters
 * campsites by owner ID and correctly computes active lottery status (`OPEN` only).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerCampsiteListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var campsiteRepository: CampsiteRepository
    private lateinit var lotteryRepository: LotteryRepository
    private lateinit var imageRepository: ImageRepository
    private lateinit var viewModel: OwnerCampsiteListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        campsiteRepository = CampsiteRepository(MockDataSource())
        lotteryRepository = LotteryRepository(MockDataSource(), campsiteRepository)
        imageRepository = ImageRepository(MockDataSource())
        viewModel = OwnerCampsiteListViewModel(campsiteRepository, lotteryRepository, imageRepository, "owner-bobby")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun listsOnlyCurrentOwnersCampsitesAndChecksActiveLotteryStatus() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }

        val bobbyCamp1 = Campsite(id = "camp-bobby-1", ownerId = "owner-bobby", name = "Bobby Camp 1")
        val bobbyCamp2 = Campsite(id = "camp-bobby-2", ownerId = "owner-bobby", name = "Bobby Camp 2")
        val bobbyCamp3 = Campsite(id = "camp-bobby-3", ownerId = "owner-bobby", name = "Bobby Camp 3")
        val jillCamp = Campsite(id = "camp-jill", ownerId = "owner-jill", name = "Jill Camp")

        campsiteRepository.add(bobbyCamp1)
        campsiteRepository.add(bobbyCamp2)
        campsiteRepository.add(bobbyCamp3)
        campsiteRepository.add(jillCamp)

        // Open lottery for camp-bobby-1 (should have hasActiveLottery = true)
        val openLottery = Lottery(
            id = "lottery-open",
            ownerId = "owner-bobby",
            campsiteIds = mutableListOf("camp-bobby-1"),
            registrationPeriod = DateRange(LocalDate.now(), LocalDate.now().plusDays(5)),
            reservationPeriod = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10)),
            stage = LotteryStatus.OPEN
        )
        // Draft lottery for camp-bobby-2 (should have hasActiveLottery = false, not OPEN)
        val draftLottery = Lottery(
            id = "lottery-draft",
            ownerId = "owner-bobby",
            campsiteIds = mutableListOf("camp-bobby-2"),
            registrationPeriod = DateRange(LocalDate.now(), LocalDate.now().plusDays(5)),
            reservationPeriod = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10)),
            stage = LotteryStatus.DRAFT
        )
        // Closed lottery for camp-bobby-3 (should have hasActiveLottery = false)
        val closedLottery = Lottery(
            id = "lottery-closed",
            ownerId = "owner-bobby",
            campsiteIds = mutableListOf("camp-bobby-3"),
            registrationPeriod = DateRange(LocalDate.now(), LocalDate.now().plusDays(5)),
            reservationPeriod = DateRange(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10)),
            stage = LotteryStatus.CLOSED
        )

        lotteryRepository.add(openLottery)
        lotteryRepository.add(draftLottery)
        lotteryRepository.add(closedLottery)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        // Should list only Bobby's 3 campsites (excluding Jill's camp)
        assertEquals(3, state.campsites.size)

        val item1 = state.campsites.first { it.campsite.id == "camp-bobby-1" }
        val item2 = state.campsites.first { it.campsite.id == "camp-bobby-2" }
        val item3 = state.campsites.first { it.campsite.id == "camp-bobby-3" }

        assertTrue(item1.hasActiveLottery)
        assertFalse(item2.hasActiveLottery)
        assertFalse(item3.hasActiveLottery)
    }

    @Test
    fun showsEachCampsitesUploadedPoster() = runTest {
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect {}
        }
        val posterData = Image.encode(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
        imageRepository.add(Image("image-pine-ridge", "owner-bobby", ImageKind.CAMPSITE, "camp-pine-ridge", posterData))
        campsiteRepository.add(
            Campsite(id = "camp-pine-ridge", ownerId = "owner-bobby", name = "Pine Ridge", posterImageId = "image-pine-ridge")
        )
        campsiteRepository.add(Campsite(id = "camp-bare-flats", ownerId = "owner-bobby", name = "Bare Flats"))
        testScheduler.advanceUntilIdle()

        val items = viewModel.uiState.value.campsites.associateBy { it.campsite.id }
        assertEquals(posterData, items.getValue("camp-pine-ridge").posterImage)
        assertNull(items.getValue("camp-bare-flats").posterImage)
    }
}

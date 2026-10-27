package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CampsiteRepository] and the shared [Repository] rules.
 *
 * **Purpose:** checks adding, updating, deleting, ID rules and owner queries.
 *
 * **Design rationale:** a fresh repository seeded from [SampleData] for
 * each test, so tests can't affect each other. runTest runs suspend code.
 *
 * **Outstanding issues:** None.
 */
class CampsiteRepositoryTest {

    private lateinit var repository: CampsiteRepository

    @Before
    fun setUp() {
        repository = CampsiteRepository()
    }

    private fun newCampsite(name: String = "Moose Meadow") = Campsite(
        id = repository.newId(),
        ownerId = "owner-jill",
        name = name,
        location = "Jasper, AB",
        capacity = 3,
    )

    @Test
    fun newId_givesDifferentIdsEachTime() {
        assertNotEquals(repository.newId(), repository.newId())
    }

    @Test
    fun add_thenGet_returnsTheCampsite() = runTest {
        val moose = newCampsite()

        repository.add(moose)

        assertEquals(moose, repository.get(moose.id))
    }

    @Test
    fun add_withBlankId_isRejected() = runTest {
        val error = runCatching { repository.add(Campsite(name = "No ID")) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun add_withExistingId_isRejected() = runTest {
        val copyOfPineRidge = Campsite(id = "campsite-pine-ridge", name = "Imposter")

        val error = runCatching { repository.add(copyOfPineRidge) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun update_replacesTheExistingCampsite() = runTest {
        val pineRidge = repository.get("campsite-pine-ridge")!!

        repository.update(pineRidge.copy(capacity = 10))

        assertEquals(10, repository.get("campsite-pine-ridge")?.capacity)
    }

    @Test
    fun update_whenCampsiteDoesNotExist_isRejected() = runTest {
        val error = runCatching { repository.update(newCampsite()) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun delete_removesTheCampsite() = runTest {
        repository.delete("campsite-aspen-hollow")

        assertNull(repository.get("campsite-aspen-hollow"))
    }

    @Test
    fun observeAll_includesNewlyAddedCampsites() = runTest {
        val moose = newCampsite()

        repository.add(moose)

        assertTrue(moose in repository.observeAll().first())
    }

    @Test
    fun getByOwner_returnsOnlyThatOwnersCampsites() = runTest {
        val names = repository.getByOwner("owner-bobby").map { it.name }

        assertEquals(setOf("Pine Ridge", "Lakeside Loop"), names.toSet())
    }

    @Test
    fun observeByOwner_updatesWhenTheOwnerAddsACampsite() = runTest {
        assertEquals(1, repository.observeByOwner("owner-jill").first().size)

        repository.add(newCampsite())

        assertEquals(2, repository.observeByOwner("owner-jill").first().size)
    }
}
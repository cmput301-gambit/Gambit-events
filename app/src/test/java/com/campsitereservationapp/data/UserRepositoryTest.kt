package com.campsitereservationapp.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [UserRepository].
 *
 * **Purpose:** checks that each device gets exactly one user, and that
 * role changes are saved and observed (US 01.09.01).
 *
 * **Design rationale:** a fresh repository backed by a [MockDataSource] seeded from [SampleData]. runTest runs suspend and Flow code.
 *
 * **Outstanding issues:** None.
 */
class UserRepositoryTest {

    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        repository = UserRepository(MockDataSource(SampleData.users))
    }

    @Test
    fun getOrCreate_newDevice_createsAndSavesAnEntrant() = runTest {
        val user = repository.getOrCreate("device-new")

        assertTrue(user.isEntrant)
        assertEquals(user, repository.get("device-new"))
    }

    @Test
    fun getOrCreate_calledTwice_returnsTheSameUser() = runTest {
        val first = repository.getOrCreate("device-new")
        val usersAfterFirst = repository.getAll().size

        val second = repository.getOrCreate("device-new")

        assertEquals(first, second)
        assertEquals(usersAfterFirst, repository.getAll().size)
    }

    @Test
    fun getOrCreate_existingDevice_returnsTheSavedUser() = runTest {
        val bobby = repository.getOrCreate("owner-bobby")

        assertEquals("Bobby Smith", bobby.name)
        assertTrue(bobby.isOwner)
    }

    @Test
    fun observe_seesRoleChanges() = runTest {
        val priya = repository.getOrCreate("entrant-priya")

        repository.update(priya.withOwnerRole())

        assertTrue(repository.observe("entrant-priya").first()?.isOwner == true)
    }
}
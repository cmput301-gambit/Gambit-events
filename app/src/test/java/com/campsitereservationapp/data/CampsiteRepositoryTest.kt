package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampsiteRepositoryTest {

    @Test(expected = IllegalArgumentException::class)
    fun blankId_isRejectedOnAdd() = runTest {
        val repository = CampsiteRepository()
        repository.add(Campsite(id = "", name = "New Site", ownerId = "owner-bobby", capacity = 3))
    }

    @Test
    fun newId_createsUniqueId() = runTest {
        val repository = CampsiteRepository()
        val id1 = repository.newId()
        val id2 = repository.newId()
        assertNotNull(id1)
        assertTrue(id1.isNotBlank())
        assertNotNull(id2)
        assertTrue(id2.isNotBlank())
        assertTrue(id1 != id2)
    }

    @Test
    fun givenId_isKeptOnAdd() = runTest {
        val repository = CampsiteRepository()
        val added = repository.add(Campsite(id = "custom-id-123", name = "Custom Site", ownerId = "owner-bobby", capacity = 3))

        assertEquals("custom-id-123", added.id)
        assertEquals(added, repository.get("custom-id-123"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateId_isRejectedOnAdd() = runTest {
        val repository = CampsiteRepository()
        val site = Campsite(id = "site-1", name = "Site 1", ownerId = "owner-bobby", capacity = 3)
        repository.add(site)
        repository.add(site)
    }

    @Test
    fun update_replacesExistingItem() = runTest {
        val repository = CampsiteRepository()
        val site = Campsite(id = "site-update", name = "Old Name", ownerId = "owner-bobby", capacity = 3)
        repository.add(site)

        val updatedSite = site.copy(name = "New Name")
        repository.update(updatedSite)

        assertEquals("New Name", repository.get("site-update")?.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun update_unknownId_fails() = runTest {
        val repository = CampsiteRepository()
        val site = Campsite(id = "unknown", name = "Unknown", ownerId = "owner-bobby", capacity = 3)
        repository.update(site)
    }

    @Test
    fun getByOwner_returnsOnlyOwnerCampsites() = runTest {
        val repository = CampsiteRepository()
        val bobbySites = repository.getByOwner("owner-bobby")
        assertTrue(bobbySites.isNotEmpty())
        assertTrue(bobbySites.all { it.ownerId == "owner-bobby" })
    }
}

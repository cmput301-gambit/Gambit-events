package com.campsitereservationapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [Campsite] validation and [CampsiteOwner.owns].
 *
 * **Purpose:** checks which campsites can be saved, and campsite ownership.
 *
 * **Design rationale:** plain JUnit tests with realistic campsite data;
 * Campsite has no Android code, so no device is needed.
 *
 * **Outstanding issues:** None.
 */
class CampsiteTest {

    @Test
    fun validCampsite_hasNoErrors() {
        val campsite = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 5)
        assertTrue(campsite.isValid())
        assertTrue(campsite.validate().isEmpty())
    }

    @Test
    fun blankName_returnsError() {
        val campsite = Campsite(name = "", ownerId = "owner-bobby", capacity = 5)
        assertFalse(campsite.isValid())
        assertTrue(campsite.validate().contains(CampsiteError.BLANK_NAME))
    }

    @Test
    fun blankOwner_returnsError() {
        val campsite = Campsite(name = "Pine Ridge", ownerId = "", capacity = 5)
        assertFalse(campsite.isValid())
        assertTrue(campsite.validate().contains(CampsiteError.BLANK_OWNER))
    }

    @Test
    fun nonPositiveCapacity_returnsError() {
        val campsite1 = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 0)
        val campsite2 = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = -1)
        assertFalse(campsite1.isValid())
        assertFalse(campsite2.isValid())
        assertTrue(campsite1.validate().contains(CampsiteError.NON_POSITIVE_CAPACITY))
        assertTrue(campsite2.validate().contains(CampsiteError.NON_POSITIVE_CAPACITY))
    }

    @Test
    fun invalidLatitude_returnsError() {
        val campsiteHigh = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 5, latitude = 91.0, longitude = 0.0)
        val campsiteLow = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 5, latitude = -91.0, longitude = 0.0)
        assertFalse(campsiteHigh.isValid())
        assertFalse(campsiteLow.isValid())
        assertTrue(campsiteHigh.validate().contains(CampsiteError.INVALID_LATITUDE))
        assertTrue(campsiteLow.validate().contains(CampsiteError.INVALID_LATITUDE))
    }

    @Test
    fun invalidLongitude_returnsError() {
        val campsiteHigh = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 5, latitude = 0.0, longitude = 181.0)
        val campsiteLow = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 5, latitude = 0.0, longitude = -181.0)
        assertFalse(campsiteHigh.isValid())
        assertFalse(campsiteLow.isValid())
        assertTrue(campsiteHigh.validate().contains(CampsiteError.INVALID_LONGITUDE))
        assertTrue(campsiteLow.validate().contains(CampsiteError.INVALID_LONGITUDE))
    }

    @Test
    fun mismatchedLatLon_returnsError() {
        val campsiteLatOnly = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 5, latitude = 10.0, longitude = null)
        val campsiteLonOnly = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 5, latitude = null, longitude = 10.0)
        assertFalse(campsiteLatOnly.isValid())
        assertFalse(campsiteLonOnly.isValid())
        assertTrue(campsiteLatOnly.validate().contains(CampsiteError.INVALID_LONGITUDE))
        assertTrue(campsiteLonOnly.validate().contains(CampsiteError.INVALID_LATITUDE))
    }

    @Test
    fun blankTag_returnsError() {
        val campsite = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 5, tags = listOf("family", ""))
        assertFalse(campsite.isValid())
        assertTrue(campsite.validate().contains(CampsiteError.BLANK_TAG))
    }

    @Test
    fun campsite_withAvailabilityDates_storesAndExposesCorrectly() {
        val start = LocalDate.of(2026, 6, 1)
        val finish = LocalDate.of(2026, 6, 7)
        val range = DateRange(start, finish)

        val campsite = Campsite(name = "Pine Ridge", ownerId = "owner-bobby", capacity = 5, availability = range)
        assertNotNull(campsite.availability)
        assertEquals(range, campsite.availability)
        assertTrue(campsite.isValid())
    }

    @Test
    fun owner_ownsOnlyTheirOwnCampsites() {
        val moose = Campsite(id = "campsite-moose-meadow", name = "Moose Meadow", ownerId = "owner-jill", capacity = 3)
        val jill = CampsiteOwner("owner-jill")
        val bobby = CampsiteOwner("owner-bobby")

        assertTrue(jill.owns(moose))
        assertFalse(bobby.owns(moose))
    }
}

package com.campsitereservationapp.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}

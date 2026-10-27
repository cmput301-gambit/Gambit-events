package com.campsitereservationapp.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Campsite] validation and [CampsiteOwner.owns].
 *
 * **Purpose:** checks which campsites can be saved, and campsite ownership.
 *
 * **Design rationale:** plain JUnit tests with realistic campsite data.
 *
 * **Outstanding issues:** None.
 */
class CampsiteTest {

    private val moose = Campsite(
        id = "campsite-moose-meadow",
        ownerId = "owner-jill",
        name = "Moose Meadow",
        location = "Jasper, AB",
        capacity = 3,
    )

    @Test
    fun completeCampsite_isValid() {
        assertTrue(moose.isValid())
    }

    @Test
    fun blankName_isNotValid() {
        assertFalse(moose.copy(name = "   ").isValid())
    }

    @Test
    fun zeroCapacity_isNotValid() {
        assertFalse(moose.copy(capacity = 0).isValid())
    }

    @Test
    fun owner_ownsOnlyTheirOwnCampsites() {
        val jill = CampsiteOwner("owner-jill")
        val bobby = CampsiteOwner("owner-bobby")

        assertTrue(jill.owns(moose))
        assertFalse(bobby.owns(moose))
    }
}
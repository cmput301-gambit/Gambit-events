package com.campsitereservationapp.model

import com.campsitereservationapp.data.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [User] and how its roles are composed.
 *
 * **Purpose:** checks new users, adding roles, and profile updates
 * (US 01.09.01, 01.07.03, 03.11.01).
 *
 * **Design rationale:** plain JUnit tests; User has no Android code.
 *
 * **Outstanding issues:** None.
 */
class UserTest {

    @Test
    fun newEntrant_hasOnlyTheEntrantRole() {
        val user = User.newEntrant("device-123")

        assertEquals("device-123", user.id)
        assertTrue(user.isEntrant)
        assertFalse(user.isOwner)
        assertFalse(user.isAdmin)
        assertEquals("device-123", user.entrant?.userId)
    }

    @Test
    fun withOwnerRole_addsOwnerAndKeepsEntrant() {
        val user = User.newEntrant("device-123").withOwnerRole()

        assertTrue(user.isOwner)
        assertTrue(user.isEntrant)
        assertEquals("device-123", user.owner?.userId)
    }

    @Test
    fun withOwnerRole_whenAlreadyOwner_returnsSameUser() {
        val bobby = SampleData.users.first { it.id == "owner-bobby" }

        assertSame(bobby, bobby.withOwnerRole())
    }

    @Test
    fun adminCanAlsoBeEntrant_onOneProfile() {
        val sam = SampleData.users.first { it.id == "admin-sam" }

        assertTrue(sam.isAdmin)
        assertTrue(sam.isEntrant)
        assertFalse(sam.isOwner)
    }

    @Test
    fun withProfile_trimsExtraSpaces() {
        val user = User.newEntrant("device-123")
            .withProfile(name = "  Priya Shah ", email = "priya@example.com ", phone = " 780-555-0199")

        assertEquals("Priya Shah", user.name)
        assertEquals("priya@example.com", user.email)
        assertEquals("780-555-0199", user.phone)
    }
}
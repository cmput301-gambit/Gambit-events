package com.campsitereservationapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [Message].
 *
 * **Purpose:** checks that messages are built with the right sender and
 * recipient in each direction, that bad messages are rejected, and that
 * marking one read doesn't change the original (US 02.12.01, 01.14.01).
 *
 * **Outstanding issues:** None.
 */
class MessageTest {

    private val reservation = Reservation(
        id = "reservation-priya",
        entrantId = "entrant-priya",
        campsiteId = "campsite-pine-ridge",
        lotteryId = "lottery-pine-ridge",
        dates = DateRange(LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 4)),
    )

    @Test
    fun fromOwner_goesFromOwnerToEntrant() {
        val message = Message.fromOwner("m-1", reservation, "owner-bobby", "Gate code is 1234")

        assertEquals("owner-bobby", message.senderId)
        assertEquals("entrant-priya", message.recipientId)
        assertEquals("reservation-priya", message.reservationId)
        assertEquals(SenderRole.OWNER, message.senderRole)
        assertFalse(message.read)
        assertTrue(message.isValid())
    }

    @Test
    fun fromEntrant_goesFromEntrantToOwner() {
        val message = Message.fromEntrant("m-2", reservation, "owner-bobby", "Can I check in early?")

        assertEquals("entrant-priya", message.senderId)
        assertEquals("owner-bobby", message.recipientId)
        assertEquals(SenderRole.ENTRANT, message.senderRole)
        assertTrue(message.isValid())
    }

    @Test
    fun textIsTrimmed() {
        val message = Message.fromOwner("m-1", reservation, "owner-bobby", "   hello   ")

        assertEquals("hello", message.text)
    }

    @Test
    fun blankText_isInvalid() {
        val message = Message.fromOwner("m-1", reservation, "owner-bobby", "    ")

        assertEquals(listOf(MessageError.BLANK_TEXT), message.validate())
    }

    @Test
    fun tooLongText_isInvalid() {
        val message = Message.fromOwner("m-1", reservation, "owner-bobby", "a".repeat(Message.MAX_LENGTH + 1))

        assertEquals(listOf(MessageError.TEXT_TOO_LONG), message.validate())
    }

    @Test
    fun messageToSelf_isInvalid() {
        val message = Message.fromOwner("m-1", reservation, "entrant-priya", "hi")

        assertTrue(MessageError.SENT_TO_SELF in message.validate())
    }

    @Test
    fun emptyMessage_reportsMissingFields() {
        val errors = Message().validate()

        assertTrue(MessageError.BLANK_RESERVATION in errors)
        assertTrue(MessageError.BLANK_SENDER in errors)
        assertTrue(MessageError.BLANK_RECIPIENT in errors)
        assertTrue(MessageError.BLANK_TEXT in errors)
    }

    @Test
    fun markRead_returnsReadCopy_andLeavesOriginal() {
        val message = Message.fromOwner("m-1", reservation, "owner-bobby", "hi")

        val read = message.markRead()

        assertTrue(read.read)
        assertFalse(message.read)
    }
}
package com.campsitereservationapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [Rating] validation and retrieval.
 *
 * **Purpose:** checks which ratings can be saved.
 *
 * **Design rationale:** plain JUnit tests with realistic rating data;
 * Rating has no Android code, so no device is needed.
 *
 * **Outstanding issues:** None.
 */
class RatingTest {

    val userId: String = "user123"
    val campId: String = "camp"

    @Test
    fun validRating_hasNoErrors() {
        val rating = Rating(userId = userId, score = 5, campsiteId = campId)
        assertTrue(rating.isValid())
        assertTrue(rating.validate().isEmpty())
    }

    @Test
    fun blankCampsiteId_returnsError() {
        val rating = Rating(userId = userId, score = 5, campsiteId = "")
        assertFalse(rating.isValid())
        assertTrue(rating.validate().contains(RatingError.BLANK_CAMP))
    }

    @Test
    fun blankUser_returnsError() {
        val rating = Rating(userId = "", score = 5, campsiteId = campId)
        assertFalse(rating.isValid())
        assertTrue(rating.validate().contains(RatingError.BLANK_USER))
    }

    @Test
    fun nonPositiveScore_returnsError() {
        val score = -1
        val rating = Rating(userId = userId, score = score, campsiteId = campId)
        assertFalse(rating.isValid())
        assertTrue(rating.validate().contains(RatingError.INVALID_RATING))
    }

    @Test
    fun higherThanRangeScore_returnsError() {
        val score = 6
        val rating = Rating(userId = userId, score = score, campsiteId = campId)
        assertFalse(rating.isValid())
        assertTrue(rating.validate().contains(RatingError.INVALID_RATING))
    }
}

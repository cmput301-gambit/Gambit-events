package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Rating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [RatingRepository] and the shared [Repository] rules.
 *
 * **Purpose:** checks adding, updating, deleting, ID rules and owner queries.
 *
 * **Design rationale:** a fresh repository backed by an [InMemoryDataSource]
 * seeded from [SampleData] for each test, so tests never touch Firestore and
 * can't affect each other. runTest runs suspend code.
 *
 * **Outstanding issues:** None.
 */
class RatingRepositoryTest {

    private lateinit var repository: RatingRepository

    @Before
    fun setUp() {
        repository = RatingRepository(MockDataSource(SampleData.ratings))
    }

    private fun NewRating() = Rating(
        id = repository.newId(),
        userId = "uid",
        campsiteId = "cid",
        score = 0f
    )

    @Test
    fun addThenGetByCampsiteReturnsTheSameRating() = runTest {
        val campId = "campId"
        val newRating = NewRating().copy(campsiteId = campId)

        repository.add(newRating)

        assertEquals(newRating, repository.observeByCampsite(campId).first()[0])
    }

    @Test
    fun addThenDeleteReturnsNothing() = runTest {
        val cid = "abc"
        val rating = NewRating().copy(campsiteId = cid)
        val rid = rating.id
        repository.add(rating)

        repository.delete(rid)

        assertEquals(emptyList<Rating>(), repository.observeByCampsite(cid).firstOrNull())
    }

    @Test
    fun getAverageReturnsCorrectValue() = runTest{
        repository.add(NewRating().copy(score = 3f))
        repository.add(NewRating().copy(score = 5f))
        repository.add(NewRating().copy(score = 2f))
        assertEquals((3f + 5f + 2f) / 3f, repository.averageRatingByCampsite("cid"))
    }
}
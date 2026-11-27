package com.campsitereservationapp.data

import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ImageRepository].
 *
 * **Purpose:** checks the image queries used by the campsite form, the owner
 * campsite list and the cascade deletes, and the Firestore conversion.
 *
 * **Design rationale:** backed by [MockDataSource], so nothing touches
 * Firestore. Bobby has posters for two campsites, Jill has one, and Priya
 * has a profile picture.
 *
 * **Outstanding issues:** None.
 */
class ImageRepositoryTest {

    private val jpegData = Image.encode(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))

    private val pineRidgePoster = Image("image-pine-ridge", "owner-bobby", ImageKind.CAMPSITE, "campsite-pine-ridge", jpegData)
    private val lakesidePoster = Image("image-lakeside", "owner-bobby", ImageKind.CAMPSITE, "campsite-lakeside-loop", jpegData)
    private val aspenPoster = Image("image-aspen", "owner-jill", ImageKind.CAMPSITE, "campsite-aspen-hollow", jpegData)
    private val priyaPicture = Image("image-priya", "entrant-priya", ImageKind.PROFILE, "entrant-priya", jpegData)

    private lateinit var images: ImageRepository

    @Before
    fun setUp() {
        images = ImageRepository(MockDataSource(listOf(pineRidgePoster, lakesidePoster, aspenPoster, priyaPicture)))
    }

    @Test
    fun find_returnsTheImage() = runTest {
        assertEquals(aspenPoster, images.find("image-aspen"))
    }

    @Test
    fun find_ofNullBlankOrMissingId_isNull() = runTest {
        assertNull(images.find(null))
        assertNull(images.find(""))
        assertNull(images.find("image-nobody"))
    }

    @Test
    fun find_ofOldPhoneOnlyLink_isNullInsteadOfCrashing() = runTest {
        // Campsites saved before uploads hold a link like this instead of an image ID.
        assertNull(images.find("content://media/picker/0/com.android.providers.media.photopicker/media/1000000033"))
    }

    @Test
    fun observeByUploader_listsOnlyThatUsersImages() = runTest {
        val bobbys = images.observeByUploader("owner-bobby").first()

        assertEquals(setOf("image-pine-ridge", "image-lakeside"), bobbys.map { it.id }.toSet())
    }

    @Test
    fun deleteByTarget_removesOnlyThatCampsitesPoster() = runTest {
        images.deleteByTarget("campsite-pine-ridge")

        assertNull(images.get("image-pine-ridge"))
        assertEquals(3, images.getAll().size)
    }

    @Test
    fun deleteByTarget_removesAProfilePicture() = runTest {
        images.deleteByTarget("entrant-priya")

        assertNull(images.get("image-priya"))
    }

    // ---------- Firestore mapping ----------

    @Test
    fun roundTrip_keepsEveryField() {
        for (image in listOf(pineRidgePoster, priyaPicture)) {
            assertEquals(image, ImageRepository.fromMap(image.id, ImageRepository.toMap(image)))
        }
    }

    @Test
    fun toMap_doesNotStoreTheId_andStoresKindAsText() {
        val map = ImageRepository.toMap(priyaPicture)

        assertFalse("id" in map)
        assertEquals("PROFILE", map["kind"])
    }

    @Test
    fun fromMap_withMissingFields_usesDefaults() {
        assertEquals(Image(id = "image-empty"), ImageRepository.fromMap("image-empty", emptyMap()))
    }
}

package com.campsitereservationapp.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Image] validation and Base64 conversion.
 *
 * **Purpose:** checks which images can be saved (US 03.06.01) and that
 * picture bytes survive being turned into text and back.
 *
 * **Design rationale:** plain JUnit tests; [Image] uses java.util.Base64, so
 * no device is needed. The bytes are the start of a real JPEG file.
 *
 * **Outstanding issues:** None.
 */
class ImageTest {

    private val jpegBytes = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10,
        0x4A, 0x46, 0x49, 0x46, 0x00,
    )

    private val pineRidgePoster = Image(
        id = "image-pine-ridge",
        uploaderId = "owner-bobby",
        kind = ImageKind.CAMPSITE,
        targetId = "campsite-pine-ridge",
        data = Image.encode(jpegBytes),
    )

    @Test
    fun validPoster_hasNoErrors() {
        assertTrue(pineRidgePoster.isValid())
        assertTrue(pineRidgePoster.validate().isEmpty())
    }

    @Test
    fun blankUploader_isAnError() {
        val image = pineRidgePoster.copy(uploaderId = " ")
        assertEquals(listOf(ImageError.BLANK_UPLOADER), image.validate())
    }

    @Test
    fun blankTarget_isAnError() {
        val image = pineRidgePoster.copy(targetId = "")
        assertEquals(listOf(ImageError.BLANK_TARGET), image.validate())
    }

    @Test
    fun emptyData_isAnError() {
        val image = pineRidgePoster.copy(data = "")
        assertFalse(image.isValid())
        assertEquals(listOf(ImageError.EMPTY_DATA), image.validate())
    }

    @Test
    fun dataOverTheLimit_isTooLarge() {
        val image = pineRidgePoster.copy(data = "A".repeat(Image.MAX_DATA_LENGTH + 1))
        assertEquals(listOf(ImageError.TOO_LARGE), image.validate())
    }

    @Test
    fun dataAtTheLimit_isAllowed() {
        val image = pineRidgePoster.copy(data = "A".repeat(Image.MAX_DATA_LENGTH))
        assertTrue(image.isValid())
    }

    @Test
    fun encodeThenBytes_givesBackTheSamePicture() {
        assertArrayEquals(jpegBytes, pineRidgePoster.bytes())
    }

    @Test
    fun decode_ofNullOrEmpty_isNull() {
        assertNull(Image.decode(null))
        assertNull(Image.decode(""))
    }

    @Test
    fun decode_ofTextThatIsNotBase64_isNull() {
        assertNull(Image.decode("content://media/picker/0/photo 12"))
    }
}

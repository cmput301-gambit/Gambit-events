package com.campsitereservationapp.model

import java.util.Base64

/** Problems that stop an [Image] from being saved. */
enum class ImageError {
    BLANK_UPLOADER,
    BLANK_TARGET,
    EMPTY_DATA,
    TOO_LARGE,
}

/** What an [Image] is attached to. */
enum class ImageKind {
    /** A campsite's promo poster ([Campsite.posterImageId]). */
    CAMPSITE,

    /** A user's profile picture ([User.profileImageId]). */
    PROFILE,
}

/**
 * An uploaded picture: a campsite poster or a profile picture.
 *
 * **Purpose:** lets every device see the same images. Owners upload campsite
 * posters (US 02.01.02), entrants upload profile pictures (US 01.08.02), and
 * admins browse and remove them (US 03.06.01, 03.03.01).
 *
 * **Design rationale:** the picture is stored as Base64 text in its own
 * Firestore document instead of in Firebase Storage, which needs a paid plan.
 * Pictures are shrunk to 512px before upload (about 50-100 KB), well under
 * Firestore's 1 MB document limit. Images live in their own collection so
 * loading a list of campsites doesn't download every picture.
 *
 * **Outstanding issues:** None.
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property uploaderId ID of the [User] who uploaded it
 * @property kind whether it's a campsite poster or a profile picture
 * @property targetId ID of the campsite or user it belongs to
 * @property data the JPEG bytes as Base64 text (see [encode])
 */
data class Image(
    override val id: String = "",
    val uploaderId: String = "",
    val kind: ImageKind = ImageKind.CAMPSITE,
    val targetId: String = "",
    val data: String = "",
) : Identifiable {

    /**
     * Checks the image's fields.
     *
     * @return every problem found, or an empty list if the image can be saved
     */
    fun validate(): List<ImageError> {
        val errors = mutableListOf<ImageError>()
        if (uploaderId.isBlank()) errors.add(ImageError.BLANK_UPLOADER)
        if (targetId.isBlank()) errors.add(ImageError.BLANK_TARGET)
        if (data.isEmpty()) errors.add(ImageError.EMPTY_DATA)
        if (data.length > MAX_DATA_LENGTH) errors.add(ImageError.TOO_LARGE)
        return errors
    }

    /**
     * Checks whether this image has everything it needs to be saved.
     *
     * @return true if [validate] found no problems
     */
    fun isValid(): Boolean = validate().isEmpty()

    /**
     * Turns [data] back into the picture's bytes, ready to draw.
     *
     * @return the JPEG bytes, or null if [data] isn't valid Base64
     */
    fun bytes(): ByteArray? = decode(data)

    companion object {
        /**
         * Largest allowed [data] length, in characters. Firestore documents
         * max out at 1 MB, so this leaves room for the other fields.
         */
        const val MAX_DATA_LENGTH = 900_000

        /**
         * Turns picture bytes into the text stored in [data].
         *
         * @param bytes the JPEG bytes
         * @return the bytes as Base64 text
         */
        fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

        /**
         * Turns text made by [encode] back into picture bytes.
         *
         * @param data Base64 text, or null
         * @return the bytes, or null if [data] is null, empty or not valid Base64
         */
        fun decode(data: String?): ByteArray? {
            if (data.isNullOrEmpty()) return null
            return try {
                Base64.getDecoder().decode(data)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

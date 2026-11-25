package com.campsitereservationapp.model

enum class RatingError {
    BLANK_CAMP,
    BLANK_USER,
    INVALID_RATING,
}
/**
 * A data class to represent a users rating of a campsite as a value.
 *
 * **Purpose:** the data behind win and lose notifications
 * (US 01.13.01).
 *
 * **Design rationale:** plain data class with no insides.
 *
 * **Outstanding issues:**
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property userId ID of the [User] who made this rating
 * @property campsiteId ID of the campsite this rating is discussing
 * @property score Integer representing the score the user gave to the campsite
 */
data class Rating(
    override val id: String = "",
    val userId: String = "",
    val campsiteId: String = "",
    val score: Float = 0f,
) : Identifiable {

/**
 * Validates the rating fields and returns a list of errors found.
 *
 * @return list of [RatingError] violations
 */
    fun validate(): List<RatingError> {
        val errors = mutableListOf<RatingError>()
        if (campsiteId.isBlank()) errors.add(RatingError.BLANK_CAMP)
        if (userId.isBlank()) errors.add(RatingError.BLANK_USER)
        if (score < 0f || score > 5f) errors.add(RatingError.INVALID_RATING)

        return errors
    }

    /**
     * Checks whether this rating has everything it needs to be saved.
     *
     * @return true if the campsite has no validation errors, false otherwise
     */
    fun isValid(): Boolean {
        return validate().isEmpty()
    }

}
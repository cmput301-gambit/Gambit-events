package com.campsitereservationapp.model

enum class RatingError {
    BLANK_CAMP,
    BLANK_USER,
    INVALID_RATING,
    REVIEW_TOO_LONG,
}
/**
 * A data class to represent a users rating of a campsite as a value.
 *
 * **Purpose:** one entrant's star score for one campsite, used for the
 * campsite averages (US 01.13.01) and removable by admins (US 03.09.01),
 * with an optional written review that other campers can read (US 01.13.02).
 *
 * **Design rationale:** plain data class with no insides. A review is part of
 * the rating rather than its own model, like a product review: stars are
 * required and the text is optional, so one rating per user and campsite
 * also means one review.
 *
 * **Outstanding issues:** None.
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property userId ID of the [User] who made this rating
 * @property campsiteId ID of the campsite this rating is discussing
 * @property score the number of stars the user gave the campsite, from 0 to 5
 * @property review the user's written review, or blank if they only gave stars
 */
data class Rating(
    override val id: String = "",
    val userId: String = "",
    val campsiteId: String = "",
    val score: Float = 0f,
    val review: String = "",
) : Identifiable {

    /** True if the user wrote something along with their stars (US 01.13.02). */
    val hasReview: Boolean get() = review.isNotBlank()

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
        if (review.length > MAX_REVIEW_LENGTH) errors.add(RatingError.REVIEW_TOO_LONG)

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

    companion object {
        /** The longest written review allowed, in characters (US 01.13.02). */
        const val MAX_REVIEW_LENGTH = 500
    }
}

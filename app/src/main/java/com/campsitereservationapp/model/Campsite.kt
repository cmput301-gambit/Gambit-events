package com.campsitereservationapp.model

/**
 * Errors that can occur when validating a [Campsite].
 */
enum class CampsiteError {
    BLANK_NAME,
    BLANK_OWNER,
    NON_POSITIVE_CAPACITY,
    CAPACITY_NOT_A_NUMBER,
    INVALID_LATITUDE,
    INVALID_LONGITUDE,
    BLANK_TAG,
    INVALID_AVAILABILITY
}

/**
 * A campsite that entrants can join lotteries for.
 *
 * **Purpose:** a [CampsiteOwner] creates a campsite, then runs one or more
 * [Lottery] draws for it. Entrants join a lottery's [WaitingList], and
 * winners receive a [Reservation].
 *
 * **Design rationale:** plain data class with no Firestore code, so it's
 * easy to unit test. CampsiteRepository handles saving and loading.
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property ownerId ID of the [CampsiteOwner] who listed this campsite
 * @property name display name shown to entrants
 * @property description longer text describing the campsite (amenities)
 * @property location where the campsite is, as entered by the owner
 * @property capacity maximum number of reservations per lottery
 * @property posterImageId ID of the poster [Image], or null if none
 * @property tags list of tags associated with the campsite
 * @property latitude latitude coordinate of the campsite, or null
 * @property longitude longitude coordinate of the campsite, or null
 * @property availability range of dates available for reservation (US 02.01.02 / #76)
 * @constructor Creates a campsite. Every field has a default, so tests can
 * set only the fields they care about.
 */
data class Campsite(
    override val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val capacity: Int = 0,
    val posterImageId: String? = null,
    val tags: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val availability: DateRange? = null,
) : Identifiable {

    /**
     * Validates the campsite fields and returns a list of errors found.
     *
     * @return list of [CampsiteError] violations
     */
    fun validate(): List<CampsiteError> {
        val errors = mutableListOf<CampsiteError>()
        if (name.isBlank()) errors.add(CampsiteError.BLANK_NAME)
        if (ownerId.isBlank()) errors.add(CampsiteError.BLANK_OWNER)
        if (capacity <= 0) errors.add(CampsiteError.NON_POSITIVE_CAPACITY)

        if ((latitude != null) != (longitude != null)) {
            if (latitude == null) errors.add(CampsiteError.INVALID_LATITUDE)
            if (longitude == null) errors.add(CampsiteError.INVALID_LONGITUDE)
        }
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            errors.add(CampsiteError.INVALID_LATITUDE)
        }
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            errors.add(CampsiteError.INVALID_LONGITUDE)
        }
        if (tags.any { it.isBlank() }) {
            errors.add(CampsiteError.BLANK_TAG)
        }

        if (availability != null && availability.finish.isBefore(availability.start)) {
            errors.add(CampsiteError.INVALID_AVAILABILITY)
        }

        return errors
    }

    /**
     * Checks whether this campsite has everything it needs to be saved.
     *
     * @return true if the campsite has no validation errors, false otherwise
     */
    fun isValid(): Boolean {
        return validate().isEmpty()
    }
}

package com.campsitereservationapp.model

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


/**
 * Errors that can occur when validating a [Reservation].
 */
enum class ReservationError {
    BLANK_ENTRANT,
    BLANK_CAMPSITE,
    BLANK_LOTTERY,
}

/**
 * Whether a reservation still holds its dates.
 */
enum class ReservationStatus {
    CONFIRMED,
    CANCELLED,
}

/**
 * A lottery winner's booking of a campsite for a range of dates.
 *
 * **Purpose:** the data behind reserving and confirming dates after winning
 * a lottery (US 01.04.01, 01.05.01), and behind viewing and cancelling
 * reservations (US 01.06.01, 01.07.01).
 *
 * **Design rationale:** immutable data class like [Lottery] and [Campsite].
 * Changes return a new copy, or null if the change isn't allowed, so
 * ReservationRepository can save them safely. It refers to the entrant,
 * campsite and lottery by ID, so each piece of data has one source of truth.
 * Dates include the first and last day, the same as [DateRange].
 *
 * **Outstanding issues:** none.
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property entrantId userId of the entrant who made the reservation
 * @property campsiteId ID of the reserved [Campsite]
 * @property lotteryId ID of the [Lottery] the entrant won
 * @property dates the reserved dates, first and last day included
 * @property status whether the reservation is confirmed or cancelled
 * @property createdAt when the reservation was made, to the second. Shown on the confirmation screen
 */
data class Reservation(
    override val id: String = "",
    val entrantId: String = "",
    val campsiteId: String = "",
    val lotteryId: String = "",
    val dates: DateRange,
    val status: ReservationStatus = ReservationStatus.CONFIRMED,
    val createdAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
) : Identifiable {

    /** True while the reservation still holds its dates. */
    val isActive: Boolean get() = status == ReservationStatus.CONFIRMED

    /**
     * Validates the reservation fields and returns a list of errors found.
     *
     * @return list of [ReservationError] violations
     */
    fun validate(): List<ReservationError> {
        val errors = mutableListOf<ReservationError>()
        if (entrantId.isBlank()) errors.add(ReservationError.BLANK_ENTRANT)
        if (campsiteId.isBlank()) errors.add(ReservationError.BLANK_CAMPSITE)
        if (lotteryId.isBlank()) errors.add(ReservationError.BLANK_LOTTERY)
        return errors
    }

    /**
     * Checks whether this reservation has everything it needs to be saved.
     *
     * @return true if the reservation has no validation errors, false otherwise
     */
    fun isValid(): Boolean = validate().isEmpty()

    /**
     * Returns a cancelled copy of this reservation, which frees its dates.
     *
     * @return the cancelled reservation, or null if it was already cancelled
     */
    fun cancelled(): Reservation? =
        if (isActive) copy(status = ReservationStatus.CANCELLED) else null

    /**
     * Checks whether this reservation and [other] book the same campsite on
     * at least one of the same days. Cancelled reservations never clash,
     * since they no longer hold their dates.
     *
     * @param other the reservation to compare with
     * @return true if both are active, for the same campsite, and their dates overlap
     */
    fun clashesWith(other: Reservation): Boolean =
        isActive && other.isActive &&
                campsiteId == other.campsiteId &&
                !dates.finish.isBefore(other.dates.start) &&
                !other.dates.finish.isBefore(dates.start)
}
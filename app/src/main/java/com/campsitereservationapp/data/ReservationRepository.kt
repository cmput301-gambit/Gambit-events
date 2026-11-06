package com.campsitereservationapp.data

import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Reservation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.campsitereservationapp.model.ReservationStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Stores and provides [Reservation] objects.
 *
 * **Purpose:** lets lottery winners reserve dates without double-booking
 * (US 01.05.01), shows which dates are taken (US 01.04.01), and lets
 * entrants view and cancel their reservations (US 01.06.01, 01.07.01).
 *
 * **Design rationale:** extends [Repository] like every other repository.
 * Booking goes through [reserve], which refuses dates that clash with an
 * active reservation for the same campsite. Cancelling goes through modify,
 * so the read, change and save happen as one step.
 *
 * **Outstanding issues:** the clash check in [reserve] reads and then adds
 * in two steps, so two devices booking overlapping dates at the same moment
 * could both succeed. Firestore transactions can't run queries, so fixing
 * this needs a lock document per campsite, which is a ton of additional work.
 *
 * @param dataSource where reservations are kept, such as [firestoreDataSource]
 * or a [MockDataSource] in tests
 *
 * @param dataSource where reservations are kept, such as a [MockDataSource]
 */
class ReservationRepository(dataSource: DataSource<Reservation>) :
    Repository<Reservation>(dataSource) {

    /**
     * Live list of one entrant's reservations, earliest dates first.
     *
     * @param entrantId the entrant's userId
     * @return a stream that updates whenever their reservations change
     */
    fun observeForEntrant(entrantId: String): Flow<List<Reservation>> =
        observeWhere(Filter("entrantId", entrantId) { it.entrantId == entrantId })
            .map { reservations -> reservations.sortedBy { it.dates.start } }

    /**
     * Returns the dates already taken at a campsite by active reservations,
     * for showing which dates a winner can still pick (US 01.04.01).
     *
     * @param campsiteId the campsite to check
     * @return the date ranges of its active reservations
     */
    suspend fun bookedDates(campsiteId: String): List<DateRange> =
        getAll().filter { it.campsiteId == campsiteId && it.isActive }.map { it.dates }

    /**
     * Saves a new reservation unless its dates clash with an active
     * reservation for the same campsite (US 01.05.01).
     *
     * @param reservation the reservation to save, with its ID already set
     * @return the saved reservation, or null if the dates are already taken
     * @throws IllegalArgumentException if the ID is blank or already in use
     */
    suspend fun reserve(reservation: Reservation): Reservation? {
        if (getAll().any { it.clashesWith(reservation) }) return null
        return add(reservation)
    }

    /**
     * Cancels a reservation and saves it, as one step, freeing its dates.
     *
     * @param reservationId the reservation to cancel
     * @return the cancelled reservation, or null if it doesn't exist or was
     * already cancelled
     */
    suspend fun cancel(reservationId: String): Reservation? =
        modify(reservationId) { it.cancelled() }

    companion object {
        /**
         * Creates the Firestore data source for reservations, used by [AppContainer].
         *
         * @return a data source backed by the "reservations" collection
         */
        fun firestoreDataSource(): FirestoreDataSource<Reservation> =
            FirestoreDataSource("reservations", ::toMap, ::fromMap)

        /**
         * Converts a reservation into the fields saved in Firestore.
         *
         * The ID is not included because it is the document ID. Dates are
         * saved as "yyyy-MM-dd" text and the status by name (such as "CONFIRMED").
         *
         * @param reservation the reservation to convert
         * @return the fields to save
         */
        fun toMap(reservation: Reservation): Map<String, Any?> = mapOf(
            "entrantId" to reservation.entrantId,
            "campsiteId" to reservation.campsiteId,
            "lotteryId" to reservation.lotteryId,
            "start" to reservation.dates.start.toString(),
            "finish" to reservation.dates.finish.toString(),
            "status" to reservation.status.name,
            "createdAt" to reservation.createdAt.toString(),

        )

        /**
         * Rebuilds a reservation from a Firestore document.
         *
         * A missing or unknown status, or missing dates, throw, so the
         * document is skipped rather than holding dates by mistake.
         *
         * @param id the document ID, used as the reservation ID
         * @param data the saved fields
         * @return the rebuilt reservation
         * @throws IllegalArgumentException if the status or a date is missing or invalid
         */
        fun fromMap(id: String, data: Map<String, Any?>): Reservation {
            val start = requireNotNull(data["start"] as? String) { "Reservation is missing a start date" }
            val finish = requireNotNull(data["finish"] as? String) { "Reservation is missing a finish date" }
            val createdAt = requireNotNull(data["createdAt"] as? String) { "Reservation is missing its creation time" }
            return Reservation(
                id = id,
                entrantId = data["entrantId"] as? String ?: "",
                campsiteId = data["campsiteId"] as? String ?: "",
                lotteryId = data["lotteryId"] as? String ?: "",
                dates = DateRange(LocalDate.parse(start), LocalDate.parse(finish)),
                status = ReservationStatus.valueOf(data["status"] as? String ?: ""),
                createdAt = LocalDateTime.parse(createdAt),
            )
        }
    }
}
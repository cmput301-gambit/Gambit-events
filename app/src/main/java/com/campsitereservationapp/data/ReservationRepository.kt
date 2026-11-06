package com.campsitereservationapp.data

import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Reservation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
 * **Outstanding issues:** no Firestore mapping yet, so reservations are
 * kept in memory. TODO: add a companion object with toMap, fromMap and
 * firestoreDataSource, like [LotteryRepository]. The clash check in
 * [reserve] reads and then adds in two steps, so two devices booking the
 * same dates at the same moment could both succeed; this needs a Firestore
 * transaction once the mapping exists.
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
}
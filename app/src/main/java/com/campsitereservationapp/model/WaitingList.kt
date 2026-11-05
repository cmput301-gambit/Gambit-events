package com.campsitereservationapp.model

/**
 * Errors that can occur when validating a [WaitingList].
 *
 */
enum class WaitingListError {
    ENTRANT_CAP_IS_ZERO,
    NO_ENTRANTS_BUT_IS_FULL,
    ENTRANT_CAP_DNE_BUT_IS_FULL,
}

/**
 * Waiting List data structure owned by a [Lottery]
 *
 * **Purpose:** During [Lottery] construction, [WaitingList] is created to hold the list of entrant
 * winners and losers. Also stores whether there is an entrant cap.
 *
 * **Design rationale:** immutable data class, so every change returns a new
 * copy (or null if the change isn't allowed) and can be saved safely by
 * [com.campsitereservationapp.data.LotteryRepository]. [entrantList] keeps
 * everyone who joined; after the draw, each entrant is also in either
 * [entrantWinners] or [entrantLosers].
 *
 * **Outstanding issues:** [isFull] is stored rather than calculated from
 * [entrantCap], so it can disagree with the list if edited by hand.
 *
 * @property entrantList The list of entrant userIds participating in the lottery.
 * @property entrantLosers Entrants who were not selected in the draw.
 * @property entrantWinners The list of entrants who have been selected to be the winners of the
 * lottery.
 * @property entrantsDeclined Winners who declined their spot (US 01.04.03).
 * @property entrantCap Integer determining how many entrants can be added to the entrantList.
 * Cannot be 0. However -1 will disable this option allowing infinite entrants to join.
 * @property isFull When [entrantCap] is enabled and the amount of entrants in [entrantList] matches
 * that amount it will return true, otherwise will be set to false.
 * @constructor Creates an empty waiting list, constructors are there for convenience for unit
 * testing.
 */
data class WaitingList(
    val entrantList: List<String> = emptyList(),
    val entrantLosers: List<String> = emptyList(),
    val entrantWinners: List<String> = emptyList(),
    val entrantsDeclined: List<String> = emptyList(),
    val entrantCap: Int = -1,
    val isFull: Boolean = false,
) {
    /**
     * Validates the WaitingList fields and returns a list of errors found.
     *
     * @return list of [WaitingListError] violations
     */
    fun validate(): List<WaitingListError> {
        val errors = mutableListOf<WaitingListError>()
        if (entrantList.isEmpty() && isFull) {
            errors.add(WaitingListError.NO_ENTRANTS_BUT_IS_FULL)
        }
        if (entrantCap == 0) {
            errors.add(WaitingListError.ENTRANT_CAP_IS_ZERO)
        }
        if (entrantCap == -1 && isFull) {
            errors.add(WaitingListError.ENTRANT_CAP_DNE_BUT_IS_FULL)
        }
        return errors
    }

    /**
     * Checks whether this waiting list has everything it needs to be saved.
     *
     * @return true if the waiting list has no validation errors, false otherwise
     */
    fun isValid(): Boolean = validate().isEmpty()

    /**
     * Returns a copy of this waiting list with an entrant added.
     *
     * @param entrantId the entrant's userId
     * @return the updated waiting list, or null if the entrant is already on
     * it or the list is full
     */
    fun withEntrant(entrantId: String): WaitingList? {
        if (entrantId in entrantList || isFull) return null
        val newList = entrantList + entrantId
        return copy(entrantList = newList, isFull = newList.size == entrantCap)
    }

    /**
     * Returns a copy of this waiting list with an entrant removed, which
     * frees their spot for someone else.
     *
     * @param entrantId the entrant's userId
     * @return the updated waiting list, or null if the entrant isn't on it
     */
    fun withoutEntrant(entrantId: String): WaitingList? {
        if (entrantId !in entrantList) return null
        return copy(entrantList = entrantList - entrantId, isFull = false)
    }

    /**
     * Returns a copy of this waiting list where a winner has declined their
     * spot, moving them from the winners to the declined list (US 01.04.03).
     *
     * @param entrantId the entrant's userId
     * @return the updated waiting list, or null if they aren't a winner or
     * already declined
     */
    fun withDecline(entrantId: String): WaitingList? {
        if (entrantId !in entrantWinners || entrantId in entrantsDeclined) return null
        return copy(
            entrantWinners = entrantWinners - entrantId,
            entrantsDeclined = entrantsDeclined + entrantId,
        )
    }
}
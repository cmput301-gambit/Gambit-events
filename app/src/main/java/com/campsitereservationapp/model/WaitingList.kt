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
 * **Design rationale:** Waiting list needs to track the list of entrant winners and losers, by
 * simply removing the winner entrants from the base entrantList we're able to separate them into
 * winners and losers.
 *
 * @property entrantList The list of entrant userIds participating in the lottery.
 * @property entrantWinners The list of entrants who have been selected to be the winners of the
 * lottery.
 * @property entrantCap Integer determining how many entrants can be added to the entrantList.
 * Cannot be 0. However -1 will disable this option allowing infinite entrants to join.
 * @property isFull When [entrantCap] is enabled and the amount of entrants in [entrantList] matches
 * that amount it will return true, otherwise will be set to false.
 * @constructor Creates an empty waiting list, constructors are there for convenience for unit
 * testing.
 */
class WaitingList(
    var entrantList: MutableList<String> = mutableListOf(),
    var entrantWinners: MutableList<String> = mutableListOf(),
    var entrantCap: Int = -1,
    var isFull: Boolean = false,
) {
    /**
     * Validates the WaitingList fields and returns a list of errors found.
     *
     * @return list of [WaitingListError] violations
     */
    fun validate(): MutableList<WaitingListError> {
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
    fun isValid(): Boolean {
        return validate().isEmpty()
    }


    /**
     * Adds an entrant to the list of entrants in the waiting list.
     *
     * @param entrantId is the entrant's userId to be copied into the [entrantList].
     * @return Returns true when the entrant has successfully been added, returns false when the
     * entrant is denied, such as the user already being in the lottery or the lottery being full.
     * */
    fun addEntrant(entrantId: String): Boolean {
        if (entrantId !in entrantList && !isFull) {
            entrantList.add(entrantId)
            if (entrantList.size == entrantCap) {
                isFull = true
            }
            return true
        }
        return false
    }

    /**
     * Removes entrant from the [entrantList] by their userId. Takes an [Entrant]
     *
     * @param entrantId Entrant user id used to remove it from the [entrantList].
     * @return Returns true when the passed [Entrant] is successfully removed from the entrantList.
     * Returns false otherwise.
     * */
    fun removeEntrant(entrantId: String): Boolean {
        if (entrantId in entrantList) {
            entrantList.remove(entrantId)
            if (isFull) { isFull = false }
            return true
        }
        return false
    }
}
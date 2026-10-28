package com.campsitereservationapp.model


/*
* TODO: Unit Tests
*
* */

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
 * removing the winner entrants from the base entrantList we're able to separate them into winners
 * and losers. All that is left TODO notify the entrants of their status.
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
     * @param entrant is an [Entrant] where the function will copy it's userId into the
     * [entrantList].
     * */
    fun addEntrant(entrant: Entrant) {
        if (entrant.userId !in entrantList && !isFull) {
            entrantList.add(entrant.userId)
            if (entrantList.size == entrantCap) {
                isFull = true
            }
        }
    }

    /**
     * Removes entrant from the [entrantList] by their userId. Takes an [Entrant]
     *
     * @param entrant Entrant class which the has a userId used to remove it from the [entrantList].
     * */
    fun removeEntrant(entrant: Entrant) {
        if (entrant.userId in entrantList) {
            entrantList.remove(entrant.userId)
            if (isFull) { isFull = false }
        }
    }
}
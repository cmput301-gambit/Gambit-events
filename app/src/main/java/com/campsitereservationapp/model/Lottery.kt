package com.campsitereservationapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.campsitereservationapp.data.DateRange
import java.time.LocalDate

/**
 * Errors that can occur when validating a [Lottery].
 *
 */
enum class LotteryError {
    NO_CAMPSITES,
    REGISTRATION_PERIOD_AFTER_RESERVATION_PERIOD,
    WINNER_AMOUNT_LESS_THAN_ONE,
    REGISTRATION_PERIOD_IN_PAST,
    RESERVATION_PERIOD_IN_PAST,
    INVALID_WAITING_LIST,

}

/**
 * Different stages the lottery can be in.
 */
enum class LotteryStatus {
    DRAFT,
    OPEN,
    DRAWN,
    CLOSED
}

/**
 * Lottery data structure that can operate the associated waiting list
 *
 * **Purpose:** [LotteryRepository] is able to create and operate a [Lottery] which
 * holds information such as the campsites, the registration and reservation periods,
 * along with the winner amounts. [Lottery] is also able to draw the entrant winners.
 *
 * **Design rationale:** plain data class with no Firestore code, so it's
 * easy to unit test. [LotteryRepository] handles saving and loading.
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property campsites Mutable list of campsite ids to draw the lottery for.
 * @property registrationPeriod Time period where entrants can join the waitlist
 * @property reservationPeriod Time period where entrants are allowed to make reservations
 * @property stage The current stage of the lottery
 * @property winnerAmount The amount of winners to be drawn from the waitlist
 * @property waitingList Contains the list of waiting entrants
 * @constructor Creates a lottery. registrationPeriod and reservationPeriod do not have default
 * arguments.
 */
@RequiresApi(Build.VERSION_CODES.O)
data class Lottery (
    override val id: String = "",
    var campsites: MutableList<String> = mutableListOf(),
    var registrationPeriod: DateRange,
    var reservationPeriod: DateRange,
    var stage: LotteryStatus = LotteryStatus.DRAFT,
    var winnerAmount: Int = 1,
    var waitingList: WaitingList = WaitingList(),
): Identifiable {
    fun validate() : List<LotteryError> {
        val errors = mutableListOf<LotteryError>()
        if (winnerAmount < 1) {
            errors.add(LotteryError.WINNER_AMOUNT_LESS_THAN_ONE)
        }
        if (campsites.isEmpty()) {
            errors.add(LotteryError.NO_CAMPSITES)
        }
        if (registrationPeriod.finish.isAfter(reservationPeriod.start)) {
            errors.add(LotteryError.REGISTRATION_PERIOD_AFTER_RESERVATION_PERIOD)
        }
        if (registrationPeriod.start.isBefore(LocalDate.now())) {
            errors.add(LotteryError.REGISTRATION_PERIOD_IN_PAST)
        }
        if (reservationPeriod.start.isBefore(LocalDate.now())) {
            errors.add(LotteryError.RESERVATION_PERIOD_IN_PAST)
        }
        if (!waitingList.isValid()) {
            errors.add(LotteryError.INVALID_WAITING_LIST)
        }
        return errors
    }

    /**
     * Checks whether this lottery has everything it needs to be saved.
     *
     * @return true if the lottery has no validation errors, false otherwise
     */
    fun isValid(): Boolean {
        return validate().isEmpty()
    }

    /**
     * Attempts to add an [Entrant] to the waiting list. Silently fails if either the lottery status
     * is not OPEN, whether waiting list cap is hit, or whether the entrant is already in the
     * waiting list.
     *
     * @param entrant Entrant to be added to the waiting list
     * @return Returns true when entrant has been successfully added, false otherwise.
     * */
    fun addEntrant(entrant: Entrant): Boolean {
        if (stage == LotteryStatus.OPEN) {
            return waitingList.addEntrant(entrant)
        }
        return false
    }

    /**
     * Attempts to remove an [Entrant] from the waiting list if the lottery status is OPEN.
     *
     * @param entrant Entrant to be removed from the waiting list
     * @return Returns true when entrant has been successfully removed, false otherwise.
     * */
    fun removeEntrant(entrant: Entrant): Boolean {
        if (stage == LotteryStatus.OPEN) {
            return waitingList.removeEntrant(entrant)
        }
        return false
    }

    /**
    * If the current lottery stage is DRAWN, removes the amount of winners from the entrants waiting
     * list and puts them into a winners list inside [WaitingList]. Sets the stage from DRAWN to CLOSED.
    *
    * */
    fun drawEntrants() {
        if (stage == LotteryStatus.DRAWN) {
            val drawAmount = minOf(winnerAmount, waitingList.entrantList.size)
            for (i in 1..drawAmount) {
                val wonEntrant = waitingList.entrantList.random()
                waitingList.entrantWinners.add(wonEntrant)
                waitingList.entrantList.remove(wonEntrant)
            }
            stage = LotteryStatus.CLOSED
            notifyEntrants()
        }
    }

    /**
     * If the lottery's current status is DRAFT sets the status to be OPEN.
     *
     * @return Returns true if the status was successfully changed to OPEN, otherwise returns false.
     * */
    fun open(): Boolean {
        if (stage == LotteryStatus.DRAFT) {
            stage = LotteryStatus.OPEN
            return true
        }
        return false
    }



    /**
     * If the current lottery stage is CLOSED then it will send the lists of lottery
     * winners and losers to the [NotificationHandler] to send to the entrants.
     *
     * */
    fun notifyEntrants() {
        if (stage == LotteryStatus.CLOSED) {
            // TODO: USE NOTIFICATION HANDLER TO SEND NOTIFICATIONS}
        }
    }
}
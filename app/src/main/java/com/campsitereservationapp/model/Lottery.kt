package com.campsitereservationapp.model

import java.time.LocalDate
import kotlin.random.Random

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
    NO_OWNER_ID,

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
 * Lottery data structure that can operate the associated waiting list.
 * Completes US 02.04.01 and resolved issue #24
 * Completes US 02.04.05 and resolves issue #28
 * Completes US 02.04.04 and partially resolves #27
 *
 * **Purpose:** [LotteryRepository] is able to create and operate a [Lottery] which
 * holds information such as the campsites, the registration and reservation periods,
 * along with the winner amounts. [Lottery] is also able to draw the entrant winners.
 *
 * **Design rationale:** plain data class with no Firestore code, so it's
 * easy to unit test. [LotteryRepository] handles saving and loading.
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property ownerId The userId of the campsite owner who created the lottery.
 * @property campsiteIds Mutable list of campsite ids to draw the lottery for.
 * @property registrationPeriod Time period where entrants can join the waitlist
 * @property reservationPeriod Time period where entrants are allowed to make reservations
 * @property stage The current stage of the lottery
 * @property winnerAmount The amount of winners to be drawn from the waitlist
 * @property waitingList Contains the list of waiting entrants
 * @constructor Creates a lottery. registrationPeriod and reservationPeriod do not have default
 * arguments.
 */

data class Lottery(
    override val id: String = "",
    val ownerId: String = "",
    val campsiteIds: List<String> = emptyList(),
    val registrationPeriod: DateRange,
    val reservationPeriod: DateRange,
    val stage: LotteryStatus = LotteryStatus.DRAFT,
    val winnerAmount: Int = 1,
    val waitingList: WaitingList = WaitingList(),
    val sentNotifications: Boolean = false,
) : Identifiable {
    fun validate() : List<LotteryError> {
        val errors = mutableListOf<LotteryError>()
        if (winnerAmount < 1) {
            errors.add(LotteryError.WINNER_AMOUNT_LESS_THAN_ONE)
        }
        if (campsiteIds.isEmpty()) {
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
        if (ownerId.isBlank()) {
            errors.add(LotteryError.NO_OWNER_ID)
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
     * Returns a copy of this lottery with an entrant added to the waiting list.
     *
     * @param entrantId the entrant's userId
     * @return the updated lottery, or null if the lottery isn't OPEN, the
     * entrant already joined, or the waiting list is full
     */
    fun withEntrant(entrantId: String): Lottery? {
        if (stage != LotteryStatus.OPEN) return null
        return waitingList.withEntrant(entrantId)?.let { copy(waitingList = it) }
    }

    /**
     * Returns a copy of this lottery with an entrant removed from the
     * waiting list (US 01.02.02).
     *
     * @param entrantId the entrant's userId
     * @return the updated lottery, or null if the lottery isn't OPEN or the
     * entrant isn't on the waiting list
     */
    fun withoutEntrant(entrantId: String): Lottery? {
        if (stage != LotteryStatus.OPEN) return null
        return waitingList.withoutEntrant(entrantId)?.let { copy(waitingList = it) }
    }

    /**
     * Returns a copy of this lottery that entrants can join.
     *
     * @return the updated lottery, or null if it isn't a DRAFT
     */
    fun opened(): Lottery? =
        if (stage == LotteryStatus.DRAFT) copy(stage = LotteryStatus.OPEN) else null

    /**
     * Returns a copy of this lottery marked ready to draw, once its
     * registration period is over (US 02.05.01). Registration includes its
     * last day, so a lottery is ready from the day after it closes.
     *
     * @param today the current date
     * @return the lottery in the DRAWN stage, or null if it isn't OPEN or
     * registration hasn't ended yet
     */
    fun readyToDraw(today: LocalDate): Lottery? =
        if (stage == LotteryStatus.OPEN && today.isAfter(registrationPeriod.finish)) {
            copy(stage = LotteryStatus.DRAWN)
        } else {
            null
        }

    /**
     * Returns a copy of this lottery with winners picked at random and
     * everyone else marked as a loser. The stage moves from DRAWN to CLOSED.
     *
     * @param random the source of randomness; tests pass a seeded one so
     * results are repeatable
     * @return the drawn lottery, or null if the stage isn't DRAWN
     */
    fun drawn(random: Random = Random.Default): Lottery? {
        if (stage != LotteryStatus.DRAWN) return null
        val entrants = waitingList.entrantList.distinct()
        val winners = entrants.shuffled(random).take(winnerAmount.coerceAtLeast(0))
        val losers = entrants.filterNot { it in winners }
        return copy(
            stage = LotteryStatus.CLOSED,
            waitingList = waitingList.copy(entrantWinners = winners, entrantLosers = losers),
        )
    }

    /**
     * Returns a copy of this lottery where a winner has declined their spot.
     *
     * @param entrantId the winner's userId
     * @return the updated lottery, or null if they aren't a winner or already declined
     */
    fun withDecline(entrantId: String): Lottery? =
        waitingList.withDecline(entrantId)?.let { copy(waitingList = it) }

    /**
     * Returns a copy of this lottery marked as having sent its win/lose
     * notifications, so they are never sent twice.
     *
     * @return the updated lottery, or null if it isn't CLOSED or was already marked
     */
    fun markNotified(): Lottery? =
        if (stage == LotteryStatus.CLOSED && !sentNotifications) copy(sentNotifications = true) else null
}
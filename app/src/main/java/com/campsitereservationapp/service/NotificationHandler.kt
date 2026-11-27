package com.campsitereservationapp.service

import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.Notification
import com.campsitereservationapp.model.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * What happened when an owner messaged a lottery's waiting list.
 *
 * @property sent the notifications that were saved, one per recipient
 * @property skippedOptedOut how many entrants were skipped because they
 * turned notifications off
 */
data class WaitlistSendResult(
    val sent: List<Notification>,
    val skippedOptedOut: Int,
)

/**
 * Builds lottery notifications and saves them.
 *
 * **Purpose:** after a [Lottery] is drawn, creates one [Notification] for
 * every entrant who joined: a win message for each winner and a lose
 * message for everyone else, and saves them in [NotificationRepository]
 * so each entrant sees theirs in their inbox (US 01.03.01, 01.03.02).
 * Also sends a campsite owner's message to everyone on a lottery's waiting
 * list (US 02.10.01).
 *
 * **Design rationale:** kept separate from [Lottery] so the lottery doesn't
 * need to know how messages are worded or stored. The repository is passed
 * in, like every other repository in the app, so tests can use a
 * [com.campsitereservationapp.data.MockDataSource] instead of Firestore.
 *
 * The notification opt-out ([User.notificationsEnabled]) is treated
 * differently for the two kinds:
 * - Win/lose results ignore it. Every entrant gets their result, because
 *   the in-app inbox should always show whether they won.
 * - Owner messages respect it. Entrants who turned notifications off are
 *   skipped, because these messages are optional updates from the owner.
 *
 * **Outstanding issues:** win/lose notifications are sent by
 * LotteryRepository.drawLotteries, which makes sure each lottery's results
 * are sent once. If the app closes partway through, the remaining
 * notifications are not retried. Owner messages are saved one at a time, so
 * if saving fails partway, some entrants already have the message and
 * sending again gives them a second copy.
 *
 * @param notificationRepository where the notifications are saved
 */
class NotificationHandler(private val notificationRepository: NotificationRepository) {

    /**
     * The entrants an owner message to this lottery's waiting list would go
     * to: everyone who joined, once each, never the sender.
     *
     * @param lottery the lottery whose waiting list is messaged
     * @param senderId ID of the owner sending the message
     * @return the IDs of the entrants, before the opt-out is checked
     */
    fun waitlistRecipientIds(lottery: Lottery, senderId: String): List<String> =
        lottery.waitingList.entrantList.distinct().filterNot { it == senderId }

    /**
     * Sends an owner's message to everyone on a lottery's waiting list and
     * saves it (US 02.10.01). Entrants who turned notifications off are
     * skipped, and so are entrants with no profile (deleted users).
     *
     * This doesn't check that [senderId] owns the lottery; the caller must.
     *
     * @param lottery the lottery whose waiting list is messaged
     * @param senderId ID of the owner sending the message
     * @param text what the message says; extra spaces are trimmed
     * @param users the profiles of the entrants on the waiting list, used to
     * check who has notifications on
     * @param now when the message is sent; every notification in this send
     * gets the same time
     * @return the saved notifications, which all share one batchId, and how
     * many entrants were skipped for having notifications off
     * @throws IllegalArgumentException if the message is blank or longer than
     * [Notification.MAX_LENGTH], or the sender is blank; nothing is saved
     */
    suspend fun sendWaitlistMessage(
        lottery: Lottery,
        senderId: String,
        text: String,
        users: List<User>,
        now: LocalDateTime = LocalDateTime.now(),
    ): WaitlistSendResult {
        val result = createWaitlistMessages(
            lottery, senderId, text, users, now, notificationRepository::newId
        )
        result.sent.forEach { notificationRepository.add(it) }
        return result
    }

    /**
     * Builds an owner's message to everyone on a lottery's waiting list,
     * without saving it. See [sendWaitlistMessage].
     *
     * @param lottery the lottery whose waiting list is messaged
     * @param senderId ID of the owner sending the message
     * @param text what the message says; extra spaces are trimmed
     * @param users the profiles of the entrants on the waiting list
     * @param now when the message is sent
     * @param newId makes a unique ID for each notification and for the batch
     * @return one notification per entrant with notifications on, all sharing
     * one batchId and send time
     * @throws IllegalArgumentException if any notification would be invalid
     */
    fun createWaitlistMessages(
        lottery: Lottery,
        senderId: String,
        text: String,
        users: List<User>,
        now: LocalDateTime = LocalDateTime.now(),
        newId: () -> String = { UUID.randomUUID().toString() },
    ): WaitlistSendResult {
        val usersById = users.associateBy { it.id }
        val recipients = waitlistRecipientIds(lottery, senderId).mapNotNull { usersById[it] }
        val (enabled, optedOut) = recipients.partition { it.notificationsEnabled }

        val batchId = newId()
        val notifications = enabled.map { user ->
            Notification.fromOwner(newId(), user.id, lottery.id, senderId, text, batchId, now)
        }
        val errors = notifications.flatMap { it.validate() }.distinct()
        require(errors.isEmpty()) { "Owner message is invalid: $errors" }
        return WaitlistSendResult(notifications, optedOut.size)
    }

    /**
     * Creates the win and lose notifications for a drawn lottery and saves them.
     *
     * @param lottery the lottery; nothing is saved unless its stage is CLOSED
     * @param campsites campsites to take names from; only those in the lottery are used
     * @param today the date to put on each notification
     * @return the notifications that were saved; empty if the lottery isn't closed
     */
    suspend fun sendResultNotifications(
        lottery: Lottery,
        campsites: List<Campsite>,
        today: LocalDate = LocalDate.now(),
    ): List<Notification> {
        val notifications = createResultNotifications(
            lottery, campsites, today, notificationRepository::newId
        )
        notifications.forEach { notificationRepository.add(it) }
        return notifications
    }

    /**
     * Creates the win and lose notifications for a drawn lottery, without saving them.
     *
     * @param lottery the lottery; nothing is created unless its stage is CLOSED
     * @param campsites campsites to take names from; only those in the lottery are used
     * @param today the date to put on each notification
     * @param newId makes a unique ID for each notification
     * @return one notification per entrant, winners first; empty if the lottery isn't closed
     */
    fun createResultNotifications(
        lottery: Lottery,
        campsites: List<Campsite>,
        today: LocalDate = LocalDate.now(),
        newId: () -> String = { UUID.randomUUID().toString() },
    ): List<Notification> {
        if (lottery.stage != LotteryStatus.CLOSED) return emptyList()

        val campsiteName = campsiteNameFor(lottery, campsites)
        val winners = lottery.waitingList.entrantWinners.distinct()
        val losers = lottery.waitingList.entrantList.distinct().filterNot { it in winners }

        val won = winners.map { Notification.won(newId(), it, lottery.id, campsiteName, today) }
        val lost = losers.map { Notification.lost(newId(), it, lottery.id, campsiteName, today) }
        return won + lost
    }

    /**
     * Builds the campsite name shown in the message, joining names if the
     * lottery covers more than one campsite.
     */
    private fun campsiteNameFor(lottery: Lottery, campsites: List<Campsite>): String {
        val names = campsites.filter { it.id in lottery.campsiteIds }.map { it.name }
        return if (names.isEmpty()) "your campsite" else names.joinToString(", ")
    }
}
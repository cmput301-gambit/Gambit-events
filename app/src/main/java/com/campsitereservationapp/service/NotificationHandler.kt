package com.campsitereservationapp.service

import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.Notification
import java.time.LocalDate
import java.util.UUID

/**
 * Turns the result of a lottery draw into win and lose notifications and saves them.
 *
 * **Purpose:** after a [Lottery] is drawn, creates one [Notification] for
 * every entrant who joined: a win message for each winner and a lose
 * message for everyone else, and saves them in [NotificationRepository]
 * so each entrant sees theirs in their inbox (US 01.03.01, 01.03.02).
 *
 * **Design rationale:** kept separate from [Lottery] so the lottery doesn't
 * need to know how messages are worded or stored. The repository is passed
 * in, like every other repository in the app, so tests can use a
 * [com.campsitereservationapp.data.MockDataSource] instead of Firestore.
 * Every entrant gets a notification, even if they turned notifications off,
 * because the in-app inbox should always show their result; the opt-out
 * only affects phone notifications.
 *
 * **Outstanding issues:** called by LotteryRepository.drawLotteries, which
 * makes sure each lottery's notifications are sent once. If the app closes
 * partway through, the remaining notifications are not retried.
 *
 * @param notificationRepository where the notifications are saved
 */
class NotificationHandler(private val notificationRepository: NotificationRepository) {

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
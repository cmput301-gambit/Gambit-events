package com.campsitereservationapp.data

import com.campsitereservationapp.model.DateRange
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.model.WaitingList
import com.campsitereservationapp.service.NotificationHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Stores and provides [Lottery] objects.
 *
 * **Purpose:** lets owners create and run lotteries, lets entrants join and
 * leave waiting lists (US 01.02.02), and draws winners once a lottery is
 * ready.
 *
 * **Design rationale:** extends [Repository], which holds the shared rules;
 * this class adds lottery-specific queries such as [listLotteries] and
 * [deleteByCampsite]. Every change to a lottery goes through modify, so the
 * read, change and save happen as one step and two devices can't overwrite
 * each other (for example, two entrants joining at the same moment). Its
 * companion object converts lotteries to and from Firestore documents.
 *
 * **Outstanding issues:** [listLotteries] filters on the device, so every
 * device downloads every lottery. Fine for a class project, but with many
 * lotteries the common filters should move into Firestore queries.
 *
 * @param dataSource where lotteries are kept, such as [firestoreDataSource]
 * or a [MockDataSource] in tests
 * @property campsiteRepository used to look up the campsites in a lottery
 * @property notificationHandler sends win/lose notifications after a draw;
 * null in tests that don't draw
 */
class LotteryRepository(
    dataSource: DataSource<Lottery>,
    val campsiteRepository: CampsiteRepository,
    val notificationHandler: NotificationHandler? = null,
) : Repository<Lottery>(dataSource) {

    /**
     * Lists the lotteries that match every condition given. Conditions left
     * as null are not checked.
     *
     * Example: `listLotteries(lotteryStage = LotteryStatus.OPEN, ownerId = "owner-bobby")`
     * returns every open lottery run by owner-bobby.
     *
     * The three `entrantIn...` conditions are about one entrant, so they
     * are only checked when [userId] is given.
     *
     * @param lotteryStage only lotteries in this stage
     * @param campsiteId only lotteries that include this campsite
     * @param ownerId only lotteries run by this owner
     * @param userId only lotteries this entrant joined
     * @param entrantInLosers whether [userId] must (true) or must not (false) be a loser
     * @param entrantInWinners whether [userId] must (true) or must not (false) be a winner
     * @param entrantInDeclined whether [userId] must (true) or must not (false) have declined
     * @param hasSentNotifications only lotteries whose notifications have (true) or haven't (false) been sent
     * @return a stream that emits the matching lotteries every time lotteries change
     */
    fun listLotteries(
        lotteryStage: LotteryStatus? = null,
        campsiteId: String? = null,
        ownerId: String? = null,
        userId: String? = null,
        entrantInLosers: Boolean? = null,
        entrantInWinners: Boolean? = null,
        entrantInDeclined: Boolean? = null,
        hasSentNotifications: Boolean? = null,
    ): Flow<List<Lottery>> =
        observeAll().map { lotteries ->
            lotteries.filter { lottery ->
                val waitingList = lottery.waitingList
                (lotteryStage == null || lottery.stage == lotteryStage) &&
                        (campsiteId == null || campsiteId in lottery.campsiteIds) &&
                        (ownerId == null || lottery.ownerId == ownerId) &&
                        (hasSentNotifications == null || lottery.sentNotifications == hasSentNotifications) &&
                        (userId == null || (
                                userId in waitingList.entrantList &&
                                        (entrantInWinners == null || (userId in waitingList.entrantWinners) == entrantInWinners) &&
                                        (entrantInLosers == null || (userId in waitingList.entrantLosers) == entrantInLosers) &&
                                        (entrantInDeclined == null || (userId in waitingList.entrantsDeclined) == entrantInDeclined)
                                ))
            }
        }

    /**
     * Draws every lottery that is ready (stage DRAWN, notifications not yet
     * sent), then sends each entrant their win or lose notification.
     *
     * Safe to call from several devices at once: each draw happens inside
     * modify, so only one device draws a given lottery and only that device
     * sends its notifications.
     *
     * @return true when the draws ran; false, without drawing anything, if
     * this repository has no [notificationHandler]
     */
    suspend fun drawLotteries(): Boolean {
        val handler = notificationHandler ?: return false
        // One snapshot of the lotteries ready to draw, not a live stream.
        // Ask the server, not the phone's cache, so recent changes are seen. CRUCIAL
        val ready = getAll().filter { it.stage == LotteryStatus.DRAWN && !it.sentNotifications }
        for (lottery in ready) {
            // Null means another device already drew it, so skip.
            val drawn = modify(lottery.id) { it.drawn() } ?: continue
            val campsites = drawn.campsiteIds.mapNotNull { campsiteRepository.get(it) }
            handler.sendResultNotifications(drawn, campsites)
            modify(drawn.id) { it.markNotified() }
        }
        return true
    }

    /**
     * Automatically draws every open lottery whose registration period has
     * ended, then notifies each entrant whether they won (US 02.05.01).
     *
     * Called when the app starts on any device, so the owner doesn't have to
     * do anything. Safe to run on many devices at once: moving each lottery
     * to DRAWN and drawing it both happen inside modify, so each lottery is
     * drawn exactly once and its notifications are sent once.
     *
     * Outstanding issues: a lottery is only drawn once someone opens the
     * app after registration ends. If the app closes between the draw and
     * the notifications, those notifications are not retried.
     *
     * @param today the current date; tests pass a fixed date
     * @return true when the draws ran; false, without changing anything, if
     * this repository has no [notificationHandler]
     */
    suspend fun drawDueLotteries(today: LocalDate = LocalDate.now()): Boolean {
        if (notificationHandler == null) return false
        // Only start a transaction for lotteries that are actually due.
        // Ask the server, not the phone's cache, so recent changes are seen. CRUCIAL
        val due = getAll().filter { it.readyToDraw(today) != null }
            .filter { it.readyToDraw(today) != null }
        for (lottery in due) {
            // Null means another device already moved it, which is fine.
            modify(lottery.id) { it.readyToDraw(today) }
        }
        return drawLotteries()
    }

    /**
     * Adds an entrant to a lottery's waiting list and saves it, as one step.
     *
     * @param lotteryId the lottery to join
     * @param entrantId the entrant's userId
     * @return the saved lottery, or null if joining isn't allowed (not OPEN,
     * already joined, full, or the lottery no longer exists)
     */
    suspend fun joinWaitingList(lotteryId: String, entrantId: String): Lottery? =
        modify(lotteryId) { it.withEntrant(entrantId) }

    /**
     * Removes an entrant from a lottery's waiting list and saves it, as one
     * step, freeing their spot for someone else (US 01.02.02).
     *
     * @param lotteryId the lottery to leave
     * @param entrantId the entrant's userId
     * @return the saved lottery, or null if leaving isn't allowed (not OPEN,
     * not on the list, or the lottery no longer exists)
     */
    suspend fun leaveWaitingList(lotteryId: String, entrantId: String): Lottery? =
        modify(lotteryId) { it.withoutEntrant(entrantId) }

    /**
     * Removes all lotteries associated with a campsite, or updates multi-campsite lotteries
     * by removing the deleted campsite ID from their list, deleting the lottery if no campsites remain.
     *
     * @param campsiteId ID of the campsite being deleted
     */
    suspend fun deleteByCampsite(campsiteId: String) {
        for (lottery in getAll()) {
            if (campsiteId in lottery.campsiteIds) {
                val updatedCampsiteIds = lottery.campsiteIds - campsiteId
                if (updatedCampsiteIds.isEmpty()) {
                    delete(lottery.id)
                } else {
                    update(lottery.copy(campsiteIds = updatedCampsiteIds))
                }
            }
        }
    }

    companion object {
        /**
         * Creates the Firestore data source for lotteries, used by [AppContainer].
         *
         * @return a data source backed by the "lotteries" collection
         */
        fun firestoreDataSource(): FirestoreDataSource<Lottery> =
            FirestoreDataSource("lotteries", ::toMap, ::fromMap)

        /**
         * Converts a lottery into the fields saved in Firestore.
         *
         * The ID is not included because it is the document ID. Dates are
         * saved as "yyyy-MM-dd" text, the stage by name (such as "OPEN"), and
         * the waiting list as a nested map.
         *
         * @param lottery the lottery to convert
         * @return the fields to save
         */
        fun toMap(lottery: Lottery): Map<String, Any?> = mapOf(
            "ownerId" to lottery.ownerId,
            "campsiteIds" to lottery.campsiteIds,
            "registrationStart" to lottery.registrationPeriod.start.toString(),
            "registrationFinish" to lottery.registrationPeriod.finish.toString(),
            "reservationStart" to lottery.reservationPeriod.start.toString(),
            "reservationFinish" to lottery.reservationPeriod.finish.toString(),
            "stage" to lottery.stage.name,
            "winnerAmount" to lottery.winnerAmount,
            "sentNotifications" to lottery.sentNotifications,
            "waitingList" to mapOf(
                "entrantList" to lottery.waitingList.entrantList,
                "entrantLosers" to lottery.waitingList.entrantLosers,
                "entrantWinners" to lottery.waitingList.entrantWinners,
                "entrantsDeclined" to lottery.waitingList.entrantsDeclined,
                "entrantCap" to lottery.waitingList.entrantCap,
                "isFull" to lottery.waitingList.isFull,
            ),
        )

        /**
         * Rebuilds a lottery from a Firestore document.
         *
         * Missing optional fields fall back to the same defaults as
         * [Lottery]'s constructor. A missing or unknown stage, or missing
         * dates, throw, so the document is skipped rather than drawn by mistake.
         *
         * @param id the document ID, used as the lottery ID
         * @param data the saved fields
         * @return the rebuilt lottery
         * @throws IllegalArgumentException if the stage or a date is missing or invalid
         */
        fun fromMap(id: String, data: Map<String, Any?>): Lottery {
            val waitingList = data["waitingList"] as? Map<*, *> ?: emptyMap<String, Any?>()
            return Lottery(
                id = id,
                ownerId = data["ownerId"] as? String ?: "",
                campsiteIds = readStringList(data["campsiteIds"]),
                registrationPeriod = readDateRange(data["registrationStart"], data["registrationFinish"]),
                reservationPeriod = readDateRange(data["reservationStart"], data["reservationFinish"]),
                stage = LotteryStatus.valueOf(data["stage"] as? String ?: ""),
                winnerAmount = (data["winnerAmount"] as? Number)?.toInt() ?: 1,
                sentNotifications = data["sentNotifications"] as? Boolean ?: false,
                waitingList = WaitingList(
                    entrantList = readStringList(waitingList["entrantList"]),
                    entrantLosers = readStringList(waitingList["entrantLosers"]),
                    entrantWinners = readStringList(waitingList["entrantWinners"]),
                    entrantsDeclined = readStringList(waitingList["entrantsDeclined"]),
                    entrantCap = (waitingList["entrantCap"] as? Number)?.toInt() ?: -1,
                    isFull = waitingList["isFull"] as? Boolean ?: false,
                ),
            )
        }

        // Reads a list of IDs, ignoring anything that isn't text.
        private fun readStringList(value: Any?): List<String> =
            (value as? List<*>)?.filterIsInstance<String>().orEmpty()

        // Both dates are required, so a missing one throws and the document is skipped.
        private fun readDateRange(start: Any?, finish: Any?): DateRange {
            val startText = requireNotNull(start as? String) { "Lottery is missing a start date" }
            val finishText = requireNotNull(finish as? String) { "Lottery is missing a finish date" }
            return DateRange(LocalDate.parse(startText), LocalDate.parse(finishText))
        }
    }
}

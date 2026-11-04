package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite
import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import com.campsitereservationapp.service.NotificationHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores and provides [Lottery] objects.
 *
 * **Purpose:** lets owners create and run lotteries, and lets entrants find
 * the lotteries they can join or have joined.
 *
 * **Design rationale:** extends [Repository], which holds the shared rules;
 * this class adds lottery-specific queries such as [listLotteries] and [deleteByCampsite].
 *
 * **Outstanding issues:** no Firestore mapping yet, so lotteries are kept in
 * memory and lost when the app closes. TODO: add a companion object with
 * toMap, fromMap and firestoreDataSource like [CampsiteRepository].
 *
 * @param dataSource where lotteries are kept
 * @param campsiteRepository used to look up the campsites in a lottery
 */
class LotteryRepository(
    dataSource: DataSource<Lottery>,
    val campsiteRepository: CampsiteRepository,
    val notificationHandler: NotificationHandler? = null,
) : Repository<Lottery>(dataSource) {
  /**
     * Lists the lotteries based on conditions passed as arguments. If a parameter is not set then
     * it will not check that argument.
     *
     * Example:
     *    listLotteries(lotteryStage = LotteryStatus.OPEN, ownerId = "owner-1") will return all the
     *  open lotteries with that specific ownerId; regardless of the campsiteId or userId.
     *
     * @param lotteryStage Condition for matching lottery stages
     * @param campsiteId condition for matching campsiteId
     * @param ownerId Condition for matching ownerId
     * @param userId Condition for matching userId
     * @param entrantInLosers Bool for whether the userId is to be in the waitingList losers list.
     * @param entrantInWinners Bool for whether the userId is to be found in the winners list.
     * @param entrantInDeclined Bool for whether the userId is in the declined list.
     * @param hasSentNotifications Bool condition for whether the filtered lotteries have sent notifications or not.
     * @return a stream that emits the lotteries matching the inputted conditions. Or returns every
     * lottery if every condition is left blank.
     * */
    fun listLotteries(lotteryStage: LotteryStatus?=null, campsiteId: String? = null,
                      ownerId: String? = null, userId: String? = null,
                      entrantInLosers: Boolean? = null,
                      entrantInWinners: Boolean? = null,
                      entrantInDeclined: Boolean? = null,
                      hasSentNotifications: Boolean? = null): Flow<List<Lottery>> =
        observeAll().map { lotteries -> lotteries.filter {
            (it!=null) && (lotteryStage == null || it.stage == lotteryStage) &&
                    (campsiteId == null || it.campsiteIds.contains(campsiteId)) &&
                    (ownerId == null || it.ownerId == ownerId) &&
                    (userId == null || it.waitingList.entrantList.contains(userId)) &&
                    ((entrantInWinners == null || userId == null) || (it.waitingList.entrantWinners.contains(userId) == entrantInWinners)) &&
                    ((entrantInLosers == null || userId == null) || (it.waitingList.entrantLosers.contains(userId) == entrantInLosers)) &&
                    ((entrantInDeclined == null || userId == null) || (it.waitingList.entrantsDeclined.contains(userId) == entrantInDeclined)) &&
                    (hasSentNotifications == null || it.sentNotifications == hasSentNotifications)
        }
    }

    /**
     * Gets a flow of the list of lotteries that matches the conditions of the status being [LotteryStatus.DRAWN]
     * and has not yet sent notifications ([Lottery.sentNotifications]). Then for each lottery it will draw the random winners
     * and losers and send the notifications using the [NotificationHandler].
     *
     * Returns true when notifications are successfully sent, returns false when the notificationHandler
     * is null in LotteryRepository construction call.
     * */
    suspend fun drawLotteries(): Boolean {
        var hasHandler: Boolean = true
        // Get list of all drawn lotteries that have not sent notifications.
        val lotteries: Flow<List<Lottery>> = listLotteries(lotteryStage = LotteryStatus.DRAWN,
            hasSentNotifications = false)
        lotteries.collect {lotteries ->
            lotteries.forEach {lottery ->
                val campsiteList: MutableList<Campsite> = mutableListOf()
                // Creates the list of campsite names
                for (campsiteId in lottery.campsiteIds) {
                    val campsite = campsiteRepository.get(campsiteId)
                    if (campsite != null) {
                        campsiteList.add(campsite)
                    }
                }
                // Draws winners, sends notifications, and updates lottery's hasSentNotifications
                lottery.drawEntrants()
                if (notificationHandler == null) {
                    hasHandler = false
                } else {
                    notificationHandler.sendResultNotifications(lottery, campsiteList)
                }
                lottery.notifiedEntrants()
            }
        }
        return hasHandler
    }

    /**
     * Removes all lotteries associated with a campsite, or updates multi-campsite lotteries
     * by removing the deleted campsite ID from their list, deleting the lottery if no campsites remain.
     *
     * @param campsiteId ID of the campsite being deleted
     */
    suspend fun deleteByCampsite(campsiteId: String) {
        val lotteries = getAll()
        for (lottery in lotteries) {
            if (lottery.campsiteIds.contains(campsiteId)) {
                val updatedCampsiteIds = lottery.campsiteIds.filter { it != campsiteId }.toMutableList()
                if (updatedCampsiteIds.isEmpty()) {
                    delete(lottery.id)
                } else {
                    update(lottery.copy(campsiteIds = updatedCampsiteIds))
                }
            }
        }
    }
}

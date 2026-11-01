package com.campsitereservationapp.data

import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LotteryRepository(campsiteRepository: CampsiteRepository) : Repository<Lottery>() {

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
     * @return a stream that emits the lotteries matching the inputted conditions. Or returns every
     * lottery if every condition is left blank.
     * */
    fun listLotteries(lotteryStage: LotteryStatus?=null, campsiteId: String? = null,
                      ownerId: String? = null, userId: String? = null): Flow<List<Lottery>> =
        observeAll().map { lotteries -> lotteries.filter {
            (it!=null) && (lotteryStage == null || it.stage == lotteryStage) &&
                    (campsiteId == null || it.campsiteIds.contains(campsiteId)) &&
                    (ownerId == null || it.ownerId == ownerId) &&
                    (userId == null || it.waitingList.entrantList.contains(userId))}
        }

}
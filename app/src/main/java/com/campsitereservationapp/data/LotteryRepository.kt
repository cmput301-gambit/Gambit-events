package com.campsitereservationapp.data

import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LotteryRepository : Repository<Lottery>() {

    /**
     * Lists the lotteries based on conditions passed as arguments. If neither arguments are filled
     * it will return every lottery.
     *
     * @param lotteryStage checks lottery stage when returning lotteries.
     * @param campsiteId Checks whether campsite exists in each lottery.
     * @return a stream that emits the lotteries matching the inputted conditions. Or returns every
     * lottery if conditions are left blank.
     * */
    fun listLotteries(lotteryStage: LotteryStatus?=null, campsiteId: String? = null): Flow<List<Lottery>> =
        observeAll().map { lotteries -> lotteries.filterNotNull().filter {
            (it!=null) && (lotteryStage == null || it.stage == lotteryStage) &&
                    (campsiteId == null || it.campsites.contains(campsiteId)) }
        }

}
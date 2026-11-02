package com.campsitereservationapp.data

import com.campsitereservationapp.model.Lottery
import com.campsitereservationapp.model.LotteryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores and provides [Lottery] objects.
 *
 * **Purpose:** lets owners create and run lotteries, and lets entrants find
 * the lotteries they can join or have joined.
 *
 * **Design rationale:** extends [Repository], which holds the shared rules;
 * this class adds lottery-specific queries such as [listLotteries].
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
    campsiteRepository: CampsiteRepository,
) : Repository<Lottery>(dataSource) {
    fun listLotteries(lotteryStage: LotteryStatus?=null, campsiteId: String? = null,
                      ownerId: String? = null, userId: String? = null): Flow<List<Lottery>> =
        observeAll().map { lotteries -> lotteries.filter {
            (it!=null) && (lotteryStage == null || it.stage == lotteryStage) &&
                    (campsiteId == null || it.campsiteIds.contains(campsiteId)) &&
                    (ownerId == null || it.ownerId == ownerId) &&
                    (userId == null || it.waitingList.entrantList.contains(userId))}
        }

}
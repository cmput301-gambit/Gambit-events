package com.campsitereservationapp.service

import com.campsitereservationapp.data.CampsiteRepository
import com.campsitereservationapp.data.LotteryRepository
import com.campsitereservationapp.data.MessageRepository
import com.campsitereservationapp.data.NotificationRepository
import com.campsitereservationapp.data.RatingRepository
import com.campsitereservationapp.data.ReservationRepository
import com.campsitereservationapp.data.UserRepository
import com.campsitereservationapp.model.User

/**
 * Deletes a profile, an owner's listings, or a campsite, together with
 * everything that depends on it.
 *
 * **Purpose:** one place for the "clean up everything that points at this"
 * rules, shared by three stories:
 * - US 01.08.01: an entrant deletes their own profile ([deleteProfile])
 * - US 03.02.01: an admin removes a profile ([deleteProfile])
 * - US 03.07.01: an admin removes a campsite owner who broke the rules ([removeOwner])
 *
 * **Design rationale:** a cascade touches many collections, so it doesn't
 * belong to any single repository (the same reason [NotificationHandler] lives
 * in `service/`). Each repository only knows how to delete its own items
 * ("delete the ratings by this user"); this class decides the order.
 * Dependents are deleted first and the thing itself last, so if the network
 * drops halfway, the profile or campsite is still there and the admin can
 * simply press Remove again. Every step is safe to repeat.
 *
 * **Outstanding issues:** the steps are separate writes, not one Firestore
 * batch, so a failure halfway leaves some data deleted and some not (pressing
 * Remove again finishes the job). Notifications about deleted lotteries are
 * kept, because they are the admin's notification log (US 03.08.01). Entrants
 * are not told when a campsite they booked is removed.
 *
 * @property userRepository where profiles are kept
 * @property campsiteRepository where campsites are kept
 * @property lotteryRepository where lotteries and their waiting lists are kept
 * @property reservationRepository where reservations are kept
 * @property notificationRepository where notifications are kept
 * @property ratingRepository where campsite ratings are kept
 * @property messageRepository where owner and entrant messages are kept
 */
class CascadeDeleter(
    private val userRepository: UserRepository,
    private val campsiteRepository: CampsiteRepository,
    private val lotteryRepository: LotteryRepository,
    private val reservationRepository: ReservationRepository,
    private val notificationRepository: NotificationRepository,
    private val ratingRepository: RatingRepository,
    private val messageRepository: MessageRepository,
) {

    /**
     * Deletes a profile and everything that belongs to it (US 01.08.01, 03.02.01).
     *
     * Removes the user from every lottery waiting list, deletes their
     * reservations, messages, notifications and ratings, and, if they own
     * campsites, those campsites and lotteries too. The profile itself is
     * deleted last.
     *
     * @param userId the ID of the profile to delete
     */
    suspend fun deleteProfile(userId: String) {
        // Checked even if the user isn't marked as an owner, so no campsite
        // is ever left without an owner.
        deleteOwnedListings(userId)

        lotteryRepository.removeUserFromAll(userId)
        reservationRepository.deleteByEntrant(userId).forEach { reservation ->
            messageRepository.deleteByReservation(reservation.id)
        }
        messageRepository.deleteInvolving(userId)
        notificationRepository.deleteForUser(userId)
        ratingRepository.deleteByUser(userId)

        userRepository.delete(userId)
    }

    /**
     * Removes a campsite owner who broke the app's rules (US 03.07.01).
     *
     * Deletes every campsite and lottery they own, then takes away their
     * owner role. Their entrant side (waiting lists, reservations) is kept;
     * use [deleteProfile] to remove the person entirely.
     *
     * @param userId the ID of the owner to remove
     * @return the updated user, or null if they don't exist or weren't an owner
     */
    suspend fun removeOwner(userId: String): User? {
        deleteOwnedListings(userId)
        return userRepository.removeOwnerRole(userId)
    }

    /**
     * Deletes a campsite and everything that depends on it: lotteries that
     * only list this campsite, its reservations and their messages, and its ratings.
     * Lotteries shared with other campsites just drop this one from their list.
     *
     * @param campsiteId the ID of the campsite to delete
     */
    suspend fun deleteCampsite(campsiteId: String) {
        lotteryRepository.deleteByCampsite(campsiteId)
        reservationRepository.deleteByCampsite(campsiteId).forEach { reservation ->
            messageRepository.deleteByReservation(reservation.id)
        }
        ratingRepository.deleteByCampsite(campsiteId)
        campsiteRepository.delete(campsiteId)
    }

    // Deletes every campsite and lottery an owner created.
    private suspend fun deleteOwnedListings(ownerId: String) {
        campsiteRepository.getByOwner(ownerId).forEach { deleteCampsite(it.id) }
        lotteryRepository.deleteByOwner(ownerId)
    }
}

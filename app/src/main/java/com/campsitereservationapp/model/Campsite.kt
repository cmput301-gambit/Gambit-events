package com.campsitereservationapp.model
/**
 * A campsite that entrants can join lotteries for.
 *
 * **Purpose:** a [CampsiteOwner] creates a campsite, then runs one or more
 * [Lottery] draws for it. Entrants join a lottery's [WaitingList], and
 * winners receive a [Reservation].
 *
 * **Design rationale:** plain data class with no Firestore code, so it's
 * easy to unit test. CampsiteRepository handles saving and loading.
 *
 * **Outstanding issues:**
 * - Field list not yet confirmed against the CRC card and storyboards.
 * - Storage format for [location] (plain text vs coordinates) undecided.
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property ownerId ID of the [CampsiteOwner] who listed this campsite
 * @property name display name shown to entrants
 * @property description longer text describing the campsite
 * @property location where the campsite is, as entered by the owner
 * @property capacity maximum number of reservations per lottery
 * @property posterImageId ID of the poster [Image], or null if none
 * @constructor Creates a campsite. Every field has a default, so tests can
 * set only the fields they care about.
 */
data class Campsite(
    override val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val location: String = "",
    val capacity: Int = 0,
    val posterImageId: String? = null,
) : Identifiable {

    /**
     * Checks whether this campsite has everything it needs to be saved.
     *
     * A valid campsite has a non-blank [name], a non-blank [ownerId], and a
     * [capacity] greater than zero.
     *
     * @return true if the campsite can be saved, false otherwise
     */
    fun isValid(): Boolean {
        // TODO: extend once the campsite stories are finalised
        return name.isNotBlank() && ownerId.isNotBlank() && capacity > 0
    }
}
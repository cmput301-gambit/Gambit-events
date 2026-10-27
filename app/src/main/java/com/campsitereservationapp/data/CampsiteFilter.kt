package com.campsitereservationapp.data
/**
 * A filter that acts on a [CampsiteRepository] based on its given values.
 *
 * **Purpose:** an [Entrant] wants to find a specific campsite based on their
 * qualifications. Each field is entered optionally and returns a list of
 * [Campsite]s which fulfill the requirements.
 *
 * **Design rationale:** plain data class with no Firestore code, so it's
 * easy to unit test. CampsiteRepository handles saving and loading.
 *
 * **Outstanding issues:**
 * - Does not have a [DateRange] as the class does not exist yet.
 *
 * @property searchText the text to query the [CampsiteRepository] name with.
 * @property location the string to query the [CampsiteRepository] locations against.
 * @property occupancy minimum number of people that the query results must accommodate for.
 * @constructor Creates a CampsiteFilter. Every field has a default, so tests can
 * set only the fields they care about.
 */
data class CampsiteFilter (
    var searchText: String = "",
    var location: String = "",
    var occupancy: Int = 0,
    //var dateRange: DateRange = null
) {

}
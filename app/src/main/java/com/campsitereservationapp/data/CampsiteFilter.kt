package com.campsitereservationapp.data

import com.campsitereservationapp.model.Campsite

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
 * - Does not have a [com.campsitereservationapp.model.DateRange] as the class does not exist yet.
 *
 * @property searchText the text to query the [CampsiteRepository] name with.
 * @property location the string to query the [CampsiteRepository] locations against.
 * @property occupancy minimum number of people that the query results must accommodate for.
 * @property occupancyText A string representation of the occupancy, which becomes an empty string
 * when the occupancy is 0.
 * @constructor Creates a CampsiteFilter. Every field has a default, so tests can
 * set only the fields they care about.
 */
data class CampsiteFilter (
     var searchText: String = "",
     var location: String = "",
     var occupancy: Int = 0,
    //var dateRange: DateRange = null
) {
    val occupancyText: String
        get() = if (occupancy == 0) "" else occupancy.toString()
    /**
    * Returns a static list of all campsites that fulfill the current filter.
    *
    * @param inputCampsites List of the campsites to apply the filter to
    * @return a list of campsites that fulfill the filter
    */
    public fun apply(inputCampsites: List<Campsite>): List<Campsite>{
        val ret = mutableListOf<Campsite>()
        inputCampsites.forEach {
            if(nameMatch(it) && locationMatch(it) && occupancyMatch(it)){
                ret.add(it)
            }
        }
        return ret
    }
    /**
     * Returns a bool for if the location of the given [Campsite] matches the requested location.
     * Always returns true of the location field is blank.
     *
     * @param campsite [Campsite] to apply the filter to
     * @return bool for if the campsite contains the given location.
     */
    private fun locationMatch(campsite: Campsite): Boolean {
        if(location.isBlank()) return true
        return campsite.location.contains(location, ignoreCase = true)
    }
    /**
     * Returns a bool for if the name of the given [Campsite] matches the requested name.
     * Always returns true of the name field is blank.
     *
     * @param campsite [Campsite] to apply the filter to
     * @return bool for if the campsite contains the given location.
     */
    private fun nameMatch(campsite: Campsite): Boolean{
        if(searchText.isBlank()) return true
        return campsite.name.contains(searchText, ignoreCase = true)
    }
    /**
     * Returns a bool for if the occupancy of the given campsite is greater than or equal to
     * the requested min occupancy.
     * Always returns true of the occupancy is 0 (the default value).
     *
     * @param campsite List of the campsites to apply the filter to
     * @return bool for if the campsite contains the given location.
     */
    private fun occupancyMatch(campsite: Campsite): Boolean {
        if (occupancy == 0) return true
        return campsite.capacity >= occupancy
    }

//    private fun dateRangeMatch(campsite: Campsite): Boolean {
//        return something
//    }
}
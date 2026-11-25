package com.campsitereservationapp.model

/**
 * The role that stores data and converts it to a CSV file format on request.
 *
 * **Purpose:** data class representative of a csv file.
 *
 * **Design rationale:** allows for cleaner usage of CSV files, especially when that is
 * required in two separate spots.
 *
 * **Outstanding issues:** None.
 *
 * @param headers headers that should be in the CSV file. Asserted when a new row is added.
 * @property rows are representative of the rows of the csv file to be made.
 */
class Csv(private val headers: List<String>) {
    //list of all rows
    private val rows = mutableListOf<List<String>>()

    /**
     * Adds a row to the CSV file, asserting that it has the correct number of columns.
     *
     * @param values is the strings that make up the row to add.
     */
    fun addRow(values: List<String>){
        require(values.size == headers.size) {
            "Expected ${headers.size} and received ${values.size}. "
        }
        rows += values
    }
    /**
     * Cleans a string to allow for it to be added to a csv file safely. This is done by checking and removing
     * anything that could escape the csv file early or cause format problems.
     *
     * @param value is the string to be cleaned.
     * @return a clean version of the string.
     */
    private fun escape(value: String): String =
        if (value.any { it == ',' ||
                    it == '"' ||
                    it == '\n' ||
                    it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    /**
     * Uses the data in the [Csv] object to create a string that can be read as a CSV file.
     * @return The object as a CSV readable string.
     */
    override fun toString(): String = buildString {
        appendLine(headers.joinToString(",") { escape(it) })
        rows.forEach { row -> appendLine(row.joinToString(",") { escape(it) }) }
    }
}
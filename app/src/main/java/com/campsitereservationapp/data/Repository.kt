package com.campsitereservationapp.data

import com.campsitereservationapp.model.Identifiable

/**
 * A base class for all the repositories in the app.
 *
 * **Purpose:** Provides the basic functions that allow data to be stored and fetched
 *
 * **Design rationale:** All repository classes seek to solve a similar problem, with only the firebase parsing being different.
 * Thus, this superclass will handle all the common methods.
 *
 * **Outstanding issues:** TODO: A temporary stand in until firebase is set up. Must be replaced later.
 */
abstract class Repository<T : Identifiable>(initialItems: List<T> = emptyList()) {
    private val items = initialItems.toMutableList()

    open suspend fun getAll(): List<T> = items.toList()
    open suspend fun get(id: String): T? = items.find { it.id == id }
    open suspend fun add(item: T) {
        items.removeAll { it.id == item.id } // remove any existing item with the same ID (No duplicates)
        items.add(item)
    }
    open suspend fun delete(id: String) { items.removeAll { it.id == id } }
}

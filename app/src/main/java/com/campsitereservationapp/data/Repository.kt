package com.campsitereservationapp.data

import com.campsitereservationapp.model.Identifiable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

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
    private val items = MutableStateFlow(initialItems)

    // peek into all the items in the repository
    open fun observeAll(): Flow<List<T>> = items

    // get a specific item from the repository
    open suspend fun get(id: String): T? = items.value.find { it.id == id }

    /* Add an item to the repository.*/
    open suspend fun add(item: T) {
        items.update { list -> list.filterNot { it.id == item.id } + item }
    }

    open suspend fun delete(id: String) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}
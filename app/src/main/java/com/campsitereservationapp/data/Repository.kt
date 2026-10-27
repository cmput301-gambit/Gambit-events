package com.campsitereservationapp.data

import com.campsitereservationapp.model.Identifiable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A base class for all the repositories in the app.
 *
 * **Purpose:** Provides the basic functions that allow data to be stored and fetched.
 *
 * **Design rationale:** All repository classes seek to solve a similar problem, with only the firebase parsing being different.
 * Thus, this superclass will handle all the common methods.
 *
 * **Outstanding issues:** TODO: A temporary stand in until firebase is set up. Must be replaced later.
 *
 * @param T the type of item managed by this repository, which must implement [Identifiable]
 * @param initialItems optional initial list of items to populate the repository
 */
abstract class Repository<T : Identifiable>(initialItems: List<T> = emptyList()) {
    private val items = MutableStateFlow(initialItems)

    /**
     * Returns a reactive stream of all items currently in the repository.
     *
     * @return a [Flow] emitting the latest list of items whenever repository contents change
     */
    open fun observeAll(): Flow<List<T>> = items

    /**
     * Fetches a single snapshot of all items currently in the repository.
     *
     * @return a [List] containing all items in the repository at this moment
     */
    open suspend fun getAll(): List<T> = items.value

    /**
     * Retrieves a single item by its unique identifier.
     *
     * @param id the unique identifier of the item to fetch
     * @return the item with the matching [id], or `null` if no such item exists
     */
    open suspend fun get(id: String): T? = items.value.find { it.id == id }

    /**
     * Adds an item to the repository. Rejects duplicate IDs and returns the saved item.
     *
     * @param item the item to insert
     * @return the saved item
     */
    open suspend fun add(item: T): T {
        require(items.value.none { it.id == item.id }) { "Item with id ${item.id} already exists" }
        return item
    }

    /**
     * Updates an existing item in the repository. Fails if the ID does not exist.
     *
     * @param item the item to update
     * @return the updated item
     */
    open suspend fun update(item: T): T {
        require(items.value.any { it.id == item.id }) { "Item with id ${item.id} does not exist" }
        items.update { list -> list.map { if (it.id == item.id) item else it } }
        return item
    }

    /**
     * Removes an item from the repository by its unique identifier.
     *
     * @param id the unique identifier of the item to remove
     */
    open suspend fun delete(id: String) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}
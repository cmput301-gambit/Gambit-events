package com.campsitereservationapp.data

import com.campsitereservationapp.model.Identifiable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * Base class for every repository in the app.
 *
 * **Purpose:** stores items and lets the rest of the app add, update,
 * delete, look up and observe them.
 *
 * **Design rationale:** abstract base class because every repository
 * shares the same storage code; subclasses only add type-specific queries.
 * Items are kept in a [MutableStateFlow] so every screen observing them
 * updates automatically when anything changes.
 *
 * **Outstanding issues:** backed by an in-memory list until Firestore is
 * connected (#62). Only the insides of these methods should need to change.
 *
 * @param T the type of item stored, which must have an ID
 * @param initialItems items to start with, such as [SampleData]
 */
abstract class Repository<T : Identifiable>(initialItems: List<T> = emptyList()) {

    // The current list of items. Private so only this class can change it;
    // everyone else reads through observeAll() or get().
    private val items = MutableStateFlow(initialItems)

    /**
     * Live view of every item in the repository.
     *
     * @return a stream that emits the full list every time it changes
     */
    open fun observeAll(): Flow<List<T>> = items

    /**
     * Looks up a single item by its ID.
     *
     * @param id the ID to look for
     * @return the item, or null if none has that ID
     */
    open suspend fun get(id: String): T? = items.value.find { it.id == id }

    /**
     * Creates a new unique ID for an item that's about to be added.
     *
     * Call this when building a new item, for example
     * `Campsite(id = campsiteRepository.newId(), name = "Moose Meadow")`.
     *
     * @return a unique ID not used by any other item
     */
    fun newId(): String = UUID.randomUUID().toString()

    /**
     * Adds a new item to the repository.
     *
     * @param item the item to add; its ID must already be set (see [newId])
     * @return the saved item
     * @throws IllegalArgumentException if the ID is blank or already in use
     */
    open suspend fun add(item: T): T {
        require(item.id.isNotBlank()) { "Item has no ID. Use newId() when creating it." }
        items.update { list ->
            require(list.none { it.id == item.id }) { "Item with id ${item.id} already exists" }
            list + item
        }
        return item
    }

    /**
     * Replaces an existing item that has the same ID.
     *
     * @param item the updated item
     * @return the saved item
     * @throws IllegalArgumentException if no item with this ID exists
     */
    open suspend fun update(item: T): T {
        items.update { list ->
            require(list.any { it.id == item.id }) { "Item with id ${item.id} does not exist" }
            list.map { if (it.id == item.id) item else it }
        }
        return item
    }

    /**
     * Removes the item with the given ID. Does nothing if no item has that ID.
     *
     * @param id the ID of the item to remove
     */
    open suspend fun delete(id: String) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}
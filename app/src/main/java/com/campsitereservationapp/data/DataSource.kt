package com.campsitereservationapp.data

import com.campsitereservationapp.model.Identifiable
import kotlinx.coroutines.flow.Flow

/**
 * Where a repository's items are actually kept.
 *
 * **Purpose:** hides the storage details from [Repository], so the same
 * repository code works with Firestore in the app and a plain list in tests.
 *
 * **Design rationale:** strategy pattern. [Repository] holds a Store and
 * calls it without knowing which kind it is. [MockDataSource] is used by unit
 * tests and previews; [FirestoreStore] is used by the real app. Unit tests
 * never touch the live, shared database.
 *
 * **Outstanding issues:** None.
 *
 * @param T the type of item stored, which must have an ID
 */
interface DataSource<T : Identifiable> {

    /**
     * Live view of every item.
     *
     * @return a stream that emits the full list every time it changes
     */
    fun observeAll(): Flow<List<T>>

    /**
     * Live view of the items that match a filter.
     *
     * @param filter the condition items must meet
     * @return a stream that emits the matching items every time they change
     */
    fun observeWhere(filter: Filter<T>): Flow<List<T>>

    /**
     * Returns every item right now, without watching for changes.
     *
     * @return a snapshot of all items
     */
    suspend fun getAll(): List<T>

    /**
     * Looks up a single item by its ID.
     *
     * @param id the ID to look for
     * @return the item, or null if none has that ID
     */
    suspend fun get(id: String): T?

    /**
     * Creates a new unique ID for an item that's about to be added.
     *
     * @return a unique ID not used by any other item
     */
    fun newId(): String

    /**
     * Saves a new item.
     *
     * @param item the item to save, with its ID already set
     * @throws IllegalArgumentException if an item with this ID already exists
     */
    suspend fun add(item: T)

    /**
     * Replaces the saved item that has the same ID.
     *
     * @param item the updated item
     * @throws IllegalArgumentException if no item with this ID exists
     */
    suspend fun update(item: T)

    /**
     * Removes the item with the given ID. Does nothing if no item has that ID.
     *
     * @param id the ID of the item to remove
     */
    suspend fun delete(id: String)
}
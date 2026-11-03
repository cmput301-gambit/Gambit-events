package com.campsitereservationapp.data

import com.campsitereservationapp.model.Identifiable
import kotlinx.coroutines.flow.Flow

/**
 * Base class for every repository in the app.
 *
 * **Purpose:** lets the rest of the app add, update, delete, look up and
 * observe items, and enforces the rules every repository shares.
 *
 * **Design rationale:** the actual storage is handed in as a [DataSource]
 * (strategy pattern), so the same repository works with [FirestoreStore] in
 * the app and [MockDataSource] in unit tests. This class keeps only the rules
 * that are the same for every store, such as rejecting blank IDs.
 * Subclasses add type-specific queries and their own Firestore mapping.
 *
 * **Outstanding issues:** None.
 *
 * @param T the type of item stored, which must have an ID
 * @param dataSource where the items are actually kept
 */
abstract class Repository<T : Identifiable>(private val dataSource: DataSource<T>) {

    /**
     * Live view of every item in the repository.
     *
     * @return a stream that emits the full list every time it changes
     */
    open fun observeAll(): Flow<List<T>> = dataSource.observeAll()

    /**
     * Live view of the items that match a filter.
     *
     * @param filter the condition items must meet
     * @return a stream that emits the matching items every time they change
     */
    open fun observeWhere(filter: Filter<T>): Flow<List<T>> = dataSource.observeWhere(filter)

    /**
     * Returns every item in the repository right now, without watching for changes.
     *
     * @return a snapshot of the current list
     */
    open suspend fun getAll(): List<T> = dataSource.getAll()

    /**
     * Looks up a single item by its ID.
     *
     * @param id the ID to look for
     * @return the item, or null if none has that ID
     */
    open suspend fun get(id: String): T? = dataSource.get(id)

    /**
     * Creates a new unique ID for an item that's about to be added.
     *
     * Call this when building a new item, for example
     * `Campsite(id = campsiteRepository.newId(), name = "Moose Meadow")`.
     *
     * @return a unique ID not used by any other item
     */
    fun newId(): String = dataSource.newId()

    /**
     * Adds a new item to the repository.
     *
     * @param item the item to add; its ID must already be set (see [newId])
     * @return the saved item
     * @throws IllegalArgumentException if the ID is blank or already in use
     */
    open suspend fun add(item: T): T {
        require(item.id.isNotBlank()) { "Item has no ID. Use newId() when creating it." }
        dataSource.add(item)
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
        dataSource.update(item)
        return item
    }

    /**
     * Removes the item with the given ID. Does nothing if no item has that ID.
     *
     * @param id the ID of the item to remove
     */
    open suspend fun delete(id: String) = dataSource.delete(id)

    /**
     * Adds starting items, but only if the repository is completely empty.
     *
     * Used on app startup so a fresh database has sample data to show,
     * without adding duplicates every time the app opens.
     *
     * @param items the items to add when empty, such as [SampleData.campsites]
     */
    suspend fun seedIfEmpty(items: List<T>) {
        if (getAll().isEmpty()) items.forEach { add(it) }
    }
}
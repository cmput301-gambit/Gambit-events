package com.campsitereservationapp.data

import com.campsitereservationapp.model.Identifiable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * A [DataSource] that keeps items in a list in memory.
 *
 * **Purpose:** lets unit tests and Compose previews use real repositories
 * without a network connection or the shared Firestore database.
 *
 * **Design rationale:** the list lives in a [MutableStateFlow], so anything
 * observing it updates automatically when it changes, just like Firestore's
 * live updates. Duplicate and missing ID checks happen inside [update], so
 * the check and the change happen together as one step.
 *
 * **Outstanding issues:** none.
 *
 * @param T the type of item stored, which must have an ID
 * @param initialItems items to start with, such as [SampleData.campsites]
 */
class MockDataSource<T : Identifiable>(initialItems: List<T> = emptyList()) : DataSource<T> {

    // The current list of items. Private so only this class can change it.
    private val items = MutableStateFlow(initialItems)

    override fun observeAll(): Flow<List<T>> = items.asStateFlow()

    override fun observeWhere(filter: Filter<T>): Flow<List<T>> =
        items.map { list -> list.filter(filter.matches) }

    override suspend fun getAll(): List<T> = items.value

    override suspend fun get(id: String): T? = items.value.find { it.id == id }

    override fun newId(): String = UUID.randomUUID().toString()

    override suspend fun add(item: T) {
        items.update { list ->
            require(list.none { it.id == item.id }) { "Item with id ${item.id} already exists" }
            list + item
        }
    }

    override suspend fun update(item: T) {
        items.update { list ->
            require(list.any { it.id == item.id }) { "Item with id ${item.id} does not exist" }
            list.map { if (it.id == item.id) item else it }
        }
    }

    override suspend fun delete(id: String) {
        items.update { list -> list.filterNot { it.id == id } }
    }
}
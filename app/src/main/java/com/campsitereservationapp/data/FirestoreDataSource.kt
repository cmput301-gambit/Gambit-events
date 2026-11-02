package com.campsitereservationapp.data

import android.util.Log
import com.campsitereservationapp.model.Identifiable
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * A [DataSource] that keeps items in one Firestore collection.
 *
 * **Purpose:** the real app's storage, shared by every device, with live
 * updates when anyone changes the data.
 *
 * **Design rationale:** generic, so one class serves every collection. Each
 * repository passes in its own collection name and its own conversion
 * functions ([toMap] and [fromMap]), so this class never needs to know what a
 * Campsite or User looks like. The item's ID is used as the document ID.
 * [add] and [update] run in a transaction so the "does it exist?" check and
 * the write happen together, and both throw [IllegalArgumentException] like
 * [InMemoryDataSource], so repositories behave the same with either source.
 *
 * **Outstanding issues:** transactions need a network connection, so [add]
 * and [update] fail while offline. If the database's security rules reject a
 * read (for example when test mode expires), observers receive an error.
 *
 * @param T the type of item stored, which must have an ID
 * @param collectionName the Firestore collection, such as "campsites"
 * @param toMap converts an item into the fields saved in Firestore (without the ID)
 * @param fromMap rebuilds an item from its document ID and saved fields
 */
class FirestoreDataSource<T : Identifiable>(
    private val collectionName: String,
    private val toMap: (T) -> Map<String, Any?>,
    private val fromMap: (id: String, data: Map<String, Any?>) -> T?,
) : DataSource<T> {

    // Created on first use, so building this class never touches Firebase.
    private val db by lazy { Firebase.firestore }
    private val collection by lazy { db.collection(collectionName) }

    // Converts one document into an item. A badly formed document (for example,
    // one hand-edited in the console) is skipped and logged instead of crashing.
    private fun DocumentSnapshot.toItem(): T? {
        val data = data ?: return null
        return runCatching { fromMap(id, data) }
            .onFailure { Log.w(TAG, "Skipping bad document $collectionName/$id", it) }
            .getOrNull()
    }

    // Turns a Firestore query into a Flow that emits a fresh list on every change.
    private fun Query.observe(): Flow<List<T>> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull { it.toItem() })
        }
        awaitClose { registration.remove() }
    }

    override fun observeAll(): Flow<List<T>> = collection.observe()

    override fun observeWhere(filter: Filter<T>): Flow<List<T>> =
        collection.whereEqualTo(filter.field, filter.value).observe()

    override suspend fun getAll(): List<T> =
        collection.get().await().documents.mapNotNull { it.toItem() }

    override suspend fun get(id: String): T? =
        collection.document(id).get().await().toItem()

    override fun newId(): String = collection.document().id

    override suspend fun add(item: T) {
        val ref = collection.document(item.id)
        val created = db.runTransaction { transaction ->
            if (transaction.get(ref).exists()) {
                false
            } else {
                transaction.set(ref, toMap(item))
                true
            }
        }.await()
        require(created) { "Item with id ${item.id} already exists" }
    }

    override suspend fun update(item: T) {
        val ref = collection.document(item.id)
        val updated = db.runTransaction { transaction ->
            if (!transaction.get(ref).exists()) {
                false
            } else {
                transaction.set(ref, toMap(item))
                true
            }
        }.await()
        require(updated) { "Item with id ${item.id} does not exist" }
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete().await()
    }

    private companion object {
        const val TAG = "FirestoreDataSource"
    }
}
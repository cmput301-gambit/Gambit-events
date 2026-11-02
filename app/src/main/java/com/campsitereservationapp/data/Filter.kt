package com.campsitereservationapp.data

/**
 * A condition for picking out matching items, such as "campsites owned by owner-bobby".
 *
 * **Purpose:** lets a repository ask its [DataSource] for only some items, so
 * devices download just the data they need (for example, their own notifications).
 *
 * **Design rationale:** holds the condition in two forms, one per kind of store.
 * [FirestoreStore] uses [field] and [value] so the database does the filtering
 * on the server; [MockDataSource] uses [matches] to filter a plain list.
 *
 * **Outstanding issues:** only supports "field equals value". Add more kinds
 * (ranges, "contains") only when a story needs them.
 *
 * @param T the type of item being filtered
 * @property field the name of the stored field to compare, such as "ownerId"
 * @property value the value that field must equal
 * @property matches the same condition written as Kotlin code
 */
data class Filter<T>(
    val field: String,
    val value: Any,
    val matches: (T) -> Boolean,
)
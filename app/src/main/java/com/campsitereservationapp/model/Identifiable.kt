package com.campsitereservationapp.model

/**
 * A base class for all models in the app.
 *
 * **Purpose:** Provides a unique ID, which is also used as the Firestore document ID.
 *
 * **Design rationale:** Every model in the app must use a unique ID, so that we can later store it in the firebase
 *
 * **Outstanding issues:** None
 */
interface Identifiable {
    val id: String
}
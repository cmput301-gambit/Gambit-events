package com.example.campsitereservationapp.model

/**
 * Anything with a unique ID that can be stored as a Firestore document.
 */
interface Identifiable {
    /** The unique ID, which is also used as the Firestore document ID. */
    val id: String
}
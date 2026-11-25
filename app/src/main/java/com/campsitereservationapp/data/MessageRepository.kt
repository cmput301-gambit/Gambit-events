package com.campsitereservationapp.data

import com.campsitereservationapp.model.Message
import com.campsitereservationapp.model.SenderRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * Stores and provides [Message] objects.
 *
 * **Purpose:** lets a campsite owner and the entrant who reserved their
 * campsite message each other about the reservation (US 02.12.01,
 * 01.14.01), and lets admins review every owner-to-entrant message
 * (US 03.12.01).
 *
 * **Design rationale:** extends [Repository] like every other repository.
 * Each conversation is filtered in the database by reservation, so a phone
 * only downloads the messages it needs. Sorting happens on the device so
 * Firestore doesn't need an extra index. Messages can't be edited, only
 * marked as read, so the admin log stays accurate. Its companion object
 * converts messages to and from Firestore documents.
 *
 * **Outstanding issues:** None.
 *
 * @param dataSource where messages are kept, such as [firestoreDataSource]
 * or a [MockDataSource] in tests
 */
class MessageRepository(dataSource: DataSource<Message>) : Repository<Message>(dataSource) {

    /**
     * Live conversation for one reservation, oldest first like a chat.
     *
     * @param reservationId ID of the reservation the messages are about
     * @return a stream that updates whenever a message is sent or read
     */
    fun observeForReservation(reservationId: String): Flow<List<Message>> =
        observeWhere(Filter("reservationId", reservationId) { it.reservationId == reservationId })
            .map { messages -> messages.sortedBy { it.sentAt } }

    /**
     * Live log of every message campsite owners sent to entrants, newest
     * first, for admins (US 03.12.01).
     *
     * @return a stream that updates whenever an owner sends a message
     */
    fun observeOwnerMessageLog(): Flow<List<Message>> =
        observeWhere(Filter("senderRole", SenderRole.OWNER.name) { it.senderRole == SenderRole.OWNER })
            .map { messages -> messages.sortedByDescending { it.sentAt } }

    /**
     * Live count of a user's unread messages, for a badge.
     *
     * @param userId ID of the user who received the messages
     * @return a stream that emits the number of unread messages
     */
    fun observeUnreadCount(userId: String): Flow<Int> =
        observeWhere(Filter("recipientId", userId) { it.recipientId == userId })
            .map { messages -> messages.count { !it.read } }

    /**
     * Sends a message by saving it.
     *
     * @param message the message to send; build it with [Message.fromOwner]
     * or [Message.fromEntrant] and an ID from [newId]
     * @return the saved message, or null if it isn't valid (for example, empty)
     */
    suspend fun send(message: Message): Message? =
        if (message.isValid()) add(message) else null

    /**
     * Marks a message as read and saves it.
     *
     * @param message the message the recipient opened
     * @return the saved message, now marked as read
     * @throws IllegalArgumentException if the message no longer exists
     */
    suspend fun markRead(message: Message): Message =
        if (message.read) message else update(message.markRead())

    /**
     * Deletes every message about a reservation, used when the reservation is deleted.
     *
     * @param reservationId the ID of the deleted reservation
     */
    suspend fun deleteByReservation(reservationId: String) {
        getAll().filter { it.reservationId == reservationId }.forEach { delete(it.id) }
    }

    /**
     * Deletes every message a user sent or received, used when their profile
     * is deleted (US 01.08.01, 03.02.01).
     *
     * @param userId the ID of the deleted user
     */
    suspend fun deleteInvolving(userId: String) {
        getAll().filter { it.senderId == userId || it.recipientId == userId }.forEach { delete(it.id) }
    }

    companion object {
        /**
         * Creates the Firestore data source for messages, used by [AppContainer].
         *
         * @return a data source backed by the "messages" collection
         */
        fun firestoreDataSource(): FirestoreDataSource<Message> =
            FirestoreDataSource("messages", ::toMap, ::fromMap)

        /**
         * Converts a message into the fields saved in Firestore.
         *
         * The ID is not included because it is the document ID. The sender
         * role is saved by name (such as "OWNER") and the time as text.
         *
         * @param message the message to convert
         * @return the fields to save
         */
        fun toMap(message: Message): Map<String, Any?> = mapOf(
            "reservationId" to message.reservationId,
            "senderId" to message.senderId,
            "recipientId" to message.recipientId,
            "senderRole" to message.senderRole.name,
            "text" to message.text,
            "sentAt" to message.sentAt.toString(),
            "read" to message.read,
        )

        /**
         * Rebuilds a message from a Firestore document.
         *
         * Missing text fields fall back to empty. A missing or unknown sender
         * role, or a missing time, throws, so the document is skipped rather
         * than shown in the wrong place.
         *
         * @param id the document ID, used as the message ID
         * @param data the saved fields
         * @return the rebuilt message
         * @throws IllegalArgumentException if the role or time is missing or invalid
         */
        fun fromMap(id: String, data: Map<String, Any?>): Message {
            val sentAt = requireNotNull(data["sentAt"] as? String) { "Message is missing its send time" }
            return Message(
                id = id,
                reservationId = data["reservationId"] as? String ?: "",
                senderId = data["senderId"] as? String ?: "",
                recipientId = data["recipientId"] as? String ?: "",
                senderRole = SenderRole.valueOf(data["senderRole"] as? String ?: ""),
                text = data["text"] as? String ?: "",
                sentAt = LocalDateTime.parse(sentAt),
                read = data["read"] as? Boolean ?: false,
            )
        }
    }
}
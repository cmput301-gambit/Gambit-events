package com.campsitereservationapp.model

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Errors that can occur when validating a [Message].
 */
enum class MessageError {
    BLANK_RESERVATION,
    BLANK_SENDER,
    BLANK_RECIPIENT,
    SENT_TO_SELF,
    BLANK_TEXT,
    TEXT_TOO_LONG,
}

/**
 * Who sent a [Message], so admins can find owner-to-entrant messages.
 */
enum class SenderRole {
    OWNER,
    ENTRANT,
}

/**
 * A message between a campsite owner and the entrant who reserved their campsite.
 *
 * **Purpose:** the data behind owners messaging an entrant about a
 * reservation (US 02.12.01), entrants messaging the owner back
 * (US 01.14.01), and the admin log of owner messages (US 03.12.01).
 *
 * **Design rationale:** immutable data class like [Reservation], with no
 * Android or Firestore code, so it is easy to unit test. Every message
 * belongs to one reservation, so a conversation is just the messages with
 * the same [reservationId]. The same class works in both directions;
 * [senderRole] records which way it went. Use [fromOwner] and [fromEntrant]
 * to build one, so the sender and recipient always match the reservation.
 *
 * **Outstanding issues:** no editing or deleting, on purpose, so the admin
 * log can't be changed after the fact.
 *
 * @property id unique ID, also used as the Firestore document ID
 * @property reservationId ID of the [Reservation] this message is about
 * @property senderId userId of the person who sent it
 * @property recipientId userId of the person it was sent to
 * @property senderRole whether the owner or the entrant sent it
 * @property text what the message says
 * @property sentAt when it was sent, to the second
 * @property read true once the recipient has opened it
 */
data class Message(
    override val id: String = "",
    val reservationId: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val senderRole: SenderRole = SenderRole.OWNER,
    val text: String = "",
    val sentAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
    val read: Boolean = false,
) : Identifiable {

    /**
     * Validates the message fields and returns a list of errors found.
     *
     * @return list of [MessageError] violations
     */
    fun validate(): List<MessageError> {
        val errors = mutableListOf<MessageError>()
        if (reservationId.isBlank()) errors.add(MessageError.BLANK_RESERVATION)
        if (senderId.isBlank()) errors.add(MessageError.BLANK_SENDER)
        if (recipientId.isBlank()) errors.add(MessageError.BLANK_RECIPIENT)
        if (senderId.isNotBlank() && senderId == recipientId) errors.add(MessageError.SENT_TO_SELF)
        if (text.isBlank()) errors.add(MessageError.BLANK_TEXT)
        if (text.trim().length > MAX_LENGTH) errors.add(MessageError.TEXT_TOO_LONG)
        return errors
    }

    /**
     * Checks whether this message has everything it needs to be sent.
     *
     * @return true if the message has no validation errors, false otherwise
     */
    fun isValid(): Boolean = validate().isEmpty()

    /**
     * Returns a copy of this message marked as read.
     *
     * @return the updated message; this one is not changed
     */
    fun markRead(): Message = copy(read = true)

    companion object {
        /** The longest message allowed, in characters. */
        const val MAX_LENGTH = 1000

        /**
         * Creates a message from a campsite owner to the entrant who made a
         * reservation (US 02.12.01).
         *
         * @param id unique ID for the message
         * @param reservation the reservation the message is about
         * @param ownerId userId of the campsite owner sending it
         * @param text what the message says; extra spaces are trimmed
         * @return a new unread message
         */
        fun fromOwner(id: String, reservation: Reservation, ownerId: String, text: String): Message =
            Message(
                id = id,
                reservationId = reservation.id,
                senderId = ownerId,
                recipientId = reservation.entrantId,
                senderRole = SenderRole.OWNER,
                text = text.trim(),
            )

        /**
         * Creates a message from the entrant who made a reservation to the
         * campsite owner (US 01.14.01).
         *
         * @param id unique ID for the message
         * @param reservation the reservation the message is about
         * @param ownerId userId of the campsite owner receiving it
         * @param text what the message says; extra spaces are trimmed
         * @return a new unread message
         */
        fun fromEntrant(id: String, reservation: Reservation, ownerId: String, text: String): Message =
            Message(
                id = id,
                reservationId = reservation.id,
                senderId = reservation.entrantId,
                recipientId = ownerId,
                senderRole = SenderRole.ENTRANT,
                text = text.trim(),
            )
    }
}

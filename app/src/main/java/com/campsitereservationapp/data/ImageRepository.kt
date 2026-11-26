package com.campsitereservationapp.data

import com.campsitereservationapp.model.Image
import com.campsitereservationapp.model.ImageKind
import kotlinx.coroutines.flow.Flow

/**
 * Stores and provides [Image] objects.
 *
 * **Purpose:** the single source of uploaded pictures (campsite posters and
 * profile pictures) for every screen, including the admin's image browser
 * (US 03.06.01).
 *
 * **Design rationale:** extends [Repository], which holds the shared rules;
 * this class only adds image-specific queries. Clearing a deleted image from
 * its campsite or user touches other repositories, so that lives in
 * [com.campsitereservationapp.service.CascadeDeleter.deleteImage].
 *
 * **Outstanding issues:** [observeAll] downloads every picture, which is fine
 * for a class project but would need paging for many images.
 *
 * @param dataSource where images are kept, such as [firestoreDataSource]
 * or a [MockDataSource] in tests
 */
class ImageRepository(dataSource: DataSource<Image>) : Repository<Image>(dataSource) {

    /**
     * Looks up an image by the ID saved on a campsite or user.
     *
     * Campsites saved before images were uploaded hold a phone-only
     * `content://` link instead of an ID. Those (and blank IDs) return null
     * instead of crashing, since a link isn't a valid Firestore document ID.
     *
     * @param id the image ID, or null
     * @return the image, or null if there isn't one
     */
    suspend fun find(id: String?): Image? {
        if (id.isNullOrBlank() || "/" in id) return null
        return get(id)
    }

    /**
     * Live list of the images one user uploaded, such as an owner's campsite posters.
     *
     * @param uploaderId the user who uploaded them
     * @return a stream that emits that user's images every time they change
     */
    fun observeByUploader(uploaderId: String): Flow<List<Image>> =
        observeWhere(Filter("uploaderId", uploaderId) { it.uploaderId == uploaderId })

    /**
     * Deletes every image attached to a campsite or user, used when that
     * campsite or profile is deleted.
     *
     * @param targetId the ID of the deleted campsite or user
     */
    suspend fun deleteByTarget(targetId: String) {
        getAll().filter { it.targetId == targetId }.forEach { delete(it.id) }
    }

    companion object {
        /**
         * Creates the Firestore data source for images, used by [AppContainer].
         *
         * @return a data source backed by the "images" collection
         */
        fun firestoreDataSource(): FirestoreDataSource<Image> =
            FirestoreDataSource("images", ::toMap, ::fromMap)

        /**
         * Converts an image into the fields saved in Firestore.
         *
         * The ID is not included because it is the document ID.
         *
         * @param image the image to convert
         * @return the fields to save
         */
        fun toMap(image: Image): Map<String, Any?> = mapOf(
            "uploaderId" to image.uploaderId,
            "kind" to image.kind.name,
            "targetId" to image.targetId,
            "data" to image.data,
        )

        /**
         * Rebuilds an image from a Firestore document.
         *
         * Missing fields fall back to the same defaults as [Image]'s constructor.
         *
         * @param id the document ID, used as the image ID
         * @param data the saved fields
         * @return the rebuilt image
         */
        fun fromMap(id: String, data: Map<String, Any?>): Image = Image(
            id = id,
            uploaderId = data["uploaderId"] as? String ?: "",
            kind = (data["kind"] as? String)
                ?.let { name -> ImageKind.entries.find { it.name == name } }
                ?: ImageKind.CAMPSITE,
            targetId = data["targetId"] as? String ?: "",
            data = data["data"] as? String ?: "",
        )
    }
}

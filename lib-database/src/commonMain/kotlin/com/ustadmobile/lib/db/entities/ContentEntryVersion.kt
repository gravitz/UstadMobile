package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.door.annotation.ReplicateEntity
import com.ustadmobile.door.annotation.ReplicateEtag
import com.ustadmobile.door.annotation.ReplicateLastModified
import com.ustadmobile.door.annotation.Trigger
import com.ustadmobile.door.annotation.Triggers
import kotlinx.serialization.Serializable

/**
 * Represents an available version of a ContentEntry. This may be stored on the same endpoint as
 * the database, or it could be stored on any other http server.
 *
 *
 */
@Entity
@ReplicateEntity(
    tableId = ContentEntryVersion.TABLE_ID,
    remoteInsertStrategy = ReplicateEntity.RemoteInsertStrategy.INSERT_INTO_RECEIVE_VIEW,
)
@Triggers(
    arrayOf(
        Trigger(
            name = "content_entry_version_remote_ins",
            order = Trigger.Order.INSTEAD_OF,
            on = Trigger.On.RECEIVEVIEW,
            events = [Trigger.Event.INSERT],
            conditionSql = TRIGGER_CONDITION_WHERE_NEWER,
            sqlStatements = [TRIGGER_UPSERT]
        )
    )
)
@Serializable
data class ContentEntryVersion(
    @PrimaryKey(autoGenerate = true)
    var cevUid: Long = 0,

    /**
     * The related ContentEntryUid
     */
    var cevContentEntryUid: Long = 0,

    /**
     * The URL to content item e.g.
     * https://endpoint.com/api/content/cevUid/tincan.xml file for xAPI content
     * https://endpoint.com/api/content/cevUid/tincan.xml for epubs
     * direct url for pdfs
     */
    var cevUrl: String? = "",

    /**
     * The content type that will be used to determine what screen will be used to display the
     * content. This can use TYPE_ presets (or content type could be added to import plugins etc).
     */
    var cevContentType: String? = "",

    /**
     * The URL for the sitemap that would be used to download this for offline usage if available.
     *
     * e.g. https://endpoint.com/api/content/cevUid/sitemap.xml
     */
    var cevSitemapUrl: String? = null,

    /**
     * The estimated total size (in bytes)
     */
    var cevSize: Long = 0,

    var cevInActive: Boolean = false,

    /**
     * The last modified of the actual content
     */
    var cevLastModified: Long = 0,

    @ReplicateEtag
    @ReplicateLastModified
    var cevLct: Long = 0,
) {

    companion object {

        @Suppress("unused")
        const val TYPE_EPUB = "epub"

        @Suppress("unused")
        const val TYPE_VIDEO = "video"

        const val TYPE_PDF = "pdf"

        @Suppress("unused")
        const val TYPE_XAPI = "xapi"

        const val PATH_POSTFIX = "api/content/"

        const val TABLE_ID = 738

    }
}
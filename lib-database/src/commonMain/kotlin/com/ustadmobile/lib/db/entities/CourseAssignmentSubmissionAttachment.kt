package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.door.annotation.*
import com.ustadmobile.lib.db.entities.CourseAssignmentSubmissionAttachment.Companion.TABLE_ID
import kotlinx.serialization.Serializable

@Entity
@EntityWithAttachment
@ReplicateEntity(
    tableId = TABLE_ID,
    remoteInsertStrategy = ReplicateEntity.RemoteInsertStrategy.INSERT_INTO_RECEIVE_VIEW,
)
@Triggers(arrayOf(
        Trigger(
                name = "courseassignmentsubmissionattachment_remote_insert",
                order = Trigger.Order.INSTEAD_OF,
                on = Trigger.On.RECEIVEVIEW,
                events = [Trigger.Event.INSERT],
                sqlStatements = [
                    TRIGGER_UPSERT_WHERE_NEWER
                    ])
    )
)
@Serializable
class CourseAssignmentSubmissionAttachment {

    @PrimaryKey(autoGenerate = true)
    var casaUid: Long = 0

    var casaSubmissionUid: Long = 0

    var casaMimeType: String? = null

    var casaFileName: String? = null

    @AttachmentUri
    var casaUri: String? = null

    @AttachmentMd5
    var casaMd5: String? = null

    @AttachmentSize
    var casaSize: Int = 0

    @ReplicateLastModified
    @ReplicateEtag
    var casaTimestamp: Long = 0

    companion object {

        const val TABLE_ID = 90
    }

}
package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.door.annotation.LastChangedBy
import com.ustadmobile.door.annotation.LocalChangeSeqNum
import com.ustadmobile.door.annotation.MasterChangeSeqNum
import com.ustadmobile.door.annotation.SyncableEntity
import kotlinx.serialization.Serializable

@Entity
@SyncableEntity(tableId = LeavingReason.TABLE_ID)
@Serializable
class LeavingReason() {

    constructor(uid: Long, title: String?) : this(){
        leavingReasonUid = uid
        leavingReasonTitle = title
    }

    @PrimaryKey(autoGenerate = true)
    var leavingReasonUid: Long = 0

    var leavingReasonTitle: String? = null

    @MasterChangeSeqNum
    var leavingReasonMCSN: Long = 0

    @LocalChangeSeqNum
    var leavingReasonCSN: Long = 0

    @LastChangedBy
    var leavingReasonLCB: Int = 0

    companion object {

        const val TABLE_ID = 410

        const val MOVED_TITLE = "Moved"

        const val MOVED_UID = 10000L

        const val MEDICAL_TITLE = "Medical"

        const val MEDICAL_UID = 10001L

        const val TRANSPORT_PROBLEM_TITLE = "Transportation problem"

        const val TRANSPORT_PROBLEM_UID = 10002L

        const val FAMILY_PROBLEM_TITLE = "Family economic problem"

        const val FAMILY_PROBLEM_UID = 10003L

        const val FAILED_TITLE = "Failed test"

        const val FAILED_UID = 10004L

        const val PASSED_TITLE = "Passed test"

        const val PASSED_UID = 10005L


        val FIXED_UIDS = mapOf(MOVED_TITLE to MOVED_UID,
                MEDICAL_TITLE to MEDICAL_UID,
                TRANSPORT_PROBLEM_TITLE to TRANSPORT_PROBLEM_UID,
                FAMILY_PROBLEM_TITLE to FAMILY_PROBLEM_UID,
                FAILED_TITLE to FAILED_UID,
                PASSED_TITLE to PASSED_UID)

    }
}
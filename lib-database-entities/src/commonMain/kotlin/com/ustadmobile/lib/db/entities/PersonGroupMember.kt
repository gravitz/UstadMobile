package com.ustadmobile.lib.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.lib.database.annotation.*

@UmEntity(tableId = 44)
@Entity
class PersonGroupMember() {


    @PrimaryKey(autoGenerate = true)
    var groupMemberUid: Long = 0

    @UmIndexField
    @ColumnInfo(index = true)
    var groupMemberPersonUid: Long = 0

    @UmIndexField
    @ColumnInfo(index = true)
    var groupMemberGroupUid: Long = 0

    @UmSyncMasterChangeSeqNum
    var groupMemberMasterCsn: Long = 0

    @UmSyncLocalChangeSeqNum
    var groupMemberLocalCsn: Long = 0

    @UmSyncLastChangedBy
    var groupMemberLastChangedBy: Int = 0

    constructor(personUid:Long, groupUid:Long) : this(){
        this.groupMemberPersonUid = personUid
        this.groupMemberGroupUid = groupUid
    }
}

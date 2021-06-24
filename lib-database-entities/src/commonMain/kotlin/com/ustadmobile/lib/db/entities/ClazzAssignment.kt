package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.door.annotation.*
import com.ustadmobile.lib.db.entities.ClazzAssignment.Companion.TABLE_ID
import kotlinx.serialization.Serializable

@Entity
@SyncableEntity(tableId = TABLE_ID)
      /*  notifyOnUpdate =  [
            """
     
        """
        ],
        syncFindAllQuery = """
          
          
        """)*/
/*        notifyOnUpdate = [
            """
        SELECT DISTINCT DeviceSession.dsDeviceId AS deviceId, $TABLE_ID AS tableId FROM 
        ChangeLog
        JOIN ClazzAssignment ON ChangeLog.chTableId = $TABLE_ID AND ClazzAssignment.caUid = ChangeLog.chEntityPk
        JOIN Clazz ON Clazz.clazzUid = ClazzAssignment.caClazzUid 
        JOIN Person ON Person.personUid IN (${Clazz.ENTITY_PERSONS_WITH_PERMISSION_PT1}  ${Role.PERMISSION_ASSIGNMENT_SELECT } ${Clazz.ENTITY_PERSONS_WITH_PERMISSION_PT2})
        JOIN DeviceSession ON DeviceSession.dsPersonUid = Person.personUid
        """
        ],
        syncFindAllQuery = """
        SELECT ClazzAssignment.* FROM
        ClazzAssignment
        JOIN Clazz ON Clazz.clazzUid = ClazzAssignment.caClazzUid
        JOIN Person ON Person.personUid IN  (${Clazz.ENTITY_PERSONS_WITH_PERMISSION_PT1} ${Role.PERMISSION_ASSIGNMENT_SELECT } ${Clazz.ENTITY_PERSONS_WITH_PERMISSION_PT2})
        JOIN DeviceSession ON DeviceSession.dsPersonUid = Person.personUid
        WHERE DeviceSession.dsDeviceId = :clientId  
    """)*/
@Serializable
open class ClazzAssignment {

    @PrimaryKey(autoGenerate = true)
    var caUid: Long = 0

    var caTitle: String? = null

    var caDescription: String? = null

    var caDeadlineDate: Long = Long.MAX_VALUE

    var caStartDate: Long = 0

    var caLateSubmissionType: Int = 0

    var caLateSubmissionPenalty: Int = 0

    var caGracePeriodDate: Long = 0

    var caActive: Boolean = true

    var caClassCommentEnabled: Boolean = true

    var caPrivateCommentsEnabled: Boolean = false

    var caClazzUid: Long = 0

    @LocalChangeSeqNum
    var caLocalChangeSeqNum: Long = 0

    @MasterChangeSeqNum
    var caMasterChangeSeqNum: Long = 0

    @LastChangedBy
    var caLastChangedBy: Int = 0

    @LastChangedTime
    var caLct: Long = 0

    companion object {

        const val TABLE_ID = 520

        const val ASSIGNMENT_LATE_SUBMISSION_REJECT = 1
        const val ASSIGNMENT_LATE_SUBMISSION_PENALTY = 2
        const val ASSIGNMENT_LATE_SUBMISSION_ACCEPT = 3
    }


}
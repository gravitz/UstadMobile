package com.ustadmobile.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.ustadmobile.door.DoorDataSourceFactory
import com.ustadmobile.door.annotation.*
import com.ustadmobile.lib.db.entities.*
import kotlin.js.JsName

@Repository
@Dao
abstract class CourseBlockDao : BaseDao<CourseBlock>, OneToManyJoinDao<CourseBlock> {

    @Query("""
    REPLACE INTO CourseBlockReplicate(cbPk, cbDestination)
      SELECT DISTINCT CourseBlock.cbUid AS cbPk,
             :newNodeId AS cbDestination
        FROM UserSession
             JOIN PersonGroupMember 
                    ON UserSession.usPersonUid = PersonGroupMember.groupMemberPersonUid
             ${Clazz.JOIN_FROM_PERSONGROUPMEMBER_TO_CLAZZ_VIA_SCOPEDGRANT_PT1}
                    ${Role.PERMISSION_CLAZZ_SELECT} 
                    ${Clazz.JOIN_FROM_PERSONGROUPMEMBER_TO_CLAZZ_VIA_SCOPEDGRANT_PT2}
               JOIN CourseBlock
                    ON CourseBlock.cbClazzUid = Clazz.clazzUid                
       WHERE UserSession.usClientNodeId = :newNodeId
         AND UserSession.usStatus = ${UserSession.STATUS_ACTIVE}
         AND CourseBlock.cbLct != COALESCE(
             (SELECT cbVersionId
                FROM CourseBlockReplicate
               WHERE cbPk = CourseBlock.cbUid
                 AND cbDestination = :newNodeId), 0) 
      /*psql ON CONFLICT(cbPk, cbDestination) DO UPDATE
             SET cbPending = true
      */       
    """)
    @ReplicationRunOnNewNode
    @ReplicationCheckPendingNotificationsFor([CourseBlock::class])
    abstract suspend fun replicateOnNewNode(@NewNodeIdParam newNodeId: Long)



    @Query("""
         REPLACE INTO CourseBlockReplicate(cbPk, cbDestination)
  SELECT DISTINCT CourseBlock.cbUid AS cbPk,
         UserSession.usClientNodeId AS cbDestination
    FROM ChangeLog
         JOIN CourseBlock
             ON ChangeLog.chTableId = ${CourseBlock.TABLE_ID}
                AND ChangeLog.chEntityPk = CourseBlock.cbUid
             JOIN Clazz
                    ON  Clazz.clazzUid = CourseBlock.cbClazzUid
         ${Clazz.JOIN_FROM_CLAZZ_TO_USERSESSION_VIA_SCOPEDGRANT_PT1}
              ${Role.PERMISSION_CLAZZ_SELECT}
              ${Clazz.JOIN_FROM_CLAZZ_TO_USERSESSION_VIA_SCOPEDGRANT_PT2}  
   WHERE UserSession.usClientNodeId != (
         SELECT nodeClientId 
           FROM SyncNode
          LIMIT 1)
     AND CourseBlock.cbLct != COALESCE(
         (SELECT cbVersionId
            FROM CourseBlockReplicate
           WHERE cbPk = CourseBlock.cbUid
             AND cbDestination = UserSession.usClientNodeId), 0)
 /*psql ON CONFLICT(cbPk, cbDestination) DO UPDATE
     SET cbPending = true
  */               
    """)
    @ReplicationRunOnChange([CourseBlock::class])
    @ReplicationCheckPendingNotificationsFor([CourseBlock::class])
    abstract suspend fun replicateOnChange()

    @JsName("findByUid")
    @Query("SELECT * FROM CourseBlock WHERE cbUid = :uid")
    abstract suspend fun findByUidAsync(uid: Long): CourseBlock?

    @Update
    abstract suspend fun updateAsync(entity: CourseBlock): Int


    @Query("""
        SELECT * 
          FROM CourseBlock 
               LEFT JOIN ClazzAssignment as assignment
               ON assignment.caUid = CourseBlock.cbTableUid
               AND CourseBlock.cbType = ${CourseBlock.BLOCK_ASSIGNMENT_TYPE}
               LEFT JOIN ContentEntry as entry
               ON entry.contentEntryUid = CourseBlock.cbTableUid
               AND CourseBlock.cbType = ${CourseBlock.BLOCK_CONTENT_TYPE}
         WHERE cbClazzUid = :clazzUid
           AND cbActive
      ORDER BY cbIndex
          """)
    abstract suspend fun findAllCourseBlockByClazzUidAsync(clazzUid: Long): List<CourseBlockWithEntity>

    @Query("""
        SELECT * 
          FROM CourseBlock 
               LEFT JOIN ClazzAssignment as assignment
               ON assignment.caUid = CourseBlock.cbTableUid
               AND CourseBlock.cbType = ${CourseBlock.BLOCK_ASSIGNMENT_TYPE}
               LEFT JOIN ContentEntry as entry
               ON entry.contentEntryUid = CourseBlock.cbTableUid
               AND CourseBlock.cbType = ${CourseBlock.BLOCK_CONTENT_TYPE}
         WHERE cbClazzUid = :clazzUid
           AND cbActive
           AND NOT cbHidden
      ORDER BY cbIndex
    """)
    abstract fun findAllCourseBlockByClazzUidLive(clazzUid: Long): DoorDataSourceFactory<Int, CourseBlockWithEntity>


    override suspend fun deactivateByUids(uidList: List<Long>){

    }

}
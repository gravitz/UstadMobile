package com.ustadmobile.core.db.dao

import com.ustadmobile.door.DoorDataSourceFactory
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.ustadmobile.door.annotation.Repository
import com.ustadmobile.lib.db.entities.*

@Dao
@Repository
abstract class ClazzAssignmentContentJoinDao : BaseDao<ClazzAssignmentContentJoin>,
        OneToManyJoinDao<ClazzAssignmentContentJoin> {

    @Query(FINDBY_CLAZZ_ASSIGNMENT_UID)
    abstract suspend fun findAllContentByClazzAssignmentUidAsync(clazzAssignmentUid: Long, personUid : Long)
            :List <ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer>

    @Query(FINDBY_CLAZZ_ASSIGNMENT_UID)
    abstract fun findAllContentByClazzAssignmentUidDF(clazzAssignmentUid: Long, personUid : Long)
            : DoorDataSourceFactory<Int, ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer>


    @Query("""
        SELECT ContentEntry.title AS contentEntryTitle, ContentEntry.contentEntryUid,
        
        COALESCE(ClazzAssignmentRollUp.cacheStudentScore,0) AS resultScore,
                                           
        COALESCE(ClazzAssignmentRollUp.cacheMaxScore,0) AS resultMax,
                                                         
        COALESCE(ClazzAssignmentRollUp.cacheProgress,0) AS progress,                   
                 
        0 as resultScaled,
                            
        COALESCE(ClazzAssignmentRollUp.cacheContentComplete,'FALSE') AS contentComplete,
        
        COALESCE(ClazzAssignmentRollUp.cacheSuccess,0) AS success,
        
        COALESCE(ClazzAssignmentRollUp.cachePenalty,0) AS penalty,
        
       COALESCE((CASE WHEN ClazzAssignmentRollUp.cacheContentComplete 
                        THEN 1 ELSE 0 END),0) AS totalCompletedContent,
                        
        1 as totalContent,                
        
        
        MIN(ResultSource.timestamp) AS startDate,
        MAX(ResultSource.timestamp)  AS endDate,
        SUM(ResultSource.resultDuration) AS duration, 
        COUNT(DISTINCT(ResultSource.contextRegistration)) AS attempts
        
        FROM  ClazzAssignmentContentJoin
                LEFT JOIN ContentEntry 
                ON ContentEntry.contentEntryUid = cacjContentUid 
                
                LEFT JOIN ClazzAssignmentRollUp
                ON cacheContentEntryUid = ClazzAssignmentContentJoin.cacjContentUid
                AND cachePersonUid = :personUid
                AND cacheClazzAssignmentUid = :clazzAssignmentUid
                
                
                LEFT JOIN ( SELECT StatementEntity.timestamp, 
                    StatementEntity.statementContentEntryUid, 
                    StatementEntity.contextRegistration, StatementEntity.resultDuration 
                         FROM PersonGroupMember
                 ${Person.JOIN_FROM_PERSONGROUPMEMBER_TO_PERSON_VIA_SCOPEDGRANT_PT1} ${Role.PERMISSION_PERSON_LEARNINGRECORD_SELECT} ${Person.JOIN_FROM_PERSONGROUPMEMBER_TO_PERSON_VIA_SCOPEDGRANT_PT2}
                           LEFT JOIN StatementEntity 
                           ON StatementEntity.statementPersonUid = :personUid 
                            AND StatementEntity.statementContentEntryUid IN (SELECT cacjContentUid
                                  FROM ClazzAssignmentContentJoin
                                        JOIN ClazzAssignment 
                                        ON ClazzAssignment.caUid = cacjAssignmentUid
                                 WHERE cacjAssignmentUid = :clazzAssignmentUid
                                  AND StatementEntity.timestamp
                                        BETWEEN ClazzAssignment.caStartDate
                                        AND ClazzAssignment.caGracePeriodDate)
                WHERE PersonGroupMember.groupMemberPersonUid = :accountPersonUid               
                  AND PersonGroupMember.groupMemberActive  
             GROUP BY StatementEntity.statementUid) AS ResultSource
          ON ClazzAssignmentContentJoin.cacjContentUid = ResultSource.statementContentEntryUid   
            
        WHERE ClazzAssignmentContentJoin.cacjAssignmentUid = :clazzAssignmentUid
          AND ClazzAssignmentContentJoin.cacjActive    
          AND (ContentEntry.publik OR :personUid != 0)  
     GROUP BY ClazzAssignmentContentJoin.cacjContentUid, ResultSource.statementContentEntryUid
     ORDER BY ContentEntry.title, ContentEntry.contentEntryUid   
    """)
    abstract fun findAllContentWithAttemptsByClazzAssignmentUid(clazzAssignmentUid: Long,
                                                               personUid: Long, accountPersonUid: Long):
            DoorDataSourceFactory<Int, ContentWithAttemptSummary>

    @Query("""
        UPDATE ClazzAssignmentContentJoin 
           SET cacjActive = :active,
               cacjLCB = (SELECT nodeClientId 
                            FROM SyncNode LIMIT 1) 
        WHERE cacjContentUid = :contentUid 
          AND cacjAssignmentUid = :clazzAssignmentUid""")
    abstract suspend fun updateInActiveByClazzAssignmentContentJoinUid(contentUid: Long, active : Boolean,
                                                                       clazzAssignmentUid: Long)

    suspend fun deactivateByUids(uidList: List<Long>, clazzAssignmentUid: Long) {
        uidList.forEach {
            updateInActiveByClazzAssignmentContentJoinUid(it, false, clazzAssignmentUid)
        }
    }

    override suspend fun deactivateByUids(uidList: List<Long>) {
        // not used
    }


    companion object{
        const val FINDBY_CLAZZ_ASSIGNMENT_UID =
                """
                    SELECT ContentEntry.*, ContentEntryParentChildJoin.*, 
                            Container.*, 
                             COALESCE(ClazzAssignmentRollUp.cacheStudentScore,0) AS resultScore,
                                           
                             COALESCE(ClazzAssignmentRollUp.cacheMaxScore,0) AS resultMax,
                                                         
                             COALESCE(ClazzAssignmentRollUp.cacheProgress,0) AS progress,                            
                            
                             COALESCE(ClazzAssignmentRollUp.cacheContentComplete,'FALSE') AS contentComplete,
                                 
                             COALESCE(ClazzAssignmentRollUp.cacheSuccess,0) AS success,
                             
                             COALESCE(ClazzAssignmentRollUp.cachePenalty,0) AS penalty,
                               
                             COALESCE((CASE WHEN ClazzAssignmentRollUp.cacheContentComplete 
                                            THEN 1 ELSE 0 END),0) AS totalCompletedContent,
                        
                             1 as totalContent
                           
                             
                      FROM ClazzAssignmentContentJoin
                            LEFT JOIN ContentEntry 
                            ON ContentEntry.contentEntryUid = cacjContentUid 
                            
                            LEFT JOIN ContentEntryParentChildJoin 
                            ON ContentEntryParentChildJoin.cepcjChildContentEntryUid = ContentEntry.contentEntryUid 
                           
                            LEFT JOIN ClazzAssignmentRollUp
                            ON cacheContentEntryUid = ClazzAssignmentContentJoin.cacjContentUid
                                AND cachePersonUid = :personUid
                                AND cacheClazzAssignmentUid = :clazzAssignmentUid
                                                        
                            
                            LEFT JOIN Container 
                            ON Container.containerUid = 
                                (SELECT containerUid 
                                   FROM Container 
                                  WHERE containerContentEntryUid =  ContentEntry.contentEntryUid 
                               ORDER BY cntLastModified DESC LIMIT 1)
                               
                    WHERE ClazzAssignmentContentJoin.cacjAssignmentUid = :clazzAssignmentUid
                      AND ClazzAssignmentContentJoin.cacjActive
                      AND NOT ContentEntry.ceInactive
                      AND (ContentEntry.publik OR :personUid != 0)
                      ORDER BY ContentEntry.title ASC , 
                               ContentEntryParentChildJoin.childIndex, ContentEntry.contentEntryUid
                               """
    }


}

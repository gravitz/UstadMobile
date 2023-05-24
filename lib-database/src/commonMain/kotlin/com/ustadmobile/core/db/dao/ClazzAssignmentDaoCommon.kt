package com.ustadmobile.core.db.dao

import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.CourseAssignmentSubmission.Companion.SUBMITTER_ENROLLED_BUT_NOT_IN_GROUP

object ClazzAssignmentDaoCommon {

    private const val SELECT_PERSONUID_IF_ENROLLED_ELSE_0_FOR_PERSONUID_AND_ASSIGNMENTUID_SQL = """
        SELECT COALESCE(
                (SELECT ClazzEnrolment.clazzEnrolmentPersonUid
                   FROM ClazzEnrolment
                  WHERE ClazzEnrolment.clazzEnrolmentPersonUid = :accountPersonUid
                    AND ClazzEnrolment.clazzEnrolmentRole = ${ClazzEnrolment.ROLE_STUDENT}
                    AND ClazzEnrolment.clazzEnrolmentClazzUid = 
                        (SELECT ClazzAssignment.caClazzUid
                           FROM ClazzAssignment
                          WHERE ClazzAssignment.caUid = :assignmentUid)
                  LIMIT 1), 0)
    """



    /**
     * Get the submitterUid for the given assignment uid and person uid, if any.
     * See doc on submitterUid on CourseAssignmentSubmission.casSubmitterUid
     *
     * Requires accountPersonUid and assignmentUid as variables
     */
    //language=RoomSql
    const val SELECT_SUBMITTER_UID_FOR_PERSONUID_AND_ASSIGNMENTUID_SQL = """
        SELECT CASE
                    WHEN (SELECT caGroupUid
                            FROM ClazzAssignment
                           WHERE caUid = :assignmentUid) = 0
                         THEN ($SELECT_PERSONUID_IF_ENROLLED_ELSE_0_FOR_PERSONUID_AND_ASSIGNMENTUID_SQL)       
                    ELSE COALESCE(
                          (SELECT CourseGroupMember.cgmGroupNumber
                             FROM CourseGroupMember
                            WHERE ($SELECT_PERSONUID_IF_ENROLLED_ELSE_0_FOR_PERSONUID_AND_ASSIGNMENTUID_SQL) > 0
                              AND CourseGroupMember.cgmSetUid = 
                                  (SELECT caGroupUid
                                     FROM ClazzAssignment
                                    WHERE caUid = :assignmentUid)
                              AND CourseGroupMember.cgmPersonUid = :accountPersonUid), $SUBMITTER_ENROLLED_BUT_NOT_IN_GROUP)
                    END
    """

    const val SUBMITTER_LIST_WITHOUT_ASSIGNMENT_CTE = """
             WITH SubmitterList (submitterId, name)
            AS (SELECT DISTINCT ClazzEnrolment.clazzEnrolmentPersonUid AS submitterId, 
                       Person.firstNames || ' ' || Person.lastName AS name
                  FROM ClazzEnrolment
                  
                       JOIN Person 
                       ON Person.personUid = ClazzEnrolment.clazzEnrolmentPersonUid
                       
                 WHERE :groupUid = 0 
                   AND clazzEnrolmentClazzUid = :clazzUid
                   AND clazzEnrolmentActive
                   AND clazzEnrolmentRole = ${ClazzEnrolment.ROLE_STUDENT}
              GROUP BY submitterId, name
            UNION                 
             SELECT DISTINCT CourseGroupMember.cgmGroupNumber AS submitterId,
                    :group || ' ' || CourseGroupMember.cgmGroupNumber AS name  
               FROM CourseGroupMember
                    JOIN CourseGroupSet
                    ON CourseGroupSet.cgsUid = :groupUid
              WHERE CourseGroupMember.cgmSetUid = CourseGroupSet.cgsUid
                AND CourseGroupMember.cgmGroupNumber != 0
           GROUP BY submitterId, name
            )
        """

    const val WITH_HAS_LEARNINGRECORD_UPDATE_PERMISSION_SQL = """
            WITH AssignmentPermission (hasPermission) AS
            (SELECT EXISTS(
               SELECT PrsGrpMbr.groupMemberPersonUid
                  FROM Clazz
                       ${Clazz.JOIN_FROM_CLAZZ_TO_USERSESSION_VIA_SCOPEDGRANT_PT1}
                          ${Role.PERMISSION_PERSON_LEARNINGRECORD_SELECT}
                          ${Clazz.JOIN_FROM_SCOPEDGRANT_TO_PERSONGROUPMEMBER}
                 WHERE Clazz.clazzUid = :clazzUid
                   AND PrsGrpMbr.groupMemberPersonUid = :loggedInPersonUid))
        """

    /**
     * CTE that will have a single row/column indicating if the person logged in (accountPersonUid)
     * has the learningrecord select permission for the given assignment's clazzUid.
     */
    //language=RoomSql
    const val HAS_LEARNINGRECORD_SELECT_PERMISSION_CTE_SQL = """
            HasLearningRecordSelectPermission (hasPermission) AS
            (SELECT EXISTS(
                    SELECT ScopedGrant.sgUid
                      FROM PersonGroupMember
                           JOIN ScopedGrant
                                ON ScopedGrant.sgGroupUid = PersonGroupMember.groupMemberGroupUid
                     WHERE PersonGroupMember.groupMemberPersonUid = :accountPersonUid
                       AND (ScopedGrant.sgTableId = ${Clazz.TABLE_ID}
                            OR
                            ScopedGrant.sgTableId = ${ScopedGrant.ALL_TABLES})
                       AND (ScopedGrant.sgEntityUid = 
                            (SELECT ClazzAssignment.caClazzUid
                               FROM ClazzAssignment
                              WHERE ClazzAssignment.caUid = :assignmentUid)
                            OR 
                            ScopedGrant.sgEntityUid = ${ScopedGrant.ALL_ENTITIES})
                       AND (ScopedGrant.sgPermissions & ${Role.PERMISSION_PERSON_LEARNINGRECORD_SELECT}) > 0)
            )
        """

    /**
     * Shorthand CTE that will get the clazzUid for a given assignment uid in the database
     */
    //Language=RoomSql
    const val ASSIGNMENT_CLAZZ_UID_CTE_SQL = """
        AssignmentClazzUid(clazzUid) AS
        (SELECT ClazzAssignment.caClazzUid
           FROM ClazzAssignment
          WHERE ClazzAssignment.caUid = :assignmentUid)  
    """

    //Language=RoomSql
    private const val SUBMITTER_UID_CTE = """
        AccountSubmitterUid(accountSubmitterUid) AS 
        ($SELECT_SUBMITTER_UID_FOR_PERSONUID_AND_ASSIGNMENTUID_SQL)
    """


    //language=RoomSql
    private const val SELECT_GROUPSET_UID_FOR_ASSIGNMENT_UID_SQL = """
        SELECT ClazzAssignment.caGroupUid
                   FROM ClazzAssignment
                  WHERE ClazzAssignment.caUid = :assignmentUid
    """

    private const val SELECT_ASSIGNMENT_IS_PEERMARKED_SQL = """
        ((SELECT ClazzAssignment.caMarkingType
           FROM ClazzAssignment
          WHERE ClazzAssignment.caUid = :assignmentUid) = ${ClazzAssignment.MARKED_BY_PEERS})
    """

    /**
     * Find the list of submitters that are visible for a given assignmentUid and a given
     * accountPersonUid (e.g. the active user).
     *
     * If the activePersonUid has the learner record select permission for the course, then they
     * can see all submitters.
     *
     * Otherwise, if this is an assignment with peer marking then this will be a list of all the
     * submitters that the activepersonuid has been assigned to review.
     */
    //language=RoomSql
    const val SUBMITTER_LIST_CTE2_SQL = """
        SubmitterList(submitterId, name) AS 
        -- List of submitter uids and names if individual assignment eg caGroupUid = 0
        (SELECT DISTINCT ClazzEnrolment.clazzEnrolmentPersonUid AS submitterId, 
                Person.firstNames || ' ' || Person.lastName AS name
           FROM ClazzEnrolment
                JOIN Person 
                     ON Person.personUid = ClazzEnrolment.clazzEnrolmentPersonUid
          WHERE ($SELECT_GROUPSET_UID_FOR_ASSIGNMENT_UID_SQL) = 0
            AND ClazzEnrolment.clazzEnrolmentClazzUid = (SELECT clazzUid FROM AssignmentClazzUid)
            AND ClazzEnrolment.clazzEnrolmentRole = ${ClazzEnrolment.ROLE_STUDENT}
            -- either the active user has learnign record select permission on class or is an assigned reviewer for submitter
            AND (
                (SELECT hasPermission 
                   FROM HasLearningRecordSelectPermission)
                OR  
                 -- check if the active person eg accountpersonuid is assigned to mark this peer
                 ($SELECT_ASSIGNMENT_IS_PEERMARKED_SQL
                  AND
                  EXISTS(SELECT PeerReviewerAllocation.praUid
                           FROM PeerReviewerAllocation
                          WHERE PeerReviewerAllocation.praAssignmentUid = :assignmentUid
                            AND PeerReviewerAllocation.praToMarkerSubmitterUid = ClazzEnrolment.clazzEnrolmentPersonUid
                            AND PeerReviewerAllocation.praMarkerSubmitterUid = :accountPersonUid))
                 )
         UNION
         -- List of submitter uids and names if the assignment is submitted by groups e.g. caGroupUid != 0
         SELECT DISTINCT CourseGroupMember.cgmGroupNumber AS submitterId,
                :group || ' ' || CourseGroupMember.cgmGroupNumber AS name
           FROM CourseGroupMember
          WHERE ($SELECT_GROUPSET_UID_FOR_ASSIGNMENT_UID_SQL) != 0
            AND CourseGroupMember.cgmSetUid = ($SELECT_GROUPSET_UID_FOR_ASSIGNMENT_UID_SQL)
            -- either the active user has learning record select permission on class or is an assigned reviewer for submitter
            AND (
                (SELECT hasPermission 
                   FROM HasLearningRecordSelectPermission)
                OR 
                --check if the active user is in a group that was allocated to do a peer review of the given submitter uid
                ($SELECT_ASSIGNMENT_IS_PEERMARKED_SQL
                 AND
                 EXISTS(SELECT PeerReviewerAllocation.praUid
                          FROM PeerReviewerAllocation
                         WHERE PeerReviewerAllocation.praAssignmentUid = :assignmentUid
                           AND PeerReviewerAllocation.praToMarkerSubmitterUid = CourseGroupMember.cgmGroupNumber
                           AND PeerReviewerAllocation.praMarkerSubmitterUid = 
                               (SELECT CourseGroupMemberInner.cgmGroupNumber
                                  FROM CourseGroupMember CourseGroupMemberInner
                                 WHERE CourseGroupMemberInner.cgmSetUid = ($SELECT_GROUPSET_UID_FOR_ASSIGNMENT_UID_SQL)
                                   AND CourseGroupMemberInner.cgmPersonUid = :accountPersonUid
                                 LIMIT 1)
                        ))
            )
        )
        
    """


    const val SORT_DEADLINE_ASC = 1

    const val SORT_DEADLINE_DESC = 2

    const val SORT_TITLE_ASC = 3

    const val SORT_TITLE_DESC = 4

}
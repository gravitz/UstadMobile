package com.ustadmobile.core.domain.assignment.submitmark

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.util.ext.roundTo
import com.ustadmobile.door.ext.withDoorTransactionAsync
import com.ustadmobile.door.util.systemTimeInMillis
import com.ustadmobile.lib.db.entities.CourseAssignmentMark
import com.ustadmobile.lib.db.entities.CourseAssignmentSubmission
import com.ustadmobile.lib.db.entities.CourseBlock
import com.ustadmobile.lib.db.entities.ext.shallowCopy

class SubmitMarkUseCase {

    suspend operator fun invoke(
        db: UmAppDatabase,
        activeUserPersonUid: Long,
        assignmentUid: Long,
        submitterUid: Long,
        draftMark: CourseAssignmentMark,
        submissions: List<CourseAssignmentSubmission>,
        courseBlock: CourseBlock,
    ) {
        val applyPenalty = submissions.isNotEmpty() &&
            (submissions.maxOf { it.casTimestamp }) > courseBlock.cbDeadlineDate

        db.withDoorTransactionAsync {
            val activeUserSubmitterUid = db.clazzAssignmentDao.getSubmitterUid(
                assignmentUid = assignmentUid,
                accountPersonUid = activeUserPersonUid,
            )

            val markToRecord = draftMark.shallowCopy {
                camAssignmentUid = assignmentUid
                camSubmitterUid = submitterUid
                camMarkerSubmitterUid = activeUserSubmitterUid
                camMarkerPersonUid = activeUserPersonUid
                camMaxMark = courseBlock.cbMaxPoints.toFloat()
                if(applyPenalty) {
                    camPenalty = (camMark * (courseBlock.cbLateSubmissionPenalty.toFloat()/100f))
                        .roundTo(2)
                    camMark -= camPenalty
                }
            }

            db.courseAssignmentMarkDao.insertAsync(markToRecord)
        }
    }
}
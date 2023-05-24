package com.ustadmobile.lib.db.entities

import androidx.room.Embedded

/**
 * @param submitterUid the personUid for individual assignments, group number for group assignments. 0 if the active person is not a submitter
 */
@kotlinx.serialization.Serializable
data class ClazzAssignmentCourseBlockAndSubmitterUid(
    @Embedded
    var clazzAssignment: ClazzAssignment? = null,
    @Embedded
    var courseBlock: CourseBlock? = null,
    var submitterUid: Long = 0,
) {



}
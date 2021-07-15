package com.ustadmobile.lib.db.entities

import androidx.room.Embedded
import kotlinx.serialization.Serializable

@Serializable
class ClazzAssignmentWithMetrics : ClazzAssignment() {

    @Embedded
    var progressSummary: AssignmentProgressSummary? = null

    @Embedded
    var studentScore: ContentEntryStatementScoreProgress? = null

}
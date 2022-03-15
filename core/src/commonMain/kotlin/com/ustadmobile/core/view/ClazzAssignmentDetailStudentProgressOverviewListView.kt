package com.ustadmobile.core.view

import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.db.entities.AssignmentProgressSummary
import com.ustadmobile.lib.db.entities.PersonWithAttemptsSummary


interface ClazzAssignmentDetailStudentProgressOverviewListView: UstadListView<PersonWithAttemptsSummary, PersonWithAttemptsSummary> {

    var progressSummary: DoorLiveData<AssignmentProgressSummary?>?

    companion object {
        const val VIEW_NAME = "ClassAssignmentDetailStudentProgressOverviewListView"
    }

}
package com.ustadmobile.core.view

import com.ustadmobile.door.DoorDataSourceFactory
import com.ustadmobile.lib.db.entities.Clazz
import com.ustadmobile.lib.db.entities.ClazzWithDisplayDetails
import com.ustadmobile.lib.db.entities.Schedule

interface ClazzDetailOverviewView: UstadDetailView<ClazzWithDisplayDetails> {

    var scheduleList: DoorDataSourceFactory<Int, Schedule>?

    var clazzCodeVisible: Boolean

    companion object {

        const val VIEW_NAME = "ClazzDetailOverviewView"

    }

}
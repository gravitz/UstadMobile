package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.view.ClazzAssignmentDetailStudentProgressView
import com.ustadmobile.core.view.ListViewMode
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.core.view.UstadView.Companion.ARG_CLAZZ_ASSIGNMENT_UID
import com.ustadmobile.core.view.UstadView.Companion.ARG_PERSON_UID
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.lib.db.entities.ClazzAssignment
import com.ustadmobile.lib.db.entities.CommentsWithPerson
import com.ustadmobile.lib.db.entities.ContentEntryWithAttemptsSummary
import com.ustadmobile.lib.db.entities.UmAccount
import kotlinx.coroutines.withTimeoutOrNull
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

class ClazzAssignmentDetailStudentProgressPresenter(context: Any, arguments: Map<String, String>, view: ClazzAssignmentDetailStudentProgressView,
                                                    di: DI, lifecycleOwner: DoorLifecycleOwner,
                                                    val newPrivateCommentListener: DefaultNewCommentItemListener =
                                                            DefaultNewCommentItemListener(di, context,
                                                                    arguments[ARG_CLAZZ_ASSIGNMENT_UID]?.toLong() ?: 0L,
                                                                    ClazzAssignment.TABLE_ID, false,
                                                                    arguments[ARG_PERSON_UID]?.toLong() ?: 0L))
    : UstadDetailPresenter<ClazzAssignmentDetailStudentProgressView, ClazzAssignment>(context, arguments, view, di, lifecycleOwner),
        NewCommentItemListener by newPrivateCommentListener {


    override suspend fun onCheckEditPermission(account: UmAccount?): Boolean {
        return false
    }

    override val persistenceMode: PersistenceMode
        get() = PersistenceMode.DB

    var selectedPersonUid: Long = 0

    var selectedClazzAssignmentUid: Long= 0


    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)
        selectedPersonUid = arguments[ARG_PERSON_UID]?.toLong() ?: 0
        selectedClazzAssignmentUid = arguments[ARG_CLAZZ_ASSIGNMENT_UID]?.toLong() ?: 0
    }

    override suspend fun onLoadEntityFromDb(db: UmAppDatabase): ClazzAssignment? {
        val clazzAssignment = withTimeoutOrNull(2000) {
            db.clazzAssignmentDao.findByUidAsync(selectedClazzAssignmentUid)
        } ?: ClazzAssignment()

        view.person = withTimeoutOrNull(2000){
            db.personDao.findByUidAsync(selectedPersonUid)
        }

        view.clazzAssignmentContent =
                withTimeoutOrNull(2000) {
                    repo.clazzAssignmentContentJoinDao.findAllContentByClazzAssignmentUidDF(
                            clazzAssignment.caUid, selectedPersonUid)
                }

        view.studentScore = repo.clazzAssignmentDao.getStatementScoreProgressForAssignment(
                clazzAssignment.caUid, selectedPersonUid)

        if(clazzAssignment.caPrivateCommentsEnabled){
            view.clazzAssignmentPrivateComments = repo.commentsDao.findPrivateByEntityTypeAndUidAndForPersonLive2(
                    ClazzAssignment.TABLE_ID, clazzAssignment.caUid,
                    selectedPersonUid)
        }

        return clazzAssignment
    }

}
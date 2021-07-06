package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.view.ClazzWorkDetailView
import com.ustadmobile.core.view.ClazzWorkEditView
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.door.doorMainDispatcher
import com.ustadmobile.lib.db.entities.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.kodein.di.DI


// Suggestion: Add a property to the view called 'tabs' which is a List<String>, then move the logic
// that determines visibility to the presenter.
class ClazzWorkDetailPresenter(context: Any,
                          arguments: Map<String, String>, view: ClazzWorkDetailView,
                           di: DI, lifecycleOwner: DoorLifecycleOwner)
    : UstadDetailPresenter<ClazzWorkDetailView, ClazzWork>(context, arguments, view, di, lifecycleOwner) {

    override val persistenceMode: PersistenceMode
        get() = PersistenceMode.DB

    override suspend fun onLoadEntityFromDb(db: UmAppDatabase): ClazzWork? {
        val entityUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L

        val clazzWork = withTimeoutOrNull(2000) {
            db.clazzWorkDao.findByUidAsync(entityUid)
        } ?: ClazzWork()

        view.ustadFragmentTitle = clazzWork.clazzWorkTitle

        return clazzWork
    }

    override fun onCreate(savedState: Map<String, String>?) {
        val clazzWorkUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L
        val loggedInPersonUid = accountManager.activeAccount.personUid
        GlobalScope.launch(doorMainDispatcher()) {
            val clazzWork = withTimeoutOrNull(2000) {
                db.clazzWorkDao.findByUidAsync(clazzWorkUid)
            } ?: ClazzWork()
            view.progressOverviewVisible = db.clazzDao.personHasPermissionWithClazz(loggedInPersonUid,
                    clazzWork.clazzWorkClazzUid, Role.PERMISSION_PERSON_LEARNINGRECORD_SELECT)
        }

        super.onCreate(savedState)

    }

    override suspend fun onCheckEditPermission(account: UmAccount?): Boolean {
        //TODO: why not use the normal permission check here? We have the UPDATE_CLASSWORK permission
        // which is checked by class.

        val loggedInPersonUid = accountManager.activeAccount.personUid

        val loggedInPerson: Person? = withTimeoutOrNull(2000){
            db.personDao.findByUid(loggedInPersonUid)
        }
        if(loggedInPerson?.admin == true){
            return true
        }

        val clazzEnrolment: ClazzEnrolment? = withTimeoutOrNull(2000) {
            db.clazzEnrolmentDao.findByPersonUidAndClazzUidAsync(loggedInPersonUid,
                    entity?.clazzWorkClazzUid?: 0L)
        }

        return if(clazzEnrolment == null){
            false
        }else{
            clazzEnrolment.clazzEnrolmentRole == ClazzEnrolment.ROLE_TEACHER
        }


    }

    override fun handleClickEdit() {
        val entityUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L
        systemImpl.go(ClazzWorkEditView.VIEW_NAME, mapOf(ARG_ENTITY_UID to entityUid.toString()),
                context)
    }
}
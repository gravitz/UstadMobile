package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.MessageIdOption
import com.ustadmobile.core.view.*
import com.ustadmobile.core.view.PersonListView.Companion.ARG_FILTER_EXCLUDE_MEMBERSOFCLAZZ
import com.ustadmobile.core.view.PersonListView.Companion.ARG_FILTER_EXCLUDE_MEMBERSOFSCHOOL
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.db.entities.Clazz
import com.ustadmobile.lib.db.entities.Role.Companion.PERMISSION_CLAZZ_INSERT
import com.ustadmobile.lib.db.entities.UmAccount

class ClazzList2Presenter(context: Any, arguments: Map<String, String>, view: ClazzList2View,
                          lifecycleOwner: DoorLifecycleOwner, systemImpl: UstadMobileSystemImpl,
                          db: UmAppDatabase, repo: UmAppDatabase,
                          activeAccount: DoorLiveData<UmAccount?>)
    : UstadListPresenter<ClazzList2View, Clazz>(context, arguments, view, lifecycleOwner, systemImpl,
        db, repo, activeAccount) {

    var searchQuery: String = "%"

    var loggedInPersonUid = 0L

    var currentSortOrder: SortOrder = SortOrder.ORDER_NAME_ASC

    private var filterExcludeMembersOfSchool : Long = 0

    enum class SortOrder(val messageId: Int) {
        ORDER_NAME_ASC(MessageID.sort_by_name_asc),
        ORDER_NAME_DSC(MessageID.sort_by_name_desc),
        ORDER_ATTENDANCE_ASC(MessageID.attendance_low_to_high),
        ORDER_ATTENDANCE_DESC(MessageID.attendance_high_to_low)
    }

    class ClazzListSortOption(val sortOrder: SortOrder, context: Any) : MessageIdOption(sortOrder.messageId, context)

    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)

        filterExcludeMembersOfSchool = arguments[ARG_FILTER_EXCLUDE_MEMBERSOFSCHOOL]?.toLong() ?: 0L

        loggedInPersonUid = UmAccountManager.getActivePersonUid(context)
        getAndSetList(SortOrder.ORDER_NAME_ASC)
        view.sortOptions = SortOrder.values().toList().map { ClazzListSortOption(it, context) }
    }

    override suspend fun onCheckAddPermission(account: UmAccount?): Boolean {
        //return repo.clazzDao.personHasPermission(loggedInPersonUid, PERMISSION_CLAZZ_INSERT)
        return true
    }

    private fun getAndSetList(sortOrder: SortOrder) {
        view.list = when(sortOrder) {
            SortOrder.ORDER_ATTENDANCE_ASC -> repo.clazzDao.findAllActiveClazzesSortByNameAsc(
                    searchQuery, loggedInPersonUid, filterExcludeMembersOfSchool)
            SortOrder.ORDER_ATTENDANCE_DESC -> repo.clazzDao.findAllActiveClazzesSortByNameDesc(
                    searchQuery, loggedInPersonUid, filterExcludeMembersOfSchool)
            SortOrder.ORDER_NAME_ASC -> repo.clazzDao.findAllActiveClazzesSortByNameAsc(
                    searchQuery, loggedInPersonUid, filterExcludeMembersOfSchool)
            SortOrder.ORDER_NAME_DSC -> repo.clazzDao.findAllActiveClazzesSortByNameDesc(
                    searchQuery, loggedInPersonUid, filterExcludeMembersOfSchool)
        }
    }

    override fun handleClickEntry(entry: Clazz) {
        if(mListMode == ListViewMode.BROWSER) {
            systemImpl.go(ClazzDetailView.VIEW_NAME,
                    mapOf(UstadView.ARG_ENTITY_UID to entry.clazzUid.toString()), context)
        }
    }

    override fun handleClickCreateNewFab() {
        systemImpl.go(ClazzEdit2View.VIEW_NAME, mapOf(), context)
    }

    override fun handleClickSortOrder(sortOption: MessageIdOption) {
        val sortOrder = (sortOption as? ClazzListSortOption)?.sortOrder ?: return
        if(sortOrder != currentSortOrder) {
            currentSortOrder = sortOrder
            getAndSetList(sortOrder)
        }
    }
}
package com.ustadmobile.core.controller

import com.ustadmobile.core.account.UstadAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.view.AboutView
import com.ustadmobile.core.view.AccountListView
import com.ustadmobile.core.view.GetStartedView
import com.ustadmobile.core.view.PersonDetailView
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.lib.db.entities.UmAccount

class AccountListPresenter(context: Any, arguments: Map<String, String>, view: AccountListView,
                           val impl: UstadMobileSystemImpl = UstadMobileSystemImpl.instance,
                           private val accountManager: UstadAccountManager)
    : UstadBaseController<AccountListView>(context, arguments, view) {

    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)

        view.accountListLive = accountManager.storedAccountsLive
        view.activeAccountLive = accountManager.activeAccountLive
    }

    fun handleClickAddAccount(){
        impl.go(GetStartedView.VIEW_NAME, mapOf(), context)
    }

    fun handleClickDeleteAccount(account: UmAccount){
        accountManager.removeAccount(account)
    }

    fun handleClickProfile(personUid: Long){
        impl.go(PersonDetailView.VIEW_NAME, mapOf(ARG_ENTITY_UID to personUid.toString()),
                context)
    }

    fun handleClickAbout(){
        impl.go(AboutView.VIEW_NAME, mapOf(), context)
    }

    fun handleClickLogout(account: UmAccount){
        accountManager.removeAccount(account)
    }
}

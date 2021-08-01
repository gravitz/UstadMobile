package com.ustadmobile.core.view

import com.ustadmobile.lib.db.entities.PersonWithSaleInfo


interface PersonWithSaleInfoListView: UstadListView<PersonWithSaleInfo, PersonWithSaleInfo> {

    fun showAddLE(show: Boolean)

    companion object {
        const val VIEW_NAME = "PersonWithSaleInfoListView"
    }

}
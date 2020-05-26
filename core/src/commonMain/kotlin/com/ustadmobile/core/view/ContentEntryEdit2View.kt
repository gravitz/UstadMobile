package com.ustadmobile.core.view

import com.ustadmobile.core.controller.ContentEntryEdit2Presenter
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UMStorageDir
import com.ustadmobile.lib.db.entities.Container
import com.ustadmobile.lib.db.entities.ContentEntryWithLanguage


interface ContentEntryEdit2View: UstadEditView<ContentEntryWithLanguage> , UstadBaseFeedbackMessageView{

    var licenceOptions: List<ContentEntryEdit2Presenter.LicenceMessageIdOptions>?

    var selectedStorageIndex: Int

    var jobTimeStamp: Long

    suspend fun saveContainerOnExit(entryUid: Long,selectedBaseDir: String, db: UmAppDatabase, repo: UmAppDatabase): Container?

    fun setUpStorageOptions(storageOptions:List<UMStorageDir>)

    fun formatLabel(storage: UMStorageDir): String

    companion object {

        const val VIEW_NAME = "ContentEntryEdit2EditView"

        const val CONTENT_TYPE = "content_type"

    }

}
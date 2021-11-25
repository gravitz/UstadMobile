package com.ustadmobile.view

import com.ustadmobile.core.controller.LanguageListPresenter
import com.ustadmobile.core.controller.UstadListPresenter
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.view.LanguageListView
import com.ustadmobile.lib.db.entities.Language
import com.ustadmobile.view.ext.createItemWithIconTitleAndDescription
import react.RBuilder
import react.RProps

class LanguageListComponent(mProps: RProps): UstadListComponent<Language, Language>(mProps) ,
    LanguageListView{

    private var mPresenter: LanguageListPresenter? = null

    override val displayTypeRepo: Any?
        get() = dbRepo?.languageDao

    override val listPresenter: UstadListPresenter<*, in Language>?
        get() = mPresenter

    override val viewName: String
        get() = LanguageListView.VIEW_NAME

    override fun onCreateView() {
        super.onCreateView()
        title = getString(MessageID.languages)
        showCreateNewItem = true
        createNewText = getString(MessageID.add_a_new_language)
        fabManager?.text = getString(MessageID.language)
        mPresenter = LanguageListPresenter(this, arguments,
            this, di, this)
        mPresenter?.onCreate(mapOf())
    }

    override fun RBuilder.renderListItem(item: Language) {
        createItemWithIconTitleAndDescription("language",item.name,
            "${item.iso_639_2_standard}/${item.iso_639_3_standard}")
    }

    override fun handleClickEntry(entry: Language) {
        mPresenter?.handleClickEntry(entry)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        mPresenter?.onDestroy()
        mPresenter = null
    }
}
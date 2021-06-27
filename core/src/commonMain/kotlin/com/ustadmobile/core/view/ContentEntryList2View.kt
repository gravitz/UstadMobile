package com.ustadmobile.core.view

import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer

interface ContentEntryList2View: UstadListView<ContentEntry, ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer> {

    fun showContentEntryAddOptions(parentEntryUid: Long)

    /**
     * Show ContentEntryList in picker mode so the user can select a folder to move entries to.
     *
     * @param selectedContentEntryParentChildJoinUids a string which is a comma separated list of
     * the selected ContentEntryParentChildJoinUids (e.g. that should be saved to the savedStateHandle)
     */
    fun showMoveEntriesFolderPicker(selectedContentEntryParentChildJoinUids: String)

    var title: String?

    var editOptionVisible: Boolean

    companion object {

        const val ARG_SHOW_ONLY_FOLDER_FILTER = "folder"

        const val ARG_DISPLAY_CONTENT_BY_OPTION = "displayOption"

        const val ARG_DISPLAY_CONTENT_BY_CLAZZ = "displayContentByClazz"

        const val ARG_DISPLAY_CONTENT_BY_PARENT = "displayContentByParent"

        const val ARG_DISPLAY_CONTENT_BY_DOWNLOADED = "displayContentByDownloaded"

        const val ARG_CLAZZ_ASSIGNMENT_FILTER = "clazzAssignmentFilter"

        const val VIEW_NAME = "ContentEntryListView"

        const val ARG_MOVING_CONTENT = "SelectedItems"

        const val ARG_MOVING_COUNT = "moveCount"

        const val ARG_SELECT_FOLDER_VISIBLE = "selectFolderVisible"

    }

}
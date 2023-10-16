package com.ustadmobile.libuicompose.view.contententry

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.paging.*
import com.toughra.ustadmobile.databinding.*
import com.ustadmobile.lib.db.entities.*

@Composable
@Preview
private fun UstadContentEntryListItemPreview() {
    UstadContentEntryListItem(
        contentEntry = ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer()
            .apply {
                contentEntryUid = 1
                leaf = true
                ceInactive = true
                scoreProgress = ContentEntryStatementScoreProgress().apply {
                    progress = 10
                    penalty = 20
                    success = RESULT_SUCCESS
                }
                contentTypeFlag = ContentEntry.TYPE_INTERACTIVE_EXERCISE
                title = "Content Title"
                description = "Content Description"
            }
    )
}
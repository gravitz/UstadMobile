package com.ustadmobile.libuicompose.view.clazzassignment.courseblockedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.ustadmobile.core.viewmodel.courseblock.edit.CourseBlockEditUiState
import com.ustadmobile.core.viewmodel.courseblock.edit.CourseBlockEditViewModel
import com.ustadmobile.lib.db.entities.CourseBlock
import com.ustadmobile.libuicompose.components.UstadCourseBlockEdit
import kotlinx.coroutines.Dispatchers
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle

@Composable
fun CourseBlockEditScreen(
    viewModel: CourseBlockEditViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(
        CourseBlockEditUiState(), Dispatchers.Main.immediate
    )

    CourseBlockEditScreen(
        uiState = uiState,
        onCourseBlockChange = viewModel::onEntityChanged
    )
}

@Composable
fun CourseBlockEditScreen(
    uiState: CourseBlockEditUiState,
    onCourseBlockChange: (CourseBlock?) -> Unit,
) {
    UstadCourseBlockEdit(
        uiState = uiState,
        onCourseBlockChange = onCourseBlockChange,
        onClickEditDescription = {

        }
    )
}

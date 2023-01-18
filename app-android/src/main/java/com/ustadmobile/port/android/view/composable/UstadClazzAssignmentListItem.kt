package com.ustadmobile.port.android.view.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.toughra.ustadmobile.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ustadmobile.core.controller.SubmissionConstants
import com.ustadmobile.core.viewmodel.listItemUiState
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.port.android.util.compose.messageIdMapResource
import com.ustadmobile.port.android.util.compose.rememberFormattedDateTime
import com.ustadmobile.port.android.view.ClazzAssignmentDetailOverviewFragment.Companion.ASSIGNMENT_STATUS_MAP
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UstadClazzAssignmentListItem(

    modifier: Modifier = Modifier,

    assignment: ClazzAssignmentWithMetrics,

    courseBlock: CourseBlockWithCompleteEntity,

    onClickAssignment: (ClazzAssignmentWithMetrics?) -> Unit = {}

){

    val blockUiState = courseBlock.listItemUiState

    val assignmentUiState = assignment.listItemUiState

    ListItem(
        modifier = modifier
            .clickable {
                onClickAssignment(assignment)
            },

        icon = {
            Icon(
                Icons.Default.AssignmentTurnedIn,
                contentDescription = "",
                modifier = Modifier.size(40.dp)
            )
        },
        text = { Text(assignment.caTitle ?: "") },
        secondaryText = {
            Column{
                if (blockUiState.cbDescriptionVisible){
                    Text(text = courseBlock.cbDescription ?: "")
                }

                DateAndPointRow(
                    courseBlock = courseBlock,
                    assignment = assignment
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ){
                    if (assignmentUiState.submissionStatusIconVisible){
                        Icon(
                            painter = painterResource(
                                id = ASSIGNMENT_STATUS_MAP[assignment.fileSubmissionStatus]
                                    ?: R.drawable.ic_baseline_done_all_24),
                            contentDescription = "")
                    }

                    if (assignmentUiState.submissionStatusVisible){
                        Text(text = messageIdMapResource(
                            map = SubmissionConstants.STATUS_MAP,
                            key = assignment.fileSubmissionStatus)
                        )
                    }
                }
                
                if (assignmentUiState.progressTextVisible){
                    Text(text = stringResource(
                        id = R.string.three_num_items_with_name_with_comma,

                        assignment.progressSummary
                            ?.calculateNotSubmittedStudents() ?: 0,
                        stringResource(R.string.not_submitted_cap),

                        assignment.progressSummary?.submittedStudents ?: 0,
                        stringResource(R.string.submitted_cap),

                        assignment.progressSummary?.markedStudents ?: 0,
                        stringResource(R.string.marked)
                    ))
                }
            }
        }
    )
}

@Composable
private fun DateAndPointRow(
    courseBlock: CourseBlockWithCompleteEntity,
    assignment: ClazzAssignmentWithMetrics,
){

    val blockUiState = courseBlock.listItemUiState

    val assignmentUiState = assignment.listItemUiState

    val dateTime = rememberFormattedDateTime(
        timeInMillis = courseBlock.cbDeadlineDate,
        timeZoneId = TimeZone.getDefault().id
    )

    Row{
        if (blockUiState.cbDeadlineDateVisible){
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = "",
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = dateTime)
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (assignmentUiState.assignmentMarkVisible){
            Text("${assignment.mark?.camMark ?: 0}/" +
                    "${courseBlock.cbMaxPoints} " +
                    stringResource(id = R.string.points))
        }
    }
}

@Composable
@Preview
private fun UstadClazzAssignmentListItemPreview() {

    val assignment = ClazzAssignmentWithMetrics().apply {
        caTitle = "Module"
        mark = CourseAssignmentMark().apply {
            camPenalty = 20
            camMark = 20F
        }
        progressSummary = AssignmentProgressSummary().apply {
            hasMetricsPermission = false
        }
        fileSubmissionStatus = CourseAssignmentSubmission.NOT_SUBMITTED
    }

    val block = CourseBlockWithCompleteEntity().apply {
        cbDescription = "Description"
        cbDeadlineDate = 1672707505000
        cbMaxPoints = 100
        cbIndentLevel = 1
    }

    UstadClazzAssignmentListItem(
        assignment = assignment,
        courseBlock = block
    )
}
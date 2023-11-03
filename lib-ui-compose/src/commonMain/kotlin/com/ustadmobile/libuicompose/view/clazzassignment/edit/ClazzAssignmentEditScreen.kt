package com.ustadmobile.libuicompose.view.clazzassignment.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ustadmobile.core.impl.locale.entityconstants.*
import com.ustadmobile.core.viewmodel.clazzassignment.ClazzAssignmentViewModelConstants.MarkingType
import com.ustadmobile.lib.db.entities.ClazzAssignment
import com.ustadmobile.core.viewmodel.clazzassignment.edit.ClazzAssignmentEditUiState
import com.ustadmobile.core.viewmodel.clazzassignment.edit.ClazzAssignmentEditViewModel
import com.ustadmobile.lib.db.entities.CourseBlock
import com.ustadmobile.lib.db.entities.ext.shallowCopy
import com.ustadmobile.libuicompose.components.UstadInputFieldLayout
import com.ustadmobile.core.MR
import com.ustadmobile.libuicompose.components.UstadClickableTextField
import com.ustadmobile.libuicompose.components.UstadCourseBlockEdit
import com.ustadmobile.libuicompose.components.UstadExposedDropDownMenuField
import com.ustadmobile.libuicompose.components.UstadMessageIdOptionExposedDropDownMenuField
import com.ustadmobile.libuicompose.components.UstadNumberTextField
import com.ustadmobile.libuicompose.components.UstadSwitchField
import com.ustadmobile.libuicompose.util.compose.courseTerminologyEntryResource
import com.ustadmobile.libuicompose.util.compose.rememberCourseTerminologyEntries
import com.ustadmobile.libuicompose.util.ext.defaultItemPadding
import com.ustadmobile.libuicompose.util.ext.defaultScreenPadding
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ClazzAssignmentEditScreenForViewModel(viewModel: ClazzAssignmentEditViewModel) {
    val uiState by viewModel.uiState.collectAsState(initial = ClazzAssignmentEditUiState())

    ClazzAssignmentEditScreen(
        uiState = uiState,
        onChangeAssignment = viewModel::onAssignmentChanged,
        onChangeCourseBlock = viewModel::onCourseBlockChanged,
        onClickAssignReviewers =  viewModel::onClickAssignReviewers,
        onClickSubmissionType = viewModel::onClickSubmissionType,
        onClickEditDescription = viewModel::onClickEditDescription,
    )
}

@Composable
fun ClazzAssignmentEditScreen(
    uiState: ClazzAssignmentEditUiState = ClazzAssignmentEditUiState(),
    onChangeAssignment: (ClazzAssignment?) -> Unit = {},
    onChangeCourseBlock: (CourseBlock?) -> Unit = {},
    onClickSubmissionType: () -> Unit = {},
    onClickAssignReviewers: () -> Unit = {},
    onClickEditDescription: () -> Unit = {},
) {

    val terminologyEntries = rememberCourseTerminologyEntries(uiState.courseTerminology)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .defaultScreenPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    )  {

        UstadCourseBlockEdit(
            uiState = uiState.courseBlockEditUiState,
            onCourseBlockChange = onChangeCourseBlock,
            onClickEditDescription = onClickEditDescription,
        )

        UstadClickableTextField(
            modifier = Modifier
                .fillMaxWidth()
                .defaultItemPadding()
                .testTag("cgsName"),
            value = uiState.entity?.assignmentCourseGroupSetName?.let {
                "${stringResource(MR.strings.groups)}: $it"
            } ?: stringResource(MR.strings.individual_submission),
            label = { Text(stringResource(MR.strings.submission_type)) },
            enabled = uiState.groupSetEnabled,
            onClick = onClickSubmissionType,
            onValueChange = {}
        )

        UstadInputFieldLayout(
            modifier = Modifier.defaultItemPadding(),
            errorText = uiState.submissionRequiredError,
        ) {
            UstadSwitchField(
                modifier = Modifier
                    .testTag("caRequireFileSubmission"),
                label = stringResource(MR.strings.require_file_submission),
                checked = uiState.entity?.assignment?.caRequireFileSubmission ?: false,
                onChange = {
                    onChangeAssignment(
                        uiState.entity?.assignment?.shallowCopy {
                            caRequireFileSubmission = it
                        }
                    )
                },
                enabled = uiState.fieldsEnabled
            )
        }

        if (uiState.fileSubmissionVisible){
            UstadMessageIdOptionExposedDropDownMenuField(
                modifier = Modifier
                    .defaultItemPadding()
                    .testTag("caFileType"),
                value = uiState.entity?.assignment?.caFileType ?: 0,
                label = stringResource(MR.strings.file_type),
                options = FileTypeConstants.FILE_TYPE_MESSAGE_IDS,
                enabled = uiState.fieldsEnabled,
                onOptionSelected = {
                    onChangeAssignment(
                        uiState.entity?.assignment?.shallowCopy {
                            caFileType = it.value
                        }
                    )
                },
            )

            UstadNumberTextField(
                modifier = Modifier
                    .defaultItemPadding()
                    .fillMaxWidth()
                    .testTag("caSizeLimit"),
                value = (uiState.entity?.assignment?.caSizeLimit ?: 0).toFloat(),
                label = { Text(stringResource(MR.strings.size_limit)) },
                enabled = uiState.fieldsEnabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                onValueChange = {
                    onChangeAssignment(
                        uiState.entity?.assignment?.shallowCopy {
                            caSizeLimit = it.toInt()
                        }
                    )
                },
            )

            UstadNumberTextField(
                modifier = Modifier
                    .defaultItemPadding()
                    .fillMaxWidth()
                    .testTag("caNumberOfFiles"),
                value = (uiState.entity?.assignment?.caNumberOfFiles ?: 0).toFloat(),
                label = { Text(stringResource(MR.strings.number_of_files)) },
                enabled = uiState.fieldsEnabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                onValueChange = {
                    onChangeAssignment(
                        uiState.entity?.assignment?.shallowCopy {
                            caNumberOfFiles = it.toInt()
                        }
                    )
                },
            )
        }

        UstadInputFieldLayout(
            modifier = Modifier.defaultItemPadding(),
            errorText = uiState.submissionRequiredError,
        ) {
            UstadSwitchField(
                modifier = Modifier
                    .testTag("caRequireTextSubmission"),
                label = stringResource(MR.strings.require_text_submission),
                checked = uiState.entity?.assignment?.caRequireTextSubmission ?: false,
                onChange = {
                    onChangeAssignment(
                        uiState.entity?.assignment?.shallowCopy {
                            caRequireTextSubmission = it
                        }
                    )
                },
                enabled = uiState.fieldsEnabled
            )
        }


        if (uiState.textSubmissionVisible) {
            UstadMessageIdOptionExposedDropDownMenuField(
                modifier = Modifier
                    .defaultItemPadding()
                    .fillMaxWidth()
                    .testTag("caTextLimitType"),
                value = uiState.entity?.assignment?.caTextLimitType ?: 0,
                label = stringResource(MR.strings.limit),
                options = TextLimitTypeConstants.TEXT_LIMIT_TYPE_MESSAGE_IDS,
                enabled = uiState.fieldsEnabled,
                onOptionSelected = {
                    onChangeAssignment(
                        uiState.entity?.assignment?.shallowCopy {
                            caTextLimitType = it.value
                        }
                    )
                },
            )

            UstadNumberTextField(
                modifier = Modifier
                    .defaultItemPadding()
                    .fillMaxWidth()
                    .testTag("caTextLimit"),
                value = (uiState.entity?.assignment?.caTextLimit ?: 0).toFloat(),
                label = { Text(stringResource(MR.strings.maximum)) },
                enabled = uiState.fieldsEnabled,
                onValueChange = {
                    onChangeAssignment(
                        uiState.entity?.assignment?.shallowCopy {
                            caTextLimit = it.toInt()
                        }
                    )
                },
            )

        }

        UstadMessageIdOptionExposedDropDownMenuField(
            modifier = Modifier
                .defaultItemPadding()
                .testTag("caSubmissionPolicy"),
            value = uiState.entity?.assignment?.caSubmissionPolicy ?: 0,
            label = stringResource(MR.strings.submission_policy),
            options = SubmissionPolicyConstants.SUBMISSION_POLICY_MESSAGE_IDS,
            enabled = uiState.fieldsEnabled,
            onOptionSelected = {
                onChangeAssignment(
                    uiState.entity?.assignment?.shallowCopy {
                        caSubmissionPolicy = it.value
                    }
                )
            },
        )

        UstadExposedDropDownMenuField<MarkingType>(
            modifier = Modifier
                .defaultItemPadding()
                .testTag("caMarkingType"),
            value = MarkingType.valueOf(uiState.entity?.assignment?.caMarkingType ?: 0),
            label = stringResource(MR.strings.marked_by),
            options = MarkingType.values().toList(),
            enabled = uiState.markingTypeEnabled,
            itemText = { markingType ->
                if(markingType == MarkingType.PEERS) {
                    stringResource(MR.strings.peers)
                }else {
                    courseTerminologyEntryResource(
                        terminologyEntries = terminologyEntries,
                        stringResource = MR.strings.teacher,
                    )
                }
            },
            onOptionSelected = {
                onChangeAssignment(
                    uiState.entity?.assignment?.shallowCopy {
                        caMarkingType = it.value
                    }
                )
            },
        )

        if (uiState.peerMarkingVisible) {
            Row(
                modifier = Modifier.defaultItemPadding(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UstadInputFieldLayout(
                    modifier = Modifier.weight(0.7F),
                    errorText = uiState.reviewerCountError,
                ) {
                    UstadNumberTextField(
                        modifier = Modifier
                            .testTag("caPeerReviewerCount"),
                        value = (uiState.entity?.assignment?.caPeerReviewerCount ?: 0).toFloat(),
                        label = { Text(stringResource(MR.strings.reviews_per_user_group)) },
                        enabled = uiState.fieldsEnabled,
                        isError = uiState.reviewerCountError != null,
                        onValueChange = {
                            onChangeAssignment(
                                uiState.entity?.assignment?.shallowCopy {
                                    caPeerReviewerCount = it.toInt()
                                }
                            )
                        },
                    )
                }

                OutlinedButton(
                    onClick = onClickAssignReviewers,
                    modifier = Modifier
                        .weight(0.3F)
                        .height(IntrinsicSize.Max)
                        .testTag("buttonAssignReviewers"),
                    enabled = uiState.fieldsEnabled,
                ) {
                    Text(stringResource(MR.strings.assign_reviewers))
                }
            }
        }

        UstadSwitchField(
            modifier = Modifier
                .defaultItemPadding()
                .testTag("caClassCommentEnabled"),
            label = stringResource(MR.strings.allow_class_comments),
            checked = uiState.entity?.assignment?.caClassCommentEnabled ?: false,
            onChange = {
                onChangeAssignment(uiState.entity?.assignment?.shallowCopy {
                    caClassCommentEnabled = it
                })
            },
            enabled = uiState.fieldsEnabled
        )

        UstadSwitchField(
            modifier = Modifier
                .defaultItemPadding()
                .testTag("caPrivateCommentsEnabled"),
            label = stringResource(MR.strings.allow_private_comments_from_students),
            checked = uiState.entity?.assignment?.caPrivateCommentsEnabled ?: false,
            onChange = {
                onChangeAssignment(uiState.entity?.assignment?.shallowCopy {
                    caPrivateCommentsEnabled = it
                })
            },
            enabled = uiState.fieldsEnabled
        )
    }
}
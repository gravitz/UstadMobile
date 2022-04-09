package com.ustadmobile.view

import com.ustadmobile.core.controller.ClazzAssignmentDetailOverviewPresenter
import com.ustadmobile.core.controller.SubmissionConstants
import com.ustadmobile.core.controller.SubmissionConstants.FILE_TYPE_MAP
import com.ustadmobile.core.controller.UstadDetailPresenter
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.view.ClazzAssignmentDetailOverviewView
import com.ustadmobile.door.DoorDataSourceFactory
import com.ustadmobile.door.ObserverFnWrapper
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.mui.components.*
import com.ustadmobile.util.DraftJsUtil.clean
import com.ustadmobile.util.StyleManager
import com.ustadmobile.util.StyleManager.contentContainer
import com.ustadmobile.util.StyleManager.defaultDoubleMarginTop
import com.ustadmobile.util.StyleManager.defaultPaddingTop
import com.ustadmobile.util.UmProps
import com.ustadmobile.util.ext.*
import com.ustadmobile.view.ext.*
import kotlinx.css.FlexDirection
import kotlinx.css.padding
import react.RBuilder
import react.setState
import styled.css
import styled.styledDiv
import styled.styledSpan
import kotlin.js.Date

class ClazzAssignmentDetailOverviewComponent(mProps: UmProps): UstadDetailComponent<ClazzAssignmentWithCourseBlock>(mProps),
    ClazzAssignmentDetailOverviewView {

    private var mPresenter: ClazzAssignmentDetailOverviewPresenter? = null

    override val detailPresenter: UstadDetailPresenter<*, *>?
        get() = mPresenter

    override val viewNames: List<String>
        get() = listOf(ClazzAssignmentDetailOverviewView.VIEW_NAME)

    private var showClassCommentDialog = false

    private var showPrivateCommentDialog = false

    private var classComments: List<CommentsWithPerson> = listOf()

    private var privateComments: List<CommentsWithPerson> = listOf()

    private var courseAssignmentSubmissions : List<CourseAssignmentSubmissionWithAttachment> = listOf()

    private val classCommentsObserver = ObserverFnWrapper<List<CommentsWithPerson>>{
        if(it.isEmpty()) return@ObserverFnWrapper
        setState {
            classComments = it
        }
    }

    private val privateCommentsObserver = ObserverFnWrapper<List<CommentsWithPerson>>{
        if(it.isEmpty()) return@ObserverFnWrapper
        setState {
            privateComments = it
        }
    }


    private val assignmentSubmissionObserver = ObserverFnWrapper<List<CourseAssignmentSubmissionWithAttachment>>{
        if(it.isEmpty()) return@ObserverFnWrapper
        setState {
            courseAssignmentSubmissions = it
        }
    }

    override var submittedCourseAssignmentSubmission: DoorDataSourceFactory<Int, CourseAssignmentSubmissionWithAttachment>? = null
        set(value) {
            field = value
            val liveData = value?.getData(0,Int.MAX_VALUE)
            liveData?.removeObserver(assignmentSubmissionObserver)
            liveData?.observe(this, assignmentSubmissionObserver)
        }

    override var addedCourseAssignmentSubmission: List<CourseAssignmentSubmissionWithAttachment>? = listOf()
        get() = field
        set(value) {
            setState {
                field = value
            }
            updateSubmitButtonVisibility()
        }

    private fun updateSubmitButtonVisibility(){
        updateUiWithStateChangeDelay(STATE_CHANGE_DELAY * 2) {
            fabManager?.visible = !hasPassedDeadline
                    && !addedCourseAssignmentSubmission.isNullOrEmpty()
        }
    }

    override var timeZone: String? = null
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override var clazzAssignmentClazzComments: DoorDataSourceFactory<Int, CommentsWithPerson>? = null
        set(value) {
            field = value
            val liveData = value?.getData(0,Int.MAX_VALUE)
            liveData?.removeObserver(classCommentsObserver)
            liveData?.observe(this, classCommentsObserver)
        }

    override var clazzAssignmentPrivateComments: DoorDataSourceFactory<Int, CommentsWithPerson>? = null
        set(value) {
            field = value
            val liveData = value?.getData(0,Int.MAX_VALUE)
            liveData?.removeObserver(privateCommentsObserver)
            liveData?.observe(this, privateCommentsObserver)
        }

    override var showPrivateComments: Boolean = false
        get() = field
        set(value) {
            setState {
                field = value
            }
        }
    override var showSubmission: Boolean = false
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override var hasPassedDeadline: Boolean = false
        get() = field
        set(value) {
            setState {
                field = value
            }
            updateSubmitButtonVisibility()
        }

    override var maxNumberOfFilesSubmission: Int = 0
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override var submissionMark: CourseAssignmentMark? = null
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override var submissionStatus: Int = 0
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override var entity: ClazzAssignmentWithCourseBlock? = null
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override fun onCreateView() {
        super.onCreateView()
        fabManager?.text = getString(MessageID.submit)
        fabManager?.icon = "check"
        mPresenter = ClazzAssignmentDetailOverviewPresenter(this, arguments, this, this, di)
        mPresenter?.onCreate(mapOf())
        fabManager?.onClickListener = {
            mPresenter?.handleSubmitButtonClicked()
        }
    }

    private fun isMaxFilesReached(): Boolean {
        val sizeOfSubmitted = courseAssignmentSubmissions.filter { it.casType == CourseAssignmentSubmission.SUBMISSION_TYPE_FILE }?.size ?: 0
        val sizeOfAddedList = addedCourseAssignmentSubmission?.filter { it.casType == CourseAssignmentSubmission.SUBMISSION_TYPE_FILE }?.size ?: 0
        return (sizeOfAddedList + sizeOfSubmitted) >= maxNumberOfFilesSubmission
    }

    override fun RBuilder.render() {
        if(entity == null) return
        styledDiv {
            css {
                +defaultPaddingTop
                +contentContainer
            }

            umGridContainer(rowSpacing = GridSpacing.spacing3) {
                if(!entity?.caDescription.isNullOrBlank()){
                    umItem(GridSize.cells12){
                        umTypography(entity?.caDescription)
                    }
                }

                val date = entity?.block?.cbDeadlineDate.toDate();
                val dateTime = if(date != null) "${entity?.block?.cbDeadlineDate.toDate()?.fullDateFormat()} " +
                        "- ${entity?.block?.cbDeadlineDate.toDate()?.formattedInHoursAndMinutes()} " else ""

                renderInformationOnDetailScreen("event_available","$dateTime(${timeZone})",
                    getString(MessageID.deadline),
                    shrink = true
                )

                if(submissionStatus == CourseAssignmentSubmission.NOT_SUBMITTED){
                    renderInformationOnDetailScreen("check",
                         getString(SubmissionConstants.STATUS_MAP[submissionStatus] ?: 0),
                        getString(MessageID.status),
                        shrink = true
                    )
                }

                val marks = if(submissionStatus == CourseAssignmentSubmission.NOT_SUBMITTED && submissionMark?.camMark != null)
                    getString(MessageID.points).format("${submissionMark?.camMark} / ${entity?.block?.cbMaxPoints}")
                else ""
                val penalty = if(submissionMark?.camPenalty != 0) " ${getString(MessageID.late_penalty).format(entity?.block?.cbLateSubmissionPenalty ?: "")}"
                else ""
                renderInformationOnDetailScreen("emoji_events",
                    "$marks$penalty",
                    getString(MessageID.xapi_result_header),
                    shrink = true
                )

                umGridContainer(GridSpacing.spacing4) {
                    css(defaultDoubleMarginTop)
                   val canAddFile = (entity?.caRequireFileSubmission ?: false) && !(isMaxFilesReached() || hasPassedDeadline)
                   if(!hasPassedDeadline){
                       umItem(GridSize.cells12, GridSize.cells4){
                           umButton(getString(MessageID.add_text),
                               variant = ButtonVariant.contained,
                               onClick = {
                               mPresenter?.handleAddTextClicked()
                           })
                       }
                   }

                   if(canAddFile){
                       umItem(GridSize.cells12, GridSize.cells6){
                           umItem(GridSize.cells12, GridSize.cells6){
                               umButton(getString(MessageID.add_file),
                                   variant = ButtonVariant.contained,
                                   onClick = {
                                   mPresenter?.handleAddFileClicked()
                               })
                           }
                       }
                   }

                    umSpacer(top = 2.spacingUnits)

                   if(canAddFile){
                       umItem(flexDirection = FlexDirection.row) {
                           styledSpan {
                               css{
                                   padding(right = 2.spacingUnits)
                               }
                               umTypography(getString(MessageID.type)+ ": ",
                                   variant = TypographyVariant.body1,
                                   paragraph = true){
                                   css(StyleManager.alignTextToStart)
                               }
                           }

                           styledSpan {
                               css{
                                   padding(right = 4.spacingUnits)
                               }
                               umTypography(getString(FILE_TYPE_MAP[entity?.caFileType ?: 0] ?: 0),
                                   variant = TypographyVariant.body1,
                                   paragraph = true){
                                   css(StyleManager.alignTextToStart)
                               }
                           }

                           styledSpan {
                               css{
                                   padding(right = 4.spacingUnits)
                               }
                               umTypography(getString(MessageID.max_number_of_files).format(entity?.caNumberOfFiles ?: 0),
                                   variant = TypographyVariant.body1,
                                   paragraph = true){
                                   css(StyleManager.alignTextToStart)
                               }
                           }
                       }
                   }
               }

                if(!addedCourseAssignmentSubmission.isNullOrEmpty()){
                    umItem {
                        addedCourseAssignmentSubmission?.forEach { submission ->
                            umListItem {
                                val dates = submission.casTimestamp.toDate()
                                renderItemWithLeftIconTitleDescriptionAndIconBtnOnRight(
                                    "class","delete",
                                    clean(submission.casText ?:""),
                                    if(dates == null ) ""
                                    else  "${getString(MessageID.submitted_cap)} " +
                                            ": ${submission.casTimestamp.toDate()?.standardFormat(timeZone)}",
                                    onMainList = true
                                ) { secondary, _ ->
                                    if (secondary) mPresenter?.handleDeleteSubmission(submission)
                                    if(!secondary) mPresenter?.handleOpenSubmission(submission, true)
                                }
                            }
                        }
                    }
                }

                if(!courseAssignmentSubmissions.isNullOrEmpty()){
                    umSpacer(top = 2.spacingUnits)
                    renderListSectionTitle(getString(MessageID.submissions))

                    courseAssignmentSubmissions.forEach { submission ->
                        umListItem {
                            renderListItemWithLeftIconTitleAndDescription(
                                "class",clean(submission.casText ?:""),
                                getString(MessageID.submitted_cap) +
                                        " : ${submission.casTimestamp.toDate()?.standardFormat(timeZone)}",
                                onMainList = true
                            )
                        }
                    }
                }

                if(entity?.caClassCommentEnabled == true){
                umGridContainer(rowSpacing = GridSpacing.spacing2) {
                    umItem(GridSize.cells12){
                        renderListSectionTitle(getString(MessageID.class_comments), TypographyVariant.h6)
                    }

                    val label = getString(MessageID.add_class_comment)
                    renderCreateCommentSection(label){
                        setState {
                            showClassCommentDialog = true
                        }
                    }

                    if(showClassCommentDialog){
                        renderCreateNewComment(label,
                            listener = mPresenter?.newClassCommentListener,
                            systemImpl = systemImpl,
                            shownAt = Date().getTime().toLong()){
                            setState {
                                showClassCommentDialog = false
                            }
                        }
                    }

                    umItem(GridSize.cells12){
                        renderComments(classComments)
                    }
                }
            }

                if(showPrivateComments){
                umGridContainer(rowSpacing = GridSpacing.spacing2) {
                    umItem(GridSize.cells12){
                        renderListSectionTitle(getString(MessageID.private_comments), TypographyVariant.h6)
                    }

                    val label = getString(MessageID.add_private_comment)
                    renderCreateCommentSection(label){
                        setState {
                            showPrivateCommentDialog = true
                        }
                    }

                    if(showPrivateCommentDialog){
                        renderCreateNewComment(label,
                            listener = mPresenter?.newPrivateCommentListener,
                            systemImpl = systemImpl,
                            shownAt = Date().getTime().toLong()){
                            setState {
                                showPrivateCommentDialog = false
                            }
                        }
                    }

                    umItem(GridSize.cells12){
                        renderComments(privateComments)
                    }
                }
            }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPresenter?.onDestroy()
        mPresenter = null
        entity = null
    }

    companion object {
        val ASSIGNMENT_STATUS_MAP = mapOf(
            CourseAssignmentSubmission.NOT_SUBMITTED to "timer",
            CourseAssignmentSubmission.SUBMITTED to "done",
            CourseAssignmentSubmission.MARKED to "done_all")
    }
}
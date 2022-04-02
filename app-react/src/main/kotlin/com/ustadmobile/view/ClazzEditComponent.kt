package com.ustadmobile.view

import com.ustadmobile.core.controller.ClazzEdit2Presenter
import com.ustadmobile.core.controller.UstadEditPresenter
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.util.ext.isAttendanceEnabledAndRecorded
import com.ustadmobile.core.view.ClazzEdit2View
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.door.ObserverFnWrapper
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.mui.components.*
import com.ustadmobile.util.FieldLabel
import com.ustadmobile.util.StyleManager
import com.ustadmobile.util.StyleManager.contentContainer
import com.ustadmobile.util.StyleManager.defaultPaddingTop
import com.ustadmobile.util.UmProps
import com.ustadmobile.util.Util.ASSET_ENTRY
import com.ustadmobile.util.ext.currentBackStackEntrySavedStateMap
import com.ustadmobile.util.ext.toDate
import com.ustadmobile.view.ext.*
import react.RBuilder
import react.setState
import styled.css
import styled.styledDiv
import kotlin.js.Date

class ClazzEditComponent (mProps: UmProps): UstadEditComponent<ClazzWithHolidayCalendarAndSchoolAndTerminology>(mProps),
    ClazzEdit2View {

    private var mPresenter: ClazzEdit2Presenter? = null

    override val mEditPresenter: UstadEditPresenter<*, ClazzWithHolidayCalendarAndSchoolAndTerminology>?
        get() = mPresenter

    override val viewNames: List<String>
        get() = listOf(ClazzEdit2View.VIEW_NAME)

    private var nameLabel = FieldLabel(text = getString(MessageID.name ))

    private var descriptionLabel = FieldLabel(text = getStringWithOptionalLabel(MessageID.description))

    private var institutionLabel = FieldLabel(text = getStringWithOptionalLabel(MessageID.institution))

    private var startDateLabel = FieldLabel(text = getString(MessageID.start_date))

    private var endDateLabel = FieldLabel(text = getString(MessageID.end_date))

    private var timeZoneLabel = FieldLabel(text = getString(MessageID.timezone))

    private var holidayCalenderLabel = FieldLabel(text = getString(MessageID.holiday_calendar))

    private var terminologyLabel = FieldLabel(text = getString(MessageID.terminology))

    private var scheduleList: List<Schedule> = listOf()

    private var courseBlockList: List<CourseBlockWithEntity> = listOf()

    private var attandenceEnabled = false;

    private val scheduleObserver = ObserverFnWrapper<List<Schedule>> {
        setState {
            scheduleList = it
        }
    }

    private val courseBlockObserver = ObserverFnWrapper<List<CourseBlockWithEntity>> {
        setState {
            courseBlockList = it
        }
    }

    override var clazzSchedules: DoorMutableLiveData<List<Schedule>>? = null
        set(value) {
            field?.removeObserver(scheduleObserver)
            field = value
            value?.observe(this, scheduleObserver)
        }

    override var courseBlocks: DoorMutableLiveData<List<CourseBlockWithEntity>>? = null
        set(value) {
            field?.removeObserver(courseBlockObserver)
            field = value
            value?.observe(this, courseBlockObserver)
        }

    override var clazzEndDateError: String? = null
        set(value) {
            setState {
                endDateLabel = endDateLabel.copy(errorText = value)
            }
        }

    override var clazzStartDateError: String? = null
        set(value) {
            setState {
                startDateLabel = startDateLabel.copy(errorText = value)
            }
        }

    private var scopeList: List<ScopedGrantAndName>? = null

    private val scopedGrantListObserver = ObserverFnWrapper<List<ScopedGrantAndName>> {
        setState {
            scopeList = it
        }
    }

    override var scopedGrants: DoorLiveData<List<ScopedGrantAndName>>? = null
        set(value) {
            field?.removeObserver(scopedGrantListObserver)
            field = value
            field?.observe(this, scopedGrantListObserver)
        }

    override var coursePicturePath: String?
        get() = TODO("Not yet implemented")
        set(value) {}

    override var coursePicture: CoursePicture?
        get() = TODO("Not yet implemented")
        set(value) {}

    override var fieldsEnabled: Boolean = false
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override var entity: ClazzWithHolidayCalendarAndSchoolAndTerminology? = null
        get() = field
        set(value) {
            if(value?.clazzName != null){
                ustadComponentTitle = value.clazzName
            }

            setState{
                field = value
                attandenceEnabled = value?.isAttendanceEnabledAndRecorded() == true
            }
        }

    override fun onCreateView() {
        super.onCreateView()
        setEditTitle(MessageID.add_a_new_course, MessageID.edit_course)
        mPresenter = ClazzEdit2Presenter(this, arguments, this,
            di, this)
        mPresenter?.onCreate(navController.currentBackStackEntrySavedStateMap())
    }

    override fun RBuilder.render() {
        styledDiv {
            css{
                +contentContainer
                +defaultPaddingTop
            }

            renderAddContentOptionsDialog()

            umGridContainer(GridSpacing.spacing4) {
                umItem(GridSize.cells12, GridSize.cells4){
                    umEntityAvatar(fallbackSrc = ASSET_ENTRY, listItem = true)
                }

                umItem(GridSize.cells12, GridSize.cells8){

                    renderListSectionTitle(getString(MessageID.basic_details))

                    umTextField(label = "${nameLabel.text}",
                        helperText = nameLabel.errorText,
                        value = entity?.clazzName,
                        error = nameLabel.error,
                        fullWidth = true,
                        disabled = !fieldsEnabled,
                        variant = FormControlVariant.outlined,
                        onChange = {
                            setState {
                                entity?.clazzName = it
                            }
                        }
                    )


                    umTextField(label = "${descriptionLabel.text}",
                        value = entity?.clazzDesc,
                        error = descriptionLabel.error,
                        disabled = !fieldsEnabled,
                        helperText = descriptionLabel.errorText,
                        variant = FormControlVariant.outlined,
                        onChange = {
                            setState {
                                entity?.clazzDesc = it
                            }
                        }
                    )

                    umTextField(label = "${institutionLabel.text}",
                        helperText = institutionLabel.errorText,
                        value = entity?.school?.schoolName,
                        error = nameLabel.error,
                        disabled = !fieldsEnabled,
                        variant = FormControlVariant.outlined,
                        onClick = {
                            mPresenter?.handleClickSchool()
                        }
                    )

                    umGridContainer(GridSpacing.spacing4) {

                        umItem(GridSize.cells12, GridSize.cells6 ) {
                            umDatePicker(
                                label = "${startDateLabel.text}",
                                error = startDateLabel.error,
                                helperText = startDateLabel.errorText,
                                value = entity?.clazzStartTime.toDate(),
                                inputVariant = FormControlVariant.outlined,
                                onChange = {
                                    setState {
                                        entity?.clazzStartTime = it.getTime().toLong()
                                        clazzStartDateError = null
                                    }
                                })
                        }

                        umItem(GridSize.cells12, GridSize.cells6 ) {

                            umDatePicker(
                                label = "${endDateLabel.text}",
                                error = endDateLabel.error,
                                helperText = endDateLabel.errorText,
                                value = entity?.clazzEndTime.toDate(),
                                inputVariant = FormControlVariant.outlined,
                                onChange = {
                                    setState {
                                        entity?.clazzEndTime = it.getTime().toLong()
                                        clazzEndDateError = null
                                    }
                                })
                        }
                    }

                    renderListSectionTitle(getString(MessageID.course_blocks))

                    val createCourse = CreateNewItem(true, getString(MessageID.add_block)){
                        setState {
                            showAddEntryOptions = true
                        }
                    }

                    mPresenter?.let { presenter ->
                        renderCourseBlocks(presenter,courseBlockList.toSet().toList(),createCourse){
                            mPresenter?.onClickEdit(it)
                        }
                    }

                    umSpacer()

                    renderListSectionTitle(getString(MessageID.schedule))

                    val createSchedule = CreateNewItem(true, getString(MessageID.add_a_schedule)){
                        mPresenter?.scheduleOneToManyJoinListener?.onClickNew()
                    }

                    mPresenter?.let { presenter ->
                        renderSchedules(presenter.scheduleOneToManyJoinListener,
                            scheduleList.toSet().toList(), createNewItem = createSchedule){
                            mPresenter?.scheduleOneToManyJoinListener?.onClickEdit(it)
                        }
                    }

                    umSpacer()


                    umGridContainer(GridSpacing.spacing4) {
                        umItem(GridSize.cells12, GridSize.cells6 ) {
                            umTextField(label = "${timeZoneLabel.text}",
                                value = entity?.clazzTimeZone,
                                error = timeZoneLabel.error,
                                disabled = !fieldsEnabled,
                                helperText = timeZoneLabel.errorText,
                                variant = FormControlVariant.outlined,
                                onChange = {
                                    setState {
                                        entity?.clazzTimeZone = it
                                    }
                                },
                                onClick = {
                                    mPresenter?.handleClickTimezone()
                                }
                            )
                        }

                        umItem(GridSize.cells12, GridSize.cells6 ) {
                            umTextField(label = "${holidayCalenderLabel.text}",
                                value = entity?.holidayCalendar?.umCalendarName,
                                error = holidayCalenderLabel.error,
                                disabled = !fieldsEnabled,
                                helperText = holidayCalenderLabel.errorText,
                                variant = FormControlVariant.outlined,
                                onChange = {
                                    setState {
                                        clazzEndDateError = null
                                    }
                                },
                                onClick = {
                                    mPresenter?.handleHolidayCalendarClicked()
                                }
                            )
                        }
                    }

                    renderListSectionTitle(getString(MessageID.course_setup))

                    umItem {
                        css(StyleManager.defaultMarginTop)
                        renderListItemWithTitleAndSwitch(getString(MessageID.attendance), attandenceEnabled){
                            setState {
                                attandenceEnabled = !attandenceEnabled
                                entity?.clazzFeatures = if(attandenceEnabled) Clazz.CLAZZ_FEATURE_ATTENDANCE else 0
                            }
                        }
                    }


                    umTextField(label = "${terminologyLabel.text}",
                        value = entity?.terminology?.ctTitle,
                        error = terminologyLabel.error,
                        disabled = !fieldsEnabled,
                        helperText = terminologyLabel.errorText,
                        variant = FormControlVariant.outlined,
                        onClick = {
                            mPresenter?.handleTerminologyClicked()
                        }
                    )


                    renderListSectionTitle(getString(MessageID.permissions))

                    mPresenter?.let { presenter ->
                        scopeList?.let { scopeList ->

                            val newItem = CreateNewItem(true, getString(MessageID.add_person_or_group)){
                                mPresenter?.scopedGrantOneToManyHelper?.onClickNew()
                            }

                            renderScopedGrants(presenter.scopedGrantOneToManyHelper,
                                scopeList.distinctBy { it.name }, newItem){ scope ->
                                mPresenter?.scopedGrantOneToManyHelper?.onClickEdit(scope)
                            }
                        }
                    }

                }

            }
        }
    }

    private fun RBuilder.renderAddContentOptionsDialog() {
        if(showAddEntryOptions){
            val options  = listOf(
                UmDialogOptionItem("apps",MessageID.module, MessageID.course_module) {
                    mPresenter?.handleClickAddModule()
                },
                UmDialogOptionItem("text_snippet",MessageID.text, MessageID.formatted_text_to_show_to_course_participants) {
                    mPresenter?.handleClickAddText()
                },
                UmDialogOptionItem("library_books",MessageID.content, MessageID.add_course_block_content_desc) {
                    mPresenter?.handleClickAddContent()
                },
                UmDialogOptionItem("assignment",MessageID.assignments, MessageID.add_assignment_block_content_desc) {
                    mPresenter?.handleClickAddAssignment()
                }
            )

            renderDialogOptions(systemImpl,options, Date().getTime().toLong()){
                setState {
                    showAddEntryOptions = false
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
        val BLOCK_ICON_MAP = mapOf(
            CourseBlock.BLOCK_MODULE_TYPE to "folder",
            CourseBlock.BLOCK_ASSIGNMENT_TYPE to "assignment_turned_in",
            CourseBlock.BLOCK_CONTENT_TYPE to "smart_display",
            CourseBlock.BLOCK_TEXT_TYPE to "title"
        )
    }

}
package com.ustadmobile.view

import com.ustadmobile.core.controller.ClazzDetailOverviewPresenter
import com.ustadmobile.core.controller.UstadDetailPresenter
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.view.ClazzDetailOverviewView
import com.ustadmobile.core.view.EditButtonMode
import com.ustadmobile.door.DoorDataSourceFactory
import com.ustadmobile.door.ObserverFnWrapper
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.mui.components.*
import com.ustadmobile.util.StyleManager
import com.ustadmobile.util.StyleManager.contentContainer
import com.ustadmobile.util.StyleManager.defaultPaddingTop
import com.ustadmobile.util.UmProps
import com.ustadmobile.util.Util
import com.ustadmobile.util.Util.ASSET_ENTRY
import com.ustadmobile.util.ext.format
import com.ustadmobile.util.ext.formatDateRange
import com.ustadmobile.view.ext.*
import react.RBuilder
import react.setState
import styled.css
import styled.styledDiv

class ClazzDetailOverviewComponent(mProps: UmProps): UstadDetailComponent<ClazzWithDisplayDetails>(mProps),
    ClazzDetailOverviewView {

    private var mPresenter: ClazzDetailOverviewPresenter? = null

    override val detailPresenter: UstadDetailPresenter<*, *>?
        get() = mPresenter

    override val viewNames: List<String>
        get() = listOf(ClazzDetailOverviewView.VIEW_NAME)

    private var schedules: List<Schedule> = listOf()

    private var courseBlocks: List<CourseBlockWithCompleteEntity> = listOf()

    private val scheduleObserver = ObserverFnWrapper<List<Schedule>>{
        if(it.isEmpty()) return@ObserverFnWrapper
        setState {
            schedules = it
        }
    }

    private val courseBlockObserver = ObserverFnWrapper<List<CourseBlockWithCompleteEntity>>{
        if(it.isEmpty()) return@ObserverFnWrapper
        setState {
            courseBlocks = it
        }
    }

    override var scheduleList: DoorDataSourceFactory<Int, Schedule>? = null
        set(value) {
            field = value
            val liveData = value?.getData(0,Int.MAX_VALUE)
            liveData?.removeObserver(scheduleObserver)
            liveData?.observe(this, scheduleObserver)
        }

    override var courseBlockList: DoorDataSourceFactory<Int, CourseBlockWithCompleteEntity>? = null
        set(value) {
            field = value
            val liveData = value?.getData(0,Int.MAX_VALUE)
            liveData?.removeObserver(courseBlockObserver)
            liveData?.observe(this, courseBlockObserver)
        }

    override var clazzCodeVisible: Boolean = false
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override var entity: ClazzWithDisplayDetails? = null
        get() = field
        set(value) {
            setState {
                field = value
            }
        }

    override fun onCreateView() {
        super.onCreateView()
        editButtonMode = EditButtonMode.FAB
        mPresenter = ClazzDetailOverviewPresenter(this, arguments, this,
            di, this)
        mPresenter?.onCreate(mapOf())
    }

    override fun RBuilder.render() {
        styledDiv {
            css {
                +defaultPaddingTop
                +contentContainer
            }

            umGridContainer(columnSpacing = GridSpacing.spacing6) {
                umItem(GridSize.cells12, GridSize.cells4){
                    umEntityAvatar(listItem = true, fallbackSrc = ASSET_ENTRY)
                }

                umItem(GridSize.cells12, GridSize.cells8){
                    umGridContainer {

                        umItem(GridSize.cells12){
                            umTypography(entity?.clazzDesc,
                                variant = TypographyVariant.body1,
                                gutterBottom = true){
                                css(StyleManager.alignTextToStart)
                            }

                        }

                        val numOfStudentTeachers = getString(MessageID.x_teachers_y_students)
                            .format(entity?.numTeachers ?: 0, entity?.numStudents ?: 0)

                        renderInformationOnDetailScreen("people", numOfStudentTeachers, getString(MessageID.members))

                        renderInformationOnDetailScreen("login", entity?.clazzCode ?: "", getString(MessageID.class_code)){
                            Util.copyToClipboard(entity?.clazzCode ?: "") {
                                showSnackBar(getString(MessageID.copied_to_clipboard))
                            }
                        }

                        renderInformationOnDetailScreen("school", entity?.clazzSchool?.schoolName)

                        renderInformationOnDetailScreen("event", entity?.clazzStartTime.formatDateRange(entity?.clazzEndTime))

                        renderInformationOnDetailScreen("event", entity?.clazzHolidayCalendar?.umCalendarName)


                        if(!schedules.isNullOrEmpty()){
                            umItem(GridSize.cells12){
                                renderListSectionTitle(getString(MessageID.schedule))
                            }

                            renderSchedules(schedules = schedules, withDelete = false)
                        }

                        umSpacer(top = 3.spacingUnits)

                        if(!courseBlocks.isNullOrEmpty()){
                            renderCourseBlocksWithComplete(blocks = courseBlocks){
                                when(it.cbType){
                                    CourseBlock.BLOCK_MODULE_TYPE ->
                                        mPresenter?.handleModuleExpandCollapseClicked(it)
                                    CourseBlock.BLOCK_ASSIGNMENT_TYPE ->
                                        mPresenter?.handleClickAssignment(it.assignment as ClazzAssignment)
                                    CourseBlock.BLOCK_CONTENT_TYPE ->
                                        mPresenter?.contentEntryListItemListener?.onClickContentEntry(it.entry!!)
                                }
                            }
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
}
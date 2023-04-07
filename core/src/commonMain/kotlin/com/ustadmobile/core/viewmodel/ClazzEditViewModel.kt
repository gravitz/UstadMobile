package com.ustadmobile.core.viewmodel

import com.ustadmobile.core.db.dao.deactivateByUids
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.appstate.ActionBarButtonUiState
import com.ustadmobile.core.impl.appstate.AppUiState
import com.ustadmobile.core.impl.appstate.LoadingUiState
import com.ustadmobile.core.impl.nav.UstadSavedStateHandle
import com.ustadmobile.core.schedule.ClazzLogCreatorManager
import com.ustadmobile.core.util.ext.*
import com.ustadmobile.core.view.*
import com.ustadmobile.door.ext.doorPrimaryKeyManager
import com.ustadmobile.door.ext.withDoorTransactionAsync
import com.ustadmobile.door.util.systemTimeInMillis
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.Clazz.Companion.CLAZZ_FEATURE_ATTENDANCE
import com.ustadmobile.lib.db.entities.ext.shallowCopy
import com.ustadmobile.lib.util.getDefaultTimeZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import org.kodein.di.DI
import org.kodein.di.instance

@Serializable
data class ClazzEditUiState(

    val fieldsEnabled: Boolean = true,

    val entity: ClazzWithHolidayCalendarAndSchoolAndTerminology? = null,

    val clazzStartDateError: String? = null,

    val clazzEndDateError: String? = null,

    val clazzSchedules: List<Schedule> = emptyList(),

    val courseBlockList: List<CourseBlockWithEntity> = emptyList(),

    val timeZone: String = "UTC"

) {

    class CourseBlockUiState internal constructor(
        val courseBlock: CourseBlockWithEntity
    ) {
        val showIndent: Boolean
            get() = courseBlock.cbType != CourseBlock.BLOCK_MODULE_TYPE && courseBlock.cbIndentLevel < BLOCK_MAX_INDENT

        val showUnindent: Boolean
            get() = courseBlock.cbIndentLevel > 0

        val showHide: Boolean
            get() = !courseBlock.cbHidden

        val showUnhide: Boolean
            get() = courseBlock.cbHidden
    }


    val clazzEditAttendanceChecked: Boolean
        get() = entity?.clazzFeatures == Clazz.CLAZZ_FEATURE_ATTENDANCE
                && Clazz.CLAZZ_FEATURE_ATTENDANCE == Clazz.CLAZZ_FEATURE_ATTENDANCE

    fun courseBlockStateFor(couresBlockWithEntity: CourseBlockWithEntity): CourseBlockUiState {
        return CourseBlockUiState(couresBlockWithEntity)
    }

    companion object {

        const val BLOCK_MAX_INDENT = 3

    }

}

class ClazzEditViewModel(
    di: DI,
    savedStateHandle: UstadSavedStateHandle,
): UstadEditViewModel(di, savedStateHandle, ClazzEdit2View.VIEW_NAME) {

    private val _uiState = MutableStateFlow(ClazzEditUiState())

    val uiState: Flow<ClazzEditUiState> = _uiState.asStateFlow()

    init {
        val title = createEditTitle(MessageID.add_a_new_course, MessageID.edit_course)
        _appUiState.update {
            AppUiState(
                title = title,
                actionBarButtonState = ActionBarButtonUiState(
                    visible = true,
                    text = systemImpl.getString(MessageID.save),
                    onClick = this::onClickSave
                ),
                loadingState = LoadingUiState.INDETERMINATE
            )
        }

        _uiState.update { prev -> prev.copy(fieldsEnabled = false) }

        viewModelScope.launch {
            awaitAll(
                async {
                    loadEntity(
                        serializer = ClazzWithHolidayCalendarAndSchoolAndTerminology.serializer(),
                        onLoadFromDb = {
                            it.clazzDao.takeIf { entityUidArg != 0L }
                                ?.findByUidWithHolidayCalendarAsync(entityUidArg)
                        },
                        makeDefault = {
                            ClazzWithHolidayCalendarAndSchoolAndTerminology().apply {
                                clazzUid = activeDb.doorPrimaryKeyManager.nextId(Clazz.TABLE_ID)
                                clazzName = ""
                                isClazzActive = true
                                clazzStartTime = systemTimeInMillis()
                                clazzTimeZone = getDefaultTimeZoneId()
                                clazzSchoolUid = savedStateHandle[UstadView.ARG_SCHOOL_UID]?.toLong() ?: 0L
                                school = activeRepo.schoolDao.takeIf { clazzSchoolUid != 0L }
                                    ?.findByUidAsync(clazzSchoolUid)
                                terminology = activeRepo.courseTerminologyDao
                                    .takeIf { clazzTerminologyUid != 0L }
                                    ?.findByUidAsync(clazzTerminologyUid)
                            }
                        },
                        uiUpdate = {
                            _uiState.update { prev ->
                                prev.copy(
                                    entity = it
                                )
                            }
                        }
                    )
                },
                async {
                    loadEntity(
                        serializer = ListSerializer(Schedule.serializer()),
                        loadFromStateKeys = listOf(STATE_KEY_SCHEDULES),
                        onLoadFromDb = {
                            it.scheduleDao.takeIf { entityUidArg != 0L }
                                ?.findAllSchedulesByClazzUidAsync(entityUidArg)
                        },
                        makeDefault = {
                            emptyList()
                        },
                        uiUpdate = {
                            _uiState.update { prev ->
                                prev.copy(clazzSchedules =  it ?: emptyList())
                            }
                        }
                    )
                }
            )

            if(savedStateHandle[KEY_INIT_STATE] == null) {
                savedStateHandle[KEY_INIT_STATE] = withContext(Dispatchers.Default) {
                    json.encodeToString(_uiState.value)
                }
            }

            launch {
                resultReturner.filteredResultFlowForKey(RESULT_KEY_SCHEDULE).collect { result ->
                    val returnedSchedule = result.result as? Schedule ?: return@collect
                    returnedSchedule.scheduleClazzUid = _uiState.value.entity?.clazzUid ?: 0L
                    val newSchedules = _uiState.value.clazzSchedules.replaceOrAppend(returnedSchedule) {
                        it.scheduleUid == returnedSchedule.scheduleUid
                    }

                    _uiState.update { prev ->
                        prev.copy(
                            clazzSchedules = newSchedules
                        )
                    }
                    savedStateHandle[STATE_KEY_SCHEDULES] = json.encodeToString(
                        ListSerializer(Schedule.serializer()), newSchedules)
                }
            }

            _uiState.update { prev ->
                prev.copy(fieldsEnabled = true)
            }

            loadingState = LoadingUiState.NOT_LOADING
        }

    }

    fun onEntityChanged(entity: ClazzWithHolidayCalendarAndSchoolAndTerminology?) {
        _uiState.update { prev ->
            prev.copy(
                entity = entity
            )
        }

        scheduleEntityCommitToSavedState(
            entity = entity,
            serializer = ClazzWithHolidayCalendarAndSchoolAndTerminology.serializer(),
            commitDelay = 200,
        )
    }

    fun onCheckedAttendanceChanged(checked: Boolean) {
        onEntityChanged(_uiState.value.entity?.shallowCopy {
            clazzFeatures = if(checked) {
                CLAZZ_FEATURE_ATTENDANCE
            }else {
                0L
            }
        })
    }

    fun onClickAddSchedule(){
        navigateForResult(ScheduleEditView.VIEW_NAME, "Schedule", currentValue = null,
            serializer = Schedule.serializer())
    }

    fun onClickEditSchedule(schedule: Schedule) {
        navigateForResult(
            nextViewName = ScheduleEditView.VIEW_NAME,
            key = RESULT_KEY_SCHEDULE,
            currentValue = schedule,
            serializer = Schedule.serializer()
        )
    }

    fun onClickDeleteSchedule(schedule: Schedule) {
        val newSchedules = _uiState.value.clazzSchedules
            .filter { it.scheduleUid != schedule.scheduleUid }
        savedStateHandle[STATE_KEY_SCHEDULES] = json.encodeToString(
            ListSerializer(Schedule.serializer()), newSchedules)
        _uiState.update { prev ->
            prev.copy(
                clazzSchedules = newSchedules
            )
        }
    }


    fun onAddCourseBlock(blockType: Int) {
        val (viewName, keyName) = when(blockType) {
            CourseBlock.BLOCK_MODULE_TYPE ->
                ModuleCourseBlockEditView.VIEW_NAME to RESULT_KEY_COURSEBLOCK_MODULE
            else -> return
        }

        navigateForResult(
            nextViewName = viewName,
            key = keyName,
            currentValue = null,
            serializer = CourseBlockWithEntity.serializer(),
        )
    }

    private fun ClazzEditUiState.hasErrors() : Boolean {
        return clazzStartDateError != null || clazzEndDateError != null
    }

    fun onClickSave() {
        val initEntity = _uiState.value.entity ?: return

        if (initEntity.clazzStartTime == 0L) {
            _uiState.update { prev ->
                prev.copy(
                    clazzStartDateError = systemImpl.getString(MessageID.field_required_prompt)
                )
            }
        }

        if(initEntity.clazzEndTime <= initEntity.clazzStartTime) {
            _uiState.update { prev ->
                prev.copy(clazzEndDateError = systemImpl.getString(MessageID.error_start_date_before_clazz_date))
            }
        }

        if(_uiState.value.hasErrors())
            return

        loadingState = LoadingUiState.INDETERMINATE

        //Entity to save
        val entity = initEntity.shallowCopy {
            this.clazzName = clazzName?.trim()
            clazzStartTime = Instant.fromEpochMilliseconds(initEntity.clazzStartTime)
                .toLocalMidnight(initEntity.effectiveTimeZone).toEpochMilliseconds()

            if(clazzEndTime != Long.MAX_VALUE){
                clazzEndTime = Instant.fromEpochMilliseconds(initEntity.clazzEndTime)
                    .toLocalEndOfDay(initEntity.effectiveTimeZone).toEpochMilliseconds()
            }
        }

        _uiState.update { prev -> prev.copy(fieldsEnabled = false, entity = entity) }

        viewModelScope.launch {
            val initState = savedStateHandle.getJson(KEY_INIT_STATE, ClazzEditUiState.serializer())
                ?: return@launch

            activeDb.withDoorTransactionAsync {
                if(entityUidArg == 0L) {
                    val termMap = activeDb.courseTerminologyDao.findByUidAsync(initEntity.clazzTerminologyUid)
                        .toTermMap(json, systemImpl)
                    activeDb.createNewClazzAndGroups(initEntity, systemImpl, termMap)
                }else {
                    activeDb.clazzDao.updateAsync(initEntity)
                }

                activeDb.scheduleDao.upsertListAsync(_uiState.value.clazzSchedules)
                activeDb.scheduleDao.deactivateByUids(
                    initState.clazzSchedules.filterKeysNotInOtherList(
                        _uiState.value.clazzSchedules
                    ) {
                        it.scheduleUid
                    }, systemTimeInMillis()
                )
            }

            val entityTimeZone = TimeZone.of(entity.effectiveTimeZone)
            val fromLocalDate = Clock.System.now().toLocalDateTime(entityTimeZone)
                .toLocalMidnight()
            val clazzLogCreatorManager: ClazzLogCreatorManager by di.instance()
            clazzLogCreatorManager.requestClazzLogCreation(
                entity.clazzUid,
                accountManager.activeAccount.endpointUrl,
                fromLocalDate.toInstant(entityTimeZone).toEpochMilliseconds(),
                fromLocalDate.toLocalEndOfDay().toInstant(entityTimeZone).toEpochMilliseconds()
            )
            _uiState.update { prev ->
                prev.copy(fieldsEnabled = true)
            }
            loadingState = LoadingUiState.NOT_LOADING

            finishWithResult(ClazzDetailView.VIEW_NAME, entity.clazzUid, entity)
        }
    }

    companion object {

        const val RESULT_KEY_SCHEDULE = "Schedule"

        const val STATE_KEY_SCHEDULES = "schedule"

        const val RESULT_KEY_COURSEBLOCK_MODULE = "module"

    }

}

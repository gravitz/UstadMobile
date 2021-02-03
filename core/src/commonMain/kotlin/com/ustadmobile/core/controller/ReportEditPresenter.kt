package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.util.*
import com.ustadmobile.core.util.ext.putEntityAsJson
import com.ustadmobile.core.util.ext.toDateRangeMoment
import com.ustadmobile.core.util.ext.toDisplayString
import com.ustadmobile.core.view.ReportDetailView
import com.ustadmobile.core.view.ReportEditView
import com.ustadmobile.core.view.UstadEditView.Companion.ARG_ENTITY_JSON
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.door.doorMainDispatcher
import com.ustadmobile.door.ext.onRepoWithFallbackToDb
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.Moment.Companion.MONTHS_REL_UNIT
import com.ustadmobile.lib.db.entities.Moment.Companion.TYPE_FLAG_RELATIVE
import com.ustadmobile.lib.db.entities.Moment.Companion.WEEKS_REL_UNIT
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.list
import org.kodein.di.DI
import kotlin.math.max


class ReportEditPresenter(context: Any,
                          arguments: Map<String, String>, view: ReportEditView,
                          di: DI,
                          lifecycleOwner: DoorLifecycleOwner)
    : UstadEditPresenter<ReportEditView, ReportWithSeriesWithFilters>(context, arguments, view, di, lifecycleOwner) {

    val seriesCounter = atomic(0)

    val filterCounter = atomic(0)

    override val persistenceMode: PersistenceMode
        get() = PersistenceMode.DB

    enum class VisualTypeOptions(val optionVal: Int, val messageId: Int) {
        BAR_CHART(ReportSeries.BAR_CHART,
                MessageID.bar_chart),
        LINE_GRAPH(ReportSeries.LINE_GRAPH,
                MessageID.line_chart)
    }

    class VisualTypeMessageIdOption(day: VisualTypeOptions, context: Any)
        : MessageIdOption(day.messageId, context, day.optionVal)

    enum class XAxisOptions(val optionVal: Int, val messageId: Int) {
        DAY(Report.DAY,
                MessageID.day),
        WEEK(Report.WEEK,
                MessageID.xapi_week),
        MONTH(Report.MONTH,
                MessageID.xapi_month),
        CONTENT_ENTRY(Report.CONTENT_ENTRY,
                MessageID.xapi_content_entry),
        GENDER(Report.GENDER,
                MessageID.gender_literal),
        CLASS(Report.CLASS,
                MessageID.clazz)
    }

    class XAxisMessageIdOption(day: XAxisOptions, context: Any)
        : MessageIdOption(day.messageId, context, day.optionVal)

    enum class DateRangeOptions(val code: Int, val messageId: Int,
                                var dateRange: DateRangeMoment?) {
        EVERYTHING(Report.EVERYTHING, MessageID.time_range_all,
                DateRangeMoment(Moment(), Moment())),
        LAST_WEEK(Report.LAST_WEEK_DATE,
                MessageID.last_week_date_range,
                DateRangeMoment(
                        Moment().apply {
                            typeFlag = TYPE_FLAG_RELATIVE
                            relUnit = WEEKS_REL_UNIT
                            relOffSet = -1
                        }, Moment().apply {
                    typeFlag = TYPE_FLAG_RELATIVE
                })),
        LAST_TWO_WEEKS(Report.LAST_TWO_WEEKS_DATE,
                MessageID.last_two_week_date_range,
                DateRangeMoment(
                        Moment().apply {
                            typeFlag = TYPE_FLAG_RELATIVE
                            relUnit = WEEKS_REL_UNIT
                            relOffSet = -2
                        }, Moment().apply {
                    typeFlag = TYPE_FLAG_RELATIVE
                })),
        LAST_MONTH(Report.LAST_MONTH_DATE,
                MessageID.last_month_date_range,
                DateRangeMoment(
                        Moment().apply {
                            typeFlag = TYPE_FLAG_RELATIVE
                            relUnit = MONTHS_REL_UNIT
                            relOffSet = -1
                        }, Moment().apply {
                    typeFlag = TYPE_FLAG_RELATIVE
                })),
        LAST_THREE_MONTHS(Report.LAST_THREE_MONTHS_DATE,
                MessageID.last_three_months_date_range,
                DateRangeMoment(
                        Moment().apply {
                            typeFlag = TYPE_FLAG_RELATIVE
                            relUnit = MONTHS_REL_UNIT
                            relOffSet = -3
                        }, Moment().apply {
                    typeFlag = TYPE_FLAG_RELATIVE
                })),
        CUSTOM_RANGE(Report.CUSTOM_RANGE,
                MessageID.selected_custom_range, null),
        NEW_CUSTOM_RANGE(Report.NEW_CUSTOM_RANGE_DATE,
                MessageID.new_custom_date_range,
                DateRangeMoment(
                        Moment().apply {
                            typeFlag = TYPE_FLAG_RELATIVE
                            relUnit = WEEKS_REL_UNIT
                        }, Moment().apply {
                    typeFlag = TYPE_FLAG_RELATIVE
                })),
    }

    enum class SubGroupOptions(val optionVal: Int, val messageId: Int) {
        NONE(ReportSeries.NONE,
                MessageID.None),
        DAY(Report.DAY,
                MessageID.day),
        WEEK(Report.WEEK,
                MessageID.xapi_week),
        MONTH(Report.MONTH,
                MessageID.xapi_month),
        CONTENT_ENTRY(Report.CONTENT_ENTRY,
                MessageID.xapi_content_entry),
        GENDER(Report.GENDER,
                MessageID.gender_literal),
        CLASS(Report.CLASS,
                MessageID.clazz)
    }

    class SubGroupByMessageIdOption(day: SubGroupOptions, context: Any)
        : MessageIdOption(day.messageId, context, day.optionVal)

    enum class YAxisOptions(val optionVal: Int, val messageId: Int) {
        TOTAL_DURATION(ReportSeries.TOTAL_DURATION,
                MessageID.content_total_duration),
        AVERAGE_DURATION(ReportSeries.AVERAGE_DURATION,
                MessageID.content_average_duration),
        NUMBER_SESSIONS(ReportSeries.NUMBER_SESSIONS,
                MessageID.count_session),
        INTERACTIONS_RECORDED(ReportSeries.INTERACTIONS_RECORDED,
                MessageID.interaction_recorded),
        NUMBER_ACTIVE_USERS(ReportSeries.NUMBER_ACTIVE_USERS,
                MessageID.number_active_users),
        AVERAGE_USAGE_TIME_PER_USER(ReportSeries.AVERAGE_USAGE_TIME_PER_USER,
                MessageID.average_usage_time_per_user),
        NUMBER_STUDENTS_COMPLETED(ReportSeries.NUMBER_OF_STUDENTS_COMPLETED_CONTENT,
                MessageID.number_students_completed),
        PERCENT_STUDENTS_COMPLETED(ReportSeries.PERCENT_OF_STUDENTS_COMPLETED_CONTENT,
                MessageID.percent_students_completed),
        TOTAL_ATTENDANCE(ReportSeries.TOTAL_ATTENDANCE,
                MessageID.total_attendances),
        TOTAL_ABSENCES(ReportSeries.TOTAL_ABSENCES,
                MessageID.total_absences),
        TOTAL_LATES(ReportSeries.TOTAL_LATES,
                MessageID.total_lates),
        PERCENT_STUDENTS_ATTENDED(ReportSeries.PERCENTAGE_STUDENTS_ATTENDED,
                MessageID.percent_students_attended),
        PERCENT_STUDENTS_ATTENDED_OR_LATE(ReportSeries.PERCENTAGE_STUDENTS_ATTENDED_OR_LATE,
                MessageID.percent_students_attended_or_late),
        TOTAL_CLASSES(ReportSeries.TOTAL_CLASSES,
                MessageID.total_number_of_classes),
        UNIQUE_STUDENTS_ATTENDING(ReportSeries.NUMBER_UNIQUE_STUDENTS_ATTENDING,
                MessageID.number_unique_students_attending)
    }

    class YAxisMessageIdOption(data: YAxisOptions, context: Any)
        : MessageIdOption(data.messageId, context, data.optionVal)


    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)

        view.visualTypeOptions = VisualTypeOptions.values().map { VisualTypeMessageIdOption(it, context) }
        view.xAxisOptions = XAxisOptions.values().map { XAxisMessageIdOption(it, context) }
        view.subGroupOptions = SubGroupOptions.values().map { SubGroupByMessageIdOption(it, context) }
        view.yAxisOptions = YAxisOptions.values().map { YAxisMessageIdOption(it, context) }
        view.dateRangeOptions = DateRangeOptions.values().filter { it.dateRange != null }
                .map {  ObjectMessageIdOption(it.messageId, context, it.code, it.dateRange) }
    }

    override suspend fun onLoadEntityFromDb(db: UmAppDatabase): ReportWithSeriesWithFilters? {
        val entityUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L

        val report = db.onRepoWithFallbackToDb(2000) {
            it.takeIf { entityUid != 0L }?.reportDao?.findByUid(entityUid)
        } ?: Report()

        handleXAxisSelected(XAxisOptions.values().map { XAxisMessageIdOption(it, context) }.find { it.code == report.xAxis } as MessageIdOption)
        if(report.reportDateRangeSelection == Report.CUSTOM_RANGE){
            handleAddCustomRange(report.toDateRangeMoment())
        }

        val reportSeries = report.reportSeries
        var reportSeriesList = listOf<ReportSeries>()
        if (!reportSeries.isNullOrBlank()) {
            reportSeriesList = safeParseList(di, ReportSeries.serializer().list, ReportSeries::class, reportSeries)
            // set the series counter with an existing series
            val maxSeries = reportSeriesList.maxBy { it.reportSeriesUid }
            var currentMaxFilter = 0L
            reportSeriesList.forEach {
                val maxFilter = it.reportSeriesFilters?.maxBy { it.reportFilterUid }?.reportFilterUid
                        ?: 0
                currentMaxFilter = max(currentMaxFilter, maxFilter)
            }
            seriesCounter.value = (maxSeries?.reportSeriesUid ?: 0 + 1).toInt()
            filterCounter.value = currentMaxFilter.toInt() + 1
        } else {
            reportSeriesList = listOf(ReportSeries().apply {
                val id = seriesCounter.getAndIncrement()
                reportSeriesName = "Series $id"
                reportSeriesUid = id.toLong()
            })
        }

        return ReportWithSeriesWithFilters(report, reportSeriesList)
    }

    override fun onLoadFromJson(bundle: Map<String, String>): ReportWithSeriesWithFilters? {
        super.onLoadFromJson(bundle)

        val entityJsonStr = bundle[ARG_ENTITY_JSON]
        val editEntity: ReportWithSeriesWithFilters
        editEntity = if (entityJsonStr != null) {
            safeParse(di, ReportWithSeriesWithFilters.serializer(), entityJsonStr)
        } else {
            ReportWithSeriesWithFilters()
        }

        handleXAxisSelected(XAxisOptions.values().map { XAxisMessageIdOption(it, context) }.find { it.code == editEntity.xAxis } as MessageIdOption)
        if(editEntity.reportDateRangeSelection == Report.CUSTOM_RANGE){
            handleAddCustomRange(editEntity.toDateRangeMoment())
        }

        val reportSeries = editEntity.reportSeries
        val reportSeriesList: List<ReportSeries>
        if (!reportSeries.isNullOrBlank()) {

            reportSeriesList = safeParseList(di, ReportSeries.serializer().list, ReportSeries::class, reportSeries)
            // set the series counter with an existing series
            val max = reportSeriesList.maxBy { it.reportSeriesUid }
            seriesCounter.value = ((max?.reportSeriesUid ?: 0) + 1).toInt()
            var currentMaxFilter = 0L
            reportSeriesList.forEach {
                val maxFilter = it.reportSeriesFilters?.maxBy { it.reportFilterUid }?.reportFilterUid
                        ?: 0
                currentMaxFilter = max(currentMaxFilter, maxFilter)
            }
            filterCounter.value = currentMaxFilter.toInt() + 1
            editEntity.reportSeriesWithFiltersList = reportSeriesList

        } else if (editEntity.reportSeriesWithFiltersList == null || editEntity.reportSeriesWithFiltersList?.isNullOrEmpty() == true) {
            reportSeriesList = listOf(ReportSeries().apply {
                val id = seriesCounter.getAndIncrement()
                reportSeriesName = "Series $id"
                reportSeriesUid = id.toLong()
            })
            editEntity.reportSeriesWithFiltersList = reportSeriesList
        }


        return editEntity
    }


    override fun onSaveInstanceState(savedState: MutableMap<String, String>) {
        super.onSaveInstanceState(savedState)
        val entityVal = entity ?: return
        entityVal.reportSeries = safeStringify(di, ReportSeries.serializer().list, entityVal.reportSeriesWithFiltersList
                ?: listOf())
        savedState.putEntityAsJson(ARG_ENTITY_JSON, null, entityVal)
    }

    fun handleAddCustomRange(dateRangeMoment: DateRangeMoment) {
        view.dateRangeOptions = DateRangeOptions.values().map {
            ObjectMessageIdOption(it.messageId, context, it.code, it.dateRange ?: dateRangeMoment,
                    if(it.dateRange != null) null else dateRangeMoment.toDisplayString())
        }
        view.selectedDateRangeMoment = dateRangeMoment
        val entityVal = entity ?: return
        entityVal.reportDateRangeSelection = Report.CUSTOM_RANGE
        view.entity = entityVal
    }

    fun handleRemoveSeries(series: ReportSeries) {
        val entityVal = entity
        val newList = entityVal?.reportSeriesWithFiltersList?.toMutableList() ?: mutableListOf()
        newList.remove(series)
        entityVal?.reportSeriesWithFiltersList = newList.toList()
        view.entity = entityVal
    }

    fun handleClickAddSeries() {
        val series = ReportSeries().apply {
            val id = seriesCounter.getAndIncrement()
            reportSeriesName = "Series $id"
            reportSeriesUid = id.toLong()
        }
        val entityVal = entity
        val newList = entityVal?.reportSeriesWithFiltersList?.toMutableList() ?: mutableListOf()
        newList.add(series)
        entityVal?.reportSeriesWithFiltersList = newList.toList()
        view.entity = entityVal
    }

    fun handleAddFilter(newFilter: ReportFilter) {
        val entityVal = entity
        val newSeriesList = entityVal?.reportSeriesWithFiltersList?.toMutableList()
                ?: mutableListOf()
        val seriesToAddFilter = newSeriesList.find { it.reportSeriesUid == newFilter.reportFilterSeriesUid }
                ?: return

        newSeriesList.remove(seriesToAddFilter)
        val newFilterList = seriesToAddFilter.reportSeriesFilters?.toMutableList()
                ?: mutableListOf()

        // new filter
        if (newFilter.reportFilterUid == 0L) {
            newFilter.reportFilterUid = filterCounter.incrementAndGet().toLong()
            newFilterList.add(newFilter)
        } else {
            val indexOfFilter = newFilterList.indexOfFirst { it.reportFilterUid == newFilter.reportFilterUid }
            newFilterList[indexOfFilter] = newFilter
        }

        newSeriesList.add(ReportSeries().apply {
            reportSeriesUid = seriesToAddFilter.reportSeriesUid
            reportSeriesVisualType = seriesToAddFilter.reportSeriesVisualType
            reportSeriesYAxis = seriesToAddFilter.reportSeriesYAxis
            reportSeriesSubGroup = seriesToAddFilter.reportSeriesSubGroup
            reportSeriesName = seriesToAddFilter.reportSeriesName
            reportSeriesFilters = newFilterList.toList()
        })

        entityVal?.reportSeriesWithFiltersList = newSeriesList.toList()
        view.entity = entityVal
    }

    fun handleRemoveFilter(filter: ReportFilter) {
        val entityVal = entity
        val newSeriesList = entityVal?.reportSeriesWithFiltersList?.toMutableList()
                ?: mutableListOf()
        val seriesToRemoveFilter = newSeriesList.find { it.reportSeriesUid == filter.reportFilterSeriesUid }
                ?: return

        newSeriesList.remove(seriesToRemoveFilter)
        val newFilterList = seriesToRemoveFilter.reportSeriesFilters?.toMutableList()
        newFilterList?.remove(filter)

        newSeriesList.add(ReportSeries().apply {
            reportSeriesUid = seriesToRemoveFilter.reportSeriesUid
            reportSeriesVisualType = seriesToRemoveFilter.reportSeriesVisualType
            reportSeriesYAxis = seriesToRemoveFilter.reportSeriesYAxis
            reportSeriesSubGroup = seriesToRemoveFilter.reportSeriesSubGroup
            reportSeriesName = seriesToRemoveFilter.reportSeriesName
            reportSeriesFilters = newFilterList?.toList()
        })

        entityVal?.reportSeriesWithFiltersList = newSeriesList.toList()
        view.entity = entityVal
    }


    override fun handleClickSave(entity: ReportWithSeriesWithFilters) {
        if (entity.reportTitle.isNullOrEmpty()) {
            view.titleErrorText = systemImpl.getString(MessageID.field_required_prompt, context)
            return
        } else {
            view.titleErrorText = null
        }

        entity.reportSeries = safeStringify(di, ReportSeries.serializer().list, entity.reportSeriesWithFiltersList
                ?: listOf())

        GlobalScope.launch(doorMainDispatcher()) {

            if (entity.reportUid != 0L) {

                repo.reportDao.updateAsync(entity)

                withContext(doorMainDispatcher()) {
                    view.finishWithResult(listOf(entity))
                }

            } else {
                systemImpl.go(ReportDetailView.VIEW_NAME,
                        mapOf(ARG_ENTITY_JSON to
                                safeStringify(di, ReportWithSeriesWithFilters.serializer(), entity)),
                        context)
            }

        }
    }

    fun handleXAxisSelected(selectedOption: IdOption) {
        if (selectedOption.optionId == Report.DAY || selectedOption.optionId == Report.MONTH || selectedOption.optionId == Report.WEEK) {
            view.subGroupOptions = SubGroupOptions.values().map { SubGroupByMessageIdOption(it, context) }
                    .filter {
                        it.code == Report.GENDER ||
                                it.code == Report.CONTENT_ENTRY ||
                                it.code == Report.CLASS || it.code == ReportSeries.NONE
                    }
        } else if (selectedOption.optionId == Report.CLASS ||
                selectedOption.optionId == Report.CONTENT_ENTRY ||
                selectedOption.optionId == Report.GENDER) {
            view.subGroupOptions = SubGroupOptions.values().map { SubGroupByMessageIdOption(it, context) }
        }
    }

    fun handleDateRangeSelected(selectedOption: IdOption) {
        if(selectedOption.optionId == DateRangeOptions.NEW_CUSTOM_RANGE.code){
            return
        }
        val dateRangeFound = view.dateRangeOptions?.find { it.code == selectedOption.optionId } ?: return
        view.selectedDateRangeMoment = dateRangeFound.obj
    }

}
package com.ustadmobile.port.android.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.FragmentReportEditBinding
import com.toughra.ustadmobile.databinding.ItemReportEditFilterBinding
import com.toughra.ustadmobile.databinding.ItemReportEditSeriesBinding
import com.ustadmobile.core.controller.ReportEditPresenter
import com.ustadmobile.core.controller.UstadEditPresenter
import com.ustadmobile.core.util.IdOption
import com.ustadmobile.core.util.ObjectMessageIdOption
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.ReportEditView
import com.ustadmobile.lib.db.entities.DateRangeMoment
import com.ustadmobile.lib.db.entities.ReportFilter
import com.ustadmobile.lib.db.entities.ReportSeries
import com.ustadmobile.lib.db.entities.ReportWithSeriesWithFilters
import com.ustadmobile.port.android.util.ext.currentBackStackEntrySavedStateMap


interface ReportEditFragmentEventHandler {
    fun onClickNewFilter(series: ReportSeries)
    fun onClickRemoveFilter(filter: ReportFilter)
    fun onClickEditFilter(filter: ReportFilter)
    fun onClickNewSeries()
    fun onClickRemoveSeries(reportSeries: ReportSeries)
}

class ReportEditFragment : UstadEditFragment<ReportWithSeriesWithFilters>(), ReportEditView,
        ReportEditFragmentEventHandler,
        DropDownListAutoCompleteTextView.OnDropDownListItemSelectedListener<IdOption> {

    private var mBinding: FragmentReportEditBinding? = null

    private var mPresenter: ReportEditPresenter? = null

    private var seriesRecyclerView: RecyclerView? = null

    private var seriesAdapter: RecyclerViewSeriesAdapter? = null

    override val mEditPresenter: UstadEditPresenter<*, ReportWithSeriesWithFilters>?
        get() = mPresenter

    class SeriesViewHolder(val itemBinding: ItemReportEditSeriesBinding,
                           val activityEventHandler: ReportEditFragmentEventHandler,
                           var presenter: ReportEditPresenter?) : RecyclerView.ViewHolder(itemBinding.root){

        val filterAdapter = RecyclerViewFilterAdapter(activityEventHandler, presenter)

        var filterList: List<ReportFilter>? = null
            set(value){
                field = value
                filterAdapter.submitList(value)
            }
    }

    class RecyclerViewSeriesAdapter(
        val activityEventHandler: ReportEditFragmentEventHandler,
        var presenter: ReportEditPresenter?
    ) : ListAdapter<ReportSeries, SeriesViewHolder>(DIFF_CALLBACK_SERIES) {

        var visualOptions: List<ReportEditPresenter.VisualTypeMessageIdOption>? = null
        var yAxisOptions: List<ReportEditPresenter.YAxisMessageIdOption>? = null
        var subGroupOptions: List<ReportEditPresenter.SubGroupByMessageIdOption>? = null
        var showDeleteButton: Boolean = false
        val boundSeriesViewHolder = mutableListOf<SeriesViewHolder>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder {
            return SeriesViewHolder(ItemReportEditSeriesBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false).apply {
                mPresenter = presenter
                eventHandler = activityEventHandler
            }, activityEventHandler, presenter)
        }

        override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
            val series =  getItem(position)
            holder.itemBinding.series = series
            holder.itemBinding.visualTypeOptions = visualOptions
            holder.itemBinding.yAxisOptions = yAxisOptions
            holder.itemBinding.subgroupOptions = subGroupOptions
            holder.itemBinding.showDeleteButton = showDeleteButton
            holder.itemBinding.itemEditReportDialogVisualTypeText.tag = series.reportSeriesVisualType
            holder.itemBinding.seriesLayout.tag = series.reportSeriesUid
            boundSeriesViewHolder += holder

            val filterRecyclerView = holder.itemBinding.itemReportEditFilterList

            holder.filterList = series.reportSeriesFilters
            filterRecyclerView.adapter = holder.filterAdapter
            filterRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        }

        override fun onViewRecycled(holder: SeriesViewHolder) {
            super.onViewRecycled(holder)
            boundSeriesViewHolder -= holder
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)
            boundSeriesViewHolder.clear()
        }
    }

    class FilterViewHolder(val itemBinding: ItemReportEditFilterBinding) : RecyclerView.ViewHolder(itemBinding.root)

    class RecyclerViewFilterAdapter(val activityEventHandler: ReportEditFragmentEventHandler?,
                                    var presenter: ReportEditPresenter?)
        : ListAdapter<ReportFilter, FilterViewHolder>(DIFF_CALLBACK_FILTER) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
            return FilterViewHolder(ItemReportEditFilterBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false).apply {
                mPresenter = presenter
                eventHandler = activityEventHandler
            })
        }

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            val filter = getItem(position)
            holder.itemBinding.filter = filter
        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView: View
        mBinding = FragmentReportEditBinding.inflate(inflater, container, false).also {
            rootView = it.root
            it.eventHandler = this
            it.xAxisSelectionListener = this
            seriesRecyclerView = it.activityReportEditSeriesList
        }

        seriesAdapter = RecyclerViewSeriesAdapter(this, null)
        seriesRecyclerView?.adapter = seriesAdapter
        seriesRecyclerView?.layoutManager = LinearLayoutManager(requireContext())

        mPresenter = ReportEditPresenter(requireContext(), arguments.toStringMap(), this,
                di, viewLifecycleOwner).withViewLifecycle()

        seriesAdapter?.presenter = mPresenter

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEditFragmentTitle(R.string.create_a_new_report, R.string.edit_report)
        mPresenter?.onCreate(findNavController().currentBackStackEntrySavedStateMap())
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_done -> {
                onSaveStateToBackStackStateHandle()
                return super.onOptionsItemSelected(item)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
        mPresenter = null
        entity = null
        seriesRecyclerView = null
        seriesAdapter = null
    }

    override fun onResume() {
        super.onResume()
        setEditFragmentTitle(R.string.create_a_new_report, R.string.edit_report)
    }

    override var entity: ReportWithSeriesWithFilters? = null
        get() = field
        set(value) {
            field = value
            mBinding?.report = value
            mBinding?.xAxisOptions = this.xAxisOptions
            seriesAdapter?.submitList(value?.reportSeriesWithFiltersList)
            val showDeleteButton = (value?.reportSeriesWithFiltersList?.size ?: 0) > 1
            seriesAdapter?.showDeleteButton = showDeleteButton
            seriesAdapter?.boundSeriesViewHolder?.forEach {
                it.itemBinding.showDeleteButton = showDeleteButton
            }
        }

    override var fieldsEnabled: Boolean = false
        get() = field
        set(value) {
            super.fieldsEnabled = value
            field = value
            mBinding?.fieldsEnabled = value
        }

    override var titleErrorText: String? = ""
        get() = field
        set(value) {
            field = value
            mBinding?.titleErrorText = value
        }

    override var visualTypeOptions: List<ReportEditPresenter.VisualTypeMessageIdOption>? = null
        get() = field
        set(value) {
            field = value
            seriesAdapter?.visualOptions = value
        }

    override var yAxisOptions: List<ReportEditPresenter.YAxisMessageIdOption>? = null
        get() = field
        set(value) {
            field = value
            seriesAdapter?.yAxisOptions = value
        }

    override var xAxisOptions: List<ReportEditPresenter.XAxisMessageIdOption>? = null
        get() = field
        set(value) {
            field = value
            mBinding?.xAxisOptions = value
        }

    override var dateRangeOptions: List<ObjectMessageIdOption<DateRangeMoment>>? = null
        get() = field
        set(value) {
            field = value
            mBinding?.dateRangeOptions = value
        }
    override var selectedDateRangeMoment: DateRangeMoment? = null
        get() = field
        set(value) {
            field = value
            mBinding?.dateRangeMomentSelected = field
        }

    override var subGroupOptions: List<ReportEditPresenter.SubGroupByMessageIdOption>? = null
        get() = field
        set(value) {
            field = value
            seriesAdapter?.subGroupOptions = value
            seriesAdapter?.boundSeriesViewHolder?.forEach {
                it.itemBinding.subgroupOptions = value
            }
        }

    override fun onDropDownItemSelected(view: AdapterView<*>?, selectedOption: IdOption) {
        if(selectedOption.optionId == ReportEditPresenter.DateRangeOptions.NEW_CUSTOM_RANGE.code) {
                mPresenter?.handleDateRangeChange()
        }
        mPresenter?.handleDateRangeSelected(selectedOption)
        mPresenter?.handleXAxisSelected(selectedOption)
    }

    override fun onNoMessageIdOptionSelected(view: AdapterView<*>?) {

    }

    override fun onClickNewFilter(series: ReportSeries) {
        mPresenter?.handleOnFilterClicked(ReportFilter().apply {
            reportFilterSeriesUid = series.reportSeriesUid
        })
    }

    override fun onClickEditFilter(filter: ReportFilter){
        mPresenter?.handleOnFilterClicked(filter)
    }

    override fun onClickRemoveFilter(filter: ReportFilter) {
        mPresenter?.handleRemoveFilter(filter)
    }

    override fun onClickNewSeries() {
        mPresenter?.handleClickAddSeries()
    }

    override fun onClickRemoveSeries(reportSeries: ReportSeries) {
        /**
         *  when removing a series, recyclerview doesn't like if
         *  any of the views are in focus(editText or idOptionAutoComplete)
         *  so go through each view and clear the focus
         */
        seriesAdapter?.boundSeriesViewHolder?.forEach {
            it.itemBinding.itemEditReportDialogSubgroupTextinputlayout.clearFocus()
            it.itemBinding.itemEditReportDialogYaxisTextinputlayout.clearFocus()
            it.itemBinding.itemEditReportDialogVisualTypeTextinputlayout.clearFocus()
            it.itemBinding.itemReportSeriesTitleTextInputlayout.clearFocus()
            it.itemBinding.itemReportSeriesDeleteButton.clearFocus()
        }
        mPresenter?.handleRemoveSeries(reportSeries)
    }

    companion object {

        val DIFF_CALLBACK_SERIES = object : DiffUtil.ItemCallback<ReportSeries>() {
            override fun areItemsTheSame(oldItem: ReportSeries, newItem: ReportSeries): Boolean {
                return oldItem.reportSeriesUid == newItem.reportSeriesUid
            }

            /* We are using a two-way binding within a recycler view. We must ensure that the
             * we rebind the exact same object, otherwise the changes will be saved into a different
             * object in memory
             */
            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldItem: ReportSeries,
                newItem: ReportSeries
            ): Boolean {
                return oldItem === newItem
            }
        }

        val DIFF_CALLBACK_FILTER = object : DiffUtil.ItemCallback<ReportFilter>() {
            override fun areItemsTheSame(oldItem: ReportFilter, newItem: ReportFilter): Boolean {
                return oldItem.reportFilterUid == newItem.reportFilterUid
            }

            /* We are using a two-way binding within a recycler view. We must ensure that the
             * we rebind the exact same object, otherwise the changes will be saved into a different
             * object in memory
             */
            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldItem: ReportFilter,
                newItem: ReportFilter
            ): Boolean {
                return oldItem === newItem
            }
        }
    }

}
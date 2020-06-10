package com.ustadmobile.port.android.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.MergeAdapter
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.FragmentReportDetailBinding
import com.toughra.ustadmobile.databinding.ItemReportChartHeaderBinding
import com.toughra.ustadmobile.databinding.ItemReportStatementListBinding
import com.ustadmobile.core.controller.ReportDetailPresenter
import com.ustadmobile.core.controller.UstadDetailPresenter
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.ReportGraphHelper
import com.ustadmobile.core.util.ext.toNullableStringMap
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.EditButtonMode
import com.ustadmobile.core.view.ReportDetailView
import com.ustadmobile.core.view.ReportEditView
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.door.DoorMigration
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.door.ext.asRepositoryLiveData
import com.ustadmobile.lib.db.entities.ReportWithFilters
import com.ustadmobile.lib.db.entities.StatementListReport
import com.ustadmobile.port.android.view.ext.navigateToEditEntity


interface ReportDetailFragmentEventHandler {
    fun onClickAddRemoveDashboard(report: ReportWithFilters)
}

class ReportDetailFragment : UstadDetailFragment<ReportWithFilters>(), ReportDetailView, ReportDetailFragmentEventHandler {

    private var mBinding: FragmentReportDetailBinding? = null

    private var mPresenter: ReportDetailPresenter? = null

    override val detailPresenter: UstadDetailPresenter<*, *>?
        get() = mPresenter

    private var chartAdapter: RecyclerViewChartAdapter? = null

    private var statementAdapter: StatementViewRecyclerAdapter? = null

    private var mergeAdapter: MergeAdapter? = null

    var dbRepo: UmAppDatabase? = null

    class ChartViewHolder(val itemBinding: ItemReportChartHeaderBinding) : RecyclerView.ViewHolder(itemBinding.root)

    class RecyclerViewChartAdapter(val activityEventHandler: ReportDetailFragmentEventHandler,
                                   var presenter: ReportDetailPresenter?) : ListAdapter<ReportGraphHelper.ChartData, ChartViewHolder>(DIFFUTIL_CHART) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
            return ChartViewHolder(ItemReportChartHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false).apply {
                mPresenter = presenter
            })
        }

        override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
            val item = getItem(position)
            holder.itemBinding.chart = item
            holder.itemBinding.previewChartView.setChartData(item)
        }

    }

    class StatementViewRecyclerAdapter(
            val activityEventHandler: ReportDetailFragmentEventHandler,
            var presenter: ReportDetailPresenter?) :
            PagedListAdapter<StatementListReport,
                    StatementViewRecyclerAdapter.StatementViewHolder>(DIFFUTIL_STATEMENT) {

        class StatementViewHolder(val binding: ItemReportStatementListBinding) :
                RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatementViewHolder {
            val viewHolder = StatementViewHolder(ItemReportStatementListBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false).apply {
                mPresenter = presenter
            })

            return viewHolder
        }

        override fun onBindViewHolder(holder: StatementViewHolder, position: Int) {
            holder.binding.report = getItem(position)
        }
    }

    override var statementList: DataSource.Factory<Int, StatementListReport>? = null
        get() = field
        set(value) {
            statementLiveData?.removeObserver(statementListObserver)
            field = value
            val statementDao = dbRepo?.statementDao ?: return
            statementLiveData = value?.asRepositoryLiveData(statementDao)
            statementLiveData?.observe(viewLifecycleOwner, statementListObserver)
        }

    private var statementLiveData: LiveData<PagedList<StatementListReport>>? = null

    private val statementListObserver = Observer<PagedList<StatementListReport>?> { t ->
        statementAdapter?.submitList(t)
    }


    override var chartData: DoorMutableLiveData<ReportGraphHelper.ChartData>? = null
        get() = field
        set(value) {
            field?.removeObserver(chartListObserver)
            field = value
            entity = value?.value?.reportWithFilters
            value?.observe(this, chartListObserver)
        }


    private val chartListObserver = Observer<ReportGraphHelper.ChartData?> { t ->
        chartAdapter?.submitList(listOf(t))
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView: View
        mBinding = FragmentReportDetailBinding.inflate(inflater, container, false).also {
            rootView = it.root
        }

        chartAdapter = RecyclerViewChartAdapter(this, null)
        statementAdapter = StatementViewRecyclerAdapter(this, null)

        mergeAdapter = MergeAdapter(chartAdapter, statementAdapter)
        mBinding?.fragmentDetailReportList?.adapter = mergeAdapter

        mPresenter = ReportDetailPresenter(requireContext(), arguments.toStringMap(), this,
                this, UstadMobileSystemImpl.instance,
                UmAccountManager.getActiveDatabase(requireContext()),
                UmAccountManager.getRepositoryForActiveAccount(requireContext()),
                UmAccountManager.activeAccountLiveData)
        mPresenter?.onCreate(savedInstanceState.toNullableStringMap())


        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fabManager?.onClickListener = {
            val report = entity
            if (report == null || report.reportUid == 0L) {
                findNavController().popBackStack()
            } else detailPresenter?.handleClickEdit()

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
        mPresenter = null
        entity = null
        chartAdapter = null
        mergeAdapter = null
        statementAdapter = null
    }

    override fun onResume() {
        super.onResume()
        if (mBinding?.report != null) {
            (activity as? AppCompatActivity)?.supportActionBar?.title = mBinding?.report?.reportTitle
        }
    }

    override var entity: ReportWithFilters? = null
        get() = field
        set(value) {
            field = value
            mBinding?.report = value
        }

    override var editButtonMode: EditButtonMode = EditButtonMode.GONE
        get() = field
        set(value) {
            mBinding?.editButtonMode = value
            field = value
        }


    override fun onClickAddRemoveDashboard(report: ReportWithFilters) {
        mPresenter?.handleOnClickAddOrRemoveFromDashboard(report)
    }


    companion object {

        val DIFFUTIL_STATEMENT = object : DiffUtil.ItemCallback<StatementListReport>() {
            override fun areItemsTheSame(oldItem: StatementListReport, newItem: StatementListReport): Boolean {
                return oldItem.statementUid == newItem.statementUid
            }

            override fun areContentsTheSame(oldItem: StatementListReport, newItem: StatementListReport): Boolean {
                return oldItem == newItem
            }
        }

        val DIFFUTIL_CHART = object : DiffUtil.ItemCallback<ReportGraphHelper.ChartData>() {
            override fun areItemsTheSame(oldItem: ReportGraphHelper.ChartData, newItem: ReportGraphHelper.ChartData): Boolean {
                return oldItem.reportWithFilters.reportUid == newItem.reportWithFilters.reportUid
            }

            override fun areContentsTheSame(oldItem: ReportGraphHelper.ChartData, newItem: ReportGraphHelper.ChartData): Boolean {
                return oldItem == newItem
            }
        }


    }

}
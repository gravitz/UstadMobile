package com.ustadmobile.port.android.view

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.FragmentClazzEditBinding
import com.toughra.ustadmobile.databinding.ItemScheduleBinding
import com.ustadmobile.core.controller.BitmaskEditPresenter
import com.ustadmobile.core.controller.ClazzEdit2Presenter
import com.ustadmobile.core.controller.ScopedGrantEditPresenter
import com.ustadmobile.core.controller.UstadEditPresenter
import com.ustadmobile.core.util.OneToManyJoinEditListener
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.ClazzEdit2View
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.port.android.util.ext.createTempFileForDestination
import com.ustadmobile.port.android.view.binding.ImageViewLifecycleObserver2
import com.ustadmobile.port.android.view.binding.MODE_END_OF_DAY
import com.ustadmobile.port.android.view.binding.MODE_START_OF_DAY
import java.io.File

interface ClazzEditFragmentEventHandler {

    fun onAddCourseBlockClicked()
}
class ClazzEditFragment() : UstadEditFragment<ClazzWithHolidayCalendarAndSchool>(),
        ClazzEdit2View, ClazzEditFragmentEventHandler,
        TitleDescBottomSheetOptionSelectedListener {

    private var bottomSheetOptionList: List<TitleDescBottomSheetOption> = listOf()
    private var mDataBinding: FragmentClazzEditBinding? = null

    private var mPresenter: ClazzEdit2Presenter? = null

    override val mEditPresenter: UstadEditPresenter<*, ClazzWithHolidayCalendarAndSchool>?
        get() = mPresenter

    private var scheduleRecyclerAdapter: ScheduleRecyclerAdapter? = null

    private var scheduleRecyclerView: RecyclerView? = null

    private var courseBlockRecyclerAdapter: CourseBlockRecyclerAdapter? = null

    private var courseBlockRecyclerView: RecyclerView? = null

    private val scheduleObserver = Observer<List<Schedule>?> {
        t -> scheduleRecyclerAdapter?.submitList(t)
    }

    private var scopedGrantRecyclerAdapter: ScopedGrantAndNameEditRecyclerViewAdapter? = null

    private val scopedGrantListObserver = Observer<List<ScopedGrantAndName>> {
        t -> scopedGrantRecyclerAdapter?.submitList(t)
    }

    private val courseBlockObserver = Observer<List<CourseBlockWithEntity>?> {
        t -> courseBlockRecyclerAdapter?.dataSet = t
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
        get() = field
        set(value) {
            field = value
            mDataBinding?.clazzEndDateError = value
        }
    override var clazzStartDateError: String? = null
        get() = field
        set(value) {
            field = value
            mDataBinding?.clazzStartDateError = value
        }

    override var scopedGrants: DoorLiveData<List<ScopedGrantAndName>>? = null
        set(value) {
            field?.removeObserver(scopedGrantListObserver)
            field = value
            field?.observe(this, scopedGrantListObserver)
        }

    private var imageViewLifecycleObserver: ImageViewLifecycleObserver2? = null

    override var coursePicture: CoursePicture?
        get() = mDataBinding?.coursePicture
        set(value) {
            mDataBinding?.coursePicture = value
        }

    /**
     * This may lead to I/O activity - do not call from the main thread!
     */
    override var coursePicturePath: String?
        get() {
            val boundPicUri = mDataBinding?.coursePictureUri
            if(boundPicUri == null) {
                return null
            }else{
                val uriObj = Uri.parse(boundPicUri)
                if(uriObj.scheme == "file") {
                    return uriObj.toFile().absolutePath
                }else {
                    val tmpFile = findNavController().createTempFileForDestination(requireContext(),
                        "coursePicture-${System.currentTimeMillis()}")
                    try {
                        val input = (context as Context).contentResolver.openInputStream(uriObj) ?: return null
                        val output = tmpFile.outputStream()
                        input.copyTo(tmpFile.outputStream())
                        output.flush()
                        output.close()
                        input.close()
                        return tmpFile.absolutePath
                    }catch(e: Exception) {
                        e.printStackTrace()
                    }
                    return null
                }
            }
        }

        set(value) {
            if(value != null) {
                mDataBinding?.coursePictureUri = Uri.fromFile(File(value)).toString()
            }else {
                mDataBinding?.coursePictureUri = null
            }
        }



    class ScheduleRecyclerAdapter(var oneToManyEditListener: OneToManyJoinEditListener<Schedule>?,
                                  var presenter: ClazzEdit2Presenter?): ListAdapter<Schedule,
            ScheduleRecyclerAdapter.ScheduleViewHolder>(DIFF_CALLBACK_SCHEDULE) {

        class ScheduleViewHolder(val binding: ItemScheduleBinding): RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
            val viewHolder = ScheduleViewHolder(ItemScheduleBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false))
            viewHolder.binding.mPresenter = presenter
            viewHolder.binding.oneToManyJoinListener = oneToManyEditListener
            return viewHolder
        }

        override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
            holder.binding.schedule = getItem(position)
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)

            oneToManyEditListener = null
            presenter = null
        }
    }



    override var entity: ClazzWithHolidayCalendarAndSchool? = null
        get() = field
        set(value) {
            mDataBinding?.clazz = value
            mDataBinding?.dateTimeMode = MODE_START_OF_DAY
            mDataBinding?.dateTimeModeEnd = MODE_END_OF_DAY
            mDataBinding?.timeZoneId = value?.clazzTimeZone?:value?.school?.schoolTimeZone?:"UTC"
            field = value
        }


    override var fieldsEnabled: Boolean = true
        set(value) {
            super.fieldsEnabled = value
            field = value
            mDataBinding?.fieldsEnabled = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageViewLifecycleObserver = ImageViewLifecycleObserver2(
            requireActivity().activityResultRegistry,null, 1).also {
            lifecycle.addObserver(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView: View

        mDataBinding = FragmentClazzEditBinding.inflate(inflater, container, false).also {
            rootView = it.root
            it.imageViewLifecycleObserver = imageViewLifecycleObserver
            it.featuresBitmaskFlags = BitmaskEditPresenter.FLAGS_AVAILABLE
            it.activityEventHandler = this
        }

        scheduleRecyclerView = rootView.findViewById(R.id.activity_clazz_edit_schedule_recyclerview)
        courseBlockRecyclerView = rootView.findViewById(R.id.activity_clazz_edit_course_block_recyclerview)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEditFragmentTitle(R.string.add_a_new_course, R.string.edit_course)

        mPresenter = ClazzEdit2Presenter(requireContext(), arguments.toStringMap(), this@ClazzEditFragment,
            di, viewLifecycleOwner).withViewLifecycle()

        mDataBinding?.scheduleOneToManyListener = mPresenter?.scheduleOneToManyJoinListener
        mDataBinding?.mPresenter = mPresenter
        scheduleRecyclerAdapter = ScheduleRecyclerAdapter(
            mPresenter?.scheduleOneToManyJoinListener, mPresenter)

        scheduleRecyclerView?.adapter = scheduleRecyclerAdapter
        scheduleRecyclerView?.layoutManager = LinearLayoutManager(requireContext())


        mDataBinding?.courseBlockOneToManyListener = mPresenter
        courseBlockRecyclerAdapter = CourseBlockRecyclerAdapter(
                mPresenter, mDataBinding?.activityClazzEditCourseBlockRecyclerview)

        courseBlockRecyclerView?.adapter = courseBlockRecyclerAdapter
        courseBlockRecyclerView?.layoutManager = LinearLayoutManager(requireContext())

        val permissionList = ScopedGrantEditPresenter.PERMISSION_LIST_MAP[Clazz.TABLE_ID]
            ?: throw IllegalStateException("ScopedGrantEdit permission list not found!")
        scopedGrantRecyclerAdapter = ScopedGrantAndNameEditRecyclerViewAdapter(
            mPresenter?.scopedGrantOneToManyHelper, permissionList)

        mDataBinding?.clazzEditFragmentPermissionsInc?.itemScopedGrantOneToNRecycler?.apply {
            adapter = scopedGrantRecyclerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        bottomSheetOptionList = listOf(
                TitleDescBottomSheetOption(
                        requireContext().getString(R.string.module),
                        requireContext().getString(R.string.course_module),
                        CourseBlock.BLOCK_MODULE_TYPE),
                TitleDescBottomSheetOption(
                        requireContext().getString(R.string.text),
                        requireContext().getString(R.string.formatted_text_to_show_to_course_participants),
                        CourseBlock.BLOCK_TEXT_TYPE),
                TitleDescBottomSheetOption(
                        requireContext().getString(R.string.content),
                        requireContext().getString(R.string.add_course_block_content_desc),
                        CourseBlock.BLOCK_CONTENT_TYPE),
                TitleDescBottomSheetOption(
                        requireContext().getString(R.string.assignments),
                        requireContext().getString(R.string.add_assignment_block_content_desc),
                        CourseBlock.BLOCK_ASSIGNMENT_TYPE),
                TitleDescBottomSheetOption(
                        requireContext().getString(R.string.discussion_board),
                        requireContext().getString(R.string.add_discussion_board_desc),
                        CourseBlock.BLOCK_DISCUSSION_TYPE),

        )

        mPresenter?.onCreate(backStackSavedState)

    }

    override fun onAddCourseBlockClicked() {
        val sheet = TitleDescBottomSheetOptionFragment(bottomSheetOptionList, this)
        sheet.show(childFragmentManager, sheet.tag)
    }

    override fun onBottomSheetOptionSelected(optionSelected: TitleDescBottomSheetOption) {
        when(optionSelected.optionCode) {
            CourseBlock.BLOCK_ASSIGNMENT_TYPE -> mPresenter?.handleClickAddAssignment()
            CourseBlock.BLOCK_MODULE_TYPE -> mPresenter?.handleClickAddModule()
            CourseBlock.BLOCK_CONTENT_TYPE -> mPresenter?.handleClickAddContent()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDataBinding = null
        scheduleRecyclerView = null
        scheduleRecyclerAdapter = null
        courseBlockRecyclerView = null
        courseBlockRecyclerAdapter = null
        courseBlocks = null
        clazzSchedules = null
    }

    companion object {

        @JvmField
        val BLOCK_ICON_MAP = mapOf(
            CourseBlock.BLOCK_MODULE_TYPE to R.drawable.ic_baseline_folder_open_24,
            CourseBlock.BLOCK_ASSIGNMENT_TYPE to R.drawable.baseline_assignment_turned_in_24,
            CourseBlock.BLOCK_CONTENT_TYPE to R.drawable.video_youtube,
            CourseBlock.BLOCK_TEXT_TYPE to R.drawable.ic_baseline_title_24
        )

        val DIFF_CALLBACK_SCHEDULE: DiffUtil.ItemCallback<Schedule> = object: DiffUtil.ItemCallback<Schedule>() {
            override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
                return oldItem.scheduleUid == newItem.scheduleUid
            }

            override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule): Boolean {
                return oldItem == newItem
            }
        }
    }


}
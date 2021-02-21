package com.ustadmobile.port.android.view

import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import androidx.navigation.fragment.findNavController
import com.toughra.ustadmobile.R
import com.toughra.ustadmobile.databinding.FragmentClazzEnrolmentBinding
import com.ustadmobile.core.controller.ClazzEnrolmentEditPresenter
import com.ustadmobile.core.controller.UstadEditPresenter
import com.ustadmobile.core.util.IdOption
import com.ustadmobile.core.util.ext.observeResult
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.ClazzEnrolmentEditView
import com.ustadmobile.lib.db.entities.ClazzEnrolmentWithLeavingReason
import com.ustadmobile.lib.db.entities.LeavingReason
import com.ustadmobile.port.android.util.ext.currentBackStackEntrySavedStateMap
import com.ustadmobile.port.android.view.ext.navigateToPickEntityFromList
import java.util.*


interface ClazzEnrolmentFragmentEventHandler {
    fun handleReasonLeavingClicked()
}

class ClazzEnrolmentEditFragment: UstadEditFragment<ClazzEnrolmentWithLeavingReason>(),
        ClazzEnrolmentEditView, ClazzEnrolmentFragmentEventHandler {

    private var mBinding: FragmentClazzEnrolmentBinding? = null

    private var mPresenter: ClazzEnrolmentEditPresenter? = null

    override val mEditPresenter: UstadEditPresenter<*, ClazzEnrolmentWithLeavingReason>?
        get() = mPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView: View
        mBinding = FragmentClazzEnrolmentBinding.inflate(inflater, container, false).also {
            rootView = it.root
            it.activityEventHandler = this
        }

        mPresenter = ClazzEnrolmentEditPresenter(requireContext(), arguments.toStringMap(), this,
                viewLifecycleOwner, di)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEditFragmentTitle(R.string.new_enrolment, R.string.edit_enrolment)
        val navController = findNavController()

        mPresenter?.onCreate(navController.currentBackStackEntrySavedStateMap())

        navController.currentBackStackEntry?.savedStateHandle?.observeResult(this,
                LeavingReason::class.java) {
            val reason = it.firstOrNull() ?: return@observeResult
            mBinding?.clazzEnrolmentEditReasonText?.setText(reason.leavingReasonTitle)
            entity?.clazzEnrolmentLeavingReasonUid = reason.leavingReasonUid
            entity?.leavingReason = reason
            mBinding?.clazzEnrolment = entity
        }
    }

    override fun handleReasonLeavingClicked() {
        onSaveStateToBackStackStateHandle()
        navigateToPickEntityFromList(LeavingReason::class.java, R.id.leaving_reason_list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
        mPresenter = null
        entity = null
    }

    override var entity: ClazzEnrolmentWithLeavingReason? = null
        get() = field
        set(value) {
            field = value
            mBinding?.clazzEnrolment = value
        }

    override var roleList: List<IdOption>? = null
        get() = field
        set(value) {
            field = value
            mBinding?.roleOptions = value
        }
    override var statusList: List<IdOption>? = null
        get() = field
        set(value) {
            field = value
            mBinding?.statusOptions = value
        }

    override var startDateError: String? = null
        get() = field
        set(value) {
            val newValue: String? = if(value?.contains("%1\$s") == true){
                val dateFormat = DateFormat.getDateFormat(requireContext())
                value.replace("%1\$s", dateFormat.format(Date(entity?.clazzEnrolmentDateLeft ?: 0L)))
            }else{
                value
            }
            field = newValue
            mBinding?.startDateError = newValue
        }

    override var endDateError: String? = null
        get() = field
        set(value) {
            field = value
            mBinding?.endDateError = value
        }

    override var fieldsEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            mBinding?.fieldsEnabled = value
        }
}
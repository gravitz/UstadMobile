package com.ustadmobile.port.android.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.toughra.ustadmobile.databinding.FragmentLeavingReasonEditBinding
import com.ustadmobile.core.controller.LeavingReasonEditPresenter
import com.ustadmobile.core.controller.UstadEditPresenter
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.LeavingReasonEditView
import com.ustadmobile.lib.db.entities.LeavingReason


interface LeavingReasonEditFragmentEventHandler {

}

class LeavingReasonEditFragment: UstadEditFragment<LeavingReason>(), LeavingReasonEditView, LeavingReasonEditFragmentEventHandler {

    private var mBinding: FragmentLeavingReasonEditBinding? = null

    private var mPresenter: LeavingReasonEditPresenter? = null

    override val mEditPresenter: UstadEditPresenter<*, LeavingReason>?
        get() = mPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView: View
        mBinding = FragmentLeavingReasonEditBinding.inflate(inflater, container, false).also {
            rootView = it.root
        }

        mPresenter = LeavingReasonEditPresenter(requireContext(), arguments.toStringMap(), this,
                viewLifecycleOwner, di)
        mPresenter?.onCreate(backStackSavedState)

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
        mPresenter = null
        entity = null
    }

    override var entity: LeavingReason? = null
        get() = field
        set(value) {
            field = value
            mBinding?.leavingReason = value
        }

    override var reasonTitleError: String? = null
        get() = field
        set(value) {
            field = value
        }

    override var fieldsEnabled: Boolean = false
        get() = field
        set(value) {
            field = value
            mBinding?.fieldsEnabled = value
        }
}
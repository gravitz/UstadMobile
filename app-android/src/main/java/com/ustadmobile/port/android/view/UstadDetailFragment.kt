package com.ustadmobile.port.android.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.UstadDetailPresenter
import com.ustadmobile.core.view.EditButtonMode
import com.ustadmobile.core.view.UstadDetailView
import com.ustadmobile.port.android.view.util.PresenterViewLifecycleObserver

abstract class UstadDetailFragment<T: Any>: UstadBaseFragment(), UstadDetailView<T> {

    abstract val detailPresenter: UstadDetailPresenter<*, *>?

    override var editButtonMode: EditButtonMode = EditButtonMode.GONE
        get() = field
        set(value) {
            field = value
            fabManager?.visible = (value == EditButtonMode.FAB)
        }

    protected open var mActivityWithFab: UstadListViewActivityWithFab? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivityWithFab = (context as? UstadListViewActivityWithFab)
    }

    override fun showSnackBar(message: String, action: () -> Unit, actionMessageId: Int) {
        (activity as? MainActivity)?.showSnackBar(message, action, actionMessageId)
    }

    override fun onDetach() {
        super.onDetach()
        mActivityWithFab = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fabManager?.text = requireContext().getString(R.string.edit)
        fabManager?.icon = R.drawable.ic_edit_white_24dp
        fabManager?.onClickListener = {
            detailPresenter?.handleClickEdit()
        }
    }
}
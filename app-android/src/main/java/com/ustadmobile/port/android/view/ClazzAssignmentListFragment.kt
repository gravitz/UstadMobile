package com.ustadmobile.port.android.view

import android.os.Bundle
import android.view.*
import com.toughra.ustadmobile.R
import com.ustadmobile.core.controller.ClazzAssignmentListPresenter
import com.ustadmobile.core.controller.UstadListPresenter
import com.ustadmobile.core.util.ext.toStringMap
import com.ustadmobile.core.view.ClazzAssignmentListView
import com.ustadmobile.lib.db.entities.ClazzAssignment
import com.ustadmobile.port.android.view.ext.navigateToEditEntity
import com.ustadmobile.port.android.view.util.ListHeaderRecyclerViewAdapter


class ClazzAssignmentListFragment(): UstadListViewFragment<ClazzAssignment, ClazzAssignment>(),
        ClazzAssignmentListView, MessageIdSpinner.OnMessageIdOptionSelectedListener, View.OnClickListener{

    private var mPresenter: ClazzAssignmentListPresenter? = null

    override val listPresenter: UstadListPresenter<*, in ClazzAssignment>?
        get() = mPresenter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        mPresenter = ClazzAssignmentListPresenter(requireContext(), arguments.toStringMap(),
                this, di, viewLifecycleOwner)

        mDataRecyclerViewAdapter = AssignmentListRecyclerAdapter(mPresenter)
        mUstadListHeaderRecyclerViewAdapter = ListHeaderRecyclerViewAdapter(
                onClickSort = this, sortOrderOption = mPresenter?.sortOptions?.get(0))
        return view
    }

    override fun onResume() {
        super.onResume()
        fabManager?.text = requireContext().getString(R.string.clazz_assignment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.menu_search).isVisible = true
    }

    /**
     * OnClick function that will handle when the user clicks to create a new item
     */
    override fun onClick(view: View?) {
        if(view?.id == R.id.item_createnew_layout)
            navigateToEditEntity(null, R.id.assignment_edit_dest, ClazzAssignment::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPresenter = null
        dbRepo = null
    }

    override val displayTypeRepo: Any?
        get() = dbRepo?.clazzAssignmentDao

}
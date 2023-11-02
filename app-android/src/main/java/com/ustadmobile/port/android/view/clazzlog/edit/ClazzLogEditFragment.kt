package com.ustadmobile.port.android.view.clazzlog.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.ustadmobile.core.viewmodel.clazzlog.edit.ClazzLogEditViewModel
import com.ustadmobile.port.android.view.UstadBaseMvvmFragment
import com.ustadmobile.libuicompose.view.clazzlog.edit.ClazzLogEditScreenForViewModel

class ClazzLogEditFragment: UstadBaseMvvmFragment() {

    private val viewModel by ustadViewModels(::ClazzLogEditViewModel)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launchNavigatorCollector(viewModel)
        viewLifecycleOwner.lifecycleScope.launchAppUiStateCollector(viewModel)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ClazzLogEditScreenForViewModel(viewModel)
            }
        }
    }
}

package com.ustadmobile.port.android.view.timezone

import android.os.Bundle
import android.view.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.themeadapter.material.MdcTheme
import com.ustadmobile.core.util.ext.formattedString
import com.ustadmobile.core.viewmodel.timezone.TimeZoneListViewModel
import com.ustadmobile.core.viewmodel.timezone.TimezoneListUiState
import com.ustadmobile.port.android.view.UstadBaseMvvmFragment
import kotlinx.datetime.Clock
import java.util.*
import kotlinx.datetime.TimeZone as TimeZoneKt

class TimeZoneListFragment : UstadBaseMvvmFragment() {

    val viewModel: TimeZoneListViewModel by ustadViewModels(::TimeZoneListViewModel)

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
                MdcTheme {
                    TimeZoneListScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)

@Composable
fun TimeZoneListScreen(
    uiState: TimezoneListUiState,
    onListItemClick: (TimeZoneKt) -> Unit,
) {

    val timeNow = Clock.System.now()

    LazyColumn(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ){
        items(
            items = uiState.timeZoneList,
            key = { it.id },
        ) { timeZone ->
            val timeZoneFormatted: String = remember(timeZone.id) {
                timeZone.formattedString(timeNow)
            }

            ListItem(
                modifier = Modifier
                    .clickable {  onListItemClick(timeZone) } ,
                text = { Text(timeZoneFormatted) }
            )
        }
    }
}

@Composable
fun TimeZoneListScreen(
    viewModel: TimeZoneListViewModel
) {
    val uiState: TimezoneListUiState by viewModel.uiState.collectAsState(TimezoneListUiState())
    TimeZoneListScreen(
        uiState = uiState,
        onListItemClick = viewModel::onClickEntry
    )
}
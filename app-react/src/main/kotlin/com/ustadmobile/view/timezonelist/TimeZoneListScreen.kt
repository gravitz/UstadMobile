package com.ustadmobile.view.timezonelist

import com.ustadmobile.core.hooks.collectAsState
import com.ustadmobile.core.util.ext.formattedString
import com.ustadmobile.core.util.ext.gmtOffsetString
import com.ustadmobile.core.viewmodel.TimeZoneListViewModel
import com.ustadmobile.core.viewmodel.TimezoneListUiState
import com.ustadmobile.hooks.useMuiAppState
import com.ustadmobile.hooks.usePagingSource
import com.ustadmobile.hooks.useUstadViewModel
import com.ustadmobile.view.components.virtuallist.VirtualList
import com.ustadmobile.view.components.virtuallist.VirtualListOutlet
import com.ustadmobile.view.components.virtuallist.virtualListContent
import csstype.Contain
import csstype.Height
import csstype.Overflow
import csstype.pct
import js.core.jso
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import mui.material.Container
import mui.material.ListItem
import mui.material.ListItemButton
import mui.material.ListItemText
import react.*

external interface TimeZoneListProps: Props {

    var uiState: TimezoneListUiState

    var onListItemClick: (TimeZone) -> Unit

}

val TimeZoneListComponent = FC<TimeZoneListProps> {props ->
    val infiniteQueryResult = usePagingSource(
        props.uiState.timeZoneList, true, 50
    )

    val muiAppState = useMuiAppState()

    VirtualList {
        style = jso {
            height = "calc(100vh - ${muiAppState.appBarHeight}px)".unsafeCast<Height>()
            width = 100.pct
            contain = Contain.strict
            overflowY = Overflow.scroll
        }

        content = virtualListContent {

            infiniteQueryPagingItems(
                items = infiniteQueryResult,
                key = { it.id }
            ) { timeZoneItem ->
                TimeZoneListItem.create {
                    timeZone = timeZoneItem
                    onClick = props.onListItemClick
                }
            }
        }


        Container {
            VirtualListOutlet()
        }
    }
}

external interface TimeZoneListItemProps: Props {

    var timeZone: TimeZone?

    var onClick: (TimeZone) -> Unit

}

val TimeZoneListItem = FC<TimeZoneListItemProps> { props ->

    val labelStr = useMemo(props.timeZone?.id) {
        props.timeZone?.formattedString() ?: ""
    }

    ListItem {
        ListItemButton {
            onClick = {
                props.timeZone?.also { props.onClick(it) }
            }

            ListItemText {
                primary = ReactNode(labelStr)
            }
        }
    }

}

val TimeZoneListScreen = FC<Props> {
    val viewModel = useUstadViewModel { di, savedStateHandle ->
        TimeZoneListViewModel(di, savedStateHandle)
    }

    val uiStateVar: TimezoneListUiState by viewModel.uiState.collectAsState(TimezoneListUiState())

    TimeZoneListComponent {
        uiState = uiStateVar
        onListItemClick = viewModel::onClickEntry

    }
}
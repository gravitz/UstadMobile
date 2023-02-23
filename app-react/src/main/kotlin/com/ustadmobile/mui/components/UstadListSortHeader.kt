package com.ustadmobile.mui.components

import com.ustadmobile.core.db.dao.ClazzDaoCommon
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.hooks.useStringsXml
import com.ustadmobile.core.util.SortOrderOption
import csstype.JustifyContent
import csstype.pct
import csstype.px
import mui.icons.material.ArrowDownward
import mui.icons.material.ArrowUpward
import mui.material.*
import mui.system.sx
import react.*
import react.dom.aria.AriaHasPopup
import react.dom.aria.ariaHasPopup
import web.html.HTMLElement

external interface UstadListSortHeaderProps : Props {

    var activeSortOrderOption: SortOrderOption

    var enabled: Boolean?

    var onClickSort: (SortOrderOption) -> Unit

    var sortOptions: List<SortOrderOption>

}

val UstadListSortHeader = FC<UstadListSortHeaderProps> { props ->

    val strings = useStringsXml()

    val sortIcon = if(props.activeSortOrderOption.order)
        ArrowDownward.create()
    else
        ArrowUpward.create()

    var anchorState by useState<HTMLElement?>(null)

    ButtonBase {
        ariaHasPopup = AriaHasPopup.`true`
        sx {
            justifyContent = JustifyContent.start
            width = 100.pct
            padding = 16.px
        }
        disabled = props.enabled == false

        onClick = {
            anchorState = it.currentTarget
        }

        Typography {
            + strings[props.activeSortOrderOption.fieldMessageId]
        }

        Icon {
            + sortIcon
        }
    }

    /**
     * As per
     * https://mui.com/material-ui/react-menu/#basic-menu
     */
    Menu {
        anchorEl = anchorState?.let { element -> { element }  }
        open = anchorState != null
        onClose = { anchorState = null }

        props.sortOptions.forEach { option ->
            MenuItem {
                onClick = {
                    anchorState = null
                    props.onClickSort(option)
                }

                + strings[option.fieldMessageId]
                val orderLabel = if(option.order) {
                    strings[MessageID.ascending]
                }else {
                    strings[MessageID.descending]
                }
                +" ($orderLabel)"
            }
        }

    }
}

val UstadListSortHeaderPreview = FC<Props> {

    UstadListSortHeader {
        activeSortOrderOption = SortOrderOption(
                MessageID.name,
                ClazzDaoCommon.SORT_CLAZZNAME_ASC,
                true
            )
        enabled = true
        onClickSort = { }
    }
}
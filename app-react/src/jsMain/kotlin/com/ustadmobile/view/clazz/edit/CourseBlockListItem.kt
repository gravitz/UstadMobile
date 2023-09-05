package com.ustadmobile.view.clazz.edit

import com.ustadmobile.core.viewmodel.clazz.edit.ClazzEditUiState
import com.ustadmobile.lib.db.entities.CourseBlock
import com.ustadmobile.lib.db.entities.CourseBlockWithEntity
import com.ustadmobile.view.CONTENT_ENTRY_TYPE_ICON_MAP
import com.ustadmobile.wrappers.reacteasysort.SortableItem
import web.cssom.number
import web.cssom.px
import mui.icons.material.SvgIconComponent
import mui.icons.material.Folder
import mui.icons.material.Article
import mui.icons.material.Collections
import mui.icons.material.Assignment
import mui.icons.material.Forum
import mui.icons.material.TextSnippet
//WARNING: DO NOT Replace with import mui.icons.material.[*] - Leads to severe IDE performance issues 10/Apr/23 https://youtrack.jetbrains.com/issue/KT-57897/Intellisense-and-code-analysis-is-extremely-slow-and-unusable-on-Kotlin-JS
import mui.material.ListItem
import mui.material.ListItemButton
import mui.material.ListItemIcon
import mui.material.ListItemText
import mui.system.sx
import react.*
import react.dom.html.ReactHTML
import web.html.HTMLDivElement

val COURSE_BLOCK_TYPE_ICON_MAP: Map<Int, SvgIconComponent> = CONTENT_ENTRY_TYPE_ICON_MAP + mapOf(
    CourseBlock.BLOCK_MODULE_TYPE to Folder,
    CourseBlock.BLOCK_TEXT_TYPE to Article,
    CourseBlock.BLOCK_CONTENT_TYPE to Collections,
    CourseBlock.BLOCK_ASSIGNMENT_TYPE to Assignment,
    CourseBlock.BLOCK_DISCUSSION_TYPE to Forum,
)

external interface CourseBlockListItemProps : Props {

    var courseBlock: CourseBlockWithEntity

    var uiState: ClazzEditUiState.CourseBlockUiState

    var onClickEditCourseBlock: (CourseBlockWithEntity) -> Unit

    var fieldsEnabled: Boolean

    var onClickHideBlockPopupMenu: (CourseBlockWithEntity) -> Unit

    var onClickUnHideBlockPopupMenu: (CourseBlockWithEntity) -> Unit

    var onClickIndentBlockPopupMenu: (CourseBlockWithEntity) -> Unit

    var onClickUnIndentBlockPopupMenu: (CourseBlockWithEntity) -> Unit

    var onClickDeleteBlockPopupMenu: (CourseBlockWithEntity) -> Unit

}

val CourseBlockListItem = FC<CourseBlockListItemProps> { props ->
    SortableItem {
        ReactHTML.div {
            val divRef : MutableRefObject<HTMLDivElement> = useRef(null)

            ListItem{
                val courseBlockEditAlpha: Double = if (props.courseBlock.cbHidden) 0.5 else 1.0
                val startPadding = (props.courseBlock.cbIndentLevel * 24).px

                val iconFlag = if(props.courseBlock.cbType == CourseBlock.BLOCK_CONTENT_TYPE)
                    props.courseBlock.entry?.contentTypeFlag
                else
                    props.courseBlock.cbType

                ListItemButton {
                    sx {
                        opacity = number(courseBlockEditAlpha)
                    }

                    onClick = {
                        //Avoid triggering the onClick listener if the dragging is in process
                        //This might not be needed
                        if(divRef.current?.classList?.contains(COURSE_BLOCK_DRAG_CLASS) != true) {
                            props.onClickEditCourseBlock(props.courseBlock)
                        }
                    }

                    ListItemIcon {
                        sx {
                            paddingLeft = startPadding
                        }
                        + (COURSE_BLOCK_TYPE_ICON_MAP[iconFlag]?.create() ?: TextSnippet.create())
                    }

                    ListItemText {
                        primary = ReactNode(props.courseBlock.cbTitle ?: "")
                    }
                }

                secondaryAction = PopUpMenu.create {

                    fieldsEnabled = props.fieldsEnabled
                    onClickHideBlockPopupMenu = props.onClickHideBlockPopupMenu
                    onClickUnHideBlockPopupMenu = props.onClickUnHideBlockPopupMenu
                    onClickIndentBlockPopupMenu = props.onClickIndentBlockPopupMenu
                    onClickUnIndentBlockPopupMenu = props.onClickUnIndentBlockPopupMenu
                    onClickDeleteBlockPopupMenu = props.onClickDeleteBlockPopupMenu
                    uiState = props.uiState
                }
            }
        }
    }
}
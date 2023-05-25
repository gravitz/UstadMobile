package com.ustadmobile.view

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.hooks.useStringsXml
import com.ustadmobile.core.paging.ListPagingSource
import com.ustadmobile.core.viewmodel.CourseDiscussionDetailUiState
import com.ustadmobile.door.util.systemTimeInMillis
import com.ustadmobile.hooks.useMuiAppState
import com.ustadmobile.hooks.usePagingSource
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.mui.components.*
import com.ustadmobile.view.components.UstadBlankIcon
import com.ustadmobile.view.components.UstadPersonAvatar
import com.ustadmobile.view.components.virtuallist.VirtualList
import com.ustadmobile.view.components.virtuallist.VirtualListOutlet
import com.ustadmobile.view.components.virtuallist.virtualListContent
import csstype.Contain
import csstype.Height
import csstype.Overflow
import csstype.pct
import csstype.px
import js.core.jso
import kotlinx.html.currentTimeMillis
import mui.material.*
import mui.icons.material.Add as AddIcon
import mui.icons.material.Chat as ChatIcon
import mui.icons.material.ReplyAll as ReplyAllIcon
import mui.material.List
import mui.material.styles.TypographyVariant
import mui.system.responsive
import mui.system.sx
import react.*
import kotlin.js.Date

external interface CourseDiscussionDetailProps: Props {
    var uiState: CourseDiscussionDetailUiState
    var onClickPost: (DiscussionPostWithDetails) -> Unit
    var onDeleteClick: (DiscussionPostWithDetails) -> Unit
    var onClickAddItem: () -> Unit
}

val CourseDiscussionDetailComponent2 = FC<CourseDiscussionDetailProps> { props ->

    val strings = useStringsXml()

    val infiniteQueryResult = usePagingSource(
        pagingSourceFactory = props.uiState.posts,
        placeholdersEnabled = true
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
            item(key = "description") {
                UstadRawHtml.create {
                    html = props.uiState.courseBlock?.cbDescription ?: ""
                }
            }
            item(key = "divider") {
                Divider.create()
            }
            infiniteQueryPagingItems(
                items = infiniteQueryResult,
                key = { it.discussionPostUid.toString() }
            ) { discussionPostItem ->
                ListItem.create {
                    alignItems = ListItemAlignItems.flexStart

                    ListItemButton {
                        alignItems = ListItemButtonAlignItems.flexStart

                        ListItemIcon {
                            UstadPersonAvatar {
                                personUid = discussionPostItem?.discussionPostStartedPersonUid ?: 0L
                            }
                        }

                        Stack {
                            Typography {
                                variant = TypographyVariant.subtitle1
                                + "${discussionPostItem?.authorPersonFirstNames} ${discussionPostItem?.authorPersonLastName}"
                            }

                            Typography {
                                variant = TypographyVariant.subtitle2
                                + (discussionPostItem?.discussionPostTitle ?:"")
                            }

                            Stack {
                                direction = responsive(StackDirection.row)

                                ChatIcon {
                                    color = SvgIconColor.action
                                    fontSize = SvgIconSize.small
                                }

                                Typography {
                                    variant = TypographyVariant.body1

                                    +(discussionPostItem?.postLatestMessage ?: discussionPostItem?.discussionPostMessage ?: "")
                                }
                            }

                            Stack {
                                direction = responsive(StackDirection.row)

                                ReplyAllIcon {
                                    color = SvgIconColor.action
                                    fontSize = SvgIconSize.small
                                }

                                Typography {
                                    variant = TypographyVariant.body2
                                    +(strings[MessageID.num_replies].replace("%1\$d",
                                        discussionPostItem?.postRepliesCount?.toString() ?: "0"))
                                }
                            }
                        }
                    }
                }
            }
        }

        Container {
            List {
                VirtualListOutlet()
            }
        }
    }
}

val CourseDiscussionDetailPreview = FC<Props> {
    CourseDiscussionDetailComponent2 {
        uiState = CourseDiscussionDetailUiState(

            courseBlock = CourseBlock().apply {
                cbTitle = "Sales and Marketting Discussion"
                cbDescription =
                    "This discussion group is for conversations and posts about Sales and Marketting course"

            },
            posts = {
                ListPagingSource(listOf(
                    DiscussionPostWithDetails().apply {
                        discussionPostTitle = "Can I join after week 2?"
                        discussionPostUid = 0L
                        discussionPostMessage = "Iam late to class, CAn I join after?"
                        discussionPostVisible = true
                        postRepliesCount = 4
                        postLatestMessage = "Just make sure you submit a late assignment."
                        authorPersonFirstNames = "Mike"
                        authorPersonLastName = "Jones"
                        discussionPostStartDate = systemTimeInMillis()
                    },
                    DiscussionPostWithDetails().apply {
                        discussionPostTitle = "How to install xlib?"
                        discussionPostMessage = "Which version of python do I need?"
                        discussionPostVisible = true
                        discussionPostUid = 1L
                        postRepliesCount = 2
                        postLatestMessage = "I have the same question"
                        authorPersonFirstNames = "Bodium"
                        authorPersonLastName = "Carafe"
                        discussionPostStartDate = systemTimeInMillis()
                    }
                ))
            },
        )
    }
}
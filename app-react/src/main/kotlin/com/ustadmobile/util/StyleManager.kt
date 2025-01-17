package com.ustadmobile.util

import Breakpoint
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.mui.components.spacingUnits
import com.ustadmobile.mui.theme.styledModule
import com.ustadmobile.redux.ReduxAppStateManager
import down
import kotlinx.css.*
import kotlinx.css.properties.LineHeight
import kotlinx.css.properties.Timing
import kotlinx.css.properties.Transition
import kotlinx.css.properties.ms
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import styled.StyleSheet
import up

/**
 * Responsible for styling HTML elements, to customize particular
 * element just check the defined style constants.
 * They are named as per component
 */
object StyleManager: StyleSheet("ComponentStyles", isStatic = true), DIAware {

    val theme = ReduxAppStateManager.getCurrentState().appTheme?.theme!!

    private val systemImpl : UstadMobileSystemImpl by instance()

    private val fullWidth = 100.pct

    private const val drawerWidth = 240

    private val tabletAndHighEnd = Breakpoint.sm

    val alignTextToStart by css {
        textAlign = TextAlign.start
    }

    val contentAfterIconMarginLeft by css {
        marginLeft = 2.pct
    }

    val umItemWithIconAndText by css {
        display = Display.flex
        flexDirection = FlexDirection.row
    }

    val alignTextCenter by css{
        textAlign = TextAlign.center
    }

    val defaultFullWidth by css {
        width = 100.pct
    }

    val defaultMarginTop  by css{
        marginTop = 2.spacingUnits
    }

    val defaultMarginBottom  by css{
        marginBottom = 2.spacingUnits
    }

    val defaultPaddingTop by css{
        paddingTop = 3.spacingUnits
    }

    val defaultPaddingTopBottom by css{
        paddingTop = 2.spacingUnits
        paddingBottom = 2.spacingUnits
    }

    val defaultDoubleMarginTop  by css{
        marginTop = 4.spacingUnits
    }

    val errorTextClass by css{
        color = Color(theme.palette.error.main)
        marginLeft = (if(systemImpl.isRtlActive()) 0 else 16).px
        marginRight= (if(systemImpl.isRtlActive()) 16 else 0).px
    }

    val errorClass by css {
        color = Color(theme.palette.error.main)
    }

    val successClass by css {
        color = Color.green.lighten(500)
    }

    val splashComponentContainer by css {
        flexGrow = 1.0
        width = 100.pct
        zIndex = 1
        overflow = Overflow.hidden
        position = Position.relative
        display = Display.flex
        flexDirection = FlexDirection.column
    }

    val splashComponentPreloadContainer by css{
        left = 50.pct
        top = 50.pct
        marginLeft = (-100).px
        marginTop = (-50).px
        position =  Position.fixed
        height = 200.px
        width = 200.px
    }

    val mainComponentErrorPaper by css{
        padding(2.spacingUnits)
        marginBottom = 2.spacingUnits
        color = Color.red
    }

    val mainComponentProgressIndicator by css {
        width = 100.pct
        display = Display.none
    }

    val mainComponentContainer by css {
        overflow = Overflow.hidden
        position = Position.relative
        display = Display.flex
        width = 100.pct
    }

    val mainComponentWrapperContainer by css {
        flexGrow = 1.0
        width = 100.pct
        zIndex = 1
        overflow = Overflow.hidden
        position = Position.relative
        display = Display.flex
        flexDirection = FlexDirection.column
        backgroundColor = Color(theme.palette.background.paper)
    }

    val mainComponentAppBar by css{
        position = Position.absolute
        marginLeft = when {
            systemImpl.isRtlActive() -> 0.px
            else -> drawerWidth.px
        }
        width = fullWidth
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            width = fullWidth - when {
                systemImpl.isRtlActive() -> 0.px
                else -> drawerWidth.px
            }
        }
    }


    val detailPaddingBottom by css {
        padding(bottom = 3.spacingUnits)
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            padding(bottom = 2.spacingUnits)
        }
    }

    val entryDetailRightSection by css {
        padding(bottom = 3.spacingUnits, top = 8.spacingUnits)
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            padding(bottom = 2.spacingUnits)
        }
    }

    val switchMargin by css {
        paddingRight = 5.spacingUnits
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            paddingRight = 1.spacingUnits
        }
    }

    val screenWithChartOnLeft by css {
        marginRight = 0.spacingUnits
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            marginRight = 2.spacingUnits
        }
    }

    val mainComponentAppBarWithNoNav by css{
        position = Position.absolute
        marginLeft = 0.px
        width = fullWidth
    }

    val mainComponentContentContainer by css {
        height = 100.vh
        flexGrow = 1.0
        minWidth = 0.px
        backgroundColor = Color(theme.palette.background.default)
        media(theme.breakpoints.down(Breakpoint.xs)){
            height = 91.vh
        }
    }

    val mainComponentBottomNav by css {
        position = Position.fixed
        bottom = 0.px
        left = 0.px
        right = 0.px
    }

    val mainComponentBrandIconContainer by css {
        height = 43.px
        marginTop = 20.px
        width = 100.pct
        padding(top = 0.px,
            right = (if(systemImpl.isRtlActive()) 20 else 0).px,
            bottom = 0.px, left = (if(systemImpl.isRtlActive()) 0 else 20 ).px)
    }

    val mainComponentBrandIcon by css{
        width = 80.pct
        height = 60.pct
    }

    val mainComponentSideNavMenuList by css {
        backgroundColor = Color(theme.palette.background.paper)
        width = drawerWidth.px
    }

    val toolbarTitle by css {
        if(systemImpl.isRtlActive()){
            textAlign = TextAlign.start
            marginRight = drawerWidth.px
            media(theme.breakpoints.down(tabletAndHighEnd)) {
                marginRight = 0.px
            }
        }
    }

    val mainComponentProfileOuterAvatar by css {
        width = 40.px
        height = 40.px
        cursor = Cursor.pointer
        margin(top = 0.px,
            right = (if(systemImpl.isRtlActive()) 10 else 0).pct, bottom = 0.px,
            left = (if(systemImpl.isRtlActive()) 0 else 10 ).pct)
        media(theme.breakpoints.down(tabletAndHighEnd)) {
            margin(top = 0.px,
                right = (if(systemImpl.isRtlActive()) 10 else 3).pct, bottom = 0.px,
                left = (if(systemImpl.isRtlActive()) 0 else 10 ).pct)
        }
        backgroundColor = Color(theme.palette.primary.light)
        alignItems = Align.center
        alignContent = Align.center
    }

    val mainComponentProfileInnerAvatar by css {
        width = 36.px
        height = 36.px
        color = Color.white
        if(systemImpl.isRtlActive()){
            marginRight = (2.5).px
        }else{
            marginLeft = (2.5).px
        }
        marginTop = 2.px
        backgroundColor = Color(theme.palette.primary.dark)
    }

    val mainComponentInputSearchClass by css {
        padding(8.px,8.px, 8.px, 50.px)
        transition += Transition("width", theme.transitions.duration.standard.ms, Timing.easeInOut, 0.ms)
        width = LinearDimension("30ch")
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            width = LinearDimension("40ch")
            focus {
                width = LinearDimension("50ch")
            }
        }
        flexGrow = 1.0
        color = Color.inherit
        paddingRight = (if(systemImpl.isRtlActive()) 60 else 0).px
    }


    val chatInputMessageClass by css {
        padding(2.spacingUnits)
        transition += Transition("width", theme.transitions.duration.standard.ms, Timing.easeInOut, 0.ms)
        flexGrow = 1.0
        color = Color.inherit
    }

    val mainComponentSearch by css {
        position = Position.relative
        borderRadius = theme.shape.borderRadius.px
        backgroundColor = styledModule.alpha(theme.palette.common.white, 0.15)
        hover {
            backgroundColor =  styledModule.alpha(theme.palette.common.white, 0.25)
        }
        marginLeft = 0.px
        width = 100.pct
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            marginLeft = 1.spacingUnits
            width = LinearDimension("auto")
        }

        media(theme.breakpoints.down(tabletAndHighEnd)) {
            width = 80.pct
        }
    }

    val typingMessage by css{
        media(theme.breakpoints.up(Breakpoint.lg)) {
            width = 75.pct
        }

        media(theme.breakpoints.down(Breakpoint.lg)) {
            width = 80.pct
        }
    }

    val messageSendButton by css{
        media(theme.breakpoints.up(Breakpoint.lg)) {
            display = Display.block
            if(!systemImpl.isRtlActive()) right = 2.spacingUnits
            if(systemImpl.isRtlActive()) left = 2.spacingUnits
        }

        media(theme.breakpoints.down(Breakpoint.lg)) {
            display = Display.none
        }
    }

    val messageContainer by css {
        position = Position.fixed
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            bottom = 15.px
            width = 81.5.pct
        }

        media(theme.breakpoints.down(tabletAndHighEnd)) {
            bottom = 70.px
            width = 84.pct
        }
    }


    val chatDetailNewMessage by css {
        borderRadius = theme.shape.borderRadius.px
        backgroundColor = styledModule.alpha(theme.palette.primary.dark, 0.8)
        hover {
            backgroundColor =  styledModule.alpha(theme.palette.primary.dark, 0.5)
        }
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            marginRight = 3.spacingUnits
        }
    }

    val mainComponentSearchIcon by css {
        padding = "0 16px"
        height = 100.pct
        position = Position.absolute
        pointerEvents = PointerEvents.none
        display = Display.flex
        alignItems = Align.center
        justifyContent = JustifyContent.center
    }

    val mainComponentToolbarMargins by css{
        if(systemImpl.isRtlActive()){
            paddingRight = 30.px
        }else{
            paddingLeft = 30.px
        }
    }

    val mainComponentFab by css{
        if(!systemImpl.isRtlActive()) right = 15.px
        if(systemImpl.isRtlActive()) left = 15.px
        position = Position.fixed
        bottom = 70.px
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            bottom = 15.px
        }
    }




    val languageComponentLanguageSelectorFormControl by css {
        margin(1.spacingUnits)
        minWidth = (drawerWidth * 0.9).px
        width = LinearDimension.auto
        display = Display.flex
        position = Position.fixed
        height = LinearDimension.auto
        bottom = 16.px
    }

    fun displayProperty(visible:Boolean, isFlexLayout:Boolean = false): Display {
       return if(visible)
           if(isFlexLayout) Display.flex else Display.block
       else Display.none
    }

    fun maxLines(builder: CssBuilder,maxLine: Int) {
        val baseLineHeight = 1.5
        builder.lineHeight = LineHeight("${baseLineHeight}em")
        builder.height = LinearDimension("${baseLineHeight * maxLine}em")
        builder.textOverflow = TextOverflow.ellipsis
        builder.overflow = Overflow.hidden
    }

    val tabsContainer by css {
        flexGrow = 1.0
        height = 100.pct
        +defaultFullWidth
    }


    val fieldsOnlyFormScreen by css {
        paddingLeft = 4.spacingUnits
        paddingRight = 4.spacingUnits
        paddingTop = 4.spacingUnits
        height = 100.vh
        overflow = Overflow.scroll
        paddingBottom = 16.spacingUnits
        width = (99.5).pct
        media(theme.breakpoints.up(tabletAndHighEnd)){
            width = (99.5).pct
        }
    }

    val scrollOnMobile by css {
        overflow = Overflow.scroll
        media(theme.breakpoints.up(tabletAndHighEnd)){
            overflow = Overflow.hidden
        }
    }

    val contentContainer by css {
        marginLeft = 1.spacingUnits
        marginRight = 1.spacingUnits
        height = 100.vh
        overflow = Overflow.scroll
        paddingLeft = 2.spacingUnits
        paddingRight = 2.spacingUnits
        paddingBottom = 16.spacingUnits
        width = (95.5).pct
        media(theme.breakpoints.up(tabletAndHighEnd)){
            width = (95.5).pct
            marginLeft = 3.spacingUnits
            marginRight = 3.spacingUnits
        }
    }


    val reportActionText by css {
        fontSize = 1.em
        textAlign = TextAlign.center
    }

    val startIcon by css{
        marginRight = 6.spacingUnits
        media(theme.breakpoints.up(tabletAndHighEnd)){
            marginRight = 2.spacingUnits
        }
    }

    val endIcon by css{
        marginLeft = 4.spacingUnits
        media(theme.breakpoints.up(tabletAndHighEnd)){
            marginLeft = 2.spacingUnits
        }
    }


    val centerContainer by css {
        display = Display.flex
        justifyContent = JustifyContent.center
        height = 70.vh
        alignItems = Align.center
    }

    val videoPlayerWrapper by css {
        display = Display.flex
        justifyContent = JustifyContent.center
        height = 80.vh
        width = 90.vw
        alignItems = Align.center
    }

    val alignCenterItems by css {
        alignItems = Align.center
        textAlign = TextAlign.center
        flexShrink = 0.0
    }

    val alignStartItems by css {
        alignItems = Align.start
        textAlign = TextAlign.start
        flexShrink = 0.0
    }

    val alignEndItems by css {
        alignItems = Align.end
        textAlign = TextAlign.end
        flexShrink = 0.0
    }


    val listComponentContainer by css {
        display = Display.inlineFlex
        flexDirection = FlexDirection.column
    }

    val listComponentContainerWithScroll by css {
        display = Display.inlineFlex
        flexDirection = FlexDirection.column
        height = 100.vh
        overflow = Overflow.scroll
        padding(top = 2.spacingUnits, horizontal = 2.spacingUnits, 0.spacingUnits)
        width = 100.pct
    }

    val entryListItemContainer by css {
        width = 100.pct
        display = Display.flex
        flexDirection = FlexDirection.row
    }

    val listCreateNewContainer by css {
        padding(10.px)
        marginBottom = 2.spacingUnits
    }

    val contentEntryListContentAvatarClass by css {
        height = 3.spacingUnits
        width = 3.spacingUnits
    }

    val contentEntryListContentTyeIconClass by css {
        fontSize = (0.65).em
        marginBottom = 4.px
    }

    val horizontalList by css {
        width = 100.pct
        backgroundColor = Color(theme.palette.background.paper)
    }

    val horizontalListEmpty by css {
        width = 100.pct
    }

    val listItemCreateNewDiv by css {
        display = Display.inlineFlex
        marginLeft = 16.px
        paddingTop = 10.px
        paddingBottom = 10.px
    }

    val listCreateNewIconClass by css {
        fontSize = (2.5).em
        marginTop = 5.px
    }

    val dragToReorderClass by css {
        fontSize = 2.em
        marginTop = 1.spacingUnits
    }

    val textGrayedOut by css {
        color = Color(theme.palette.action.disabled)
    }

    val chipSetFilter by css{
        display = Display.flex
        justifyContent = JustifyContent.start
        flexWrap = FlexWrap.wrap
    }

    val selectionContainer by css{
        paddingTop = 12.px
        paddingBottom = 12.px
        width = 100.pct
        backgroundColor = Color(theme.palette.background.default)
    }

    val entityImageClass by css {
        textAlign = TextAlign.center
        position = Position.relative
        width = 98.pct
        height = 300.px
    }

    val entityThumbnailClass by css {
        textAlign = TextAlign.center
        position = Position.relative
        width = 100.pct
        height = 80.px
        media(theme.breakpoints.up(tabletAndHighEnd)){
            width = 70.pct
            height = 120.px
        }
    }

    val entityImageIconClass by css {
        fontSize = 3.em
    }

    val emptyListIcon by css {
        display = Display.table
        margin = "0 auto"
        fontSize = 7.em
    }

    val tabWarningIconClass by css {
        display = Display.table
        margin = "0 auto"
        fontSize = 10.em
    }

    val entryItemImageContainer by css {
        width = 100.pct
        textAlign = TextAlign.center
    }

    val fallBackAvatarClass by css {
        fontSize = 2.em
        marginBottom = 4.px
    }

    val maxThumbnailClass by css {
        fontSize = 5.em
    }

    val mediumThumbnailClass by css {
        fontSize = 2.em
    }

    val defaultThumbnailClass by css {
        fontSize = (1.2).em
    }

    val secondaryActionBtn by css{
        width = 60.px
        padding(16.px)
    }

    val personListItemAvatar by css {
        width = 50.px
        height = 50.px
        margin(2.px, (if(systemImpl.isRtlActive()) 2.4 else 0 ).px, 0.px, (if(systemImpl.isRtlActive()) 0 else 2.4).px)
        color = Color(theme.palette.background.paper)
        backgroundColor = Color(theme.palette.action.disabled)
    }

    val contentEntryDetailOverviewComponentOpenBtn by css {
        margin(3.pct,(1.5).pct)
        width = 98.pct
        media(theme.breakpoints.up(tabletAndHighEnd)){
            margin(2.pct,(1.5).pct, 0.pct,(1.5).pct)
        }
    }

    val detailContentProgress by css{
        margin(left = 1.5.pct, right = 1.5.pct)
        width = 98.pct
    }

    val itemContentProgress by css{
        margin(top = 2.spacingUnits, left = 1.5.pct, right = 1.5.pct)
        width = 43.pct
        media(theme.breakpoints.up(tabletAndHighEnd)){
            margin(top = 1.spacingUnits, left = 1.5.pct, right = 1.5.pct)
        }
    }

    val detailIconClass by css {
        fontSize = 2.em
        marginTop = 3.px
    }

    val iframeComponentResponsiveIframe by css{
        overflow = Overflow.hidden
        width = 100.pct
        backgroundColor = Color.transparent
        border = "0px"
        minHeight = 75.pct
        margin(top = 10.pct)
        media(theme.breakpoints.up(tabletAndHighEnd)){
            minHeight = 100.pct
            margin(top = 5.pct)
        }
    }

    val personDetailComponentActions by css{
        display = Display.flex
        flexDirection = FlexDirection.column
        alignContent = Align.center
        alignItems = Align.center
        paddingBottom = 16.px
        padding(16.px,30.px , 16.px ,30.px)
        cursor = Cursor.pointer
        width = 100.pct
        hover {
            backgroundColor = Color(theme.palette.action.selected)
        }
    }

    val personDetailComponentActionIcon by css{
        marginBottom = 10.px
    }

    val videoComponentResponsiveMedia by css{
        overflow = Overflow.hidden
        width = 95.pct
        minHeight = 70.pct
        height = 70.pct
        margin(top = 10.pct)
        backgroundColor = Color.transparent
        media(theme.breakpoints.up(tabletAndHighEnd)){
            height = 100.pct
            minHeight = 100.pct
            margin(top = 5.pct)
        }
    }

    val clazzItemClass by css {
        height = 200.px
        width = 100.pct
    }

    val clazzDetailExtraInfo by css {
        width = 100.pct
        margin(0.px, 2.pct,0.px, 2.pct)
        paddingBottom = 10.spacingUnits
    }

    val clazzListRoleChip by css{
        position = Position.absolute
        right = 10.px
        top = 10.px
    }

    val gridListSecondaryItemIcons by css {
        marginTop = 4.px
        fontSize = 1.em
    }

    val gridListSecondaryItemDesc by css {
        marginTop = 4.px
        fontSize = 0.68.rem
    }

    val hideOnMobile by css {
        display = Display.none
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            display = Display.block
        }
    }

    val showOnMobile by css {
        display = Display.block
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            display = Display.none
        }
    }


    val partnerItem by css {
        height = 40.px
        padding(left = 1.spacingUnits)
        verticalAlign = VerticalAlign.middle
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            padding(left = 2.spacingUnits)
            height = 60.px
        }
    }

    val partnersList by css {
        position = Position.fixed
        right = 2.spacingUnits
        bottom = 2.spacingUnits
        width = 100.vh
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            bottom = 4.spacingUnits
            right = 3.spacingUnits
        }
    }

    val studentProgressBar by css{
        width = 50.pct
        paddingRight = 2.spacingUnits
        marginTop = 10.px
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            width = 70.pct
        }
    }

    val dropZoneArea by css{
        backgroundColor = Color("transparent !important")
        width = 90.pct
        minHeight = 70.vh
        border = "10px dashed ${theme.palette.action.selected} !important"
        alignItems = Align.center
        display = Display.flex
        cursor = Cursor.pointer
        justifyContent = JustifyContent.center
        flexDirection = FlexDirection.column
    }

    val dropZoneAreaActive by css{
        border = "10px dashed ${theme.palette.info.main} !important"
    }

    val dropZoneAreaSuccess by css{
        border = "10px dashed ${theme.palette.success.main} !important"
    }

    val dropZoneAreaError by css{
        border = "10px dashed ${theme.palette.error.main} !important"
    }

    val dropZoneIcon by css {
        marginTop = 15.pct
        fontSize = 16.em
        color = Color("${theme.palette.action.selected} !important")
        media(theme.breakpoints.up(tabletAndHighEnd)) {
            marginTop = 5.pct
        }
    }

    val dropZoneTxt by css {
        fontSize = 2.em
    }

    val chatMessageContent by css {
        padding(1.spacingUnits, 2.spacingUnits)
        borderRadius = 4.px
        marginBottom = 4.px
        display = Display.inlineBlock
        wordBreak = WordBreak.breakWord
        fontSize = (1.2).em
    }

    val chatLeft by css {
        borderTopRightRadius = 20.px
        borderBottomRightRadius = 20.px

    }

    val chatRight by css {
        borderTopLeftRadius = 20.px
        borderBottomLeftRadius = 20.px
    }

    override val di: DI
        get() = ReduxAppStateManager.getCurrentState().di.instance

}
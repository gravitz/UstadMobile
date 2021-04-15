package com.ustadmobile.util

import com.ccfraser.muirwik.components.spacingUnits
import com.ccfraser.muirwik.components.styles.Breakpoint
import com.ccfraser.muirwik.components.styles.Theme
import com.ccfraser.muirwik.components.styles.up
import kotlinx.browser.window
import kotlinx.css.*
import kotlinx.css.properties.Timing
import kotlinx.css.properties.Transition
import kotlinx.css.properties.ms
import styled.StyleSheet

/**
 * Responsible for styling HTML elements, to customize particular
 * element just check the defined style constants.
 * They are named as per component
 */
object CssStyleManager: StyleSheet("ComponentStyles", isStatic = true) {

    private val theme = StateManager.getCurrentState().theme!!

    val appContainer by css {
        flexGrow = 1.0
        width = 100.pct
        zIndex = 1
        overflow = Overflow.hidden
        position = Position.relative
        display = Display.flex
        flexDirection = FlexDirection.column
    }

    val fab by css{
        position = Position.fixed
        right = 15.px
        bottom = 15.px
        zIndex = 99999
    }

    val defaultMarginTop  by css{
        marginTop = 16.px
    }

    val progressIndicator by css {
        width = 100.pc
    }

    val preloadComponentCenteredDiv by css{
        height = 200.px
        width = 200.px
        left = LinearDimension("50%")
        top = LinearDimension("50%")
        marginLeft = (-100).px
        marginTop = (-50).px
        position =  Position.fixed
    }

    val preloadComponentProgressBar by css {
        width = 200.px
        marginTop = 140.px
        position = Position.absolute
    }

    val preloadComponentCenteredImage by css{
        width = 180.px
        marginLeft = 10.px
        position = Position.absolute
    }


    val mainComponentContainer by css {
        overflow = Overflow.hidden
        position = Position.relative
        display = Display.flex
        width = 100.pct
    }

    val mainComponentContentArea by css {
        height = LinearDimension.fillAvailable
        flexGrow = 1.0
        minWidth = 0.px
    }

    val mainComponentSearchIcon by css {
        width = 9.spacingUnits
        height = 100.pct
        position = Position.absolute
        pointerEvents = PointerEvents.none
        display = Display.flex
        alignItems = Align.center
        justifyContent = JustifyContent.center
    }

    val mainComponentSearch by css {
        position = Position.relative
        borderRadius = theme.shape.borderRadius.px
        backgroundColor = Color(fade(theme.palette.common.white, 0.15))
        hover {
            backgroundColor = Color(fade(theme.palette.common.white, 0.25))
        }
        marginLeft = 0.px
        marginRight = 30.px
        width = 100.pct
        media(theme.breakpoints.up(Breakpoint.sm)) {
            marginLeft = 1.spacingUnits
            width = LinearDimension.auto
        }
    }

    val mainComponentInputSearch by css {
        paddingTop = 1.spacingUnits
        paddingRight = 1.spacingUnits
        paddingBottom = 1.spacingUnits
        paddingLeft = 10.spacingUnits
        transition += Transition("width", theme.transitions.duration.standard.ms, Timing.easeInOut, 0.ms)
        width = 100.pct
        media(theme.breakpoints.up(Breakpoint.sm)) {
            width = 120.px
            focus {
                width = 300.px
            }
        }
    }

    val ustadListViewComponentContainer by css {
        display = Display.inlineFlex
        flexDirection = FlexDirection.column
        padding(1.spacingUnits)
    }

    val entryListItemContainer by css {
        width = LinearDimension.auto
        display = Display.flex
        flexDirection = FlexDirection.row
    }

    val entryListItemImage by css{
        marginRight = 20.px
        width = (8.5).pc
        height = 150.px
    }

    val contentEntryListEditOptions by css{
        display = Display.flex
        flexDirection = FlexDirection.row
        margin = "10px 0px 10px 80%"
    }

    val entryListItemInfo by css {
        width = LinearDimension.auto
        display = Display.flex
        flexDirection = FlexDirection.column
    }


    val horizontalList by css {
        width = LinearDimension.auto
        backgroundColor = Color(theme.palette.background.paper)
    }

    val listCreateNewContainer by css {
        padding = "10px"
    }

    val contentEntryListAvatar by css {
        height = 3.spacingUnits
        width = 3.spacingUnits
    }

    val contentEntryListIcon by css {
        fontSize = LinearDimension("0.65em")
        marginBottom = 4.px
    }

    val listItemCreateNewDiv by css {
        display = Display.inlineFlex
        marginLeft = 16.px
    }

    val listCreateNewIcon by css {
        fontSize = LinearDimension("2.5em")
        marginTop = 5.px
    }


    val entryDetailComponentContainer by css {
        width = LinearDimension.auto
        display = Display.flex
        flexDirection = FlexDirection.row
    }

    val entryDetailComponentEntryImage by css {
        width = LinearDimension("100%")
        height = LinearDimension.initial
    }

    val entryDetailComponentEntryImageAndButtonContainer by css {
        width = LinearDimension("40%")
        display = Display.flex
        marginRight = LinearDimension("3%")
        flexDirection = FlexDirection.column
    }

    val entryDetailComponentEntryExtraInfo by css {
        width = LinearDimension("57%")
        flexDirection = FlexDirection.column
    }

    val chipSet by css{
        display = Display.flex
        justifyContent = JustifyContent.start
        flexWrap = FlexWrap.wrap
    }

}
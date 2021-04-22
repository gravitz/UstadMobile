package com.ustadmobile.view

import com.ccfraser.muirwik.components.*
import com.ccfraser.muirwik.components.button.MButtonSize
import com.ccfraser.muirwik.components.button.MButtonVariant
import com.ccfraser.muirwik.components.button.mButton
import com.ccfraser.muirwik.components.button.mFab
import com.ccfraser.muirwik.components.input.mInput
import com.ccfraser.muirwik.components.list.mList
import com.ccfraser.muirwik.components.list.mListItemWithIcon
import com.ccfraser.muirwik.components.styles.Breakpoint
import com.ccfraser.muirwik.components.styles.down
import com.ccfraser.muirwik.components.styles.up
import com.ustadmobile.core.account.UstadAccountManager
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.ext.observeWithLifecycleOwner
import com.ustadmobile.lib.db.entities.UmAccount
import com.ustadmobile.model.UmReactDestination
import com.ustadmobile.model.statemanager.FabState
import com.ustadmobile.model.statemanager.GlobalStateSlice
import com.ustadmobile.model.statemanager.SnackBarState
import com.ustadmobile.util.CssStyleManager
import com.ustadmobile.util.CssStyleManager.appContainer
import com.ustadmobile.util.CssStyleManager.fab
import com.ustadmobile.util.CssStyleManager.mainComponentAvatarInner
import com.ustadmobile.util.CssStyleManager.mainComponentAvatarOuter
import com.ustadmobile.util.CssStyleManager.mainComponentContainer
import com.ustadmobile.util.CssStyleManager.mainComponentContentArea
import com.ustadmobile.util.CssStyleManager.mainComponentErrorPaper
import com.ustadmobile.util.CssStyleManager.mainComponentSearch
import com.ustadmobile.util.CssStyleManager.mainComponentSearchIcon
import com.ustadmobile.util.CssStyleManager.progressIndicator
import com.ustadmobile.util.RouteManager.destinationList
import com.ustadmobile.util.RouteManager.renderRoutes
import com.ustadmobile.util.StateManager
import com.ustadmobile.util.UmReactUtil.drawerWidth
import com.ustadmobile.util.UmReactUtil.fullWidth
import com.ustadmobile.util.UmReactUtil.isDarkModeEnabled
import com.ustadmobile.util.UmReactUtil.zeroPx
import kotlinext.js.jsObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.onClick
import org.kodein.di.instance
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span
import styled.css
import styled.styledDiv

interface MainProps: RProps {
    var currentDestination: UmReactDestination
}


class MainComponent(props: MainProps): UmBaseComponent<MainProps, RState>(props){

    private var activeAccount: UmAccount? = null

    private var currentTile: String = ""

    private var responsiveDrawerOpen: Boolean = false

    private var isRTLSupport: Boolean = false

    private var showSearch: Boolean = false

    private var showFab: Boolean = false

    private var fabIcon: String = ""

    private var fabLabel: String = ""

    private var onFabClicked:(Event)-> Unit = {}

    private var onSnackActionClicked:(Event)-> Unit = {}

    private var snackBarOpen = false

    private var snackBarActionLabel:String? = null

    private var snackBarMessage:String? = null

    private val altBuilder = RBuilder()

    private lateinit var currentDestination: UmReactDestination

    private val accountManager: UstadAccountManager by instance()

    private var stateChangeListener : (GlobalStateSlice) -> Unit = {
        when (it.state.type) {
            is FabState -> {
                showFab = it.state.showFab
                fabLabel = it.state.fabLabel
                fabIcon = it.state.fabIcon
                onFabClicked = it.state.onFabClicked
            }
            is SnackBarState -> {
                snackBarOpen = true
                snackBarMessage = it.state.snackBarMessage
                snackBarActionLabel = it.state.snackBarActionLabel
                onSnackActionClicked = it.state.onSnackActionClicked
            }
        }
    }

    private val mActiveUserObserver:(UmAccount?) -> Unit = {
        GlobalScope.launch(Dispatchers.Main) {
            setState { activeAccount = it }
        }
    }

    override fun RState.init(props: MainProps) {
        currentDestination = props.currentDestination
    }

    override fun componentDidMount() {
        super.componentDidMount()
        setState {
            currentTile = systemImpl.getString(currentDestination.labelId, this)
            responsiveDrawerOpen = true
            showSearch = currentDestination.showSearch
        }
        StateManager.subscribe(stateChangeListener)
        accountManager.activeAccountLive.observeWithLifecycleOwner(this,mActiveUserObserver)
    }


    override fun RBuilder.render() {
        errorBoundary(fallbackComponent(systemImpl.getString(MessageID.error, this))){
            mCssBaseline()
            themeContext.Consumer { theme ->
                styledDiv {
                    css {
                        +appContainer
                        backgroundColor = Color(theme.palette.background.paper)
                    }

                    //Loading indicator
                    mLinearProgress {
                        css{
                            +progressIndicator
                            display = Display.none
                        }
                        attrs.asDynamic().id = "um-progress"
                        attrs {
                            color = if(isDarkModeEnabled()) MLinearProgressColor.secondary
                            else MLinearProgressColor.primary
                        }
                    }
                    styledDiv {
                        css { +mainComponentContainer }
                        mAppBar(position = MAppBarPosition.absolute) {
                            css {
                                val removeLeftMargin = isRTLSupport or !currentDestination.showNavigation
                                position = Position.absolute
                                marginLeft = if(removeLeftMargin) zeroPx else drawerWidth
                                media(theme.breakpoints.up(Breakpoint.md)) {
                                    width = fullWidth - if(removeLeftMargin) zeroPx else drawerWidth
                                }
                            }

                            mToolbar {
                                mHidden(xsDown = true){
                                    mToolbarTitle(currentTile)
                                }

                                styledDiv {
                                    css{
                                        +mainComponentSearch
                                        display = if(showSearch) Display.block else Display.none
                                    }
                                    styledDiv {
                                        css(mainComponentSearchIcon)
                                        mIcon("search")
                                    }
                                    val inputProps = object: RProps {
                                        val className = "${CssStyleManager.name}-mainComponentInputSearch"
                                        val id = "um-search"
                                    }
                                    mInput(placeholder = "${systemImpl.getString(MessageID.search,this)}...",
                                        disableUnderline = true) {
                                        attrs.inputProps = inputProps
                                        css {
                                            color = Color.inherit
                                        }
                                    }
                                }

                                mAvatar {
                                    css {
                                        display = if(currentDestination.showNavigation) Display.block else Display.none
                                        +mainComponentAvatarOuter
                                    }

                                    attrs {
                                        onClick = {}
                                    }

                                    mLink {
                                        mAvatar{
                                            css {
                                                +mainComponentAvatarInner
                                            }
                                            mTypography("${activeAccount?.firstName?.first()}",
                                                align = MTypographyAlign.center,
                                                variant = MTypographyVariant.h6){
                                                css{ marginTop = (1.5).px }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if(currentDestination.showNavigation){
                            renderSideNavigation()
                        }

                        // Main content area, this div holds the contents
                        styledDiv {
                            css {
                                +mainComponentContentArea
                                backgroundColor = Color(theme.palette.background.default)
                            }
                            appBarSpacer()
                            styledDiv {
                                css {
                                    media(theme.breakpoints.down(Breakpoint.sm)) {
                                        height = 100.vh - 57.px
                                    }
                                    media(theme.breakpoints.up(Breakpoint.sm)) {
                                        height = 100.vh - 65.px
                                    }

                                    overflowY = Overflow.auto
                                    padding(2.spacingUnits)
                                    backgroundColor = Color(theme.palette.background.default)
                                }
                                renderRoutes()
                            }
                        }

                        if(showFab){
                            mFab(fabIcon, fabLabel.toUpperCase(), color = MColor.secondary) {
                                css(fab)
                                attrs {
                                    onClick = onFabClicked
                                }
                            }
                        }
                    }

                    mSnackbar(altBuilder.span { +"$snackBarMessage"}, open = snackBarOpen,
                        horizAnchor = MSnackbarHorizAnchor.center, autoHideDuration = 5000,
                        onClose = { _, _ -> handleSnackBarClosing()}) {
                        attrs.action = altBuilder.div {
                            mButton("$snackBarActionLabel", color = MColor.secondary,
                                variant = MButtonVariant.text, size = MButtonSize.small,
                                onClick = {
                                    onSnackActionClicked(it)
                                    handleSnackBarClosing()
                                })
                        }
                    }

                }
            }
        }
    }

    private fun handleSnackBarClosing(){
        setState {
            onFabClicked = {  }
            snackBarOpen = false
            snackBarMessage = null
            snackBarActionLabel = null
        }
    }

    private fun RBuilder.renderSideNavigation(){
        val p: MPaperProps = jsObject { }
        p.asDynamic().style = kotlinext.js.js {
            position = "relative"; width = drawerWidth.value; display = "block"; height =
            "100%"; minHeight = "100vh"
        }

        mHidden(smDown = true, implementation = MHiddenImplementation.css) {
            mDrawer(true, MDrawerAnchor.left, MDrawerVariant.permanent, paperProps = p) {
                appBarSpacer()
                themeContext.Consumer { theme ->
                    mList {
                        css {
                            backgroundColor = Color(theme.palette.background.paper)
                            width = drawerWidth
                        }
                        destinationList.filter { it.icon != null }.forEach { destination ->
                            destination.icon?.let {
                                mListItemWithIcon(it, systemImpl.getString(destination.labelId, this),
                                    divider = destination.divider , onClick = {
                                        setState {
                                            showSearch = destination.showSearch
                                            currentTile = systemImpl.getString(destination.labelId, this)
                                            currentDestination = destination
                                        }
                                        systemImpl.go(destination.view, destination.args,this)
                                    })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fallbackComponent(text: String): ReactElement {
        // Note we purposely use a new RBuilder so we don't render into our normal display
        return RBuilder().mPaper {
            css(mainComponentErrorPaper)
            mTypography(text)
        }
    }

    private fun RBuilder.appBarSpacer() {
        themeContext.Consumer { theme ->
            styledDiv {
                css{
                    toolbarJsCssToPartialCss(theme.mixins.toolbar)
                }
            }
            mDivider {  }
        }
    }

    override fun componentWillUnmount() {
        stateChangeListener = {}
    }
}

fun RBuilder.mainScreen(destination: UmReactDestination, args: Map<String,String>) = child(MainComponent::class){
    attrs.currentDestination = destination
    UstadMobileSystemImpl.instance.go(destination.view, args, this)
}
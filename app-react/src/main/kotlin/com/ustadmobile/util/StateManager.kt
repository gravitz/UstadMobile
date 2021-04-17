package com.ustadmobile.util

import com.ccfraser.muirwik.components.styles.Theme
import com.ustadmobile.core.account.EndpointScope
import com.ustadmobile.core.account.UstadAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemCommon
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.ContentEntryOpener
import com.ustadmobile.model.statemanager.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.kodein.di.*
import redux.*
import kotlin.reflect.KProperty1

/**
 * State management which uses Redux to manage state of an app. With Redux app states can be
 * accessed on all app components.
 *
 * To listen for state change, call subscribe() and pass your functional listener, this listener
 * will be invoked when state changes.
 * To change state, call dispatch() and pass your state to be changed
 *
 * In here we have a GlobalState which holds all state that can be changed by any component within
 * the app. Like FAB visibility, Title, FAB label, FAB icon and etc.
 *
 * We have also, different state action as per element to be changed, check under
 * com.ustadmobile.model.statemanager for all state actions.
 */
object StateManager{

    private data class UmDi(var di: DI): RAction

    //App theme state action
    data class UmTheme(var theme: Theme): RAction

    private lateinit var stateStore: Store<GlobalStateSlice, RAction, WrapperAction>

    // Credit https://github.com/JetBrains/kotlin-wrappers/blob/master/kotlin-redux/README.md
    private fun <S, A, R> combineReducersInferred(reducers: Map<KProperty1<S, R>, Reducer<*, A>>):
            Reducer<S, A> { return combineReducers(reducers.mapKeys { it.key.name}) }

    //Construct dependency injection object
    private val diModule = DI.Module("UstadApp-React"){

        bind<UstadMobileSystemImpl>() with singleton { UstadMobileSystemImpl.instance }

        bind<UstadAccountManager>() with singleton {
            UstadAccountManager(instance(), this, di)
        }

        constant(UstadMobileSystemCommon.TAG_DOWNLOAD_ENABLED) with true

        bind<UmTheme>() with singleton{
            UmTheme(getCurrentState().theme!!)
        }

        bind<CoroutineDispatcher>(tag = UstadMobileSystemCommon.TAG_MAIN_COROUTINE_CONTEXT) with singleton { Dispatchers.Main }

        bind<ContentEntryOpener>() with scoped(EndpointScope.Default).singleton {
            ContentEntryOpener(di, context)
        }
    }

    /**
     * Function which determines state changes from actions and act accordingly
     */
    private fun reducer(state: GlobalState = GlobalState(), action: RAction): GlobalState {
        state.type = action
        return when (action) {
            is AppBarState -> state.copy(title = if(action.title.isNullOrBlank()) state.title
                else action.title, loading = action.loading)
            is FabState -> state.copy(showFab = action.visible, onFabClicked = action.onClick,
                fabLabel = action.label, fabIcon = action.icon)
            is UmDi -> state.copy(di = action.di)
            is UmTheme -> state.copy(theme = action.theme)
            is SnackBarState -> state.copy(snackBarActionLabel = action.actionLabel,
                snackBarMessage = action.message, onFabClicked = action.onClick)
            else -> state
        }
    }

    /**
     * Update a particular state
     * @param action state action to be changed
     */
    fun dispatch(action: RAction){
        stateStore.dispatch(action)
    }

    /**
     * Listen for state changes event
     * @param mListener functional listen which will be invoked when state changes
     */
    fun subscribe(mListener: (GlobalStateSlice)-> Unit){
        stateStore.subscribe { mListener(stateStore.getState()) }
    }

    /**
     * Get current state of an app
     */
    fun getCurrentState() = stateStore.getState().state

    /**
     * Create redux app state store which store and manage all states.
     */
    fun createStore() : Store<GlobalStateSlice,RAction, WrapperAction>{
        stateStore = createStore(combineReducersInferred(
            mapOf(GlobalStateSlice::state to StateManager::reducer)),
            GlobalStateSlice(),rEnhancer())
        dispatch(UmDi(di = DI.lazy { import(diModule)}))
        return stateStore
    }
}

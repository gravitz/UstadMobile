package com.ustadmobile.util

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.events.Event

/**
 * Manages Floating action button functionality
 */
class  FabManager(private val viewId: String = "um-fab", visible: Boolean = false) {

    private var fabView: Element? = null

    private var viewInitTimeoutId = -1

    private var clickEventHandler:(Event) -> Unit = {
        onClickListener?.invoke()
    }

    init {
        fabView = document.getElementById(viewId)
        fabView?.addEventListener("click", clickEventHandler)
    }

    var onClickListener: (() -> Unit)? = null
        get() = field
        set(value) {
            field = value
        }

    var visible: Boolean = visible
        set(value) {
            field = value
            if(fabView!= null){
                val style = fabView?.asDynamic().style
                if(style != null)
                    style.display = if(value) "flex" else "none"
            }
        }

    var icon: String? = null
        get() = field
        set(value){
            field = value
            updateIconAndFabText()
        }

    var text: String? = null
        get() = field
        set(value) {
            field = value
            updateIconAndFabText()
        }

    private fun updateIconAndFabText(){
        if(!text.isNullOrBlank() && !icon.isNullOrBlank()){
            fabView?.childNodes?.item(0)?.textContent = icon
            fabView?.childNodes?.item(1)?.textContent = text

        }else{
            visible = false
        }
    }

    fun onDestroy(){
        window.removeEventListener("click",clickEventHandler)
        window.clearTimeout(viewInitTimeoutId)
        onClickListener = null
        fabView = null
    }

}
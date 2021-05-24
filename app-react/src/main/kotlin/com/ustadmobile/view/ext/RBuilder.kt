package com.ustadmobile.view.ext

import com.ccfraser.muirwik.components.*
import com.ustadmobile.util.CssStyleManager
import com.ustadmobile.util.CssStyleManager.imageContainer
import com.ustadmobile.util.CssStyleManager.personListItemAvatar
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.RBuilder
import react.RProps
import styled.StyledHandler
import styled.css
import styled.styledDiv
import styled.styledSpan

fun RBuilder.umProfileAvatar(attachmentId: Long, fallback: String){
    val src = null
    mAvatar(src,variant = MAvatarVariant.circular){
        css (personListItemAvatar)
        if(src == null) mIcon(fallback, className= "${CssStyleManager.name}-fallBackAvatarClass")
    }
}

fun RBuilder.umGridContainer(spacing: MGridSpacing = MGridSpacing.spacing0,
                             alignContent: MGridAlignContent = MGridAlignContent.stretch,
                             alignItems: MGridAlignItems = MGridAlignItems.stretch,
                             justify: MGridJustify = MGridJustify.flexStart,
                             wrap: MGridWrap = MGridWrap.wrap,className: String? = null, handler: StyledHandler<MGridProps>? = null) {
    mGridContainer(spacing,alignContent,alignItems,justify, wrap) {
        setStyledPropsAndRunHandler(className, handler)
    }
}

fun RBuilder.umItem(xs: MGridSize, sm: MGridSize? = null, lg: MGridSize? = null, className: String? = null, handler: StyledHandler<MGridProps>? = null) {
    mGridItem(xs = xs) {
        sm?.let { attrs.sm = it }
        lg?.let { attrs.md = it }
        setStyledPropsAndRunHandler(className, handler)
    }
}

fun RBuilder.umEntityAvatar (src: String? = null, fallbackSrc: String? = "assets/account.jpg",
                             iconName: String = "add_a_photo", imgProps: RProps? = null,
                             variant: MAvatarVariant = MAvatarVariant.rounded,
                             showIcon: Boolean = true,
                             className: String? = "${CssStyleManager.name}-entityImageClass",
                             clickEvent:(Event) -> Unit = {  }){

    styledDiv {
        css(imageContainer)
        mAvatar(src = if(src.isNullOrEmpty()) fallbackSrc else src,
            variant = variant, imgProps = imgProps, className = className) {
            styledSpan{
                css{
                    position = Position.absolute
                    cursor = Cursor.pointer
                    padding = "20px"
                }
                attrs{
                    onClickFunction = clickEvent
                }
                if(showIcon){
                    mIcon(iconName, className = "${CssStyleManager.name}-entityImageIconClass")
                }
            }
        }
    }
}

fun RBuilder.handleCall(phoneNumber: String?){

}

fun RBuilder.handleMail(email: String?){

}

fun RBuilder.handleSMS(phoneNumber: String?){

}

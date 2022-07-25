package com.ustadmobile.mui.components

import Breakpoint
import com.ustadmobile.mui.ext.createReUsableComponent
import mui.material.*
import react.RBuilder
import react.ReactNode
import styled.StyledHandler
import styled.StyledProps

external interface UMDialogTitleProps: DialogTitleProps, StyledProps

fun RBuilder.umDialog(
    open: Boolean,
    onClose: () -> Unit,
    fullWidth: Boolean = true,
    maxWidth: Breakpoint = Breakpoint.sm,
    className: String? = null,
    handler: StyledHandler<DialogProps>? = null
) = createReUsableComponent(Dialog, className, handler){
    attrs.open = open
    attrs.fullWidth = fullWidth
    attrs.maxWidth = maxWidth
    attrs.onClose = { _, _ ->
        onClose.invoke()
    }
}


fun RBuilder.umDialogTitle(
    title: String,
    className: String? = null,
    handler: StyledHandler<UMDialogTitleProps>? = null
) = createReUsableComponent(DialogTitle, className, handler){

    attrs.children = ReactNode(title)
}


fun RBuilder.umDialogActions(
    className: String? = null,
    handler: StyledHandler<DialogActionsProps>? = null
) = createReUsableComponent(DialogActions, className, handler)


fun RBuilder.umDialogContent(
    className: String? = null,
    handler: StyledHandler<DialogContentProps>? = null
) = createReUsableComponent(DialogContent, className, handler)
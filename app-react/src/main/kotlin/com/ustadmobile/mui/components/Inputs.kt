package com.ustadmobile.mui.components

import com.ustadmobile.mui.ext.createStyledComponent
import kotlinx.css.color
import mui.material.Input
import mui.material.InputProps
import org.w3c.dom.events.Event
import react.RBuilder
import react.dom.html.InputType
import styled.StyledHandler
import styled.css

fun RBuilder.umInput(
    value: Any? = null,
    required: Boolean? = null,
    disabled: Boolean? = null,
    readOnly: Boolean? = null,
    error: Boolean? = null,
    fullWidth: Boolean = false,
    defaultValue: String? = null,
    placeholder: String? = null,
    disableUnderline: Boolean? = null,
    autoFocus: Boolean? = null,
    type: InputType = InputType.text,
    id: String? = null,
    name: String? = null,
    multiline: Boolean = false,
    rows: Int? = null,
    textColor: kotlinx.css.Color,
    rowsMax: Int? = null,
    onChange: ((Event) -> Unit)? = null,
    className: String? = null,
    handler: StyledHandler<InputProps>? = null
) = createStyledComponent(Input, className, handler) {
    autoFocus?.let{ attrs.autoFocus = it }
    defaultValue?.let { attrs.defaultValue = it }
    disabled?.let { attrs.disabled = it }
    disableUnderline?.let { attrs.disableUnderline = it }
    error?.let { attrs.error = it }
    attrs.fullWidth = fullWidth
    id?.let { attrs.id = it }
    attrs.multiline = multiline
    name?.let { attrs.name = it }
    attrs.onChange = {
        it.persist()
        onChange?.invoke(it.nativeEvent)
    }
    placeholder?.let { attrs.placeholder = it }
    readOnly?.let { attrs.readOnly = it }
    required?.let { attrs.required = it }
    rows?.let { attrs.rows = it }
    rowsMax?.let { attrs.maxRows = it }
    attrs.type = type.toString()
    attrs.color = "#fff"
    value?.let { attrs.value = it }
    css {
        color = textColor
    }
}
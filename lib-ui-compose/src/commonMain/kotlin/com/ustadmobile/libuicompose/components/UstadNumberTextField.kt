package com.ustadmobile.libuicompose.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * See
 *  https://dev.to/pchmielowski/jetpack-compose-textfield-which-accepts-and-emits-value-other-than-string-1g3o
 *
 *  @param value Float - decimal point values are not currently supported, however this will be
 *  an option in future. Hence type is set as float.
 */
@Composable
fun UstadNumberTextField(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number
    )
) {

    var rawValue by remember(value) {
        mutableStateOf(if(value != 0.toFloat()) value.toInt().toString() else "")
    }

    OutlinedTextField(
        value = rawValue,
        modifier = modifier,
        onValueChange = { text ->
            val filteredText = text.filter { it.isDigit() }
            rawValue = filteredText
            val floatVal = filteredText.toFloatOrNull() ?: 0.toFloat()
            onValueChange(floatVal)
        },
        isError = isError,
        label = label,
        placeholder = placeholder,
        enabled = enabled,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
    )
}
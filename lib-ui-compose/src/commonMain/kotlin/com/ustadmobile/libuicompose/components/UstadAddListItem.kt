package com.ustadmobile.libuicompose.components

import androidx.compose.foundation.clickable
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UstadAddListItem(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Filled.Add,
    onClickAdd: (() -> Unit) = {  }
){
    ListItem(
        modifier = modifier
            .clickable(
                enabled = enabled,
                onClick = { onClickAdd() }
            ),
        icon = {
            Icon(
                icon,
                contentDescription = null
            )
        },
        text = {
            Text(text)}
    )
}
package com.ustadmobile.port.android.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.toughra.ustadmobile.R
import com.ustadmobile.core.viewmodel.CourseDetailProgressUiState
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.port.android.util.ext.defaultItemPadding
import com.ustadmobile.port.android.view.composable.UstadPersonAvatar
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CourseDetailProgressScreen(
    uiState: CourseDetailProgressUiState = CourseDetailProgressUiState(),
    onClickStudent: (Person) -> Unit = {},
) {

    val stateRowX = rememberLazyListState() // State for the first Row, X
    val stateRowY = rememberLazyListState() // State for the second Row, Y
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollableState { delta ->
        scope.launch {
            stateRowX.scrollBy(-delta)
            stateRowY.scrollBy(-delta)
        }
        delta
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {

        stickyHeader {
            LazyRow(
                modifier = Modifier
                    .defaultItemPadding()
                    .padding(start = 40.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End,
                state = stateRowX
            ) {
                items(
                    items = uiState.results,
                ){ result ->
                    CheckBoxTitle(text = result)
                }
            }

        }

        items(
            items = uiState.students,
            key = { student -> student.personUid }
        ) { student ->

            ListItem(
                modifier = Modifier.clickable {
                    onClickStudent(student)
                },
                icon = {
                    UstadPersonAvatar(personUid = 0)
                },
                text = { Text(student.fullName()) },
                trailing = {

//                    LazyRow(
//                        state = stateRowY,
//                    ) {
//                        items(count = 120) { colIndex ->
//                            Text(text = "          |$colIndex")
//                        }
//                    }
                    LazyRow{
                        items(
                            items = uiState.results,
                        ){ _ ->
                            Icon(
                                Icons.Outlined.CheckBox,
                                contentDescription = "",
                                modifier = Modifier.defaultMinSize()
                            )
                        }
                    }
                }
            )

        }

//        item {
//            LazyRow(
//                state = stateRowY,
//                userScrollEnabled = false
//            ) {
//                items(LoremIpsum(10).values.toList()) {
//                    Text(text = it)
//                }
//            }
//        }


    }
}

//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .defaultScreenPadding(),
//    ) {
//
//        stickyHeader {
//
////            LazyRow(
////                modifier = Modifier
////                    .defaultItemPadding()
////                    .padding(start = 40.dp),
////                state = stateRowX,
////                userScrollEnabled = false
////            ) {
////                items(
////                    items = uiState.results,
////                ){ result ->
////                    CheckBoxTitle(text = result)
////                }
////            }
//
//        }
//
//        items(
//            items = uiState.students,
//            key = { student -> student.personUid }
//        ){ student ->
//            ListItem(
//                modifier = Modifier.clickable {
//                    onClickStudent(student)
//                },
//                icon = {
//                    UstadPersonAvatar(personUid = 0)
//                },
//                text = { Text(student.fullName()) },
//                trailing = {
//
////                    LazyRow(
////                        state = stateRowY,
////                        userScrollEnabled = false
////                    ) {
////                        items(
////                            items = uiState.results,
////                        ){ _ ->
////                            Icon(
////                                Icons.Outlined.CheckBox,
////                                contentDescription = "",
////                                modifier = Modifier.defaultMinSize()
////                            )
////                        }
////                    }
//                }
//            )
//        }
//    }
//}

@Composable
private fun CheckBoxTitle(
    text: String
){
    Text(
        modifier = Modifier.vertical()
            .rotate(-90f)
            .height(22.dp),
        text = text)
}

private fun Modifier.vertical() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.height, placeable.width) {
        placeable.place(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2)
        )
    }
}

@Composable
@Preview
fun CourseDetailProgressScreenPreview() {

    val uiState = CourseDetailProgressUiState(
        students = listOf(
            Person().apply {
                personUid = 1
                firstNames = "Bart"
                lastName = "Simpson"
            },
            Person().apply {
                personUid = 2
                firstNames = "Shelly"
                lastName = "Mackleberry"
            },
            Person().apply {
                personUid = 3
                firstNames = "Tracy"
                lastName = "Mackleberry"
            },
            Person().apply {
                personUid = 4
                firstNames = "Nelzon"
                lastName = "Muntz"
            },
            Person().apply {
                personUid = 5
                firstNames = "Nelzon"
                lastName = "Muntz"
            },
            Person().apply {
                personUid = 6
                firstNames = "Nelzon"
                lastName = "Muntz"
            },
            Person().apply {
                personUid = 7
                firstNames = "Nelzon"
                lastName = "Muntz"
            },
            Person().apply {
                personUid = 8
                firstNames = "Nelzon"
                lastName = "Muntz"
            },
            Person().apply {
                personUid = 9
                firstNames = "Nelzon"
                lastName = "Muntz"
            },
            Person().apply {
                personUid = 10
                firstNames = "Nelzon"
                lastName = "Muntz"
            },
            Person().apply {
                personUid = 11
                firstNames = "Nelzon"
                lastName = "Muntz"
            },
            Person().apply {
                personUid = 12
                firstNames = "Nelzon"
                lastName = "Muntz"
            }
        ),
        results = listOf(
            stringResource(R.string.discussion_board),
            stringResource(R.string.dashboard),
            stringResource(R.string.module),
            stringResource(R.string.video),
            stringResource(R.string.assignments),
            stringResource(R.string.document),
            stringResource(R.string.audio),
            stringResource(R.string.phone),
            stringResource(R.string.change_photo),
            stringResource(R.string.ebook),
            stringResource(R.string.discussion_board),
            stringResource(R.string.dashboard),
            stringResource(R.string.module),
            stringResource(R.string.video),
            stringResource(R.string.assignments),
            stringResource(R.string.document),
            stringResource(R.string.audio),
            stringResource(R.string.phone),
            stringResource(R.string.change_photo),
            stringResource(R.string.ebook),
        )
    )

    CourseDetailProgressScreen(uiState)
}
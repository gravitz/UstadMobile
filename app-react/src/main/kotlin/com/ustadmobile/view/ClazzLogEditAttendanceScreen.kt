package com.ustadmobile.view

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.hooks.useStringsXml
import com.ustadmobile.core.util.ext.personFullName
import com.ustadmobile.core.viewmodel.ClazzLogEditAttendanceUiState
import com.ustadmobile.hooks.useFormattedDateAndTime
import com.ustadmobile.lib.db.entities.ClazzLog
import com.ustadmobile.lib.db.entities.ClazzLogAttendanceRecord
import com.ustadmobile.lib.db.entities.ClazzLogAttendanceRecordWithPerson
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.lib.db.entities.ext.shallowCopy
import com.ustadmobile.mui.common.xs
import csstype.TextAlign
import csstype.px
import mui.icons.material.*
import mui.material.*
import mui.material.StackDirection
import mui.system.responsive
import mui.system.sx
import react.FC
import react.Props
import react.ReactNode
import react.create

external interface ClazzLogEditAttendanceScreenProps : Props {

    var uiState: ClazzLogEditAttendanceUiState

    var onClickMarkAllPresent: (Int) -> Unit

    var onClickMarkAllAbsent: (Int) -> Unit

    var onClickPreviousClazzLog: () -> Unit

    var onClickNextClazzLog: () -> Unit

    var onClazzLogAttendanceChanged: (ClazzLogAttendanceRecordWithPerson) -> Unit

}

val ClazzLogEditAttendanceScreenPreview = FC<Props> {
    ClazzLogEditAttendanceScreenComponent2 {
        uiState = ClazzLogEditAttendanceUiState(
            clazzLogAttendanceRecordList = listOf(
                ClazzLogAttendanceRecordWithPerson().apply {
                    clazzLogAttendanceRecordUid = 0
                    attendanceStatus = ClazzLogAttendanceRecord.STATUS_ATTENDED
                    person = Person().apply {
                        firstNames = "Student Name"
                    }
                },
                ClazzLogAttendanceRecordWithPerson().apply {
                    clazzLogAttendanceRecordUid = 1
                    attendanceStatus = ClazzLogAttendanceRecord.STATUS_ATTENDED
                    person = Person().apply {
                        firstNames = "Student Name"
                    }
                },
                ClazzLogAttendanceRecordWithPerson().apply {
                    clazzLogAttendanceRecordUid = 2
                    attendanceStatus = ClazzLogAttendanceRecord.STATUS_ABSENT
                    person = Person().apply {
                        firstNames = "Student Name"
                    }
                }
            ),
            clazzLogsList = listOf(
                ClazzLog().apply {
                    logDate = 1671629979000
                },
                ClazzLog().apply {
                    logDate = 1671975579000
                },
                ClazzLog().apply {
                    logDate = 1671975579000
                }
            )
        )
    }
}

private val ClazzLogEditAttendanceScreenComponent2 = FC<ClazzLogEditAttendanceScreenProps> { props ->

    val strings = useStringsXml()

    Container {
        maxWidth = "lg"

        Stack {
            direction = responsive(StackDirection.column)
            spacing = responsive(10.px)

            PagerView{
                timeZone = props.uiState.timeZone
                clazzLog = props.uiState.clazzLogsList
            }

            List{

                ListItem {
                    ListItemButton {
                        onClick = {props
                            .onClickMarkAllPresent(ClazzLogAttendanceRecord.STATUS_ATTENDED)
                        }

                        + LibraryAddCheckOutlined.create()

                        ListItemText {
                            primary = ReactNode(strings[MessageID.mark_all_absent])
                        }
                    }
                }

                ListItem {
                    ListItemButton {
                        onClick = {props
                            .onClickMarkAllAbsent(ClazzLogAttendanceRecord.STATUS_ABSENT)
                        }

                        + CheckBoxOutlined.create()

                        ListItemText {
                            primary = ReactNode(strings[MessageID.mark_all_absent])
                        }
                    }
                }

                props.uiState.clazzLogAttendanceRecordList.forEach { clazzLogAttendance ->

                    ClazzLogItemView {
                        clazzLog = clazzLogAttendance
                        onClazzLogAttendanceChanged = props.onClazzLogAttendanceChanged
                        fieldsEnabled = props.uiState.fieldsEnabled
                    }

                }
            }
        }
    }
}



external interface PagerViewProps : Props {

    var timeZone: String

    var onClickPreviousClazzLog: () -> Unit

    var onClickNextClazzLog: () -> Unit

    var clazzLog: List<ClazzLog>

}

private val PagerView = FC<PagerViewProps> { props ->

    val dateTime = useFormattedDateAndTime(
        props.clazzLog[0].logDate,
        props.timeZone
    )

    Grid {
        container = true

        Grid {
            item = true
            xs = 1

            Button {
                variant = ButtonVariant.text
                onClick = {
                    props.onClickPreviousClazzLog()
                }

                + ArrowBack.create()
            }
        }

        Grid {
            item = true
            xs = 10

            Typography {
                sx {
                    textAlign = TextAlign.center
                }
                + dateTime
            }
        }

        Grid {
            item = true
            xs = 1

            Button {
                variant = ButtonVariant.text
                onClick = {
                    props.onClickNextClazzLog()
                }

                + ArrowForward.create()
            }
        }
    }
}



external interface ClazzLogItemViewProps : Props {

    var clazzLog: ClazzLogAttendanceRecordWithPerson

    var onClazzLogAttendanceChanged: (ClazzLogAttendanceRecordWithPerson) -> Unit

    var fieldsEnabled: Boolean
}

private val ClazzLogItemView = FC<ClazzLogItemViewProps> { props ->

    val iconList = listOf(
        Done.create(),
        Close.create(),
        AccessTime.create()
    )
    val statusList = listOf(
        ClazzLogAttendanceRecord.STATUS_ATTENDED,
        ClazzLogAttendanceRecord.STATUS_ABSENT,
        ClazzLogAttendanceRecord.STATUS_PARTIAL
    )

    ListItem{

        ListItemIcon {
            Icon {
                + mui.icons.material.Person.create()
            }
        }

        ListItemText {
            primary = ReactNode(
                props.clazzLog.person?.personFullName() ?: ""
            )
        }

        secondaryAction = ButtonGroup.create {

            iconList.forEachIndexed() { index ,icon ->
                ToggleButton {
                    disabled = !props.fieldsEnabled
                    selected = (props.clazzLog.attendanceStatus == statusList[index])

                    onChange = { _,_ ->
                        props.onClazzLogAttendanceChanged(
                            props.clazzLog.shallowCopy {
                                attendanceStatus = statusList[index]
                            }
                        )
                    }

                    + Close.create()
                }
            }

        }
    }
}
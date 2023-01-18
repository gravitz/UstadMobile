package com.ustadmobile.view

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.hooks.useStringsXml
import com.ustadmobile.core.viewmodel.HolidayCalendarEditUiState
import com.ustadmobile.hooks.useFormattedDate
import com.ustadmobile.lib.db.entities.Holiday
import com.ustadmobile.lib.db.entities.HolidayCalendar
import com.ustadmobile.lib.db.entities.ext.shallowCopy
import com.ustadmobile.mui.components.UstadTextEditField
import com.ustadmobile.view.components.UstadBlankIcon
import kotlinx.css.px
import mui.icons.material.Add
import mui.icons.material.Delete
import mui.material.*
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.FC
import react.Props
import react.ReactNode
import react.create

external interface HolidayCalendarEditProps: Props {
    var uiState: HolidayCalendarEditUiState
    var onAddItemClick: () -> Unit
    var onHolidayCalendarChange: (HolidayCalendar?) -> Unit
    var onDeleteItemClick: (Holiday?) -> Unit
    var onItemClick: (Holiday) -> Unit
}

val HolidayCalendarEditComponent2 = FC<HolidayCalendarEditProps> { props ->

    val strings = useStringsXml()

    Container{
        maxWidth = "lg"

        Stack{
            direction = responsive(mui.material.StackDirection.column)
            spacing = responsive(10.px)

            UstadTextEditField {
                value = props.uiState.holidayCalendar?.umCalendarName ?: ""
                label = strings[MessageID.name]
                enabled = props.uiState.fieldsEnabled
                onChange = {
                    props.onHolidayCalendarChange(
                        props.uiState.holidayCalendar?.shallowCopy {
                            umCalendarName = it
                        })
                }
            }

            ListItem {

                disablePadding = true

                ListItemButton{
                    ListItemIcon {
                        Add{}
                    }

                    ListItemText{
                        primary = ReactNode((strings[MessageID.add_a_holiday]))
                    }

                    onClick = {
                        props.onAddItemClick()
                    }
                }
            }

            List{
                props.uiState.holidayList?.forEach { holiday ->
                    ListItem {

                        disablePadding = true

                        val holidayStart = useFormattedDate(holiday.holStartTime, "UTC")
                        val holidayEnd = useFormattedDate(holiday.holEndTime, "UTC")

                        ListItemButton{

                            onClick = {
                                props.onItemClick(holiday)
                            }

                            ListItemIcon{
                                UstadBlankIcon()
                            }

                            ListItemText{
                                primary = ReactNode((holiday.holName ?: ""))
                                secondary = ReactNode("$holidayStart - $holidayEnd")
                            }
                        }

                        secondaryAction = IconButton.create {
                            onClick = {
                                props.onDeleteItemClick(holiday)
                            }
                            Delete{}
                        }
                    }
                }
            }

        }
    }

}

val HolidayCalendarEditPreview = FC<Props> {
    HolidayCalendarEditComponent2{
        uiState = HolidayCalendarEditUiState(
            holidayCalendar = HolidayCalendar().apply {
                umCalendarName = "my cal"
            },
            holidayList = listOf(
                Holiday().apply {
                    holName = "Eid"
                    holStartTime = 1352958816
                    holEndTime = 1352958818
                }
            )
        )
    }
}
package com.ustadmobile.view

import com.ustadmobile.FieldLabel
import com.ustadmobile.core.controller.HolidayCalendarEditPresenter
import com.ustadmobile.core.controller.UstadEditPresenter
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.view.HolidayCalendarEditView
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.door.ObserverFnWrapper
import com.ustadmobile.lib.db.entities.Holiday
import com.ustadmobile.lib.db.entities.HolidayCalendar
import com.ustadmobile.mui.components.*
import com.ustadmobile.util.StyleManager.contentContainer
import com.ustadmobile.util.StyleManager.defaultPaddingTop
import com.ustadmobile.util.UmProps
import com.ustadmobile.util.ext.currentBackStackEntrySavedStateMap
import com.ustadmobile.util.ext.standardFormat
import com.ustadmobile.view.ext.createItemWithIconTitleAndDescription
import com.ustadmobile.view.ext.umGridContainer
import com.ustadmobile.view.ext.umItem
import kotlinx.css.padding
import org.w3c.dom.events.Event
import react.RBuilder
import react.setState
import styled.css
import styled.styledDiv
import kotlin.js.Date

class HolidayCalendarEditComponent(mProps: UmProps): UstadEditComponent<HolidayCalendar>(mProps),
    HolidayCalendarEditView{

    private var mPresenter: HolidayCalendarEditPresenter? = null

    override val mEditPresenter: UstadEditPresenter<*, HolidayCalendar>?
        get() = mPresenter

    override val viewName: String
        get() = HolidayCalendarEditView.VIEW_NAME

    private var holidays: List<Holiday> = listOf()

    private val holidayObserver = ObserverFnWrapper<List<Holiday>> {
        if(!it.isNullOrEmpty())
            console.log(it.map { x -> x.holUid.toString()})
            setState {
                holidays = it
            }
    }

    override var holidayList: DoorLiveData<List<Holiday>>? = null
        get() = field
        set(value) {
            field = value
            field?.removeObserver(holidayObserver)
            value?.observe(this, holidayObserver)
        }

    private val holidayLabel = FieldLabel(getString(MessageID.name))

    override var fieldsEnabled: Boolean = false
        get() = field
        set(value) {
            setState{
                field = value
            }
        }

    override var entity: HolidayCalendar? = null
        get() = field
        set(value) {
            setState{
                field = value
            }
        }

    override fun onCreateView() {
        super.onCreateView()

        mPresenter = HolidayCalendarEditPresenter(this, arguments, this, this,di)
        mPresenter?.onCreate(navController.currentBackStackEntrySavedStateMap())

        setEditTitle(MessageID.add_a_holiday, MessageID.edit_holiday)
    }

    override fun RBuilder.render() {
        styledDiv {
            css {
                +contentContainer
                +defaultPaddingTop
            }

            umGridContainer(GridSpacing.spacing4) {
                css{
                    padding(2.spacingUnits, 2.spacingUnits,0.spacingUnits)
                }
                umItem(GridSize.cells12) {
                    umTextField(
                        label = "${holidayLabel.text}",
                        helperText = holidayLabel.errorText,
                        value = entity?.umCalendarName, error = holidayLabel.error,
                        disabled = !fieldsEnabled,
                        variant = FormControlVariant.outlined,
                        onChange = {
                            setState {
                                entity?.umCalendarName = it
                            }
                        })
                }
            }

            val newItem = CreateNewItem(true,MessageID.add_a_holiday){
                mPresenter?.holidayToManyJoinListener?.onClickEdit(Holiday())
            }

            renderHolidays(holidays, newItem){ holiday ->
                mPresenter?.holidayToManyJoinListener?.onClickEdit(holiday)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mPresenter?.onDestroy()
        mPresenter = null
        entity = null
    }
}


class HolidayListComponent(mProps: ListProps<Holiday>): UstadSimpleList<ListProps<Holiday>>(mProps){
    override fun RBuilder.renderListItem(item: Holiday, onClick: (Event) -> Unit) {
        umGridContainer {
            attrs.onClick = {
                onClick.invoke(it.nativeEvent)
            }

            createItemWithIconTitleAndDescription("date_range",item.holName,
                "${Date(item.holStartTime).standardFormat()} " +
                        "- ${Date(item.holEndTime).standardFormat()}"
            )
        }
    }
}

fun RBuilder.renderHolidays(
    holidays: List<Holiday>,
    createNewItem: CreateNewItem = CreateNewItem(),
    onEntryClicked: ((Holiday) -> Unit)? = null
) = child(HolidayListComponent::class) {
    attrs.entries = holidays
    attrs.onEntryClicked = onEntryClicked
    attrs.createNewItem = createNewItem
    attrs.mainList = true
}
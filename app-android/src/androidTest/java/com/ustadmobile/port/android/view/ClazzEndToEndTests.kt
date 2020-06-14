package com.ustadmobile.port.android.view

import android.widget.DatePicker
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.toughra.ustadmobile.R
import com.ustadmobile.adbscreenrecorder.client.AdbScreenRecord
import com.ustadmobile.adbscreenrecorder.client.AdbScreenRecordRule
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.lib.db.entities.HolidayCalendar
import com.ustadmobile.lib.db.entities.UmAccount
import org.hamcrest.Matchers.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@AdbScreenRecord("Class end-to-end test")
class ClazzEndToEndTests {

    private lateinit var db: UmAppDatabase

    @JvmField
    @Rule
    val screenRecordRule = AdbScreenRecordRule()

    @AdbScreenRecord("Given an empty class list, when the user clicks add class and fills in form, then the new class is shown in list")
    @Test
    fun givenEmptyClazzList_whenUserClicksAddAndFillsInForm_thenClassIsCreatedAndShownInList() {
        val activeAccount = UmAccount(7L, "bond", "", "http://localhost")
        UmAccountManager.setActiveAccount(activeAccount, ApplicationProvider.getApplicationContext())

        db = UmAccountManager.getActiveDatabase(ApplicationProvider.getApplicationContext())
        db.clearAllTables()

        val calendarUid = db.holidayCalendarDao.insert(HolidayCalendar().apply {
            this.umCalendarName = "Test Calendar"
        })

        val activityScenario = launchActivity<MainActivity>()

        onView(withId(R.id.home_clazzlist_dest)).perform(click())
        onView(withText(R.string.clazz)).perform(click())
        onView(withId(R.id.activity_clazz_edit_name_text)).perform(typeText("Test Class"))
        closeSoftKeyboard()

        //select holiday calendar
        onView(withId(R.id.activity_clazz_edit_holiday_calendar_text)).perform(click())
        onView(withId(R.id.fragment_list_recyclerview)).perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                        withTagValue(equalTo(calendarUid)),
                        click()))

        //Set the start date
        onView(withId(R.id.start_date_text)).perform(click())
        onView(withClassName(equalTo(DatePicker::class.java.name)))
                .perform(PickerActions.setDate(2020, 5, 31))
        onView(withId(android.R.id.button1)).perform(click())

        onView(withId(R.id.menu_done)).perform(click())


        val createdClazz = db.clazzDao.findByClazzName("Test Class").first()
        onView(allOf(withId(R.id.item_clazzlist_clazz_cl), withTagValue(equalTo(createdClazz.clazzUid))))
                .check(matches(isDisplayed()))
    }

}
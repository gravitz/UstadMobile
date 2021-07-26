package com.ustadmobile.port.android.view

import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.ustadmobile.adbscreenrecorder.client.AdbScreenRecord
import com.ustadmobile.adbscreenrecorder.client.AdbScreenRecordRule
import com.toughra.ustadmobile.R
import com.ustadmobile.core.util.ext.grantScopedPermission
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.lib.db.entities.Role
import com.ustadmobile.lib.db.entities.ScopedGrant
import com.ustadmobile.port.android.screen.ContentEntryDetailAttemptsListScreen
import com.ustadmobile.test.port.android.util.installNavController
import com.ustadmobile.test.rules.SystemImplTestNavHostRule
import com.ustadmobile.test.rules.UmAppDatabaseAndroidClientRule
import com.ustadmobile.util.test.ext.insertStatementForSessions
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@AdbScreenRecord("ContentEntryDetail Attempt List screen tests")
class ContentEntryDetailAttemptsListFragmentTest : TestCase()  {

    @JvmField
    @Rule
    var dbRule = UmAppDatabaseAndroidClientRule()

    @JvmField
    @Rule
    var systemImplNavRule = SystemImplTestNavHostRule()

    @JvmField
    @Rule
    val screenRecordRule = AdbScreenRecordRule()

    @Before
    fun setup(){
        runBlocking {
            val adminPerson = Person().apply {
                firstNames = "Bob"
                lastName = "Jones"
                admin = true
                personUid = 42
            }
            dbRule.insertPersonForActiveUser(adminPerson)
            dbRule.repo.grantScopedPermission(adminPerson, Role.ALL_PERMISSIONS,
                ScopedGrant.ALL_TABLES, ScopedGrant.ALL_ENTITIES)
            dbRule.repo.insertStatementForSessions()
        }
    }


    @AdbScreenRecord("Given list when personWithAttempt clicked then navigate to PersonSessionList")
    @Test
    fun givenPersonsAttemptedContent_whenClickOnPerson_thenShouldNavigateToSessionListForPerson() {

        val fragmentScenario = launchFragmentInContainer(themeResId = R.style.UmTheme_App,
                fragmentArgs = bundleOf(UstadView.ARG_ENTITY_UID to 1000L.toString())) {
            ContentEntryDetailAttemptsListFragment().also {
                it.installNavController(systemImplNavRule.navController)
            }
        }

        init{

        }.run{

            ContentEntryDetailAttemptsListScreen{

                recycler{

                    childWith<ContentEntryDetailAttemptsListScreen.PersonWithStatementDisplay>{
                        withDescendant { withText("John Doe") }
                    }perform {
                        scoreText{
                            hasText("Score 100%")
                        }
                        attemptsCount{
                            hasText("2 attempts")
                        }
                        progressText{
                            hasText("100% complete")
                        }
                        personName {
                            hasText("John Doe")
                            click()
                        }
                    }

                }

                flakySafely {
                    Assert.assertEquals("After clicking on item, it navigates to detail view",
                            R.id.content_entry_detail_session_list_dest,
                            systemImplNavRule.navController.currentDestination?.id)
                }


            }

        }
    }

}
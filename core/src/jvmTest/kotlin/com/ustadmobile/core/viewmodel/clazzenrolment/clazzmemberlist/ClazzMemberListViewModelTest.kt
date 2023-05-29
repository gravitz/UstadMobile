package com.ustadmobile.core.viewmodel.clazzenrolment.clazzmemberlist

import app.cash.turbine.test
import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.core.domain.clazzenrolment.pendingenrolment.IApproveOrDeclinePendingEnrolmentRequestUseCase
import com.ustadmobile.core.test.viewmodeltest.ViewModelTestBuilder
import com.ustadmobile.core.test.viewmodeltest.testViewModel
import com.ustadmobile.core.util.ext.awaitItemWhere
import com.ustadmobile.core.util.ext.createNewClazzAndGroups
import com.ustadmobile.core.util.ext.enrolPersonIntoClazzAtLocalTimezone
import com.ustadmobile.core.util.ext.grantScopedPermission
import com.ustadmobile.core.util.ext.insertPersonAndGroup
import com.ustadmobile.core.util.ext.loadFirstList
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.core.viewmodel.person.list.EmptyPagingSource
import com.ustadmobile.door.ext.doorPrimaryKeyManager
import com.ustadmobile.door.ext.withDoorTransactionAsync
import com.ustadmobile.lib.db.entities.Clazz
import com.ustadmobile.lib.db.entities.ClazzEnrolment
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.lib.db.entities.Role
import org.kodein.di.bind
import org.kodein.di.singleton
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ClazzMemberListViewModelTest {

    class ClazzMemberViewModelTestContext(
        val clazz: Clazz,
        val activeUserPerson: Person,
    )

    val endpoint = Endpoint("https://www.test.com/")

    private fun testClazzMemberViewModel(
        activeUserRole: Int,
        block: suspend ViewModelTestBuilder<ClazzMemberListViewModel>.(ClazzMemberViewModelTestContext) -> Unit,
    ) {
        testViewModel {
            val activePerson = setActiveUser(endpoint)

            val context = activeDb.withDoorTransactionAsync {
                val clazzUid = activeDb.doorPrimaryKeyManager.nextId(Clazz.TABLE_ID)
                val clazz = Clazz().apply {
                    this.clazzUid = clazzUid
                    clazzName = "Test Course"
                }
                activeDb.createNewClazzAndGroups(clazz, systemImpl, emptyMap())

                activeDb.takeIf { activeUserRole != 0 }?.enrolPersonIntoClazzAtLocalTimezone(
                    activePerson, clazzUid, activeUserRole
                )

                ClazzMemberViewModelTestContext(clazz, activePerson)
            }

            block(context)
        }
    }

    @Test
    fun givenActiveUserDoesNotHaveAddPermissions_whenOnCreateCalled_thenShouldQueryDatabaseAndSetOnViewAndSetAddVisibleToFalse() {
        testClazzMemberViewModel(
            activeUserRole = ClazzEnrolment.ROLE_STUDENT
        ) { testContext ->
            viewModelFactory {
                savedStateHandle[UstadView.ARG_CLAZZUID] = testContext.clazz.clazzUid.toString()
                ClazzMemberListViewModel(di, savedStateHandle)
            }

            viewModel.uiState.test(timeout = 5.seconds) {
                val readyState = awaitItemWhere {
                    it.studentList() !is EmptyPagingSource && it.teacherList() !is EmptyPagingSource
                }

                assertFalse(readyState.addStudentVisible)
                assertFalse(readyState.addTeacherVisible)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun givenActiveAccountHasAddPermissions_whenOnCreateCalled_thenShouldSetAddOptionsToBeVisible() {
        testClazzMemberViewModel(
            activeUserRole = 0
        ) { testContext ->

            activeDb.grantScopedPermission(testContext.activeUserPerson,
                Role.PERMISSION_CLAZZ_ADD_STUDENT or Role.PERMISSION_CLAZZ_ADD_TEACHER,
                Clazz.TABLE_ID, testContext.clazz.clazzUid)

            viewModelFactory {
                savedStateHandle[UstadView.ARG_CLAZZUID] = testContext.clazz.clazzUid.toString()
                ClazzMemberListViewModel(di, savedStateHandle)
            }

            viewModel.uiState.test(timeout = 5.seconds) {
                val readyState = awaitItemWhere {
                    it.addStudentVisible && it.addTeacherVisible
                }

                assertTrue(readyState.addStudentVisible)
                assertTrue(readyState.addTeacherVisible)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun givenClazzWithPendingRequest_whenClickApprove_thenShouldCallApproveOrDeclineUseCase() {
        testClazzMemberViewModel(
            activeUserRole = ClazzEnrolment.ROLE_TEACHER
        ) { testContext ->
            val pendingStudentPerson = activeDb.insertPersonAndGroup(Person().apply {
                firstNames = "Pending"
                lastName = "Person"
            })
            val mockApproveOrDeclineUseCase = mock<IApproveOrDeclinePendingEnrolmentRequestUseCase>()

            activeDb.enrolPersonIntoClazzAtLocalTimezone(
                personToEnrol = pendingStudentPerson,
                clazzUid = testContext.clazz.clazzUid,
                role = ClazzEnrolment.ROLE_STUDENT_PENDING,
            )

            extendDi {
                bind<IApproveOrDeclinePendingEnrolmentRequestUseCase>() with singleton {
                    mockApproveOrDeclineUseCase
                }
            }

            viewModelFactory {
                savedStateHandle[UstadView.ARG_CLAZZUID] = testContext.clazz.clazzUid.toString()
                ClazzMemberListViewModel(di, savedStateHandle)
            }

            viewModel.uiState.test(timeout = 5.seconds) {
                val readyState = awaitItemWhere {
                    it.pendingStudentList() !is EmptyPagingSource
                }

                val pendingStudents = readyState.pendingStudentList().loadFirstList()
                viewModel.onClickRespondToPendingEnrolment(pendingStudents.first(), true)
                verifyBlocking(mockApproveOrDeclineUseCase) {
                    invoke(pendingStudentPerson.personUid, testContext.clazz.clazzUid, true)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

}
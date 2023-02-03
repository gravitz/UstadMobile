package com.ustadmobile.core.viewmodel

import com.soywiz.klock.DateTime
import com.ustadmobile.core.account.AccountRegisterOptions
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.AppConfig
import com.ustadmobile.core.impl.UstadMobileConstants
import com.ustadmobile.core.impl.UstadMobileSystemCommon
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.impl.appstate.LoadingUiState
import com.ustadmobile.core.impl.appstate.Snack
import com.ustadmobile.core.impl.nav.NavResult
import com.ustadmobile.core.impl.nav.UstadSavedStateHandle
import com.ustadmobile.core.schedule.age
import com.ustadmobile.core.util.ext.hasFlag
import com.ustadmobile.core.util.ext.insertPersonAndGroup
import com.ustadmobile.core.util.ext.putFromSavedStateIfPresent
import com.ustadmobile.core.util.ext.validEmail
import com.ustadmobile.core.view.*
import com.ustadmobile.core.view.PersonEditView.Companion.REGISTER_MODE_MINOR
import kotlinx.coroutines.flow.*
import org.kodein.di.DI
import com.ustadmobile.door.ext.onDbThenRepoWithTimeout
import com.ustadmobile.door.ext.withDoorTransactionAsync
import com.ustadmobile.door.util.systemTimeInMillis
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.Person.Companion.GENDER_UNSET
import kotlinx.coroutines.launch
import org.kodein.di.instance

data class PersonEditUiState(

    val person: PersonWithAccount? = null,

    val personPicture: PersonPicture? = null,

    val fieldsEnabled: Boolean = true,

    /**
     * This is set only when registering a minor
     */
    val approvalPersonParentJoin: PersonParentJoin? = null,

    val registrationMode: Int = 0,

    val usernameError: String? = null,

    val passwordConfirmedError: String? = null,

    val passwordError: String? = null,

    val emailError: String? = null,

    val confirmError: String? = null,

    val dateOfBirthError: String? = null,

    val parentContactError: String? = null,

    val genderError: String? = null,

    val firstNameError: String? = null,

    val lastNameError: String? = null,

    val usernameVisible: Boolean = false,

    val passwordVisible: Boolean = false,

    val parentalEmailVisible: Boolean = false,
)

class PersonEditViewModel(
    di: DI,
    savedStateHandle: UstadSavedStateHandle
): UstadEditViewModel(di, savedStateHandle) {

    private val systemImpl: UstadMobileSystemImpl by instance()

    private val _uiState: MutableStateFlow<PersonEditUiState> = MutableStateFlow(
        PersonEditUiState(
            fieldsEnabled = false,
        )
    )

    val uiState: Flow<PersonEditUiState> = _uiState.asStateFlow()

    private val registrationModeFlags = savedStateHandle[PersonEditView.ARG_REGISTRATION_MODE]?.toInt()
        ?: PersonEditView.REGISTER_MODE_NONE

    private val entityUid: Long
        get() = savedStateHandle[UstadView.ARG_ENTITY_UID]?.toLong() ?: 0

    private val serverUrl = savedStateHandle[UstadView.ARG_SERVER_URL]
        ?: systemImpl.getAppConfigString(AppConfig.KEY_API_URL, "http://localhost") ?: ""

    private val nextDestination = savedStateHandle[UstadView.ARG_NEXT] ?: systemImpl.getAppConfigString(
        AppConfig.KEY_FIRST_DEST, ContentEntryList2View.VIEW_NAME
    ) ?: ContentEntryList2View.VIEW_NAME

    init {
        loadingState = LoadingUiState.INDETERMINATE

        viewModelScope.launch {
            val person: PersonWithAccount = savedStateHandle.getOrPutJson(KEY_ENTITY_STATE) {
                activeRepo.onDbThenRepoWithTimeout(5000) { db, _ ->
                    db.personDao.findPersonAccountByUid(entityUid)
                } ?: PersonWithAccount().also {
                    it.dateOfBirth = savedStateHandle[PersonEditView.ARG_DATE_OF_BIRTH]?.toLong() ?: 0L
                }
            }

            val personParentJoin = if(registrationModeFlags.hasFlag(REGISTER_MODE_MINOR)) {
                PersonParentJoin()
            }else {
                null
            }

            _uiState.update { prev ->
                prev.copy(
                    person = person,
                    approvalPersonParentJoin = personParentJoin,
                    fieldsEnabled = true,
                )
            }
            loadingState = LoadingUiState.NOT_LOADING
        }
    }


    fun onEntityChanged(entity: PersonWithAccount?) {
        _uiState.update { prev ->
            prev.copy(person = entity)
        }
    }

    fun onApprovalPersonParentJoinChanged(personParentJoin: PersonParentJoin?) {
        _uiState.update {prev ->
            prev.copy(approvalPersonParentJoin = personParentJoin)
        }
    }

    private fun PersonEditUiState.hasErrors(): Boolean {
        return usernameError != null ||
            passwordError != null ||
            confirmError != null ||
            dateOfBirthError != null ||
            firstNameError != null ||
            lastNameError != null ||
            genderError != null ||
            emailError != null ||
            parentContactError != null
    }

    fun onClickSave() {

        loadingState = LoadingUiState.INDETERMINATE
        _uiState.update { prev -> prev.copy(fieldsEnabled = false) }
        val savePerson = _uiState.value.person ?: return
        val requiredFieldMessage = systemImpl.getString(MessageID.field_required_prompt)

        _uiState.update { prev ->
            prev.copy(
                usernameError = null,
                passwordError = null,
                emailError = if(savePerson.emailAddr.let { !it.isNullOrBlank() && !it.validEmail() })
                    systemImpl.getString(MessageID.invalid_email)
                else
                    null,
                confirmError = null,
                dateOfBirthError = null,
                parentContactError = null,
                firstNameError = if(savePerson.firstNames.isNullOrEmpty()) requiredFieldMessage else null,
                lastNameError = if(savePerson.lastName.isNullOrEmpty()) requiredFieldMessage else null,
                genderError = if(savePerson.gender == GENDER_UNSET) requiredFieldMessage else null,
            )
        }

        if(_uiState.value.hasErrors()) {
            loadingState = LoadingUiState.NOT_LOADING
            _uiState.update { prev -> prev.copy(fieldsEnabled = true) }
            return
        }

        viewModelScope.launch {
            if(registrationModeFlags.hasFlag(PersonEditView.REGISTER_MODE_ENABLED)) {
                val parentJoin = _uiState.value.approvalPersonParentJoin
                _uiState.update { prev ->
                    prev.copy(
                        usernameError = if(savePerson.username.isNullOrEmpty()) {
                            requiredFieldMessage
                        }else {
                            null
                        },
                        passwordError = if(savePerson.newPassword.isNullOrEmpty()) {
                            requiredFieldMessage
                        }else {
                            null
                        },
                        parentContactError = when {
                            !registrationModeFlags.hasFlag(REGISTER_MODE_MINOR) -> null
                            parentJoin?.ppjEmail.isNullOrEmpty() -> requiredFieldMessage
                            parentJoin?.ppjEmail?.let { it.validEmail() } != true -> {
                                systemImpl.getString(MessageID.invalid_email)
                            }
                            else -> null
                        },
                        dateOfBirthError = if(savePerson.dateOfBirth == 0L) {
                            requiredFieldMessage
                        } else {
                            null
                        }
                    )
                }

                if(_uiState.value.hasErrors()) {
                    loadingState = LoadingUiState.NOT_LOADING
                    _uiState.update { prev -> prev.copy(fieldsEnabled = true) }
                    return@launch
                }

                try {
                    accountManager.register(savePerson, serverUrl, AccountRegisterOptions(
                        makeAccountActive = !registrationModeFlags.hasFlag(REGISTER_MODE_MINOR),
                        parentJoin = parentJoin))

                    val popUpToViewName = savedStateHandle[UstadView.ARG_POPUPTO_ON_FINISH] ?: UstadView.CURRENT_DEST

                    if(registrationModeFlags.hasFlag(REGISTER_MODE_MINOR)) {
                        val goOptions = UstadMobileSystemCommon.UstadGoOptions(
                            RegisterAgeRedirectView.VIEW_NAME, true)
                        val args = mutableMapOf<String, String>().also {
                            it[RegisterMinorWaitForParentView.ARG_USERNAME] = savePerson.username ?: ""
                            it[RegisterMinorWaitForParentView.ARG_PARENT_CONTACT] =
                                parentJoin?.ppjEmail ?: ""
                            it[RegisterMinorWaitForParentView.ARG_PASSWORD] = savePerson.newPassword ?: ""
                            it.putFromSavedStateIfPresent(savedStateHandle, UstadView.ARG_POPUPTO_ON_FINISH)
                        }

                        navController.navigate(RegisterMinorWaitForParentView.VIEW_NAME, args,
                            goOptions)
                    }else {
                        val goOptions = UstadMobileSystemCommon.UstadGoOptions(
                            popUpToViewName, true)
                        navController.navigateToViewUri(nextDestination, goOptions)
                    }
                } catch (e: Exception) {
                    if (e is IllegalStateException) {
                        _uiState.update { prev ->
                            prev.copy(usernameError = systemImpl.getString(MessageID.person_exists))
                        }
                    } else {
                        snackDispatcher.showSnackBar(
                            Snack(systemImpl.getString(MessageID.login_network_error))
                        )
                    }

                    return@launch
                }finally {
                    loadingState = LoadingUiState.NOT_LOADING
                    _uiState.update { prev -> prev.copy(fieldsEnabled = true) }
                }
            }else {
                activeDb.withDoorTransactionAsync {
                    if(savePerson.personUid == 0L) {
                        val personWithGroup = activeDb.insertPersonAndGroup(savePerson)
                        savePerson.personGroupUid = personWithGroup.personGroupUid
                        savePerson.personUid = personWithGroup.personUid
                    }else {
                        activeDb.personDao.updateAsync(savePerson)
                    }

                    val dateBirthDateTime = DateTime(savePerson.dateOfBirth)

                    if(dateBirthDateTime.age() < UstadMobileConstants.MINOR_AGE_THRESHOLD) {
                        //Mark this as approved by the current user
                        val approved = activeDb.personParentJoinDao.isMinorApproved(
                            savePerson.personUid)

                        if(!approved) {
                            activeDb.personParentJoinDao.insertAsync(PersonParentJoin().apply {
                                ppjMinorPersonUid = savePerson.personUid
                                ppjParentPersonUid = accountManager.activeAccount.personUid
                                ppjStatus = PersonParentJoin.STATUS_APPROVED
                                ppjApprovalTiemstamp = systemTimeInMillis()
                            })
                        }
                    }
                }

                val personPictureVal = _uiState.value.personPicture
                if(personPictureVal != null) {
                    personPictureVal.personPicturePersonUid = savePerson.personUid

                    if(personPictureVal.personPictureUid == 0L) {
                        activeDb.personPictureDao.insertAsync(personPictureVal)
                    }else {
                        activeDb.personPictureDao.updateAsync(personPictureVal)
                    }
                }
            }
        }

        //Handle the following scenario: ClazzMemberList (user selects to add a student to enrol),
        // PersonList, PersonEdit, EnrolmentEdit
        val goToOnComplete = savedStateHandle[UstadView.ARG_GO_TO_COMPLETE]
        if(goToOnComplete != null) {
            navController.navigate(goToOnComplete, mutableMapOf<String, String>().apply {
                putFromSavedStateIfPresent(savedStateHandle, ON_COMPLETE_PASS_ARGS)
                put(UstadView.ARG_PERSON_UID, savePerson.personUid.toString())
            }.toMap())
        }else {
            finishWithResult(PersonDetailView.VIEW_NAME, savePerson.personUid, savePerson)
        }
    }

    companion object {

        val ON_COMPLETE_PASS_ARGS = listOf(
            UstadView.ARG_CLAZZUID,
            UstadView.ARG_FILTER_BY_ENROLMENT_ROLE,
            UstadView.ARG_POPUPTO_ON_FINISH,
        )

    }

}
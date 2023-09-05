package com.ustadmobile.view

import com.ustadmobile.core.MR
import com.ustadmobile.core.hooks.useStringProvider
import com.ustadmobile.core.impl.UstadMobileConstants
import com.ustadmobile.core.viewmodel.RegisterAgeRedirectUiState
import com.ustadmobile.mui.components.UstadDateField
import web.cssom.px
import mui.material.Button
import mui.material.ButtonVariant
import mui.material.Typography
import mui.system.Container
import mui.system.Stack
import mui.system.StackDirection
import mui.system.responsive
import react.FC
import react.Props
import react.ReactNode
import react.useState

external interface RegisterAgeRedirectProps : Props {

    var uiState: RegisterAgeRedirectUiState

    var onSetDate: (Long) -> Unit

    var onClickNext: () -> Unit

}

val RegisterAgeRedirectPreview = FC<Props> {
    val uiStateVal by useState {
        RegisterAgeRedirectUiState()
    }
    RegisterAgeRedirectComponent2 {
        uiState = uiStateVal
    }
}

val RegisterAgeRedirectComponent2 = FC<RegisterAgeRedirectProps> { props ->

    val strings = useStringProvider()

    Container {
        maxWidth = "lg"

        Stack {
            direction = responsive(StackDirection.column)
            spacing = responsive(15.px)

            Typography {
                + strings[MR.strings.what_is_your_date_of_birth]
            }

            UstadDateField {
                timeInMillis = props.uiState.dateOfBirth
                timeZoneId = UstadMobileConstants.UTC
                label = ReactNode(strings[MR.strings.birthday])
                onChange = { props.onSetDate(it) }
            }

            Button {
                variant = ButtonVariant.contained
                onClick = { props.onClickNext }

                + strings[MR.strings.next].uppercase()
            }
        }
    }
}
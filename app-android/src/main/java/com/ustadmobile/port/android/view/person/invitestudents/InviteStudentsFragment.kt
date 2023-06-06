package com.ustadmobile.port.android.view.person.invitestudents

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.themeadapter.material.MdcTheme
import com.toughra.ustadmobile.R
import com.ustadmobile.core.viewmodel.person.invitestudents.InviteStudentsUiState
import com.ustadmobile.core.viewmodel.person.invitestudents.InviteStudentsViewModel
import com.ustadmobile.port.android.util.ext.defaultItemPadding
import com.ustadmobile.port.android.util.ext.defaultScreenPadding
import com.ustadmobile.port.android.view.UstadBaseMvvmFragment
import com.ustadmobile.port.android.view.composable.UstadInputFieldLayout


class InviteStudentsFragment : UstadBaseMvvmFragment() {

    private val viewModel: InviteStudentsViewModel by ustadViewModels(::InviteStudentsViewModel)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launchNavigatorCollector(viewModel)
        viewLifecycleOwner.lifecycleScope.launchAppUiStateCollector(viewModel)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                MdcTheme {
                    InviteStudentsScreenForViewModel(viewModel)
                }
            }
        }
    }
}

@Composable
private fun InviteStudentsScreenForViewModel(
    viewModel: InviteStudentsViewModel
) {
    val uiState: InviteStudentsUiState by viewModel.uiState.collectAsState(InviteStudentsUiState())
    InviteStudentsScreen(
        uiState = uiState,
        onTextFieldChanged = viewModel::onTextFieldChanged,
        onClickAddRecipient = viewModel::onClickAddRecipient,
        onClickRemoveRecipient = viewModel::onClickRemoveRecipient,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun InviteStudentsScreen(
    uiState: InviteStudentsUiState = InviteStudentsUiState(),
    onTextFieldChanged: (String) -> Unit = {},
    onClickAddRecipient: () -> Unit = {},
    onClickRemoveRecipient: (String) -> Unit = {},
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .defaultScreenPadding(),
    ) {

        items(
            items = uiState.recipients,
            key = { recipient -> recipient }
        ){ recipient ->
            Chip(
                onClick = { },
                enabled = uiState.fieldsEnabled,
                leadingIcon = {
                    Text(recipient)
                },
                content = {
                    IconButton(
                        onClick = {
                            onClickRemoveRecipient(recipient)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "",
                        )
                    }
                }
            )
        }

        item {
            UstadInputFieldLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultItemPadding(),
                errorText = uiState.textFieldError
            ) {
                OutlinedTextField(
                    modifier = Modifier.testTag("textField").fillMaxWidth(),
                    value = uiState.textField,
                    label = { Text(stringResource( R.string.phone_or_email )) },
                    enabled = uiState.fieldsEnabled,
                    isError = uiState.textFieldError != null,
                    singleLine = true,
                    onValueChange = {
                        onTextFieldChanged(it)
                    }
                )
            }
        }

        item {
            if (uiState.addRecipientVisible){
                TextButton(
                    modifier = Modifier
                        .defaultItemPadding(),
                    onClick = {
                        onClickAddRecipient()
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(stringResource(R.string.add_recipient))
                        Text(uiState.textField)
                    }
                }
            }
        }
    }

}

@Composable
@Preview
fun InviteStudentsScreenPreview() {

    MdcTheme {
        InviteStudentsScreen()
    }
}
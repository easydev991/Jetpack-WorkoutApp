package com.swparks.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.KeyboardAwareBottomBar
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SWTextField
import com.swparks.ui.ds.TextFieldConfig
import com.swparks.ui.state.ChangePasswordEvent
import com.swparks.ui.state.ChangePasswordUiState
import com.swparks.ui.viewmodel.IChangePasswordViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.Icons.AutoMirrored.Filled as AutoMirroredIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    modifier: Modifier = Modifier,
    viewModel: IChangePasswordViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ChangePasswordEvent.NavigateBack -> onBackClick()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.change_password)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = AutoMirroredIcons.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            KeyboardAwareBottomBar {
                SaveButton(
                    enabled = uiState.canSave,
                    onClick = viewModel::onSaveClick
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(dimensionResource(R.dimen.spacing_regular)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
            ) {
                PasswordFields(
                    uiState = uiState,
                    isEnabled = !uiState.isSaving,
                    onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
                    onNewPasswordChange = viewModel::onNewPasswordChange,
                    onConfirmPasswordChange = viewModel::onConfirmPasswordChange
                )
            }

            if (uiState.isSaving) {
                LoadingOverlayView()
            }
        }
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    SWButton(
        config = ButtonConfig(
            mode = SWButtonMode.FILLED,
            size = SWButtonSize.LARGE,
            text = stringResource(R.string.save_changes),
            enabled = enabled,
            onClick = onClick
        )
    )
}


@Composable
private fun PasswordFields(
    uiState: ChangePasswordUiState,
    isEnabled: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        SWTextField(
            config = TextFieldConfig(
                text = uiState.currentPassword,
                labelID = R.string.current_password,
                secure = true,
                enabled = isEnabled,
                onTextChange = onCurrentPasswordChange
            )
        )
        SWTextField(
            config = TextFieldConfig(
                text = uiState.newPassword,
                labelID = R.string.new_password,
                secure = true,
                enabled = isEnabled,
                isError = uiState.newPasswordError != null,
                supportingText = uiState.newPasswordError?.let { stringResource(it) }
                    ?: "",
                onTextChange = onNewPasswordChange
            )
        )

        SWTextField(
            config = TextFieldConfig(
                text = uiState.confirmPassword,
                labelID = R.string.password_confirmation,
                secure = true,
                enabled = isEnabled,
                isError = uiState.confirmPasswordError != null,
                supportingText = uiState.confirmPasswordError?.let { stringResource(it) }
                    ?: "",
                onTextChange = onConfirmPasswordChange
            )
        )
    }
}

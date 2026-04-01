package com.swparks.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.swparks.R
import com.swparks.domain.exception.NetworkException
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SWTextField
import com.swparks.ui.ds.TextFieldConfig
import com.swparks.ui.state.LoginEvent
import com.swparks.ui.state.LoginUiState
import com.swparks.ui.testtags.ScreenshotTestTags
import com.swparks.ui.viewmodel.ILoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: ILoginViewModel,
    onDismiss: () -> Unit = {},
    onLoginSuccess: (userId: Long) -> Unit = {},
    onResetSuccess: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val loginError by viewModel.loginErrorState.collectAsState()
    val resetError by viewModel.resetErrorState.collectAsState()

    val screenState = rememberLoginScreenState()

    LaunchedEffect(Unit) { screenState.focusRequester.requestFocus() }

    Scaffold(
        modifier = modifier,
        topBar = {
            LoginModalAppBar(
                onDismiss = onDismiss,
                isLoading = uiState.isBusy
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LoginContent(
                viewModel = viewModel,
                state = LoginContentState(
                    loginError = loginError,
                    resetError = resetError,
                    isLoading = uiState.isBusy
                ),
                focusRequester = screenState.focusRequester,
                onResetPasswordClick = { showForgotPasswordAlertIfNeeded(viewModel, screenState) },
                modifier = Modifier.padding(paddingValues)
            )

            if (uiState.isBusy) {
                LoadingOverlayView()
            }
        }
    }

    HandleLoginEvents(
        viewModel = viewModel,
        screenState = screenState,
        onLoginSuccess = onLoginSuccess,
        onResetSuccess = onResetSuccess
    )
}


@Composable
private fun HandleLoginEvents(
    viewModel: ILoginViewModel,
    screenState: LoginScreenState,
    onLoginSuccess: (Long) -> Unit,
    onResetSuccess: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loginEvents.collect { event ->
            when (event) {
                is LoginEvent.Success -> onLoginSuccess(event.userId)
                is LoginEvent.ResetSuccess -> {
                    screenState.setShowResetSuccessAlert(true)
                    onResetSuccess(event.email)
                }
            }
        }
    }

    HandleLoginErrorsOnly(viewModel.uiState.collectAsState().value, screenState)

    LoginScreenAlerts(
        state = LoginAlertsState(
            showNoInternet = screenState.showNoInternetAlert,
            showForgotPassword = screenState.showForgotPasswordAlert,
            showResetSuccess = screenState.showResetSuccessAlert
        ),
        onAction = { action ->
            when (action) {
                LoginAlertAction.DismissNoInternet -> screenState.setShowNoInternetAlert(false)
                LoginAlertAction.DismissForgotPassword -> {
                    screenState.setShowForgotPasswordAlert(false)
                    screenState.focusRequester.requestFocus()
                }

                LoginAlertAction.DismissResetSuccess -> screenState.setShowResetSuccessAlert(false)
            }
        }
    )
}

/** Состояние экрана авторизации. */
@Suppress("AssignedValueIsNeverRead")
@Composable
private fun rememberLoginScreenState(): LoginScreenState {
    var showNoInternetAlert by rememberSaveable { mutableStateOf(false) }
    var showForgotPasswordAlert by rememberSaveable { mutableStateOf(false) }
    var showResetSuccessAlert by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    return LoginScreenState(
        showNoInternetAlert = showNoInternetAlert,
        showForgotPasswordAlert = showForgotPasswordAlert,
        showResetSuccessAlert = showResetSuccessAlert,
        setShowNoInternetAlert = { showNoInternetAlert = it },
        setShowForgotPasswordAlert = { showForgotPasswordAlert = it },
        setShowResetSuccessAlert = { showResetSuccessAlert = it },
        focusRequester = focusRequester
    )
}

/** Данные состояния экрана авторизации. */
private data class LoginScreenState(
    var showNoInternetAlert: Boolean,
    var showForgotPasswordAlert: Boolean,
    var showResetSuccessAlert: Boolean,
    val setShowNoInternetAlert: (Boolean) -> Unit,
    val setShowForgotPasswordAlert: (Boolean) -> Unit,
    val setShowResetSuccessAlert: (Boolean) -> Unit,
    val focusRequester: FocusRequester
)

/**
 * TopAppBar для модального окна авторизации с кнопкой закрытия.
 *
 * @param onDismiss Callback для закрытия модального окна
 * @param isLoading Флаг загрузки (блокирует кнопку закрытия)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginModalAppBar(
    onDismiss: () -> Unit,
    isLoading: Boolean,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.sign_in),
                modifier = Modifier.testTag("loginTitle")
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.close)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

private data class LoginContentState(
    val loginError: String?,
    val resetError: String?,
    val isLoading: Boolean
)

/** Контент экрана авторизации. */
@Composable
private fun LoginContent(
    viewModel: ILoginViewModel,
    state: LoginContentState,
    focusRequester: FocusRequester,
    onResetPasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    top = dimensionResource(R.dimen.spacing_regular),
                    start = dimensionResource(R.dimen.spacing_regular),
                    end = dimensionResource(R.dimen.spacing_regular),
                    bottom = dimensionResource(R.dimen.spacing_regular)
                ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        // Верхний VStack с текстовыми полями
        LoginFieldsColumn(
            viewModel = viewModel,
            loginError = state.loginError,
            resetError = state.resetError,
            isLoading = state.isLoading,
            focusRequester = focusRequester
        )

        Spacer(modifier = Modifier.weight(1f))

        // Нижний VStack с кнопками
        ButtonsColumn(
            viewModel = viewModel,
            loginError = state.loginError,
            isLoading = state.isLoading,
            onResetPasswordClick = onResetPasswordClick
        )
    }
}

/** Колонка с текстовыми полями. */
@Composable
private fun LoginFieldsColumn(
    viewModel: ILoginViewModel,
    loginError: String?,
    resetError: String?,
    isLoading: Boolean,
    focusRequester: FocusRequester
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
        LoginField(
            config = LoginFieldConfig(
                value = viewModel.credentials.login,
                isError = resetError != null,
                supportingText = resetError ?: "",
                enabled = !isLoading,
                focusRequester = focusRequester
            ),
            onValueChange = if (!isLoading) viewModel::onLoginChange else { _ -> }
        )

        PasswordField(
            config =
                PasswordFieldConfig(
                    value = viewModel.credentials.password,
                    onValueChange = if (!isLoading) viewModel::onPasswordChange else { _ -> },
                    isError = loginError != null,
                    supportingText = loginError ?: "",
                    enabled = !isLoading,
                    onDone = {
                        if (viewModel.credentials.canLogIn(isError = loginError != null) && !isLoading) {
                            viewModel.login()
                        }
                    }
                )
        )
    }
}

/** Колонка с кнопками. */
@Composable
private fun ButtonsColumn(
    viewModel: ILoginViewModel,
    loginError: String?,
    isLoading: Boolean,
    onResetPasswordClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
        LoginButton(
            enabled = viewModel.credentials.canLogIn(isError = loginError != null) && !isLoading,
            onClick = { viewModel.login() }
        )

        ResetPasswordButton(
            enabled = !isLoading,
            onClick = onResetPasswordClick
        )
    }
}

/** Обработчик клика на кнопку восстановления пароля. */
private fun showForgotPasswordAlertIfNeeded(
    viewModel: ILoginViewModel,
    screenState: LoginScreenState
) {
    if (viewModel.credentials.login.isEmpty()) {
        screenState.setShowForgotPasswordAlert(true)
    } else {
        viewModel.resetPassword()
    }
}

/** Конфигурация для поля пароля. */
private data class PasswordFieldConfig(
    val value: String,
    val onValueChange: (String) -> Unit,
    val isError: Boolean,
    val supportingText: String,
    val enabled: Boolean = true,
    val modifier: Modifier = Modifier,
    val onDone: () -> Unit = {}
)

data class LoginFieldConfig(
    val value: String,
    val isError: Boolean,
    val supportingText: String,
    val enabled: Boolean = true,
    val focusRequester: FocusRequester? = null
)

/** Поле для ввода логина или email. */
@Composable
private fun LoginField(
    modifier: Modifier = Modifier,
    config: LoginFieldConfig,
    onValueChange: (String) -> Unit
) {
    SWTextField(
        config =
            TextFieldConfig(
                modifier = modifier.testTag(ScreenshotTestTags.LOGIN_FIELD),
                text = config.value,
                labelID = R.string.login_or_email,
                secure = false,
                singleLine = true,
                isError = config.isError,
                supportingText = config.supportingText,
                enabled = config.enabled,
                onTextChange = onValueChange,
                focusRequester = config.focusRequester
            )
    )
}

/** Поле для ввода пароля. */
@Composable
private fun PasswordField(config: PasswordFieldConfig) {
    SWTextField(
        config =
            TextFieldConfig(
                modifier = config.modifier.testTag(ScreenshotTestTags.PASSWORD_FIELD),
                text = config.value,
                labelID = R.string.password,
                secure = true,
                singleLine = true,
                isError = config.isError,
                supportingText = config.supportingText,
                enabled = config.enabled,
                onTextChange = config.onValueChange,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { config.onDone() })
            )
    )
}

/** Кнопка "Войти". */
@Composable
private fun LoginButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SWButton(
        config =
            ButtonConfig(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag(ScreenshotTestTags.LOGIN_BUTTON),
                size = SWButtonSize.LARGE,
                mode = SWButtonMode.FILLED,
                text = stringResource(id = R.string.sign_in),
                enabled = enabled,
                onClick = onClick
            )
    )
}

/** Кнопка "Восстановить пароль". */
@Composable
private fun ResetPasswordButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            modifier = Modifier.testTag("resetPasswordButton"),
            onClick = onClick,
            enabled = enabled
        ) {
            Text(stringResource(id = R.string.reset_password))
        }
    }
}

/** Обработка ошибок (только для LoginError и ResetError). */
@Composable
private fun HandleLoginErrorsOnly(
    uiState: LoginUiState,
    screenState: LoginScreenState
) {
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.LoginError -> {
                if (uiState.exception is NetworkException) screenState.setShowNoInternetAlert(true)
            }

            is LoginUiState.ResetError -> {
                if (uiState.exception is NetworkException) screenState.setShowNoInternetAlert(true)
                // НЕ вызываем clearErrors() - сбрасываем только алерт, ошибку оставляем
            }

            else -> {} // Success и Loading здесь игнорируем
        }
    }
}

data class LoginAlertsState(
    val showNoInternet: Boolean = false,
    val showForgotPassword: Boolean = false,
    val showResetSuccess: Boolean = false
)

sealed class LoginAlertAction {
    object DismissNoInternet : LoginAlertAction()
    object DismissForgotPassword : LoginAlertAction()
    object DismissResetSuccess : LoginAlertAction()
}

/** Алерты экрана авторизации. */
@Composable
private fun LoginScreenAlerts(
    state: LoginAlertsState,
    onAction: (LoginAlertAction) -> Unit
) {
    if (state.showNoInternet) {
        NoInternetAlert(onDismiss = { onAction(LoginAlertAction.DismissNoInternet) })
    }

    if (state.showForgotPassword) {
        ForgotPasswordAlert(onDismiss = { onAction(LoginAlertAction.DismissForgotPassword) })
    }

    if (state.showResetSuccess) {
        ResetSuccessAlert(onDismiss = { onAction(LoginAlertAction.DismissResetSuccess) })
    }
}

/** Алерт "Нет интернета". */
@Composable
private fun NoInternetAlert(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.alert_no_connection_title)) },
        text = { Text(text = stringResource(id = R.string.alert_no_connection_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    )
}

/** Алерт "Забыли пароль". */
@Composable
private fun ForgotPasswordAlert(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.alert_forgot_password_title)) },
        text = { Text(text = stringResource(id = R.string.alert_forgot_password_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    )
}

/** Алерт "Готово" (успешное восстановление пароля). */
@Composable
private fun ResetSuccessAlert(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.alert_reset_password_success_title))
        },
        text = {
            Text(text = stringResource(id = R.string.alert_reset_password_success_message))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    )
}

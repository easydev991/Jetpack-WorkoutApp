package com.swparks.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.model.SocialUpdates
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SWTextField
import com.swparks.ui.ds.TextFieldConfig
import com.swparks.ui.state.LoginUiState
import com.swparks.ui.viewmodel.LoginViewModel

/**
 * Экран авторизации.
 *
 * Позволяет пользователю войти в систему или восстановить пароль.
 *
 * @param modifier Модификатор для расположения экрана
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onDismiss Callback для закрытия модального окна
 * @param onLoginSuccess Callback для передачи данных пользователя после успешной авторизации
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel,
    onDismiss: () -> Unit = {},
    onLoginSuccess: (Result<SocialUpdates>) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val loginError by viewModel.loginErrorState.collectAsState()
    val resetError by viewModel.resetErrorState.collectAsState()

    val screenState = rememberLoginScreenState()

    LaunchedEffect(Unit) { screenState.focusRequester.requestFocus() }

    Scaffold(
        modifier = modifier, // Применяем модификатор к всему Scaffold
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
                loginError = loginError,
                resetError = resetError,
                isLoading = uiState.isBusy,
                focusRequester = screenState.focusRequester,
                onResetPasswordClick = { showForgotPasswordAlertIfNeeded(viewModel, screenState) },
                modifier = Modifier.padding(paddingValues)
            )

            // Оверлей загрузки
            if (uiState.isBusy) {
                LoadingOverlayView()
            }
        }
    }

    // Обработка состояний UI
    HandleLoginUiState(
        uiState = uiState,
        viewModel = viewModel,
        onLoginSuccess = { socialUpdates ->
            // Передаем данные пользователя через callback
            // НЕ закрываем LoginScreen - это сделает RootScreen
            onLoginSuccess(Result.success(socialUpdates))
        },
        onResetSuccess = { screenState.setShowResetSuccessAlert(true) },
        onResetError = { viewModel.clearErrors() }
    )

    // Алерты
    LoginScreenAlerts(
        showNoInternetAlert = screenState.showNoInternetAlert,
        showForgotPasswordAlert = screenState.showForgotPasswordAlert,
        showResetSuccessAlert = screenState.showResetSuccessAlert,
        onDismissNoInternetAlert = { screenState.setShowNoInternetAlert(false) },
        onDismissForgotPasswordAlert = {
            screenState.setShowForgotPasswordAlert(false)
            screenState.focusRequester.requestFocus()
        },
        onDismissResetSuccessAlert = { screenState.setShowResetSuccessAlert(false) }
    )
}

/** Состояние экрана авторизации. */
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
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        )
    )
}

/** Контент экрана авторизации. */
@Composable
private fun LoginContent(
    viewModel: LoginViewModel,
    loginError: String?,
    resetError: String?,
    isLoading: Boolean,
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
            loginError = loginError,
            resetError = resetError,
            isLoading = isLoading,
            focusRequester = focusRequester
        )

        Spacer(modifier = Modifier.weight(1f))

        // Нижний VStack с кнопками
        ButtonsColumn(
            viewModel = viewModel,
            loginError = loginError,
            isLoading = isLoading,
            onResetPasswordClick = onResetPasswordClick
        )
    }
}

/** Колонка с текстовыми полями. */
@Composable
private fun LoginFieldsColumn(
    viewModel: LoginViewModel,
    loginError: String?,
    resetError: String?,
    isLoading: Boolean,
    focusRequester: FocusRequester
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))) {
        LoginField(
            value = viewModel.credentials.login,
            onValueChange = if (!isLoading) viewModel::onLoginChange else { _ -> },
            isError = resetError != null,
            supportingText = resetError ?: "",
            modifier = Modifier.focusRequester(focusRequester),
            enabled = !isLoading
        )

        PasswordField(
            config =
                PasswordFieldConfig(
                    value = viewModel.credentials.password,
                    onValueChange = if (!isLoading) viewModel::onPasswordChange else { _ -> },
                    isError = loginError != null,
                    supportingText = loginError ?: "",
                    enabled = !isLoading
                )
        )
    }
}

/** Колонка с кнопками. */
@Composable
private fun ButtonsColumn(
    viewModel: LoginViewModel,
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
    viewModel: LoginViewModel,
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
    val modifier: Modifier = Modifier
)

/** Поле для ввода логина или email. */
@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    supportingText: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    SWTextField(
        config =
            TextFieldConfig(
                modifier = modifier,
                text = value,
                labelID = R.string.login_or_email,
                secure = false,
                singleLine = true,
                isError = isError,
                supportingText = supportingText,
                enabled = enabled,
                onTextChange = onValueChange
            )
    )
}

/** Поле для ввода пароля. */
@Composable
private fun PasswordField(config: PasswordFieldConfig) {
    SWTextField(
        config =
            TextFieldConfig(
                modifier = config.modifier,
                text = config.value,
                labelID = R.string.password,
                secure = true,
                singleLine = true,
                isError = config.isError,
                supportingText = config.supportingText,
                enabled = config.enabled,
                onTextChange = config.onValueChange
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
                    .testTag("signInButton"),
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
    SWButton(
        config =
            ButtonConfig(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("resetPasswordButton"),
                size = SWButtonSize.LARGE,
                mode = SWButtonMode.TINTED,
                text = stringResource(id = R.string.reset_password),
                enabled = enabled,
                onClick = onClick
            )
    )
}

/** Обработка состояний UI. */
@Composable
private fun HandleLoginUiState(
    uiState: LoginUiState,
    viewModel: LoginViewModel,
    onLoginSuccess: (SocialUpdates) -> Unit = {},
    onResetSuccess: () -> Unit = {},
    onResetError: () -> Unit = {}
) {
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Idle -> {
                // Начальное состояние - ничего не делаем
            }

            is LoginUiState.Loading -> {
                // Состояние загрузки - показываем оверлей
            }

            is LoginUiState.LoginSuccess -> {
                // Успешная авторизация - загружаем данные пользователя
                // Аналог iOS: LoginScreen.swift:111-117
                viewModel.loginAndLoadUserData()
                    .onSuccess { socialUpdates ->
                        // Передаем данные пользователя через callback
                        onLoginSuccess(socialUpdates)
                    }
                    .onFailure { error ->
                        // Ошибка загрузки данных пользователя
                        // Не закрываем LoginScreen, показываем ошибку пользователю
                        // TODO: Можно добавить показ error state
                    }
            }

            is LoginUiState.LoginError -> {
                // Ошибка авторизации - отображается под полем пароля через loginError
            }

            is LoginUiState.ResetSuccess -> {
                // Успешное восстановление пароля
                onResetSuccess()
            }

            is LoginUiState.ResetError -> {
                // Ошибка восстановления - отображается под полем логина через resetError
                // НЕ очищаем ошибку - она должна отобразиться пользователю
                // Ошибка очищается при следующем вводе данных (onLoginChange)
            }
        }
    }
}

/** Алерты экрана авторизации. */
@Composable
private fun LoginScreenAlerts(
    showNoInternetAlert: Boolean,
    showForgotPasswordAlert: Boolean,
    showResetSuccessAlert: Boolean,
    onDismissNoInternetAlert: () -> Unit,
    onDismissForgotPasswordAlert: () -> Unit,
    onDismissResetSuccessAlert: () -> Unit
) {
    // Алерт "Нет интернета"
    if (showNoInternetAlert) {
        NoInternetAlert(onDismiss = onDismissNoInternetAlert)
    }

    // Алерт "Забыли пароль"
    if (showForgotPasswordAlert) {
        ForgotPasswordAlert(onDismiss = onDismissForgotPasswordAlert)
    }

    // Алерт "Готово" (успешное восстановление)
    if (showResetSuccessAlert) {
        ResetSuccessAlert(onDismiss = onDismissResetSuccessAlert)
    }
}

/** Алерт "Нет интернета". */
@Composable
private fun NoInternetAlert(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.alert_forgot_password_title)) },
        text = { Text(text = stringResource(id = R.string.alert_no_connection)) },
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

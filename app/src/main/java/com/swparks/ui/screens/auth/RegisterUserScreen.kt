package com.swparks.ui.screens.auth

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.core.net.toUri
import com.swparks.R
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.DateTimePickerConfig
import com.swparks.ui.ds.FormCardContainer
import com.swparks.ui.ds.ListRowData
import com.swparks.ui.ds.ListRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SWDatePickerMode
import com.swparks.ui.ds.SWDateTimePicker
import com.swparks.ui.ds.SWTextField
import com.swparks.ui.ds.TextFieldConfig
import com.swparks.ui.model.Gender
import com.swparks.ui.screen.components.common.RadioButton
import com.swparks.ui.state.RegisterEvent
import com.swparks.ui.viewmodel.IRegisterViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.ZoneId

/**
 * Экран регистрации нового пользователя.
 *
 * @param modifier Модификатор для расположения экрана
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onRegisterSuccess Callback для уведомления об успешной регистрации с userId
 * @param onClose Callback для закрытия экрана
 * @param onSelectCountry Callback для открытия экрана выбора страны
 * @param onSelectCity Callback для открытия экрана выбора города
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterUserScreen(
    modifier: Modifier = Modifier,
    viewModel: IRegisterViewModel,
    onRegisterSuccess: (Long) -> Unit = {},
    onClose: () -> Unit = {},
    onSelectCountry: () -> Unit = {},
    onSelectCity: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val form by viewModel.form.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val emailFormatError by viewModel.emailFormatError.collectAsState()
    val passwordLengthError by viewModel.passwordLengthError.collectAsState()
    val birthDateError by viewModel.birthDateError.collectAsState()

    val scrollState = rememberScrollState()
    val isLoading = uiState.isBusy

    // Обработка событий
    LaunchedEffect(Unit) {
        viewModel.registerEvents.collectLatest { event ->
            when (event) {
                is RegisterEvent.Success -> onRegisterSuccess(event.userId)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RegisterTopAppBar(
                isLoading = isLoading,
                onClose = onClose
            )
        },
        bottomBar = {
            RegisterBottomBar(
                isPolicyAccepted = form.isPolicyAccepted,
                isLoading = isLoading,
                isRegisterEnabled = form.isValid && !isLoading,
                onPolicyChange = viewModel::onPolicyAcceptedChange,
                onRegisterClick = viewModel::register
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = dimensionResource(R.dimen.spacing_regular),
                        vertical = dimensionResource(R.dimen.spacing_regular)
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
            ) {
                // Текстовые поля
                FormCardContainer(enabled = !isLoading) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        LoginField(
                            value = form.login,
                            error = loginError,
                            enabled = !isLoading,
                            onValueChange = viewModel::onLoginChange
                        )
                        EmailField(
                            value = form.email,
                            error = emailFormatError,
                            enabled = !isLoading,
                            onValueChange = viewModel::onEmailChange
                        )
                        PasswordField(
                            value = form.password,
                            error = passwordLengthError,
                            enabled = !isLoading,
                            onValueChange = viewModel::onPasswordChange
                        )
                        FullNameField(
                            value = form.fullName,
                            enabled = !isLoading,
                            onValueChange = viewModel::onFullNameChange
                        )
                    }
                }

                FormCardContainer(enabled = !isLoading) {
                    GenderRadioButtons(
                        selectedGenderCode = form.genderCode,
                        enabled = !isLoading,
                        onGenderChange = viewModel::onGenderChange
                    )
                }

                // Пикеры (дата рождения, страна, город)
                FormCardContainer(enabled = !isLoading) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
                    ) {
                        BirthdayPicker(
                            birthDate = form.birthDate,
                            error = birthDateError,
                            enabled = !isLoading,
                            onBirthDateChange = { timestamp ->
                                val date = if (timestamp != 0L) {
                                    val millisPerDay = 24L * 60L * 60L * 1000L
                                    LocalDate.ofEpochDay(timestamp / millisPerDay)
                                } else {
                                    null
                                }
                                viewModel.onBirthDateChange(date)
                            }
                        )
                        CountryPicker(
                            countryName = selectedCountry?.name,
                            enabled = !isLoading,
                            onClick = onSelectCountry
                        )
                        CityPicker(
                            cityName = selectedCity?.name,
                            enabled = !isLoading,
                            onClick = onSelectCity
                        )
                    }
                }
            }

            // Оверлей загрузки
            if (isLoading) {
                LoadingOverlayView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterTopAppBar(
    isLoading: Boolean,
    onClose: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.registration))
        },
        navigationIcon = {
            IconButton(
                onClick = onClose,
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

@Composable
private fun RegisterBottomBar(
    isPolicyAccepted: Boolean,
    isLoading: Boolean,
    isRegisterEnabled: Boolean,
    onPolicyChange: (Boolean) -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.spacing_regular)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        PolicyToggle(
            isAccepted = isPolicyAccepted,
            enabled = !isLoading,
            onCheckedChange = onPolicyChange
        )
        RegisterButton(
            enabled = isRegisterEnabled,
            onClick = onRegisterClick
        )
    }
}

@Composable
private fun LoginField(
    value: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    val hint = stringResource(R.string.login_hint)
    SWTextField(
        config = TextFieldConfig(
            text = value,
            labelID = R.string.login,
            enabled = enabled,
            isError = error != null,
            supportingText = error ?: hint,
            onTextChange = onValueChange
        )
    )
}

@Composable
private fun EmailField(
    value: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    SWTextField(
        config = TextFieldConfig(
            text = value,
            labelID = R.string.email,
            enabled = enabled,
            isError = error != null,
            supportingText = error ?: "",
            onTextChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
    )
}

@Composable
private fun PasswordField(
    value: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    SWTextField(
        config = TextFieldConfig(
            text = value,
            labelID = R.string.password,
            secure = true,
            enabled = enabled,
            isError = error != null,
            supportingText = error ?: "",
            onTextChange = onValueChange
        )
    )
}

@Composable
private fun FullNameField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    SWTextField(
        config = TextFieldConfig(
            text = value,
            labelID = R.string.full_name,
            enabled = enabled,
            onTextChange = onValueChange
        )
    )
}

@Composable
private fun GenderRadioButtons(
    selectedGenderCode: Int?,
    enabled: Boolean,
    onGenderChange: (Int) -> Unit
) {
    Column(Modifier.padding(dimensionResource(R.dimen.spacing_small))) {
        Text(
            text = stringResource(R.string.gender),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_xsmall))
        )
        Column(modifier = Modifier.selectableGroup()) {
            Gender.entries.forEach { gender ->
                RadioButton(
                    text = stringResource(gender.sex),
                    selected = selectedGenderCode == gender.rawValue,
                    onClick = { onGenderChange(gender.rawValue) },
                    onClickable = enabled
                )
            }
        }
    }
}

@Composable
private fun BirthdayPicker(
    birthDate: LocalDate?,
    error: String?,
    enabled: Boolean,
    onBirthDateChange: (Long) -> Unit
) {
    val initialTimestamp = remember(birthDate) {
        if (birthDate != null) {
            birthDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } else {
            System.currentTimeMillis()
        }
    }

    val currentYear = LocalDate.now().year

    Column {
        SWDateTimePicker(
            config = DateTimePickerConfig(
                mode = SWDatePickerMode.BIRTHDAY,
                initialSelectedDateMillis = initialTimestamp,
                yearRange = 1900..(currentYear - 13),
                enabled = enabled,
                onClickSaveDate = onBirthDateChange
            )
        )
        if (!error.isNullOrEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xsmall))
            )
        }
    }
}

@Composable
private fun CountryPicker(
    countryName: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
    ) {
        ListRowView(
            data = ListRowData(
                leadingText = stringResource(R.string.country),
                trailingText = countryName ?: stringResource(R.string.select_country),
                showChevron = true,
                enabled = enabled
            )
        )
    }
}

@Composable
private fun CityPicker(
    cityName: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
    ) {
        ListRowView(
            data = ListRowData(
                leadingText = stringResource(R.string.city),
                trailingText = cityName ?: stringResource(R.string.select_city),
                showChevron = true,
                enabled = enabled
            )
        )
    }
}

@Composable
private fun PolicyToggle(
    isAccepted: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val agreementUrl = stringResource(R.string.user_agreement_url)

    val annotatedText: AnnotatedString = buildAnnotatedString {
        append(stringResource(R.string.i_accept_terms))
        pushStringAnnotation(tag = "agreement", annotation = agreementUrl)
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(stringResource(R.string.user_agreement))
        }
        pop()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, agreementUrl.toUri())
                    context.startActivity(intent)
                }
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = isAccepted,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun RegisterButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    SWButton(
        config = ButtonConfig(
            modifier = Modifier.fillMaxWidth(),
            size = SWButtonSize.LARGE,
            mode = SWButtonMode.FILLED,
            text = stringResource(id = R.string.register),
            enabled = enabled,
            onClick = onClick
        )
    )
}

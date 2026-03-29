package com.swparks.ui.screens.auth

import android.content.Intent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.swparks.data.model.City
import com.swparks.data.model.Country
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.DateTimePickerConfig
import com.swparks.ui.ds.FormCardContainer
import com.swparks.ui.ds.FormCardContainerParams
import com.swparks.ui.ds.ListRowData
import com.swparks.ui.ds.ListRowView
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWRadioButton
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SWDatePickerMode
import com.swparks.ui.ds.SWDateTimePicker
import com.swparks.ui.ds.SWTextField
import com.swparks.ui.ds.TextFieldConfig
import com.swparks.ui.model.Gender
import com.swparks.ui.model.RegisterForm
import com.swparks.ui.state.RegisterEvent
import com.swparks.ui.utils.disabledAlpha
import com.swparks.ui.viewmodel.IRegisterViewModel
import com.swparks.ui.viewmodel.RegisterContentAction
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.ZoneId

sealed class RegisterNavigationAction {
    data class RegisterSuccess(val userId: Long) : RegisterNavigationAction()
    data object Close : RegisterNavigationAction()
    data object SelectCountry : RegisterNavigationAction()
    data object SelectCity : RegisterNavigationAction()
}

data class RegisterValidationErrors(
    val loginError: String?,
    val emailFormatError: String?,
    val passwordLengthError: String?,
    val birthDateError: String?
)

data class RegisterContentParams(
    val form: RegisterForm,
    val errors: RegisterValidationErrors,
    val selectedCountry: Country?,
    val selectedCity: City?,
    val isLoading: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterUserScreen(
    modifier: Modifier = Modifier,
    viewModel: IRegisterViewModel,
    onNavigationAction: (RegisterNavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val form by viewModel.form.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val errors = rememberValidationErrors(viewModel)
    val scrollState = rememberScrollState()
    val isLoading = uiState.isBusy

    HandleRegisterEvents(viewModel, onNavigationAction)

    Scaffold(
        modifier = modifier,
        topBar = {
            RegisterTopAppBar(
                isLoading = isLoading,
                onClose = { onNavigationAction(RegisterNavigationAction.Close) }
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
        RegisterScreenContent(
            paddingValues = paddingValues,
            scrollState = scrollState,
            params = RegisterContentParams(
                form = form,
                errors = errors,
                selectedCountry = selectedCountry,
                selectedCity = selectedCity,
                isLoading = isLoading
            ),
            onNavigationAction = onNavigationAction,
            onAction = viewModel::onAction
        )
    }
}

@Composable
private fun rememberValidationErrors(viewModel: IRegisterViewModel): RegisterValidationErrors {
    val loginError by viewModel.loginError.collectAsState()
    val emailFormatError by viewModel.emailFormatError.collectAsState()
    val passwordLengthError by viewModel.passwordLengthError.collectAsState()
    val birthDateError by viewModel.birthDateError.collectAsState()
    return remember(loginError, emailFormatError, passwordLengthError, birthDateError) {
        RegisterValidationErrors(loginError, emailFormatError, passwordLengthError, birthDateError)
    }
}

@Composable
private fun HandleRegisterEvents(
    viewModel: IRegisterViewModel,
    onNavigationAction: (RegisterNavigationAction) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.registerEvents.collectLatest { event ->
            when (event) {
                is RegisterEvent.Success -> onNavigationAction(
                    RegisterNavigationAction.RegisterSuccess(event.userId)
                )
            }
        }
    }
}

@Composable
private fun RegisterScreenContent(
    paddingValues: PaddingValues,
    scrollState: ScrollState,
    params: RegisterContentParams,
    onNavigationAction: (RegisterNavigationAction) -> Unit,
    onAction: (RegisterContentAction) -> Unit
) {
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
            RegisterFormFieldsSection(
                form = params.form,
                errors = params.errors,
                isLoading = params.isLoading,
                onAction = onAction
            )

            RegisterGenderSection(
                genderCode = params.form.genderCode,
                isLoading = params.isLoading,
                onAction = onAction
            )

            RegisterBirthdaySection(
                birthDate = params.form.birthDate,
                birthDateError = params.errors.birthDateError,
                isLoading = params.isLoading,
                onAction = onAction
            )

            RegisterLocationSection(
                selectedCountry = params.selectedCountry,
                selectedCity = params.selectedCity,
                isLoading = params.isLoading,
                onSelectCountry = { onNavigationAction(RegisterNavigationAction.SelectCountry) },
                onSelectCity = { onNavigationAction(RegisterNavigationAction.SelectCity) }
            )
        }

        if (params.isLoading) {
            LoadingOverlayView()
        }
    }
}

@Composable
private fun RegisterFormFieldsSection(
    form: RegisterForm,
    errors: RegisterValidationErrors,
    isLoading: Boolean,
    onAction: (RegisterContentAction) -> Unit
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = !isLoading)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            LoginField(
                value = form.login,
                error = errors.loginError,
                enabled = !isLoading,
                onValueChange = { onAction(RegisterContentAction.LoginChange(it)) }
            )
            EmailField(
                value = form.email,
                error = errors.emailFormatError,
                enabled = !isLoading,
                onValueChange = { onAction(RegisterContentAction.EmailChange(it)) }
            )
            PasswordField(
                value = form.password,
                error = errors.passwordLengthError,
                enabled = !isLoading,
                onValueChange = { onAction(RegisterContentAction.PasswordChange(it)) }
            )
            FullNameField(
                value = form.fullName,
                enabled = !isLoading,
                onValueChange = { onAction(RegisterContentAction.FullNameChange(it)) }
            )
        }
    }
}

@Composable
private fun RegisterGenderSection(
    genderCode: Int?,
    isLoading: Boolean,
    onAction: (RegisterContentAction) -> Unit
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = !isLoading)
    ) {
        GenderRadioButtons(
            selectedGenderCode = genderCode,
            enabled = !isLoading,
            onGenderChange = { onAction(RegisterContentAction.GenderChange(it)) }
        )
    }
}

@Composable
private fun RegisterBirthdaySection(
    birthDate: LocalDate?,
    birthDateError: String?,
    isLoading: Boolean,
    onAction: (RegisterContentAction) -> Unit
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = !isLoading)
    ) {
        BirthdayPicker(
            birthDate = birthDate,
            error = birthDateError,
            enabled = !isLoading,
            onBirthDateChange = { timestamp ->
                val date = if (timestamp != 0L) {
                    val millisPerDay = 24L * 60L * 60L * 1000L
                    LocalDate.ofEpochDay(timestamp / millisPerDay)
                } else {
                    null
                }
                onAction(RegisterContentAction.BirthDateChange(date))
            }
        )
    }
}

@Composable
private fun RegisterLocationSection(
    selectedCountry: Country?,
    selectedCity: City?,
    isLoading: Boolean,
    onSelectCountry: () -> Unit,
    onSelectCity: () -> Unit
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = !isLoading)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
        ) {
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
    Column(
        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        Text(
            text = stringResource(R.string.gender),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing_xsmall))
        )
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
        ) {
            Gender.entries.forEach { gender ->
                SWRadioButton(
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

    Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small))) {
        SWDateTimePicker(
            config = DateTimePickerConfig(
                mode = SWDatePickerMode.BIRTHDAY,
                initialSelectedDateMillis = initialTimestamp,
                yearRange = 1900..currentYear,
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
        modifier = Modifier
            .fillMaxWidth()
            .disabledAlpha(!enabled),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = enabled) {
                    val intent = Intent(Intent.ACTION_VIEW, agreementUrl.toUri())
                    context.startActivity(intent)
                }
        )
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

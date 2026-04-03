package com.swparks.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swparks.R
import com.swparks.data.model.User
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.DateTimePickerConfig
import com.swparks.ui.ds.FormCardContainer
import com.swparks.ui.ds.FormCardContainerParams
import com.swparks.ui.ds.KeyboardAwareBottomBar
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
import com.swparks.ui.state.EditProfileEvent
import com.swparks.ui.viewmodel.IEditProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import androidx.compose.material.icons.Icons.AutoMirrored.Filled as AutoMirroredIcons

private const val GENDER_DROPDOWN_MENU_WIDTH_FRACTION = 0.3f

sealed class EditProfileNavigationAction {
    object Back : EditProfileNavigationAction()

    object ChangePassword : EditProfileNavigationAction()

    object SelectCountry : EditProfileNavigationAction()

    object SelectCity : EditProfileNavigationAction()

    object NavigateToLogin : EditProfileNavigationAction()
}

private data class EditProfileContentParams(
    val avatarUrl: String?,
    val selectedAvatarUri: Uri?,
    val avatarError: String?,
    val userForm: com.swparks.ui.model.MainUserForm,
    val emailError: String?,
    val birthDateError: String?,
    val selectedCountry: com.swparks.data.model.Country?,
    val selectedCity: com.swparks.data.model.City?,
    val isLoading: Boolean,
    val onChangeAvatarClick: () -> Unit,
    val onLoginChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onFullNameChange: (String) -> Unit,
    val onGenderChange: (Gender) -> Unit,
    val onBirthDateChange: (Long) -> Unit,
    val onCountryClick: () -> Unit,
    val onCityClick: () -> Unit,
    val onChangePasswordClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    currentUser: User?,
    viewModel: IEditProfileViewModel,
    onAction: (EditProfileNavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            viewModel.onAvatarSelected(uri)
        }

    val launchPhotoPicker: () -> Unit = {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    HandleEditProfileEvents(viewModel, onAction)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetChanges()
        }
    }

    Scaffold(
        topBar = {
            EditProfileTopBar(
                onBackClick = { onAction(EditProfileNavigationAction.Back) },
                onDeleteClick = { showDeleteDialog = true }
            )
        },
        bottomBar = {
            KeyboardAwareBottomBar {
                SaveButton(
                    enabled = uiState.canSave && !uiState.isLoading,
                    onClick = viewModel::onSaveClick
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        EditProfileContent(
            params =
                createContentParams(
                    uiState = uiState,
                    currentUser = currentUser,
                    launchPhotoPicker = launchPhotoPicker,
                    viewModel = viewModel
                ),
            paddingValues = paddingValues,
            scrollState = scrollState
        )
    }

    if (showDeleteDialog) {
        DeleteProfileDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.onDeleteProfileClick()
            }
        )
    }
}

@Composable
private fun HandleEditProfileEvents(
    viewModel: IEditProfileViewModel,
    onAction: (EditProfileNavigationAction) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditProfileEvent.NavigateBack -> onAction(EditProfileNavigationAction.Back)
                is EditProfileEvent.NavigateToLogin -> onAction(EditProfileNavigationAction.NavigateToLogin)
                is EditProfileEvent.NavigateToChangePassword -> onAction(EditProfileNavigationAction.ChangePassword)
                is EditProfileEvent.NavigateToSelectCountry -> onAction(EditProfileNavigationAction.SelectCountry)
                is EditProfileEvent.NavigateToSelectCity -> onAction(EditProfileNavigationAction.SelectCity)
            }
        }
    }
}

private fun createContentParams(
    uiState: com.swparks.ui.state.EditProfileUiState,
    currentUser: User?,
    launchPhotoPicker: () -> Unit,
    viewModel: IEditProfileViewModel
): EditProfileContentParams =
    EditProfileContentParams(
        avatarUrl = currentUser?.image,
        selectedAvatarUri = uiState.selectedAvatarUri,
        avatarError = uiState.avatarError,
        userForm = uiState.userForm,
        emailError = uiState.emailError,
        birthDateError = uiState.birthDateError,
        selectedCountry = uiState.selectedCountry,
        selectedCity = uiState.selectedCity,
        isLoading = uiState.isLoading,
        onChangeAvatarClick = launchPhotoPicker,
        onLoginChange = viewModel::onLoginChange,
        onEmailChange = viewModel::onEmailChange,
        onFullNameChange = viewModel::onFullNameChange,
        onGenderChange = viewModel::onGenderChange,
        onBirthDateChange = viewModel::onBirthDateChange,
        onCountryClick = viewModel::onCountryClick,
        onCityClick = viewModel::onCityClick,
        onChangePasswordClick = viewModel::onChangePasswordClick
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileTopBar(
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text(text = stringResource(R.string.edit_profile)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = AutoMirroredIcons.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }
    )
}

@Composable
private fun EditProfileContent(
    params: EditProfileContentParams,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    scrollState: ScrollState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = dimensionResource(R.dimen.spacing_regular),
                        vertical = dimensionResource(R.dimen.spacing_regular)
                    ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
        ) {
            val isEnabled = !params.isLoading

            AvatarSection(
                avatarUrl = params.avatarUrl,
                selectedAvatarUri = params.selectedAvatarUri,
                avatarError = params.avatarError,
                enabled = isEnabled,
                onChangeAvatarClick = params.onChangeAvatarClick
            )

            MainFieldsSection(
                params = params,
                isEnabled = isEnabled
            )

            GenderSection(
                params = params,
                isEnabled = isEnabled
            )

            BirthdaySection(
                params = params,
                isEnabled = isEnabled
            )

            LocationSection(
                params = params,
                isEnabled = isEnabled
            )
        }

        if (params.isLoading) {
            LoadingOverlayView()
        }
    }
}

@Composable
private fun MainFieldsSection(
    params: EditProfileContentParams,
    isEnabled: Boolean
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = isEnabled)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            LoginField(
                value = params.userForm.name,
                enabled = isEnabled,
                onValueChange = params.onLoginChange
            )
            EmailField(
                value = params.userForm.email,
                error = params.emailError,
                enabled = isEnabled,
                onValueChange = params.onEmailChange
            )
            FullNameField(
                value = params.userForm.fullname,
                enabled = isEnabled,
                onValueChange = params.onFullNameChange
            )
            ChangePasswordButton(
                enabled = isEnabled,
                onClick = params.onChangePasswordClick
            )
        }
    }
}

@Composable
private fun GenderSection(
    params: EditProfileContentParams,
    isEnabled: Boolean
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = isEnabled)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small))
        ) {
            GenderPicker(
                selectedGender = Gender.entries.find { it.rawValue == params.userForm.genderCode },
                enabled = isEnabled,
                onGenderChange = params.onGenderChange
            )
        }
    }
}

@Composable
private fun BirthdaySection(
    params: EditProfileContentParams,
    isEnabled: Boolean
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = isEnabled)
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small))) {
            BirthdayPicker(
                birthDate = params.userForm.birthDate,
                error = params.birthDateError,
                enabled = isEnabled,
                onBirthDateChange = params.onBirthDateChange
            )
        }
    }
}

@Composable
private fun LocationSection(
    params: EditProfileContentParams,
    isEnabled: Boolean
) {
    FormCardContainer(
        params = FormCardContainerParams(enabled = isEnabled)
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
        ) {
            CountryPicker(
                countryName = params.selectedCountry?.name,
                enabled = isEnabled,
                onClick = params.onCountryClick
            )
            CityPicker(
                cityName = params.selectedCity?.name,
                enabled = isEnabled,
                onClick = params.onCityClick
            )
        }
    }
}

@Composable
private fun AvatarSection(
    avatarUrl: String?,
    selectedAvatarUri: Uri?,
    avatarError: String?,
    enabled: Boolean,
    onChangeAvatarClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        AsyncImage(
            model = selectedAvatarUri ?: avatarUrl,
            contentDescription = stringResource(R.string.photo),
            modifier =
                Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.spacing_small))),
            contentScale = ContentScale.Crop,
            alpha = if (enabled) 1f else 0.5f
        )

        // Отображение ошибки
        if (!avatarError.isNullOrEmpty()) {
            Text(
                text = avatarError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        SWButton(
            config =
                ButtonConfig(
                    mode = SWButtonMode.TINTED,
                    size = SWButtonSize.SMALL,
                    text = stringResource(R.string.change_photo),
                    enabled = enabled,
                    onClick = onChangeAvatarClick
                )
        )
    }
}

@Composable
private fun LoginField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    SWTextField(
        config =
            TextFieldConfig(
                text = value,
                labelID = R.string.login,
                enabled = enabled,
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
        config =
            TextFieldConfig(
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
private fun FullNameField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    SWTextField(
        config =
            TextFieldConfig(
                text = value,
                labelID = R.string.full_name,
                enabled = enabled,
                onTextChange = onValueChange
            )
    )
}

@Composable
private fun ChangePasswordButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
    ) {
        ListRowView(
            data =
                ListRowData(
                    leadingIconID = R.drawable.outline_key,
                    leadingText = stringResource(R.string.change_password),
                    showChevron = true,
                    enabled = enabled
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderPicker(
    selectedGender: Gender?,
    enabled: Boolean,
    onGenderChange: (Gender) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        ListRowView(
            data =
                ListRowData(
                    leadingText = stringResource(R.string.gender),
                    trailingText =
                        selectedGender?.let { stringResource(it.sex) }
                            ?: stringResource(R.string.select_gender),
                    showChevron = true,
                    enabled = enabled
                )
        )
        // Якорь для DropdownMenu в правой части
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(GENDER_DROPDOWN_MENU_WIDTH_FRACTION)
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Gender.entries.forEach { gender ->
                    DropdownMenuItem(
                        text = { Text(stringResource(gender.sex)) },
                        onClick = {
                            onGenderChange(gender)
                            expanded = false
                        }
                    )
                }
            }
        }
        // Прозрачный слой для перехвата кликов по всей строке
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clickable(enabled = enabled) { expanded = true }
        )
    }
}

@Composable
private fun BirthdayPicker(
    birthDate: String,
    error: String?,
    enabled: Boolean,
    onBirthDateChange: (Long) -> Unit
) {
    val initialTimestamp =
        remember(birthDate) {
            if (birthDate.isNotBlank()) {
                try {
                    val localDate = LocalDate.parse(birthDate)
                    // Используем UTC для корректного отображения даты без смещения на 1 день
                    localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                } catch (_: DateTimeParseException) {
                    System.currentTimeMillis()
                } catch (_: IllegalArgumentException) {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }
        }

    val currentYear = LocalDate.now().year

    Column {
        SWDateTimePicker(
            config =
                DateTimePickerConfig(
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
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
    ) {
        ListRowView(
            data =
                ListRowData(
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
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
    ) {
        ListRowView(
            data =
                ListRowData(
                    leadingText = stringResource(R.string.city),
                    trailingText = cityName ?: stringResource(R.string.select_city),
                    showChevron = true,
                    enabled = enabled
                )
        )
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    SWButton(
        config =
            ButtonConfig(
                mode = SWButtonMode.FILLED,
                size = SWButtonSize.LARGE,
                text = stringResource(R.string.save_changes),
                enabled = enabled,
                onClick = onClick
            )
    )
}

@Composable
private fun DeleteProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.delete_profile_title))
        },
        text = {
            Text(text = stringResource(R.string.delete_profile_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

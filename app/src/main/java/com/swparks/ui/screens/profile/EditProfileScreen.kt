package com.swparks.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    currentUser: User?,
    viewModel: IEditProfileViewModel,
    onBackClick: () -> Unit,
    onNavigateToChangePassword: (Long) -> Unit = {},
    onNavigateToSelectCountry: (Int?) -> Unit = {},
    onNavigateToSelectCity: (Int?, Int) -> Unit = { _, _ -> },
    onNavigateToLogin: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Photo Picker для выбора изображения
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        viewModel.onAvatarSelected(uri)
    }

    // Функция для запуска выбора фото
    val launchPhotoPicker: () -> Unit = {
        // Используем Photo Picker с ограничением только на изображения
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Обработка событий
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditProfileEvent.NavigateBack -> onBackClick()
                is EditProfileEvent.NavigateToLogin -> onNavigateToLogin()
                is EditProfileEvent.NavigateToChangePassword -> onNavigateToChangePassword(event.userId)
                is EditProfileEvent.NavigateToSelectCountry -> onNavigateToSelectCountry(event.currentCountryId)
                is EditProfileEvent.NavigateToSelectCity -> onNavigateToSelectCity(
                    event.currentCityId,
                    event.countryId
                )
            }
        }
    }

    // Сброс несохранённых изменений при закрытии экрана
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetChanges()
        }
    }

    Scaffold(
        topBar = {
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
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
            )
        },
        bottomBar = {
            SaveButton(
                enabled = uiState.canSave && !uiState.isLoading,
                onClick = viewModel::onSaveClick
            )
        },
        modifier = modifier
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
                val isEnabled = !uiState.isLoading

                // Секция аватара
                AvatarSection(
                    avatarUrl = currentUser?.image,
                    selectedAvatarUri = uiState.selectedAvatarUri,
                    avatarError = uiState.avatarError,
                    enabled = isEnabled,
                    onChangeAvatarClick = launchPhotoPicker
                )

                // Текстовые поля
                FormCardContainer(
                    params = FormCardContainerParams(enabled = isEnabled)
                ) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
                    ) {
                        LoginField(
                            value = uiState.userForm.name,
                            enabled = isEnabled,
                            onValueChange = viewModel::onLoginChange
                        )
                        EmailField(
                            value = uiState.userForm.email,
                            error = uiState.emailError,
                            enabled = isEnabled,
                            onValueChange = viewModel::onEmailChange
                        )
                        FullNameField(
                            value = uiState.userForm.fullname,
                            enabled = isEnabled,
                            onValueChange = viewModel::onFullNameChange
                        )
                        ChangePasswordButton(
                            enabled = isEnabled,
                            onClick = viewModel::onChangePasswordClick
                        )
                    }
                }
                FormCardContainer(
                    params = FormCardContainerParams(enabled = isEnabled)
                ) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
                    ) {
                        GenderPicker(
                            selectedGender = Gender.entries.find { it.rawValue == uiState.userForm.genderCode },
                            enabled = isEnabled,
                            onGenderChange = viewModel::onGenderChange
                        )
                    }
                }
                FormCardContainer(
                    params = FormCardContainerParams(enabled = isEnabled)
                ) {
                    Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small))) {
                        BirthdayPicker(
                            birthDate = uiState.userForm.birthDate,
                            error = uiState.birthDateError,
                            enabled = isEnabled,
                            onBirthDateChange = viewModel::onBirthDateChange
                        )
                    }
                }
                FormCardContainer(
                    params = FormCardContainerParams(enabled = isEnabled)
                ) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small)),
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
                    ) {
                        CountryPicker(
                            countryName = uiState.selectedCountry?.name,
                            enabled = isEnabled,
                            onClick = viewModel::onCountryClick
                        )
                        CityPicker(
                            cityName = uiState.selectedCity?.name,
                            enabled = isEnabled,
                            onClick = viewModel::onCityClick
                        )
                    }
                }
            }

            // Оверлей загрузки при сохранении или удалении профиля
            if (uiState.isLoading) {
                LoadingOverlayView()
            }
        }
    }

    // Диалог подтверждения удаления профиля
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
            modifier = Modifier
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
            config = ButtonConfig(
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
        config = TextFieldConfig(
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
private fun ChangePasswordButton(
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
            data = ListRowData(
                leadingText = stringResource(R.string.gender),
                trailingText = selectedGender?.let { stringResource(it.sex) }
                    ?: stringResource(R.string.select_gender),
                showChevron = true,
                enabled = enabled
            )
        )
        // Якорь для DropdownMenu в правой части
        Box(
            modifier = Modifier
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
            modifier = Modifier
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
    val initialTimestamp = remember(birthDate) {
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
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    SWButton(
        config = ButtonConfig(
            modifier = Modifier.padding(all = dimensionResource(R.dimen.spacing_regular)),
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

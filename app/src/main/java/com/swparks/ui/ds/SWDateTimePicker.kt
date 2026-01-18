package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import java.text.DateFormat

/**
 * Вариант пикера даты
 *
 * @property titleID Идентификатор локализованной строки с текстом слева от пикера
 * @property showTimePicker Нужно ли показывать пикер времени
 */
enum class SWDatePickerMode(
    @StringRes val titleID: Int,
    val showTimePicker: Boolean
) {
    /**
     * Дата рождения (день, месяц и год)
     */
    BIRTHDAY(R.string.birthdate, false),

    /**
     * Дата проведения мероприятия (день, месяц, год, время)
     */
    EVENT(R.string.date_time, true)
}

/**
 * Пикер для даты/времени. Полезная [статья](https://semicolonspace.com/jetpack-compose-date-picker-material3/)
 *
 * @param modifier Модификатор
 * @param mode Вариант пикера - [SWDatePickerMode]
 * @param initialSelectedDateMillis Изначально заданная дата в миллисекундах
 * @param yearRange Допустимый диапазон в годах от и до
 * @param initialHour Изначально заданный час
 * @param initialMinute Изначально заданные минуты
 * @param enabled Доступность кнопок для вызова пикера
 * @param onClickSaveDate Действие при сохранении даты
 * @param onClickSaveTime Действие при сохранении времени
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SWDateTimePicker(
    modifier: Modifier = Modifier,
    mode: SWDatePickerMode,
    initialSelectedDateMillis: Long? = null,
    yearRange: IntRange,
    initialHour: Int? = null,
    initialMinute: Int? = null,
    enabled: Boolean = true,
    onClickSaveDate: (Long) -> Unit,
    onClickSaveTime: (Int, Int) -> Unit = { _, _ -> }
) {
    var selectedDate by remember {
        mutableLongStateOf(initialSelectedDateMillis ?: System.currentTimeMillis())
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        yearRange = yearRange
    )

    var selectedHour by remember { mutableIntStateOf(initialHour ?: 0) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute ?: 0) }
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = true
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(id = mode.titleID))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SWDatePicker(
                initialSelectedDateMillis = selectedDate,
                formattedString = DateFormat.getDateInstance(DateFormat.MEDIUM).format(selectedDate),
                state = datePickerState,
                enabled = enabled,
                onClickSaveDate = { dateLong ->
                    selectedDate = dateLong
                    onClickSaveDate(dateLong)
                }
            )
            if (mode.showTimePicker) {
                SWTimePicker(
                    state = timePickerState,
                    formattedString = "$selectedHour:$selectedMinute",
                    enabled = enabled,
                    onClickSave = { hour, minute ->
                        selectedHour = hour
                        selectedMinute = minute
                        onClickSaveTime(hour, minute)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SWDatePicker(
    modifier: Modifier = Modifier,
    formattedString: String,
    state: DatePickerState,
    initialSelectedDateMillis: Long,
    enabled: Boolean = true,
    onClickSaveDate: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    SWButton(
        modifier = modifier,
        text = formattedString,
        size = SWButtonSize.SMALL,
        mode = SWButtonMode.TINTED,
        enabled = enabled
    ) {
        showDatePicker = true
    }
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        onClickSaveDate(state.selectedDateMillis ?: initialSelectedDateMillis)
                    }
                ) {
                    Text(stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text(stringResource(id = R.string.close))
                }
            },
            colors = DatePickerDefaults.colors()
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SWTimePicker(
    modifier: Modifier = Modifier,
    formattedString: String,
    state: TimePickerState,
    enabled: Boolean = true,
    onClickSave: (Int, Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    SWButton(
        modifier = modifier,
        text = formattedString,
        size = SWButtonSize.SMALL,
        mode = SWButtonMode.TINTED,
        enabled = enabled
    ) {
        showTimePicker = true
    }
    if (showTimePicker) {
        AlertDialog(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(size = 12.dp)
                ),
            onDismissRequest = { showTimePicker = false }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = state)
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(text = stringResource(id = R.string.close))
                    }
                    TextButton(
                        onClick = {
                            showTimePicker = false
                            onClickSave(
                                state.hour,
                                state.minute
                            )
                        }
                    ) {
                        Text(text = stringResource(id = R.string.save))
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun SWDateTimePickerPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SWDateTimePicker(
                    mode = SWDatePickerMode.BIRTHDAY,
                    yearRange = 1900..2010,
                    onClickSaveDate = {}
                )
                SWDateTimePicker(
                    mode = SWDatePickerMode.EVENT,
                    yearRange = 2023..2024,
                    initialHour = 12,
                    initialMinute = 22,
                    onClickSaveDate = {},
                    onClickSaveTime = { _, _ -> }
                )
            }
        }
    }
}
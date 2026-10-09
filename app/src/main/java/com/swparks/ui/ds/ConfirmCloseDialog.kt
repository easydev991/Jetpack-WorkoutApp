package com.swparks.ui.ds

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.swparks.R

/**
 * Общий диалог подтверждения закрытия формы/диалога с несохранёнными правками.
 *
 * Используется формами площадки и мероприятия, редактированием профиля,
 * настройками дневника и фильтром площадок — единая реализация вместо копипасты.
 * Отмена (тап вне, возврат по самому диалогу) закрывает только диалог — правки на месте.
 *
 * @param onDismiss Отмена закрытия — правки сохраняются в поле ввода
 * @param onConfirm Подтверждение закрытия — правки теряются
 */
@Composable
fun ConfirmCloseDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.confirm_close_title))
        },
        text = {
            Text(text = stringResource(R.string.confirm_close_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.close),
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

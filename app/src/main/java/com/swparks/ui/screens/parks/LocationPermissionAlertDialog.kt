package com.swparks.ui.screens.parks

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.swparks.R

@Composable
fun LocationPermissionAlertDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onOpenSettings: () -> Unit,
    isDeniedForever: Boolean = false
) {
    if (!visible) return

    val messageRes =
        if (isDeniedForever) {
            R.string.location_permission_need_in_settings
        } else {
            R.string.location_permission_alert_message
        }
    val confirmTextRes =
        if (isDeniedForever) {
            R.string.open_settings
        } else {
            R.string.allow_location_access
        }
    val onConfirmClick = if (isDeniedForever) onOpenSettings else onConfirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.location_permission_alert_title)) },
        text = { Text(text = stringResource(messageRes)) },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(
                    text = stringResource(confirmTextRes),
                    color = MaterialTheme.colorScheme.primary
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

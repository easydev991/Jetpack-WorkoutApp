package com.swparks.ui.ds

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class PickedImagesController(
    val launch: () -> Unit,
    val remainingSlots: Int
)

internal enum class PickerLaunchMode {
    Disabled,
    Single,
    Multiple
}

internal fun resolvePickerLaunchMode(remainingSlots: Int): PickerLaunchMode =
    when {
        remainingSlots <= 0 -> PickerLaunchMode.Disabled
        remainingSlots == 1 -> PickerLaunchMode.Single
        else -> PickerLaunchMode.Multiple
    }

@Composable
fun rememberPickedImagesController(
    currentImageCount: Int,
    selectionLimit: Int,
    onImagesSelected: (List<Uri>) -> Unit
): PickedImagesController {
    val remainingSlots = (selectionLimit - currentImageCount).coerceAtLeast(0)
    val launchMode = resolvePickerLaunchMode(remainingSlots)

    val singlePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                onImagesSelected(listOf(uri))
            }
        }

    val multiPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.PickMultipleVisualMedia(
                    remainingSlots.coerceAtLeast(
                        2
                    )
                )
        ) { uris ->
            if (uris.isNotEmpty()) {
                onImagesSelected(uris)
            }
        }

    val mediaRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    return remember(remainingSlots) {
        PickedImagesController(
            launch = {
                when (launchMode) {
                    PickerLaunchMode.Disabled -> Unit
                    PickerLaunchMode.Single -> singlePicker.launch(mediaRequest)
                    PickerLaunchMode.Multiple -> multiPicker.launch(mediaRequest)
                }
            },
            remainingSlots = remainingSlots
        )
    }
}

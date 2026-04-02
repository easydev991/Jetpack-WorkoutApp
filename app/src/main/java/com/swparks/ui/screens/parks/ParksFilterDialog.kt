package com.swparks.ui.screens.parks

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.swparks.R
import com.swparks.data.model.ParkFilter
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.CheckmarkRowView
import com.swparks.ui.ds.FormCardContainer
import com.swparks.ui.ds.FormCardContainerParams
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SectionView
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@Composable
fun ParksFilterDialog(
    filter: ParkFilter,
    onFilterChange: (ParkFilter) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberParksFilterDialogState(filter)

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(horizontal = dimensionResource(R.dimen.spacing_regular))
            .fillMaxWidth(),
        onDismissRequest = onDismiss,
        title = { DialogTitle(onDismiss) },
        text = {
            FilterDialogContent(state = state)
        },
        confirmButton = {
            SWButton(
                config = ButtonConfig(
                    size = SWButtonSize.SMALL,
                    text = stringResource(R.string.apply_button),
                    enabled = state.canApply,
                    onClick = {
                        onFilterChange(state.toParkFilter())
                        onApply()
                    }
                )
            )
        },
        dismissButton = {
            TextButton(
                onClick = {
                    state.reset()
                },
                enabled = state.isEdited
            ) {
                Text(stringResource(R.string.reset_filter))
            }
        }
    )
}

@Composable
private fun FilterDialogContent(
    state: ParksFilterDialogState
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = dimensionResource(R.dimen.spacing_xsmall)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        FilterSection(
            titleID = R.string.size,
            items = ParkSize.entries.map { size ->
                FilterItem(
                    text = stringResource(size.description),
                    isChecked = state.isSizeSelected(size),
                    onToggle = {
                        state.toggleSize(size)
                    }
                )
            }
        )
        FilterSection(
            titleID = R.string.type,
            items = ParkType.entries.map { type ->
                FilterItem(
                    text = stringResource(type.description),
                    isChecked = state.isTypeSelected(type),
                    onToggle = {
                        state.toggleType(type)
                    }
                )
            }
        )
    }
}

private data class FilterItem(
    val text: String,
    val isChecked: Boolean,
    val onToggle: () -> Unit
)

private class ParksFilterDialogState(
    initialFilter: ParkFilter
) {
    private val _sizes = mutableStateListOf<ParkSize>().apply { addAll(initialFilter.sizes) }
    private val _types = mutableStateListOf<ParkType>().apply { addAll(initialFilter.types) }
    private val _initialSizes = initialFilter.sizes.toSet()
    private val _initialTypes = initialFilter.types.toSet()
    private val _defaultSizes = ParkSize.entries.toSet()
    private val _defaultTypes = ParkType.entries.toSet()

    val isEdited: Boolean
        get() = _sizes.toSet() != _defaultSizes || _types.toSet() != _defaultTypes

    val canApply: Boolean
        get() = _sizes.toSet() != _initialSizes || _types.toSet() != _initialTypes

    fun isSizeSelected(size: ParkSize): Boolean = _sizes.contains(size)

    fun isTypeSelected(type: ParkType): Boolean = _types.contains(type)

    fun toggleSize(size: ParkSize) {
        if (_sizes.contains(size) && _sizes.size > 1) {
            _sizes.remove(size)
        } else if (!_sizes.contains(size)) {
            _sizes.add(size)
        }
    }

    fun toggleType(type: ParkType) {
        if (_types.contains(type) && _types.size > 1) {
            _types.remove(type)
        } else if (!_types.contains(type)) {
            _types.add(type)
        }
    }

    fun reset() {
        _sizes.clear()
        _sizes.addAll(_defaultSizes)
        _types.clear()
        _types.addAll(_defaultTypes)
    }

    fun toParkFilter(): ParkFilter = ParkFilter(
        sizes = _sizes.toSet(),
        types = _types.toSet()
    )
}

@Composable
private fun rememberParksFilterDialogState(filter: ParkFilter): ParksFilterDialogState {
    return remember(filter) { ParksFilterDialogState(filter) }
}

@Composable
private fun DialogTitle(onDismiss: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.filter_parks),
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close)
            )
        }
    }
}

@Composable
private fun FilterSection(
    @StringRes titleID: Int,
    items: List<FilterItem>
) {
    SectionView(titleID = titleID, addPaddingToTitle = false) {
        FormCardContainer(params = FormCardContainerParams()) {
            Column {
                items.forEach { item ->
                    CheckmarkRowView(
                        text = item.text,
                        isChecked = item.isChecked,
                        onCheckedChange = { item.onToggle() }
                    )
                    if (item != items.last()) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ParksFilterDialogPreviewDefault() {
    JetpackWorkoutAppTheme {
        ParksFilterDialog(
            filter = ParkFilter(),
            onFilterChange = {},
            onApply = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ParksFilterDialogPreviewDark() {
    JetpackWorkoutAppTheme {
        ParksFilterDialog(
            filter = ParkFilter(),
            onFilterChange = {},
            onApply = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ParksFilterDialogPreviewCustomFilter() {
    JetpackWorkoutAppTheme {
        ParksFilterDialog(
            filter = ParkFilter(
                sizes = setOf(ParkSize.SMALL, ParkSize.LARGE),
                types = setOf(ParkType.SOVIET, ParkType.MODERN)
            ),
            onFilterChange = {},
            onApply = {},
            onDismiss = {}
        )
    }
}

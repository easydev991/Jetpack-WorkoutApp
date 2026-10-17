package com.swparks.ui.screens.themeicon

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.domain.model.AppIcon
import com.swparks.domain.model.AppTheme
import com.swparks.ui.ds.SWRadioButton
import com.swparks.ui.theme.DynamicColors
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.IThemeIconViewModel

/** Параметры для ThemeIconScreenContent. */
@Suppress("DataClassContainsFunctions")
data class ThemeIconScreenParams(
    val theme: AppTheme = AppTheme.SYSTEM,
    val useDynamicColors: Boolean = true,
    val icon: AppIcon = AppIcon.DEFAULT,
    val onThemeChange: (AppTheme) -> Unit = {},
    val onDynamicColorsChange: (Boolean) -> Unit = {},
    val onIconChange: (AppIcon) -> Unit = {},
    val onBackClick: () -> Unit = {},
    val parentPaddingValues: PaddingValues = PaddingValues(0.dp, 0.dp)
)

/** Экран для выбора темы и иконки приложения. */
@Composable
fun ThemeIconScreen(
    viewModel: IThemeIconViewModel,
    onBackClick: () -> Unit,
    parentPaddingValues: PaddingValues = PaddingValues(0.dp, 0.dp)
) {
    val uiState by viewModel.uiState.collectAsState()

    ThemeIconScreenContent(
        params =
            ThemeIconScreenParams(
                theme = uiState.theme,
                useDynamicColors = uiState.useDynamicColors,
                icon = uiState.icon,
                onThemeChange = { viewModel.updateTheme(it) },
                onDynamicColorsChange = { viewModel.updateDynamicColors(it) },
                onIconChange = { viewModel.updateIcon(it) },
                onBackClick = onBackClick,
                parentPaddingValues = parentPaddingValues
            )
    )
}

/**
 * Контент экрана для выбора темы и иконки приложения.
 *
 * Эта функция используется для UI-тестов в изоляции без ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeIconScreenContent(params: ThemeIconScreenParams) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appearance)) },
                navigationIcon = {
                    IconButton(onClick = params.onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(params.parentPaddingValues)
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThemeSection(
                theme = params.theme,
                onThemeChange = params.onThemeChange
            )

            HorizontalDivider()

            DynamicColorsSection(
                useDynamicColors = params.useDynamicColors,
                onDynamicColorsChange = params.onDynamicColorsChange
            )

            HorizontalDivider()

            IconSection(
                icon = params.icon,
                onIconChange = params.onIconChange
            )
        }
    }
}

/** Секция выбора темы приложения. */
@Composable
private fun ThemeSection(
    theme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    Text(
        text = stringResource(R.string.app_theme),
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth()
    )

    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
    ) {
        // Светлая тема
        SWRadioButton(
            text = stringResource(R.string.light),
            selected = theme == AppTheme.LIGHT,
            onClick = { onThemeChange(AppTheme.LIGHT) }
        )

        // Тёмная тема
        SWRadioButton(
            text = stringResource(R.string.dark),
            selected = theme == AppTheme.DARK,
            onClick = { onThemeChange(AppTheme.DARK) }
        )

        // Системная тема
        SWRadioButton(
            text = stringResource(R.string.system),
            selected = theme == AppTheme.SYSTEM,
            onClick = { onThemeChange(AppTheme.SYSTEM) }
        )
    }
}

/** Секция выбора динамических цветов. */
@Composable
private fun DynamicColorsSection(
    useDynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit
) {
    // Секция отображается только если доступны динамические цвета (Android 12+)
    val dynamicColorAvailable = DynamicColors.isDynamicColorAvailable()

    if (dynamicColorAvailable) {
        Text(
            text = stringResource(R.string.dynamic_colors),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.use_dynamic_colors),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = useDynamicColors,
                    onCheckedChange = onDynamicColorsChange
                )
            }
            Text(
                text = stringResource(R.string.dynamic_colors_hint),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Секция выбора иконки приложения. */
@Composable
private fun IconSection(
    icon: AppIcon,
    onIconChange: (AppIcon) -> Unit
) {
    Text(
        text = stringResource(R.string.app_icon),
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
    )

    IconGrid(
        icons = AppIcon.entries,
        selectedIcon = icon,
        onIconClick = onIconChange
    )
}

/**
 * Сетка иконок приложения с адаптивным количеством колонок: доступная ширина
 * делится на минимальные ячейки «превью иконки + отступ», а недостающая до
 * полного ряда ширина распределяется по ячейкам — превью остаётся своим
 * размером по центру ячейки. При повороте экрана или на широких экранах
 * сетка автоматически перестраивается.
 */
@Composable
private fun IconGrid(
    icons: List<AppIcon>,
    selectedIcon: AppIcon,
    onIconClick: (AppIcon) -> Unit
) {
    val spacing = dimensionResource(R.dimen.spacing_xsmall)
    val previewSize = dimensionResource(R.dimen.icon_size_app_preview)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val (columns, cellSize) =
            iconGridCells(
                maxWidth = maxWidth,
                spacing = spacing,
                minCellSize = previewSize,
                density = LocalDensity.current.density
            )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            icons.forEach { appIcon ->
                Box(
                    modifier = Modifier.size(cellSize),
                    contentAlignment = Alignment.Center
                ) {
                    IconPreviewItem(
                        appIcon = appIcon,
                        isSelected = appIcon == selectedIcon,
                        onClick = { onIconClick(appIcon) }
                    )
                }
            }
        }
    }
}

/** Результат расчёта сетки иконок. */
internal data class IconGridCells(
    val columns: Int,
    val cellSize: Dp
)

/**
 * Расчёт сетки иконок от доступной ширины: колонок столько, сколько минимальных
 * ячеек «[minCellSize] + [spacing]» помещается в [maxWidth], а недостающая до
 * полного ряда ширина распределяется по ячейкам (максимум +spacing на ячейку).
 *
 * Усечение размера ячейки до целого пикселя обязательно: `Modifier.size`
 * измеряет ячейку roundToPx, и округление каждой ячейки вверх переполняет ряд
 * на 1–2px — FlowRow переносит последнюю ячейку ряда, справа остаётся пустой
 * «столбец под ещё одну иконку».
 */
internal fun iconGridCells(
    maxWidth: Dp,
    spacing: Dp,
    minCellSize: Dp,
    density: Float
): IconGridCells {
    // Минимум minCellSize на ячейку + spacing между ними; Dp/Dp даёт Float,
    // toInt усекает вниз
    val columns = ((maxWidth + spacing) / (minCellSize + spacing)).toInt().coerceAtLeast(1)
    val cellSize = (maxWidth - spacing * (columns - 1)) / columns
    val cellSizePx = (cellSize.value * density).toInt()
    return IconGridCells(columns, Dp(cellSizePx / density))
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
private fun ThemeIconScreenPreview() {
    JetpackWorkoutAppTheme {
        ThemeIconScreenContent(params = ThemeIconScreenParams(icon = AppIcon.DEFAULT))
    }
}

// region Превью секции иконок крупным планом

/** Телефон (360dp): 4 колонки вплотную; планшет (1280dp): колонок больше, чем иконок. */
@Preview(name = "Секция иконок, телефон (360dp)", showBackground = true, widthDp = 360, locale = "ru")
@Preview(name = "Секция иконок, планшет (1280dp)", showBackground = true, widthDp = 1280, locale = "ru")
@Composable
private fun IconGridWidthsPreview() {
    JetpackWorkoutAppTheme {
        // Column, а не Box: IconSection отдаёт заголовок и сетку как соседние
        // элементы — в Box они накладывались бы друг на друга
        Column(modifier = Modifier.padding(16.dp)) {
            IconSection(icon = AppIcon.DEFAULT, onIconChange = {})
        }
    }
}

// endregion

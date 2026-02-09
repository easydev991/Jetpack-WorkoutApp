package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Оверлей с индикатором загрузки для самостоятельного использования.
 * Занимает всё доступное пространство и блокирует все жесты.
 *
 * @param modifier Модификатор
 */
@Composable
fun LoadingOverlayView(modifier: Modifier = Modifier) {
    val loadingText = stringResource(R.string.loading_content_description)
    Box(
        modifier = modifier
            .fillMaxSize()
            .blockInput(loadingText),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

/**
 * Оверлей с индикатором загрузки для использования внутри Box.
 * Принимает размер родительского Box и блокирует все жесты.
 *
 * @param modifier Модификатор
 */
@Composable
fun BoxScope.LoadingOverlayView(modifier: Modifier = Modifier) {
    val loadingText = stringResource(R.string.loading_content_description)
    Box(
        modifier = modifier
            .matchParentSize()
            .blockInput(loadingText),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator()
    }
}

/**
 * Приватный компонент индикатора загрузки
 */
@Composable
private fun LoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(50.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Модификатор для блокировки всех жестов с поддержкой доступности
 */
private fun Modifier.blockInput(contentDescription: String): Modifier = this
    .semantics {
        this.contentDescription = contentDescription
    }
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
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
fun LoadingOverlayViewPreview() {
    JetpackWorkoutAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                LoadingOverlayView()
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
fun LoadingOverlayViewBoxScopePreview() {
    JetpackWorkoutAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingOverlayView()
            }
        }
    }
}

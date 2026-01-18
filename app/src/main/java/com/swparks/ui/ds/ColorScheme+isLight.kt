package com.swparks.ui.ds

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

/**
 * Проверяет, включена ли светлая тема.
 * Спасибо [интернету](https://stackoverflow.com/a/71594753/11830041)
 */
@Composable
fun ColorScheme.isLight() = this.background.luminance() > 0.5
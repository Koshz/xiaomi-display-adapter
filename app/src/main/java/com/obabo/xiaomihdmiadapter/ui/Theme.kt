package com.obabo.xiaomihdmiadapter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3DDC84),
    secondary = Color(0xFF8DA2FB),
    tertiary = Color(0xFFFFC857),
    background = Color(0xFF111318),
    surface = Color(0xFF171B23),
    error = Color(0xFFE85D75)
)

@Composable
fun HdmiAdapterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}

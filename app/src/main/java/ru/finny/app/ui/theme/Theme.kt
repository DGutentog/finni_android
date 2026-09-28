package ru.finny.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FinnyColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = PurpleLight,
    onPrimaryContainer = PurpleDark,
    secondary = Orange,
    onSecondary = Color.White,
    secondaryContainer = OrangeLight,
    onSecondaryContainer = OrangeDark,
    background = Bg,
    onBackground = TextMain,
    surface = CardBg,
    onSurface = TextMain,
    surfaceVariant = Bg,
    onSurfaceVariant = Muted,
    outline = Line,
)

@Composable
fun FinnyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FinnyColors,
        typography = FinnyTypography,
        content = content,
    )
}

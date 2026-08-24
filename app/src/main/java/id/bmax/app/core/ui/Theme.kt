package id.bmax.app.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GlassBlue = Color(0xFF42A5F5)
private val GlassCyan = Color(0xFF26C6DA)

private val Light = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    secondary = Color(0xFF00838F),
    background = Color(0xFFEAF4FF),
    surface = Color(0xFFF6FAFF),
    surfaceVariant = Color(0xFFDCEBFA),
)

private val Dark = darkColorScheme(
    primary = GlassBlue,
    onPrimary = Color(0xFF001A2D),
    secondary = GlassCyan,
    background = Color(0xFF06111C),
    surface = Color(0xFF0D1D2B),
    surfaceVariant = Color(0xFF183247),
)

@Composable
fun BmaxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = Typography(),
        content = content,
    )
}

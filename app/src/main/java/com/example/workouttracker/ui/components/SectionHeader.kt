package com.example.workouttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Глобальный стиль шапок. ВАЖНО: никаких обращений к MaterialTheme здесь по умолчанию!
 */
data class SectionHeaderStyle(
    val height: Dp = 48.dp,
    val horizontalPadding: Dp = 12.dp,
    val tonalElevation: Dp = 2.dp,
    val shadowElevation: Dp = 4.dp,
    val showDivider: Boolean = true,
    val containerColor: Color = Color.Unspecified,  // ← НЕ MaterialTheme по умолчанию
    val contentPadding: PaddingValues = PaddingValues(1.dp),
    val useStatusBarInsets: Boolean = true
)

private val LocalSectionHeaderStyle = staticCompositionLocalOf { SectionHeaderStyle() }

@Composable
fun ProvideSectionHeaderStyle(
    style: SectionHeaderStyle,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalSectionHeaderStyle provides style, content = content)
}

/**
 * Унифицированная шапка раздела.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    height: Dp = LocalSectionHeaderStyle.current.height,
    horizontalPadding: Dp = LocalSectionHeaderStyle.current.horizontalPadding,
    tonalElevation: Dp = LocalSectionHeaderStyle.current.tonalElevation,
    shadowElevation: Dp = LocalSectionHeaderStyle.current.shadowElevation,
    containerColor: Color = LocalSectionHeaderStyle.current.containerColor,
    showDivider: Boolean = LocalSectionHeaderStyle.current.showDivider,
    backgroundBrush: Brush? = null,

    // 👇 Добавлены новые параметры:
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    subtitleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val resolvedContainerColor =
        if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.surface else containerColor

    Surface(
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        color = if (backgroundBrush == null) resolvedContainerColor else Color.Transparent
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .let { base ->
                    if (LocalSectionHeaderStyle.current.useStatusBarInsets)
                        base.windowInsetsPadding(WindowInsets.statusBars)
                    else base
                }
                .padding(LocalSectionHeaderStyle.current.contentPadding)
                .then(
                    if (backgroundBrush != null) Modifier.background(backgroundBrush)
                    else Modifier.background(Color.Transparent)
                )
                .padding(horizontal = horizontalPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = true)
                ) {
                    if (leading != null) Row(content = leading)
                    Column(modifier = Modifier.weight(1f, fill = true)) {
                        // 👇 теперь размер задаётся через параметр
                        Text(title, style = titleStyle, maxLines = 1)
                        if (subtitle != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                subtitle,
                                style = subtitleStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                Row(content = actions)
            }
            if (showDivider) Divider()
        }
    }
}


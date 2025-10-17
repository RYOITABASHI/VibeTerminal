package com.vibeterminal.ui.terminal

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeterminal.core.translator.TranslatedOutput

/**
 * Translation overlay that appears above terminal output
 * Shows translated text in Japanese with emoji and suggestions
 */
@Composable
fun TranslationOverlay(
    translation: TranslatedOutput,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = getCategoryColor(translation.category)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with emoji, category, and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                translation.emoji?.let { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }

                Text(
                    text = "日本語説明",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Source badge
                SourceBadge(source = translation.source)

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Translated text
            Text(
                text = translation.translatedText,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            // Suggestion (if available)
            translation.suggestion?.let { suggestion ->
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Suggestion",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )

                    Column {
                        Text(
                            text = "💡 解決方法",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Confidence indicator (for debugging)
            if (translation.confidence < 0.7f) {
                Text(
                    text = "⚠️ 翻訳の信頼度が低い場合があります",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SourceBadge(source: com.vibeterminal.core.translator.TranslationSource) {
    val (text, color) = when (source) {
        com.vibeterminal.core.translator.TranslationSource.LOCAL_PATTERN ->
            "ローカル" to Color(0xFF4CAF50)
        com.vibeterminal.core.translator.TranslationSource.LLM_API ->
            "AI" to Color(0xFF2196F3)
        com.vibeterminal.core.translator.TranslationSource.CACHE ->
            "キャッシュ" to Color(0xFF9E9E9E)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "error" -> Color(0xFFFFEBEE)     // Light red
        "warning" -> Color(0xFFFFF3E0)   // Light orange
        "success" -> Color(0xFFE8F5E9)   // Light green
        "info" -> Color(0xFFE3F2FD)      // Light blue
        "progress" -> Color(0xFFFCE4EC)  // Light pink
        else -> Color(0xFFF5F5F5)        // Light gray
    }
}

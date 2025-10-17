package com.vibeterminal.ui.aicli

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Gesture actions for AI CLI interaction
 */
enum class GestureAction {
    SWIPE_RIGHT,    // Yes を送信
    SWIPE_LEFT,     // No を送信
    SWIPE_UP,       // 前回のプロンプトをコピー
    LONG_PRESS,     // 翻訳ダイアログ表示
    DOUBLE_TAP      // 実行中断 (Ctrl+C)
}

/**
 * Gesture handler for terminal
 */
class TerminalGestureHandler(
    private val onSendInput: (String) -> Unit,
    private val onInterrupt: () -> Unit,
    private val onCopyLastPrompt: () -> Unit,
    private val onShowTranslation: () -> Unit
) {
    fun handleGesture(action: GestureAction) {
        when (action) {
            GestureAction.SWIPE_RIGHT -> onSendInput("yes\n")
            GestureAction.SWIPE_LEFT -> onSendInput("no\n")
            GestureAction.SWIPE_UP -> onCopyLastPrompt()
            GestureAction.LONG_PRESS -> onShowTranslation()
            GestureAction.DOUBLE_TAP -> onInterrupt()
        }
    }
}

@Composable
fun GestureOverlay(
    onGesture: (GestureAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showHints by remember { mutableStateOf(false) }
    var gestureIndicator by remember { mutableStateOf<GestureAction?>(null) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                var totalDrag = androidx.compose.ui.geometry.Offset.Zero

                detectDragGestures(
                    onDragStart = {
                        totalDrag = androidx.compose.ui.geometry.Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        val threshold = 100f

                        when {
                            totalDrag.x > threshold && abs(totalDrag.y) < threshold -> {
                                gestureIndicator = GestureAction.SWIPE_RIGHT
                                onGesture(GestureAction.SWIPE_RIGHT)
                            }
                            totalDrag.x < -threshold && abs(totalDrag.y) < threshold -> {
                                gestureIndicator = GestureAction.SWIPE_LEFT
                                onGesture(GestureAction.SWIPE_LEFT)
                            }
                            totalDrag.y < -threshold && abs(totalDrag.x) < threshold -> {
                                gestureIndicator = GestureAction.SWIPE_UP
                                onGesture(GestureAction.SWIPE_UP)
                            }
                        }
                        gestureIndicator = null
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onGesture(GestureAction.LONG_PRESS)
                    },
                    onDoubleTap = {
                        onGesture(GestureAction.DOUBLE_TAP)
                    }
                )
            }
    ) {
        // ジェスチャーフィードバック表示
        gestureIndicator?.let { gesture ->
            GestureFeedback(gesture, Modifier.align(Alignment.Center))
        }

        // ヘルプボタン
        FloatingActionButton(
            onClick = { showHints = !showHints },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                if (showHints) Icons.Default.Close else Icons.Default.Help,
                contentDescription = "ジェスチャーヘルプ"
            )
        }

        // ジェスチャーヒント表示
        if (showHints) {
            GestureHints(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun GestureFeedback(
    gesture: GestureAction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                when (gesture) {
                    GestureAction.SWIPE_RIGHT -> Icons.Default.ArrowForward
                    GestureAction.SWIPE_LEFT -> Icons.Default.ArrowBack
                    GestureAction.SWIPE_UP -> Icons.Default.ArrowUpward
                    GestureAction.LONG_PRESS -> Icons.Default.Translate
                    GestureAction.DOUBLE_TAP -> Icons.Default.Stop
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Text(
                when (gesture) {
                    GestureAction.SWIPE_RIGHT -> "\"yes\" を送信"
                    GestureAction.SWIPE_LEFT -> "\"no\" を送信"
                    GestureAction.SWIPE_UP -> "コピー"
                    GestureAction.LONG_PRESS -> "翻訳表示"
                    GestureAction.DOUBLE_TAP -> "中断"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun GestureHints(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "ジェスチャー操作",
                style = MaterialTheme.typography.titleLarge
            )

            Divider()

            GestureHintItem(
                icon = Icons.Default.ArrowForward,
                gesture = "右スワイプ →",
                action = "\"yes\" を自動入力"
            )

            GestureHintItem(
                icon = Icons.Default.ArrowBack,
                gesture = "左スワイプ ←",
                action = "\"no\" を自動入力"
            )

            GestureHintItem(
                icon = Icons.Default.ArrowUpward,
                gesture = "上スワイプ ↑",
                action = "前回のプロンプトをコピー"
            )

            GestureHintItem(
                icon = Icons.Default.TouchApp,
                gesture = "長押し",
                action = "AI出力を翻訳表示"
            )

            GestureHintItem(
                icon = Icons.Default.TouchApp,
                gesture = "ダブルタップ",
                action = "実行中断 (Ctrl+C)"
            )
        }
    }
}

@Composable
fun GestureHintItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gesture: String,
    action: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column {
            Text(
                gesture,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                action,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

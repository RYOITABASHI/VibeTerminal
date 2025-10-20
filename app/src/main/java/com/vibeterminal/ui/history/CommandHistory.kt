package com.vibeterminal.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Command history panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandHistoryPanel(
    commands: List<CommandHistoryItem>,
    onCommandClick: (String) -> Unit,
    onCommandDelete: (CommandHistoryItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(300.dp),
        color = Color(0xFF1E1E1E),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF4EC9B0)
                    )
                    Text(
                        "コマンド履歴",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "閉じる",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF808080)
                    )
                }
            }

            Divider(color = Color(0xFF3D3D3D))

            // Command list
            if (commands.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF505050)
                        )
                        Text(
                            "履歴なし",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF808080)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(commands) { item ->
                        CommandHistoryItemView(
                            item = item,
                            onClick = { onCommandClick(item.command) },
                            onDelete = { onCommandDelete(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandHistoryItemView(
    item: CommandHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF2D2D2D)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    item.command,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFFCCCCCC),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatTime(item.timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = Color(0xFF707070)
                )
                if (item.favorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "お気に入り",
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFFFFC107)
                    )
                }
            }

            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF808080)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("削除確認") },
            text = { Text("このコマンドを履歴から削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "たった今"
        diff < 3600000 -> "${diff / 60000}分前"
        diff < 86400000 -> "${diff / 3600000}時間前"
        else -> SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN).format(Date(timestamp))
    }
}

/**
 * Command history item data class
 */
data class CommandHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val favorite: Boolean = false
)

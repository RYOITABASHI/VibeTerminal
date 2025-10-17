package com.vibeterminal.ui.mcp

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * MCP Server Management Panel
 * Shows all available MCP servers and their connection status
 */
@Composable
fun MCPServerPanel(
    viewModel: MCPViewModel,
    modifier: Modifier = Modifier
) {
    val servers by viewModel.servers.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Header
            MCPPanelHeader(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                connectedCount = servers.count { it.status == MCPConnectionStatus.CONNECTED },
                totalCount = servers.size
            )

            // Server list (collapsible)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    servers.forEach { server ->
                        MCPServerCard(
                            server = server,
                            onStart = { viewModel.startServer(server.id) },
                            onStop = { viewModel.stopServer(server.id) },
                            onRestart = { viewModel.restartServer(server.id) },
                            onTest = { viewModel.testConnection(server.id) },
                            onClick = { viewModel.selectServer(server) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MCPPanelHeader(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    connectedCount: Int,
    totalCount: Int
) {
    Surface(
        onClick = { onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Cable,
                contentDescription = null,
                tint = if (connectedCount > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "MCPサーバー",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "$connectedCount / $totalCount 接続中",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "閉じる" else "開く"
            )
        }
    }
}

@Composable
private fun MCPServerCard(
    server: MCPServer,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onTest: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status indicator
                ConnectionStatusIndicator(server.status)

                // Server icon and info
                Icon(
                    server.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        server.displayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        server.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action button
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        when (server.status) {
                            MCPConnectionStatus.CONNECTED -> {
                                DropdownMenuItem(
                                    text = { Text("切断") },
                                    onClick = {
                                        showMenu = false
                                        onStop()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("再起動") },
                                    onClick = {
                                        showMenu = false
                                        onRestart()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("接続テスト") },
                                    onClick = {
                                        showMenu = false
                                        onTest()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    }
                                )
                            }
                            MCPConnectionStatus.DISCONNECTED, MCPConnectionStatus.ERROR -> {
                                DropdownMenuItem(
                                    text = { Text("起動") },
                                    onClick = {
                                        showMenu = false
                                        onStart()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    }
                                )
                            }
                            MCPConnectionStatus.CONNECTING -> {
                                DropdownMenuItem(
                                    text = { Text("キャンセル") },
                                    onClick = {
                                        showMenu = false
                                        onStop()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Status details
            AnimatedVisibility(visible = server.status != MCPConnectionStatus.DISCONNECTED) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Divider()
                    Spacer(Modifier.height(8.dp))

                    when (server.status) {
                        MCPConnectionStatus.CONNECTED -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Text(
                                    "接続中",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                                server.lastConnected?.let { timestamp ->
                                    Text(
                                        "• ${formatTimestamp(timestamp)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                "${server.tools.size} tools available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        MCPConnectionStatus.CONNECTING -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    "接続中...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        MCPConnectionStatus.ERROR -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    server.errorMessage ?: "接続エラー",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusIndicator(status: MCPConnectionStatus) {
    Surface(
        shape = CircleShape,
        color = when (status) {
            MCPConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
            MCPConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.outline
            MCPConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.primary
            MCPConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
        },
        modifier = Modifier.size(12.dp)
    ) {}
}

/**
 * MCP Connection Logs Viewer
 */
@Composable
fun MCPLogsViewer(
    viewModel: MCPViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.connectionLogs.collectAsState()

    Card(modifier = modifier) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "接続ログ",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { viewModel.clearLogs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "ログをクリア")
                }
            }

            Divider()

            // Logs list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    MCPLogItem(log)
                }

                if (logs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ログがありません",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MCPLogItem(log: MCPLogEntry) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            formatTime(log.timestamp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Level indicator
        Icon(
            when (log.level) {
                MCPLogLevel.INFO -> Icons.Default.Info
                MCPLogLevel.SUCCESS -> Icons.Default.CheckCircle
                MCPLogLevel.ERROR -> Icons.Default.Error
                MCPLogLevel.WARNING -> Icons.Default.Warning
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = when (log.level) {
                MCPLogLevel.INFO -> MaterialTheme.colorScheme.primary
                MCPLogLevel.SUCCESS -> Color(0xFF4CAF50)
                MCPLogLevel.ERROR -> MaterialTheme.colorScheme.error
                MCPLogLevel.WARNING -> Color(0xFFFFA726)
            }
        )

        // Message
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "[${log.serverName}]",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                log.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// Helper functions

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "たった今"
        diff < 3600_000 -> "${diff / 60_000}分前"
        diff < 86400_000 -> "${diff / 3600_000}時間前"
        else -> SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN).format(Date(timestamp))
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.JAPAN).format(Date(timestamp))
}
